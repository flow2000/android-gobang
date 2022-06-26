package top.panghai.fifthchess.view;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ejlchina.data.Mapper;
import com.ejlchina.okhttps.HttpResult;
import com.ejlchina.okhttps.OkHttps;
import com.ejlchina.okhttps.WebSocket;

import java.io.Serializable;
import java.util.List;

import top.panghai.fifthchess.MainActivity;
import top.panghai.fifthchess.MatchDialog;
import top.panghai.fifthchess.R;
import top.panghai.fifthchess.entity.Message;
import top.panghai.fifthchess.entity.User;
import top.panghai.fifthchess.game.BaseComputerAi;
import top.panghai.fifthchess.game.ChessBoard;
import top.panghai.fifthchess.game.HumanPlayer;
import top.panghai.fifthchess.game.IChessboard;
import top.panghai.fifthchess.game.IPlayer;
import top.panghai.fifthchess.game.Point;
import top.panghai.fifthchess.utils.HttpStatus;
import top.panghai.fifthchess.utils.LocalCacheUtils;

public class HumanChessView extends View {

    private int mPanelWidth;
    private static int MAX_LINE = 15;
    private float mLineHeight;

    private Paint paint = new Paint();

    //棋子图片
    private Bitmap whiteChess;
    private Bitmap blackChess;
    private Bitmap lastWhiteChess;
    private Bitmap lastBlackChess;

    private float pieceLineHeight = 0.8f;

    private boolean isBlack = true; //玩家是否为黑棋
    private boolean isWin = false;//是否已获胜
    private boolean isChess = false;//是否执棋
    private boolean isRivalOnline = false; //对方是否在线

    private IPlayer humanPlayer; //玩家
    private IPlayer rivalPlayer; //对手
    private Context context;
    private User user, rivalUser;
    private WebSocket webSocket;
    private IChessboard chessboard = new ChessBoard(MAX_LINE);//棋盘
    private Handler handler;

    //音效
    private SoundPool soundWin;
    private SoundPool soundDefeat;
    private SoundPool soundChess;

    private static final String USER = "user";
    private static final String RIVAL_USER = "rivalUser";
    private static final int TEXT = 1;
    private static final int PLAY = 0;
    private static final int closeCode = 3000;
    private int resetNum = 0;
    private int playRes = 0;
    private static final String remoteAvatarUrl = "http://119.91.232.147:8095/";
//    private static final String remoteAvatarUrl = "https://api.multiavatar.com/";

    public HumanChessView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setKeepScreenOn(true);//设置屏幕常亮
        Resources resources = getResources();
        this.context = context;
        blackChess = BitmapFactory.decodeResource(resources, R.drawable.stone_b1);
        lastBlackChess = BitmapFactory.decodeResource(resources, R.drawable.last_stone_b1);
        whiteChess = BitmapFactory.decodeResource(resources, R.drawable.stone_w2);
        lastWhiteChess = BitmapFactory.decodeResource(resources, R.drawable.last_stone_w2);
        initPaint();
        initGame();
        readUser();
        play();
    }

    private void readUser() {
        user = LocalCacheUtils.readUser(context, USER);
        rivalUser = LocalCacheUtils.readUser(context, RIVAL_USER);
        if (user == null || rivalUser == null) {
            showShortMsg("系统错误");
            Intent intent = new Intent(context, MainActivity.class);
            context.startActivity(intent);
        }
    }

    private void play() {
        Handler successStep = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull android.os.Message message) {
                Point point = (Point) message.obj;
                isChess = true;
                if (!isWin) {
                    chessboard.getFreePoints().remove(point);
                    point.setLast(true);
                    rivalPlayer.run(humanPlayer.getMyPoints(), point);
                    soundChess.play(1, 1, 1, 0, 0, 1);
                    invalidate();
                    checkWin();
                    for (Point p : rivalPlayer.getMyPoints()) {
                        p.setLast(false);
                    }
                }
                return false;
            }
        });
        webSocket = OkHttps.webSocket("/play?userId=" + user.getUserId())
                // 遇到网络错误
                .setOnException(new WebSocket.Listener<Throwable>() {
                    @Override
                    public void on(WebSocket ws, Throwable data) {
                        Looper.prepare();
                        Log.e("error", "========网络错误========");
                        showShortMsg("网络错误");
                        Looper.loop();
                    }
                })
                // 收到消息
                .setOnMessage(new WebSocket.Listener<WebSocket.Message>() {
                    @Override
                    public void on(WebSocket ws, WebSocket.Message message) {
                        Log.i("tag", message.toString());
                        Mapper mapper = null;
                        try {
                            mapper = message.toMapper();
                            String code = mapper.getString(HttpStatus.CODE);
                            String type = mapper.getString(HttpStatus.TYPE);
                            String info = mapper.getString(HttpStatus.INFO);
                            if (HttpStatus.RIVAL_GIVE_UP.equals(info)) {
                                Log.i("tag", "========对方已认输========");
                                sendWin(1);
                                soundWin.play(1, 1, 1, 0, 0, 1);
                                Looper.prepare();
                                alert("对方已认输");
                                Looper.loop();
                            } else {
                                // 收到测试连接消息
                                if (String.valueOf(TEXT).equals(type)) {
                                    Log.i("tag", "收到测试连接消息");
                                    isRivalOnline = true;
                                    return;
                                }
                                // 收到棋子消息
                                if (String.valueOf(PLAY).equals(type)) {
                                    Point point = JSON.parseObject(info, Point.class);
                                    if (point == null) {
                                        Log.e("error", "========棋子传输错误========");
                                        throw new Exception();
                                    }
                                    android.os.Message m = android.os.Message.obtain();
                                    m.obj = point;
                                    successStep.sendMessage(m);

                                    android.os.Message message1 = android.os.Message.obtain();
                                    message1.obj = true;
                                    handler.sendMessage(message1);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            isRivalOnline = false;
                        }
                    }
                })
                .listen();

        // 发送测试连接验证对手是否已连接
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (resetNum = 0; resetNum < 10; resetNum++) {
                        if (isRivalOnline) {
                            Looper.prepare();
                            showShortMsg("对局开始");
                            Looper.loop();
                            resetNum = 3;
                            break;
                        }
                        Message message = new Message();
                        message.setFrom(user.getUserId());
                        message.setTo(rivalUser.getUserId());
                        message.setType(TEXT);
                        message.setInfo("");
                        Log.i("发送消息", message.toString());
                        webSocket.send(message);
                        Thread.sleep(500);
                        isRivalOnline = true;
                    }
                } catch (InterruptedException e) {
                    Log.e("error", "========发送测试连接失败========");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void setChess(boolean isChess) {
        this.isChess = isChess;
        this.isBlack = isChess;
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void closeWebSocket() {
        if (webSocket != null) {
            Message message = new Message();
            message.setFrom(user.getUserId());
            message.setTo(rivalUser.getUserId());
            message.setInfo(HttpStatus.RIVAL_GIVE_UP);
            Log.i("发送消息", message.toString());
            webSocket.send(message);
            sendWin(-1);
            webSocket.close(closeCode, "认输");
        }
    }

    private void initGame() {
        humanPlayer = new HumanPlayer();
        humanPlayer.setChessboard(chessboard);
        rivalPlayer = new HumanPlayer();
        rivalPlayer.setChessboard(chessboard);

        humanPlayer.clear();
        rivalPlayer.clear();

        soundWin = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundDefeat = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundChess = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        soundWin.load(getContext(), R.raw.win, 1);
        soundDefeat.load(getContext(), R.raw.defeat, 1);
        soundChess.load(getContext(), R.raw.chess, 1);

    }

    private void initPaint() {
        paint.setColor(0x88000000);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
    }

    /**
     * 测量棋盘
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthModel = MeasureSpec.getMode(widthMeasureSpec);

        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightModel = MeasureSpec.getMode(heightMeasureSpec);

        int size = 0;
        if (widthModel == MeasureSpec.UNSPECIFIED) {
            size = heightSize;
        } else if (heightModel == MeasureSpec.UNSPECIFIED) {
            size = widthSize;
        } else {
            size = Math.min(widthSize, heightSize);
        }
        setMeasuredDimension(size, size);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mPanelWidth = w;
        mLineHeight = mPanelWidth / MAX_LINE;
        int pieceWidth = (int) (mLineHeight * pieceLineHeight);
        whiteChess = Bitmap.createScaledBitmap(whiteChess, pieceWidth, pieceWidth, false);
        blackChess = Bitmap.createScaledBitmap(blackChess, pieceWidth, pieceWidth, false);
        lastWhiteChess = Bitmap.createScaledBitmap(lastWhiteChess, pieceWidth, pieceWidth, false);
        lastBlackChess = Bitmap.createScaledBitmap(lastBlackChess, pieceWidth, pieceWidth, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawChessBoard(canvas);
        drawPieces(canvas);
    }

    /**
     * 绘制棋子，并将对手最后一步标记处理
     *
     * @param canvas
     */
    private void drawPieces(Canvas canvas) {
        for (Point point : humanPlayer.getMyPoints()) {
            canvas.drawBitmap(
                    isBlack ? point.isLast() ? lastBlackChess : blackChess : point.isLast() ? lastWhiteChess : whiteChess,
                    (point.x + (1 - pieceLineHeight) / 2) * mLineHeight,
                    (point.y + (1 - pieceLineHeight) / 2) * mLineHeight,
                    null);
        }
        for (Point point : rivalPlayer.getMyPoints()) {
            canvas.drawBitmap(
                    !isBlack ? point.isLast() ? lastBlackChess : blackChess : point.isLast() ? lastWhiteChess : whiteChess,
                    (point.x + (1 - pieceLineHeight) / 2) * mLineHeight,
                    (point.y + (1 - pieceLineHeight) / 2) * mLineHeight,
                    null);
        }
    }

    private void drawChessBoard(Canvas canvas) {
        int w = mPanelWidth;
        float lineHeight = mLineHeight;

        for (int i = 0; i < MAX_LINE; i++) {
            float startX = lineHeight / 2;
            float endX = w - lineHeight / 2;

            float y = (float) ((0.5 + i) * lineHeight);
            canvas.drawLine(startX, y, endX, y, paint);
            canvas.drawLine(y, startX, y, endX, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isRivalOnline) {
            showShortMsg("等待对手加载...");
            return true;
        }
        // 当前为对手回合
        if (!isChess) {
            showShortMsg("当前为对手回合");
            return true;
        }
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {

            int x = (int) (event.getX() / mLineHeight);
            int y = (int) (event.getY() / mLineHeight);

            if (x >= 0 && x < MAX_LINE && y >= 0 && y < MAX_LINE) {
                onPoint(x, y);
            }
            return true;
        } else if (action == MotionEvent.ACTION_DOWN) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void onPoint(int x, int y) {
        if (isWin) {
            return;
        }
        Point point = new Point(x, y);
        if (chessboard.getFreePoints().contains(point)) {
            Message m = new Message();
            m.setFrom(user.getUserId());
            m.setTo(rivalUser.getUserId());
            m.setType(PLAY);
            m.setInfo(JSONObject.toJSONString(point));
            Log.i("发送棋子信息", JSON.toJSONString(m));
            // 发送棋子信息
            webSocket.send(JSON.toJSONString(m));
            android.os.Message message = android.os.Message.obtain();
            message.obj = false;
            handler.sendMessage(message);
            isChess = false;
            // 绘制棋子
            humanPlayer.run(rivalPlayer.getMyPoints(), point);
            invalidate();
            soundChess.play(1, 1, 1, 0, 0, 1);
            chessboard.getFreePoints().remove(point);
            checkWin();
        }
    }

    private void checkWin() {
        if (humanPlayer.hasWin()) {
            isWin = true;
            soundWin.play(1, 1, 1, 0, 0, 1);
            playRes = 1;
            alert("你赢了！");
        }
        if (rivalPlayer.hasWin()) {
            isWin = true;
            soundDefeat.play(1, 1, 1, 0, 0, 1);
            playRes = -1;
            alert("你输了！");
        }
        if (chessboard.getFreePoints().isEmpty()) {
            isWin = true;
            playRes = 0;
            alert("和棋！");
        }
        if (isWin) {
            sendWin(playRes);
        }
    }

    private void sendWin(int playRes) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpResult httpResult = OkHttps.sync("/user/playRes")
                        .addBodyPara("userId", user.getUserId())
                        .addBodyPara("playRes", playRes)
                        .addBodyPara("rivalUserId", rivalUser.getUserId())
                        .bodyType(OkHttps.JSON)
                        .put();
                Mapper mapper = httpResult.getBody().toMapper();
                String code = mapper.getString(HttpStatus.CODE);
                Log.i("tag", mapper.toString());
                if (HttpStatus.SUCCESS.equals(code)) {
                    Log.i("tag", "发送比赛结果成功！");
                }
            }
        }).start();
    }

    private void alert(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("比赛结果");
        builder.setMessage(msg);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                webSocket.close(closeCode, "对局结束");
                dialogInterface.dismiss();
                Intent intent = new Intent(context, MainActivity.class);
                context.startActivity(intent);
                ((Activity) context).finish();
            }
        });
        builder.setNegativeButton("重新匹配", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                isWin = false;
                webSocket.close(closeCode, "重新匹配");
                dialogInterface.dismiss();
                final MatchDialog matchDialog = new MatchDialog(getContext());
                matchDialog.show();
                Window window = matchDialog.getWindow();
                window.setGravity(Gravity.CENTER);
                matchDialog.setCanceledOnTouchOutside(false);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static final String instance = "INSTANCE";
    private static final String win = "ISWIN";
    private static final String black = "ISBLACK";
    private static final String human = "HUMAN";
    private static final String rival = "RIVAL";

    //view的存储与恢复

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        Log.d("ChessView", "保存棋局");
        bundle.putParcelable(instance, super.onSaveInstanceState());
        bundle.putBoolean(win, isWin);
        bundle.putBoolean(black, isBlack);
        bundle.putSerializable(human, (Serializable) humanPlayer.getMyPoints());
        bundle.putSerializable(rival, (Serializable) rivalPlayer.getMyPoints());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Log.d("ChessView", "加载棋局");
            Bundle bundle = (Bundle) state;
            isWin = bundle.getBoolean(win);
            isBlack = bundle.getBoolean(black);
            chessboard = new ChessBoard(MAX_LINE);
            humanPlayer = new HumanPlayer();
            humanPlayer.setChessboard(chessboard);
            rivalPlayer = new BaseComputerAi();
            rivalPlayer.setChessboard(chessboard);
            List<Point> humanPoints = (List<Point>) bundle.getSerializable(human);
            List<Point> aiPoints = (List<Point>) bundle.getSerializable(rival);
            humanPlayer.getMyPoints().addAll(humanPoints);
            rivalPlayer.getMyPoints().addAll(aiPoints);
            for (Point point : humanPoints) {
                chessboard.getFreePoints().remove(point);
            }
            for (Point point : aiPoints) {
                chessboard.getFreePoints().remove(point);
            }
            super.onRestoreInstanceState(bundle.getParcelable(instance));
            return;
        }
        super.onRestoreInstanceState(state);
    }

    /**
     * Toast短提示
     *
     * @param msg 内容
     */
    private void showShortMsg(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
