package top.panghai.fifthchess;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import top.panghai.fifthchess.entity.User;
import top.panghai.fifthchess.utils.LocalCacheUtils;

public class ImageBottomDialog extends Dialog {

    private TextView nickname, rivalNickname;
    private ImageView avatar, rivalAvatar;
    private User user;
    private String androidID;
    private Context context;
    private static String remoteAvatarUrl = "https://api.multiavatar.com/";
    private static final String KEY = "user";

    public ImageBottomDialog(@NonNull Context context) {
        super(context);
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
        context = getContext();
        //按空白处不能取消动画
        setCanceledOnTouchOutside(false);
        //初始化界面控件
        initView();
        //初始化界面数据
        initData();
    }

    private void initData() {
        androidID = (String) LocalCacheUtils.readString(context, "androidID");
        if (androidID == null || "".equals(androidID)) {
            androidID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            LocalCacheUtils.writeString(context, "androidID", androidID);
        }

        //子线程与主线程通过Handler来进行通信
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                String url = remoteAvatarUrl + user.getNickname() + ".png";
                User user = (User) msg.obj;
                nickname.setText(user.getNickname());
                Bitmap bitmap = LocalCacheUtils.getCache(context, url);
                if (bitmap != null) {
                    avatar.setImageBitmap(bitmap);
                } else {
                    Bitmap bm = LocalCacheUtils.getBitmap(url);
                    avatar.setImageBitmap(bm);
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
                    user = (User) JSON.parseObject(json, User.class);
                    if (user == null) {
                        throw new Exception();
                    }
                    // 保存用户
                    LocalCacheUtils.writeUser(context, user);

                    // 获取网络头像
                    String url = remoteAvatarUrl + user.getNickname() + ".png";
                    Bitmap bitmap = LocalCacheUtils.getBitmap(url);
                    // 保存网络头像
                    LocalCacheUtils.setCache(context, url, bitmap);
                    // 发送消息
                    Message handlerMessage = Message.obtain();
                    handlerMessage.obj = user;
                    handler.sendMessage(handlerMessage);

                } catch (Exception e) {
                    Log.e("error", e.getMessage());
                }
            }
        });
        try {
            user = (User) LocalCacheUtils.readUser(context);
            if (user != null) {
                nickname.setText(user.getNickname());
                String url = remoteAvatarUrl + user.getNickname() + ".png";
                Bitmap bitmap = LocalCacheUtils.getCache(context, url);
                if (bitmap != null) {
                    avatar.setImageBitmap(bitmap);
                } else {
                    avatar.setImageBitmap(LocalCacheUtils.getBitmap(url));
                }
            } else {
                thread.start();
            }
        } catch (Exception e) {
            Log.e("error", e.getMessage());
        }
    }

    private void initView() {
        nickname = findViewById(R.id.nickname);
        rivalNickname = findViewById(R.id.rivalNickname);
        avatar = findViewById(R.id.avatar);
        rivalAvatar = findViewById(R.id.rivalAvatar);
    }

    @Override
    public void show() {
        super.show();
    }

    /**
     * Toast长提示
     *
     * @param msg 内容
     */
    private void showLongMsg(String msg) {
        Toast.makeText(this.getContext(), msg, Toast.LENGTH_LONG).show();
    }
}


