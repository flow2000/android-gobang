package top.panghai.fifthchess;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.ejlchina.data.Mapper;
import com.ejlchina.okhttps.HttpResult;
import com.ejlchina.okhttps.OkHttps;
import com.ejlchina.okhttps.WebSocket;

import top.panghai.fifthchess.entity.User;
import top.panghai.fifthchess.utils.EloUtil;
import top.panghai.fifthchess.utils.HttpStatus;
import top.panghai.fifthchess.view.HumanChessView;

public class HumanActivity extends AppCompatActivity {

    private HumanChessView view;
    private Context context;
    private static final int PLAYER_CODE = 4399;
    private ImageView avatar, rivalAvatar, rankImage, rivalRankImage, chess, rivalChess;
    private TextView rankName, rivalRankName, nickname, rivalNickname, lastTime, rivalLastTime;
    private User user, rivalUser;
    private Bitmap v1, v2, v3, v4, v5, v6, v7, white, black;
    private static final String remoteAvatarUrl = "http://119.91.232.147:8095/";
    //    private static final String remoteAvatarUrl = "https://api.multiavatar.com/";
    // 标识是否先手
    private boolean isFirst;
    private boolean isChess = false;//是否执棋

    // 显示剩余时间
    Handler visibleHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            boolean isVisible = (boolean) message.obj;
            if (isVisible) {
                lastTime.setVisibility(View.VISIBLE);
                rivalLastTime.setVisibility(View.GONE);
            } else {
                lastTime.setVisibility(View.GONE);
                rivalLastTime.setVisibility(View.VISIBLE);
            }
            return false;
        }
    });

    // 修改剩余时间
    Handler setTimeHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            boolean isVisible = (boolean) message.obj;
            int time = message.what;
            if (isVisible) {
                lastTime.setText(String.valueOf(time));
            } else {
                rivalLastTime.setText(String.valueOf(time));
            }
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_human);
        view = findViewById(R.id.humanChessView);
        context = this;
        // 初始化视图
        initView();
        // 设置执棋者
        view.setChess(isChess);
        // 设置handler通信
        view.setHandler(visibleHandler);

    }

    private void initView() {
        avatar = findViewById(R.id.avatar1);
        rivalAvatar = findViewById(R.id.avatar2);
        rankName = findViewById(R.id.rankName1);
        rivalRankName = findViewById(R.id.rankName2);
        rankImage = findViewById(R.id.rank1);
        rivalRankImage = findViewById(R.id.rank2);
        nickname = findViewById(R.id.nickname1);
        rivalNickname = findViewById(R.id.nickname2);
        lastTime = findViewById(R.id.lastTime1);
        rivalLastTime = findViewById(R.id.lastTime2);
        chess = findViewById(R.id.chess1);
        rivalChess = findViewById(R.id.chess2);

        black = BitmapFactory.decodeResource(getResources(), R.drawable.stone_b1);
        white = BitmapFactory.decodeResource(getResources(), R.drawable.stone_w2);
        v1 = BitmapFactory.decodeResource(getResources(), R.drawable.rank_v1);
        v2 = BitmapFactory.decodeResource(getResources(), R.drawable.rank_v2);
        v3 = BitmapFactory.decodeResource(getResources(), R.drawable.rank_v3);
        v4 = BitmapFactory.decodeResource(getResources(), R.drawable.rank_v4);
        v5 = BitmapFactory.decodeResource(getResources(), R.drawable.rank_v5);
        v6 = BitmapFactory.decodeResource(getResources(), R.drawable.rank_v6);
        v7 = BitmapFactory.decodeResource(getResources(), R.drawable.rank_v7);

        // 获取用户信息
        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("info");
        user = (User) bundle.getBinder("user");
        rivalUser = (User) bundle.getBinder("rivalUser");
        EloUtil.match(user);
        EloUtil.match(rivalUser);

        avatar.setImageBitmap(user.getAvatar());
        rivalAvatar.setImageBitmap(rivalUser.getAvatar());
        nickname.setText(user.getNickname());
        rivalNickname.setText(rivalUser.getNickname());
        rankName.setText(user.getRankName());
        rivalRankName.setText(rivalUser.getRankName());
        if (user.getRank() == 1) {
            rankImage.setImageBitmap(v1);
        } else if (user.getRank() == 2) {
            rankImage.setImageBitmap(v2);
        } else if (user.getRank() == 3) {
            rankImage.setImageBitmap(v3);
        } else if (user.getRank() == 4) {
            rankImage.setImageBitmap(v4);
        } else if (user.getRank() == 5) {
            rankImage.setImageBitmap(v5);
        } else if (user.getRank() == 6) {
            rankImage.setImageBitmap(v6);
        } else if (user.getRank() == 7) {
            rankImage.setImageBitmap(v7);
        }
        if (rivalUser.getRank() == 1) {
            rivalRankImage.setImageBitmap(v1);
        } else if (rivalUser.getRank() == 2) {
            rivalRankImage.setImageBitmap(v2);
        } else if (rivalUser.getRank() == 3) {
            rivalRankImage.setImageBitmap(v3);
        } else if (rivalUser.getRank() == 4) {
            rivalRankImage.setImageBitmap(v4);
        } else if (rivalUser.getRank() == 5) {
            rivalRankImage.setImageBitmap(v5);
        } else if (rivalUser.getRank() == 6) {
            rivalRankImage.setImageBitmap(v6);
        } else if (rivalUser.getRank() == 7) {
            rivalRankImage.setImageBitmap(v7);
        }

        isFirst = rivalUser.isFirst();
        if (isFirst) {
            isChess = true;
            chess.setImageBitmap(black);
            rivalChess.setImageBitmap(white);
            lastTime.setVisibility(View.VISIBLE);
            rivalLastTime.setVisibility(View.GONE);
        } else {
            isChess = false;
            rivalChess.setImageBitmap(black);
            chess.setImageBitmap(white);
            rivalLastTime.setVisibility(View.VISIBLE);
            lastTime.setVisibility(View.GONE);
        }

    }

    public void setLastTime() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 30; i > 0; i--) {

                }
            }
        }).start();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            alert("退出将直接判负，确认退出？");
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void alert(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("提示");
        builder.setMessage(msg);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                view.closeWebSocket();
                Intent intent = new Intent(HumanActivity.this, MainActivity.class);
                startActivityForResult(intent, PLAYER_CODE);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}