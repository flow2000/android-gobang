package top.panghai.fifthchess.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import top.panghai.fifthchess.entity.User;

public class LocalCacheUtils {

    /**
     * SharedPreferences文件名
     */
    private static final String FileName = "userProfile";

    /**
     * 将网络图片转成bitmap
     *
     * @param s 网络图片路径
     * @return bitmap
     */
    public static Bitmap getBitmap(String s) {
        try {
            //把传过来的路径转成URL
            URL url = new URL(s);
            //获取连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            //使用GET方法访问网络
            connection.setRequestMethod("GET");
            //超时时间为10秒
            connection.setConnectTimeout(10000);
            //获取返回码
            int code = connection.getResponseCode();
            if (code == 200) {
                InputStream inputStream = connection.getInputStream();
                //使用工厂把网络的输入流生产Bitmap
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (IOException e) {
            Log.e("error", e.getMessage());
        }
        return null;
    }

    /**
     * 存入缓存文件
     *
     * @param context 上下文
     * @param url     图片路径
     * @param bitmap  图片对象
     */
    public static void setCache(Context context, String url, Bitmap bitmap) {
        String basePath = context.getApplicationContext().getFilesDir().getAbsolutePath() + "/pic";
        Log.i("basePath", basePath);
        File baseFile = new File(basePath);
        if (!baseFile.exists() || !baseFile.isDirectory()) {
            // 如果不存在 或者不是一个文件夹 就去创建文件夹
            baseFile.mkdirs();
        }
        String md5 = encode(url);
        // 生成图片对应的文件
        File bitemapFile = new File(baseFile, md5);
        try {
            // 写入流
            FileOutputStream fos = new FileOutputStream(bitemapFile);
            // 压缩 参1 格式 参2 100就是不压缩 参3写入流
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (FileNotFoundException e) {
            Log.e("error", e.getMessage());
        }
    }

    /**
     * 从本地取出图片
     *
     * @param context 上下文
     * @param url     图片路径
     * @return Bitmap
     */
    public static Bitmap getCache(Context context, String url) {
        String basePath = context.getApplicationContext().getFilesDir().getAbsolutePath() + "/pic";
        Bitmap bitmap = null;
        File baseFile = new File(basePath);
        if (!baseFile.exists() || !baseFile.isDirectory()) {
            // 如果不存在 直接返回
            return null;
        }
        // 用url的md5值作为文件的名字
        String md5 = encode(url);
        // 生成图片对应的文件
        File bitmapFile = new File(baseFile, md5);
        if (bitmapFile.exists()) {
            try {
                // 文件转bitmap
                bitmap = BitmapFactory.decodeStream(new FileInputStream(bitmapFile));
            } catch (FileNotFoundException e) {
                Log.e("error", e.getMessage());
            }
        }
        return bitmap;
    }

    /**
     * 字符串计算md5
     *
     * @param string 字符串
     * @return md5值
     */
    public static String encode(String string) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 保存字符串
     *
     * @param context 上下文
     * @param key     键
     * @param str     字符串
     */
    public static void writeString(Context context, String key, String str) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, str);
        editor.apply();
    }

    /**
     * 保存用户
     *
     * @param context 上下文
     * @param user    用户
     */
    public static void writeUser(Context context, User user) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FileName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = JSON.toJSONString(user);
        editor.putString("user", json);
        editor.apply();
    }

    /**
     * 读取状态并转为对象
     *
     * @param context 上下文
     */
    public static User readUser(Context context) {
        SharedPreferences pref = context.getSharedPreferences(FileName, Context.MODE_PRIVATE);
        String json = pref.getString("user", "");
        if (!"".equals(json)) {
            try {
                return JSONObject.parseObject(json, User.class);
            } catch (Exception e) {
                Log.e("error", e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * 读取状态并转为字符串
     *
     * @param context 上下文
     * @param key     键
     */
    public static String readString(Context context, String key) {
        SharedPreferences pref = context.getSharedPreferences(FileName, Context.MODE_PRIVATE);
        return pref.getString(key, "");
    }

}
