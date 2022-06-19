package top.panghai.fifthchess;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.ejlchina.data.Mapper;
import com.ejlchina.okhttps.HttpResult;
import com.ejlchina.okhttps.OkHttps;

import de.hdodenhof.circleimageview.CircleImageView;
import top.panghai.fifthchess.entity.User;
import top.panghai.fifthchess.utils.LocalCacheUtils;

public class MainActivity extends Activity implements View.OnClickListener {

    private Button machineBtn, playerBtn;
    private Context context = this;
    private CircleImageView avatar;
    private TextView nickname;
    private User user;
    private String androidID;
    private static final String remoteAvatarUrl = "https://api.multiavatar.com/";
    private static final String KEY = "user";
    private static final int machineCode = 4399;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        // 初始化视图
        initView();
        // 设置初始化弹窗
        setInitDialog();
        // 初始化数据
        initData();
    }

    /**
     * 初始化视图
     */
    private void initView() {
        avatar = findViewById(R.id.avatar);
        nickname = findViewById(R.id.nickname);
        playerBtn = findViewById(R.id.player);
        machineBtn = findViewById(R.id.machine);

        avatar.setOnClickListener(this);
        nickname.setOnClickListener(this);
        playerBtn.setOnClickListener(this);
        machineBtn.setOnClickListener(this);
        // 网络对战按钮设置不可见
        playerBtn.setVisibility(View.GONE);
    }

    /**
     * 设置初始化弹窗
     */
    private void setInitDialog() {
    }

    /**
     * 初始化数据
     */
    private void initData() {

        androidID = (String) LocalCacheUtils.readString(context, "androidID");
        if (androidID == null || "".equals(androidID)) {
            androidID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            LocalCacheUtils.writeString(context, "androidID", androidID);
        }

        Log.i("androidID", androidID);

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
                showLongMsg("欢迎，" + user.getNickname());
                playerBtn.setVisibility(View.VISIBLE);
                return true;
            }
        });

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
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
                    Looper.prepare();
                    showLongMsg("网络繁忙");
                    Looper.loop();
                }

            }
        });
        try {
            user = LocalCacheUtils.readUser(context);
            if (user != null) {
                nickname.setText(user.getNickname());
                String url = remoteAvatarUrl + user.getNickname() + ".png";
                Bitmap bitmap = LocalCacheUtils.getCache(context, url);
                if (bitmap != null) {
                    avatar.setImageBitmap(bitmap);
                } else {
                    avatar.setImageBitmap(LocalCacheUtils.getBitmap(url));
                }
                playerBtn.setVisibility(View.VISIBLE);
            } else {
                thread.start();
            }
        } catch (Exception e) {
            Log.e("error", e.getMessage());
        }

    }

    /**
     * 重写onClick方法
     *
     * @param view 视图
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.player:
                final ImageBottomDialog imageBottomDialog = new ImageBottomDialog(context);
                imageBottomDialog.show();
                Window window = imageBottomDialog.getWindow();
                window.setGravity(Gravity.CENTER);
                imageBottomDialog.setCanceledOnTouchOutside(false);
//                imageBottomDialog.dismiss();
//                Intent intent = new Intent(MainActivity.this, HumanActivity.class);
//                startActivity(intent);
                break;
            case R.id.machine:
                Intent intent = new Intent(MainActivity.this, AiActivity.class);
                startActivityForResult(intent, machineCode);
                break;
        }
    }


    /**
     * 回调方法
     *
     * @param requestCode 请求码
     * @param resultCode  响应码
     * @param data        数据
     */
    @SuppressLint("Range")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == machineCode) {
        }
    }

    // 定义一个变量，来标识是否退出
    private static boolean isExit = false;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            isExit = false;
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isExit) {
                isExit = true;
                showShortMsg("再按一次退出程序");
                // 利用handler延迟发送更改状态信息
                mHandler.sendEmptyMessageDelayed(0, 2000);
            } else {
                finishAffinity();
                System.exit(0);
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Toast长提示
     *
     * @param msg 内容
     */
    private void showLongMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Toast短提示
     *
     * @param msg 内容
     */
    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}
