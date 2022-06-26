package top.panghai.fifthchess;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;
import com.ejlchina.data.Mapper;
import com.ejlchina.okhttps.HttpResult;
import com.ejlchina.okhttps.OkHttps;
import com.ejlchina.okhttps.WebSocket;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Supplier;

import top.panghai.fifthchess.entity.User;
import top.panghai.fifthchess.utils.HttpStatus;
import top.panghai.fifthchess.utils.LocalCacheUtils;

public class MatchDialog extends Dialog {

    private TextView nickname, rivalNickname, matchTime, matchText;
    private ImageView avatar, rivalAvatar;
    private User user, rivalUser;
    private Bitmap bitmap, rivalBitmap;
    private WebSocket webSocket;
    private String androidID;
    private Context context;
    private static String remoteAvatarUrl = "http://119.91.232.147:8095/";
    //    private static final String remoteAvatarUrl = "https://api.multiavatar.com/";
    private Long startMatch = 0L;
    private static final String USER = "user";
    private static final String RIVAL_USER = "rivalUser";
    private static final int CLOSE_CODE = 4000;
    private static final int PLAYER_CODE = 4000;

    /**
     * 匹配标志
     * 匹配成功 0
     * 正在匹配 1
     * 匹配超时 -1
     * 匹配失败 -2
     */
    private int hasMatch = 1;

    public MatchDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog);
        Window window = this.getWindow();
        window.setGravity(Gravity.CENTER);
        WindowManager.LayoutParams lp = window.getAttributes();
//        lp.alpha=0.5f;
        window.setAttributes(lp);
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        // 按空白处不能取消动画
        setCanceledOnTouchOutside(false);
        // 初始化界面控件
        initView();
        // 初始化界面数据
        initData();
        // 开始匹配玩家
        matchPlayer();
    }

    private void initView() {
        nickname = findViewById(R.id.nickname);
        rivalNickname = findViewById(R.id.rivalNickname);
        avatar = findViewById(R.id.avatar);
        rivalAvatar = findViewById(R.id.rivalAvatar);
        matchTime = findViewById(R.id.matchTime);
        matchText = findViewById(R.id.matchText);

        startMatch = System.currentTimeMillis();
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (hasMatch == 1) {
                    Date date = new Date(System.currentTimeMillis() - startMatch);
                    matchTime.setText(new SimpleDateFormat("mm:ss").format(date));
                }
                return false;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                do {
                    try {
                        handler.sendEmptyMessage(1);
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while (true);
            }
        }).start();//启动线程
    }

    private void initData() {
        androidID = LocalCacheUtils.readString(context, "androidID");
        if (androidID == null || "".equals(androidID)) {
            androidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            LocalCacheUtils.writeString(context, "androidID", androidID);
        }

        //子线程与主线程通过Handler来进行通信
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                nickname.setText(user.getNickname());
                if (bitmap != null) {
                    avatar.setImageBitmap(bitmap);
                }
                return true;
            }
        });

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 获取用户数据和头像
                try {
                    // 获取用户数据和头像
                    HttpResult httpResult = OkHttps.sync("/user/login")
                            .addBodyPara("userId", androidID)
                            .bodyType(OkHttps.JSON)
                            .post();
                    Mapper mapper = httpResult.getBody().toMapper();
                    String json = mapper.getString("data");
                    user = JSON.parseObject(json, User.class);
                    if (user == null) {
                        throw new Exception();
                    }
                    // 保存用户
                    LocalCacheUtils.writeUser(context, user, RIVAL_USER);

                    // 获取网络头像
                    String url = remoteAvatarUrl + user.getNickname() + ".png";
                    bitmap = LocalCacheUtils.getBitmap(url);
                    // 保存网络头像
                    LocalCacheUtils.setCache(context, url, bitmap);
                    // 发送消息
                    handler.sendEmptyMessage(0);

                } catch (Exception e) {
                    Log.e("error", e.getMessage());
                }
            }
        });
        try {
            user = LocalCacheUtils.readUser(context, USER);
            if (user != null) {
                nickname.setText(user.getNickname());
                String url = remoteAvatarUrl + user.getNickname() + ".png";
                Bitmap bm = LocalCacheUtils.getCache(context, url);
                if (bm != null) {
                    avatar.setImageBitmap(bm);
                    bitmap = bm;
                }
            } else {
                thread.start();
            }
        } catch (Exception e) {
            Log.e("error", e.getMessage());
        }
    }

    private void matchPlayer() {
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (rivalBitmap != null) {
                    rivalAvatar.setImageBitmap(rivalBitmap);
                    rivalNickname.setText(rivalUser.getNickname());
                    matchText.setText("匹配成功");
                }
                showShortMsg("匹配成功");
                webSocket.close(3000, "匹配成功");
                matchSuccess();
                return false;
            }
        });

        webSocket = OkHttps.webSocket("/match?userId=" + androidID)
                // 分别指定客户端与服务器的心跳时间间隔
                .heatbeat(10, 10)
                // 遇到网络错误时，延迟2秒再退出匹配
                .setOnException(new WebSocket.Listener<Throwable>() {
                    final Handler handler = new Handler(new Handler.Callback() {
                        @Override
                        public boolean handleMessage(@NonNull Message message) {
                            MatchDialog.super.dismiss();
                            return false;
                        }
                    });

                    @Override
                    public void on(WebSocket ws, Throwable data) {
                        Looper.prepare();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    handler.sendEmptyMessageDelayed(1, 2000);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                        showShortMsg("网络错误");
                        Looper.loop();
                    }
                })
                // 监听服务器发送的消息
                .setOnMessage(new WebSocket.Listener<WebSocket.Message>() {
                    @Override
                    public void on(WebSocket ws, WebSocket.Message message) {
                        Mapper mapper = message.toMapper();
                        String code = mapper.getString(HttpStatus.CODE);
                        String msg = mapper.getString(HttpStatus.MSG);
                        String data = mapper.getString(HttpStatus.DATA);
                        // 收到服务器的200状态码，发送匹配请求
                        if (HttpStatus.SUCCESS.equals(code)) {
                            ws.send(androidID);
                        }
                        // 收到服务器的0状态码，匹配成功
                        if (HttpStatus.MATCH_SUCCESS.equals(code)) {
                            Looper.prepare();
                            rivalUser = JSON.parseObject(data, User.class);
                            Log.i("WebSocket=======", rivalUser.toString());
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    // 获取头像
                                    try {
                                        String url = remoteAvatarUrl + rivalUser.getNickname() + ".png";
                                        // 获取缓存图片
                                        rivalBitmap = LocalCacheUtils.getCache(context, url);
                                        // 如果没有缓存，获取对手网络头像
                                        if (rivalBitmap == null) {
                                            rivalBitmap = LocalCacheUtils.getBitmap(url);
                                        }
                                        // 发送消息
                                        handler.sendEmptyMessage(0);
                                        // 保存对手网络头像
                                        LocalCacheUtils.setCache(context, url, rivalBitmap);
                                        // 保存对手信息
                                        LocalCacheUtils.writeUser(context, rivalUser, RIVAL_USER);
                                    } catch (Exception e) {
                                        Log.e("error", e.getMessage());
                                    }
                                }
                            }).start();
                            Looper.loop();
                        }
                        // 收到服务器的10000状态码，匹配超时
                        if (HttpStatus.TIMEOUT.equals(code)) {
                            Looper.prepare();
                            showShortMsg(msg);
                            MatchDialog.super.dismiss();
                            Looper.loop();
                        }
                        // 缺少参数
                        if (HttpStatus.PARAMS_LACK_ERROR.equals(code)) {
                            Looper.prepare();
                            showShortMsg("错误");
                            MatchDialog.super.dismiss();
                            Looper.loop();
                        }
                        // 没有该用户
                        if (HttpStatus.NOT_USER_ERROR.equals(code)) {
                            Looper.prepare();
                            showShortMsg("错误");
                            MatchDialog.super.dismiss();
                            Looper.loop();
                        }
                    }
                })
                .listen();
    }

    /**
     * 匹配成功后的页面变化
     */
    private void matchSuccess() {
        hasMatch = 0;
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                if (message.what > 0) {
                    matchText.setText("即将进入游戏...");
                    matchTime.setText(String.valueOf(message.what));
                } else {
                    MatchDialog.super.dismiss();
                    user.setAvatar(bitmap);
                    rivalUser.setAvatar(rivalBitmap);
                    Intent intent = new Intent(context, HumanActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putBinder("user", user);
                    bundle.putBinder("rivalUser", rivalUser);
                    intent.putExtra("info", bundle);
                    context.startActivity(intent);
                    ((Activity) context).finish();
                }
                return false;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 3; i > 0; i--) {
                    handler.sendEmptyMessage(i);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                handler.sendEmptyMessage(-1);
            }
        }).start();

    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            webSocket.close(CLOSE_CODE, "用户退出匹配");
            showShortMsg("取消匹配");
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Toast长提示
     *
     * @param msg 内容
     */
    private void showLongMsg(String msg) {
        Toast.makeText(this.getContext(), msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Toast短提示
     *
     * @param msg 内容
     */
    private void showShortMsg(String msg) {
        Toast.makeText(this.getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}


