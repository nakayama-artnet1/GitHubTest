package com.example.nakayama.myapplication;

/**
 * Created by Nakayama on 2016/09/06.
 */
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import android.util.Log;

import android.app.Activity;
import android.os.AsyncTask;

import android.app.ProgressDialog;
import android.os.Handler;
import android.os.Message;
import android.os.AsyncTask;

public class AsyncFileDownload extends AsyncTask<String, Void, Boolean>
{
    private final String TAG = "AsyncFileDownload";
    private final int TIMEOUT_READ = 5000;
    private final int TIMEOUT_CONNECT = 30000;

    public Activity owner = null;
    private final int BUFFER_SIZE = 1024;

    private String urlString;
    private File outputFile;
    private InputStream inputStream;
    private BufferedInputStream bufferedInputStream;

    private int totalByte = 0;
    private int currentByte = 0;

    private byte[] buffer = new byte[BUFFER_SIZE];

    private URL url;
    private URLConnection urlConnection;

    private boolean cancelFlag = false;

    // 初期化
    public AsyncFileDownload(Activity activity, String url, File oFile)
    {
        owner = activity;
        urlString = url;
        outputFile = oFile;
        cancelFlag = false;
        totalByte = 0;
    }

    // ダウンロードパーセンテージ取得
    public int GetLoadedBytePercent()
    {
        if (totalByte <= 0) {
            return totalByte;
        }

        return (int)Math.floor(100 * currentByte/totalByte);
    }

    // ダウンロードキャンセル要求
    public void LordingCancel()
    {
        cancelFlag = true;
    }

    // ダウンロード中かどうか
    public boolean IsDownloading()
    {
        if ((currentByte == totalByte)
         || (totalByte == 0)) {
            return false;
        }

        return true;
    }

    // ダウンロードスレッド実行処理
    @Override
    protected Boolean doInBackground(String... url)
    {
        boolean ret = false;

        // 初期化
        if(!connect()) {
            return ret;
        }

        // ダウンロード処理
        int size = download();
        if(size == totalByte) {
            // 成功
            ret = true;
        }
        else {
            // 失敗
            totalByte = -1;
        }

        // 後始末
        close();


        return ret;
    }

    // ダウンロード初期化
    private boolean connect()
    {
        currentByte = 0;

        try {
            // コネクション
            url = new URL(urlString);
            urlConnection = url.openConnection();
            urlConnection.setReadTimeout(TIMEOUT_READ);
            urlConnection.setConnectTimeout(TIMEOUT_CONNECT);
            inputStream = urlConnection.getInputStream();
            bufferedInputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
            totalByte = urlConnection.getContentLength();
        }
        catch (Exception ex) {
            Log.d(TAG, "ConnectError:" + ex.toString());
            return false;
        }

        return true;
    }

    // ダウンロード終了処理
    private void close()
    {
        try {
            bufferedInputStream.close();
        }
        catch (Exception ex) {
            Log.d(TAG, "CloseError:" + ex.toString());
        }
    }

    // 指定URL ダウンロード
    private int download()
    {
        int len;

        try{
            // ダウンロードデータ出力先
            FileOutputStream OutputStream = new FileOutputStream(outputFile);

            Log.d(TAG, "read start");

            while((len = bufferedInputStream.read(buffer)) != -1) {
                OutputStream.write(buffer, 0, len);
                OutputStream.flush();
                currentByte += len;
                if(cancelFlag){
                    Log.d(TAG, "read cancel");
                    return -1;
                }
            }

            OutputStream.close();

            Log.d(TAG, "read end "+ currentByte);
        }
        catch(IOException e) {
            Log.d(TAG, e.toString());
            return -1;
        }

        return currentByte;
    }
}