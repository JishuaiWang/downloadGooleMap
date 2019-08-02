package com.hb.downloadmap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTMS extends AppCompatActivity {
    static String TAG = "地图下载";
    String url;//TMS服务地址
    int minLevel;//最小地图等级
    int maxLevel;//最大地图等级
    String storageUrl;//存储路径
    final OkHttpClient okHttpClient = new OkHttpClient();
    class Data {
        public String url;
        public String filePath;

        public Data(String url, String filePath) {
            this.url = url;
            this.filePath = filePath;
        }
    }
    List<Data> dataList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        storageUrl = Environment.getExternalStorageDirectory().getAbsolutePath() + "/map";//默认使用外部存储根目录
        //请求权限
        requestPermission();
    }

    /**
     * 开始下载
     *
     * @param view
     */
    public void downLoad(View view) {
        EditText editText = findViewById(R.id.url);
        url = String.valueOf(editText.getText());
        EditText editText1 = findViewById(R.id.minLevel);
        minLevel = Integer.parseInt(String.valueOf(editText1.getText()));
        EditText editText2 = findViewById(R.id.maxLevel);
        maxLevel = Integer.parseInt(String.valueOf(editText2.getText()));

        for (int z = minLevel; z <= maxLevel; z++) {
            createFolder(storageUrl + "/" + z);
            int maxValue = (int) Math.pow(2, z) - 1;
            Log.e(TAG, "第" + z + "级");
            for (int x = 0; x <= maxValue; x++) {
                createFolder(storageUrl + "/" + z + "/" + x);
                for (int y = 0; y <= maxValue; y++) {
                    dataList.add(new Data(getCurrentUrl(x, y, z), storageUrl + "/" + z + "/" + x + "/" + y + ".png"));
                }
            }
        }
        Log.e(TAG, "下载数量 = " + dataList.size());
        runNext();
    }

    private void runNext(){
        if(dataList.size() > 0){
            final int index = 0;
            final Data data = dataList.get(index);
            Request request = new Request.Builder().get().url(data.url).build();
            Call call = okHttpClient.newCall(request);
            Log.e(TAG, "开始下载 = " + data.url);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "下载失败 = " + data.url + ", 重新下载");
                    dataList.add(data);
                    onFinish();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    byte[] picture_bt = response.body().bytes();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(picture_bt, 0, picture_bt.length);
                    File file = new File(data.filePath);
                    bitMapToPNG(bitmap, file);
                    onFinish();
                }

                private void onFinish(){
                    dataList.remove(index);
                    Log.e(TAG, "下载结束，剩余数量 = " + dataList.size());
                    runNext();
                }
            });
        }else{
            Log.e(TAG, "下载结束");
        }
    }

    /**
     * 通过替换xyz参数生成每一个图片的下载url
     * @param x
     * @param y
     * @param z
     * @return
     */
    String getCurrentUrl(int x, int y, int z) {
        String temp = url.replace("{x}", String.valueOf(x));
        temp = temp.replace("{y}", String.valueOf(y));
        return temp.replace("{z}", String.valueOf(z));
    }

    /**
     * 创建文件夹
     *
     * @param url
     */
    void createFolder(String url) {
        File dir = new File(url);
        // 如果目录不中存在，创建这个目录
        if (!dir.exists()){
            dir.mkdirs();
        }
    }

    /**
     * 通过OkHttp下载图片
     *
     * @param url
     */
    void downImage(String url, final String filePath) {
        Request request = new Request.Builder().get().url(url).build();
        Call call = okHttpClient.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "下载失败");
                Log.e(TAG, String.valueOf(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    byte[] picture_bt = response.body().bytes();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(picture_bt, 0, picture_bt.length);
                    Log.e(TAG, String.valueOf(bitmap.getWidth()));
                    Log.e(TAG, String.valueOf(bitmap.getHeight()));
                    File file = new File(filePath);
                    bitMapToPNG(bitmap, file);
                }
            }
        });
    }

    /**
     * Bitmap写入本地文件
     *
     * @param file 图片路径文件
     */
    static void bitMapToPNG(Bitmap bitmap, File file) {
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            //100为不压缩
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 请求授权授予，主要在模拟器使用
     */
    private void requestPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) { //表示未授权时
            //进行授权
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Button button = findViewById(R.id.button);
                    button.setClickable(true);
                }
                break;
            default:
                break;
        }
    }
}
