package top.panghai.fifthchess;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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

import java.io.InputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import top.panghai.fifthchess.entity.User;
import top.panghai.fifthchess.utils.LocalCacheUtils;

public class MainActivity extends Activity implements View.OnClickListener {

    private Button machineBtn, playerBtn, integralBtn;
    private Context context = this;
    private CircleImageView avatar;
    private TextView nickname;

    private User user;
    private Bitmap bitmap, rivalBitmap;
    private String androidID;

    private static final String remoteAvatarUrl = "http://119.91.232.147:8095/";
//    private static final String remoteAvatarUrl = "https://api.multiavatar.com/";

    // activity申请码
    private static final int machineCode = 4399;
    private static final int playerCode = 3399;
    private static final int integralCode = 5399;

    // 权限申请码
    private static final int PERMISSION_CODE = 8848;

    private static final String USER = "user";
    private static final String RIVAL_USER = "rivalUser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        // 申请权限
        requestPermission();
        // 初始化视图
        initView();
        // 设置加载弹窗
        setLoadingDialog();
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
        integralBtn = findViewById(R.id.integral);

        avatar.setOnClickListener(this);
        nickname.setOnClickListener(this);
        playerBtn.setOnClickListener(this);
        machineBtn.setOnClickListener(this);
        integralBtn.setOnClickListener(this);
        // 网络对战按钮设置不可见
        playerBtn.setVisibility(View.GONE);
        // 积分排行按钮设置不可见
        integralBtn.setVisibility(View.GONE);
    }

    /**
     * 设置加载弹窗
     */
    private void setLoadingDialog() {
    }

    /**
     * 获取用户信息，用户头像
     */
    private void initData() {
        androidID = LocalCacheUtils.readString(context, "androidID");
        if (androidID == null || "".equals(androidID)) {
            androidID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
            LocalCacheUtils.writeString(context, "androidID", androidID);
        }
        Log.i("androidID", androidID);

        //子线程与主线程通过Handler来进行通信
        Handler handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                nickname.setText(user.getNickname());
                avatar.setImageBitmap(bitmap);
                showLongMsg("欢迎，" + user.getNickname());
                playerBtn.setVisibility(View.VISIBLE);
                integralBtn.setVisibility(View.VISIBLE);
                return true;
            }
        });

        // 新开一个线程获取用户信息
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
                    user = JSON.parseObject(json, User.class);
                    if (user == null) {
                        throw new Exception();
                    }
                    // 获取网络头像
                    String url = remoteAvatarUrl + user.getNickname() + ".png";
                    bitmap = LocalCacheUtils.getBitmap(url);
                    if (bitmap == null) {
                        throw new Exception();
                    }
                    // 保存网络头像
                    LocalCacheUtils.setCache(context, url, bitmap);
                    // 保存用户
                    LocalCacheUtils.writeUser(context, user, USER);
                    // 发送消息
                    handler.sendEmptyMessage(0);
                } catch (Exception e) {
                    Looper.prepare();
                    showShortMsg("网络繁忙");
                    Looper.loop();
                }
            }
        });
        try {
            user = LocalCacheUtils.readUser(context, "user");
            if (user != null) {
                nickname.setText(user.getNickname());
                String url = remoteAvatarUrl + user.getNickname() + ".png";
                Bitmap bm = LocalCacheUtils.getCache(context, url);
                if (bm != null) {
                    avatar.setImageBitmap(bm);
                    bitmap = bm;
                }
                playerBtn.setVisibility(View.VISIBLE);
                integralBtn.setVisibility(View.VISIBLE);
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
        Intent intent;
        switch (view.getId()) {
            case R.id.player:
                final MatchDialog matchDialog = new MatchDialog(context);
                matchDialog.show();
                Window window = matchDialog.getWindow();
                window.setGravity(Gravity.CENTER);
                matchDialog.setCanceledOnTouchOutside(false);
                break;
            case R.id.machine:
                intent = new Intent(MainActivity.this, AiActivity.class);
                startActivityForResult(intent, machineCode);
                break;
            case R.id.integral:
                user.setAvatar(bitmap);
                intent = new Intent(context, IntegralActivity.class);
                Bundle bundle = new Bundle();
                bundle.putBinder("user", user);
                intent.putExtra("info", bundle);
                startActivityForResult(intent, integralCode);
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
     * 权限请求
     */
    @AfterPermissionGranted(PERMISSION_CODE)
    private void requestPermission() {
        String[] param = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
        };
        if (EasyPermissions.hasPermissions(this, param)) {
            //已有权限
            //showShortMsg("已获得权限");
        } else {
            //无权限 则进行权限请求
            EasyPermissions.requestPermissions(this, "请求权限", PERMISSION_CODE, param);
        }
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
