package com.sdk.common;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PlatformActivity extends UnityPlayerActivity
{
	private Handler m_downloadHandler;
    private static final int DOWNLOAD = 1;
    private static final int DOWNLOAD_FINISH = 2;
    
    private int m_progress;
    private String m_url;
    private String m_savePath;
    private String m_saveName;
    
    private static PlatformActivity m_instacne = null;
    
    public static PlatformActivity getInstance()
    {
    	return m_instacne;
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		m_instacne = this;
	}
	
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
	}
	
	@Override
	protected void onStart()
	{
		super.onStart();
	}
	
	@Override
	protected void onStop()
	{
		super.onStop();
	}
	
	@Override
	protected void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	public void onConfigurationChanged(Configuration arg0)
	{
		super.onConfigurationChanged(arg0);
	}
	

	public void DownloadGame(String url, String saveName)
	{
		m_url = url;
		m_saveName = saveName;
		
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				RunDownloadGame();
			}
		});
	}
	
	@SuppressLint("HandlerLeak")
	private void RunDownloadGame()
	{
		m_downloadHandler = new Handler()
		{
			public void handleMessage(Message msg)
	        {
	            switch (msg.what)
	            {
	            case DOWNLOAD:
	            	UnityPlayer.UnitySendMessage("AndroidSDKListener", "DownloadGameProgressValueChangeCallback", String.valueOf(m_progress));
	                break;
	            case DOWNLOAD_FINISH:
	            	InstallApk();
	                break;
	            default:
	                break;
	            }
	        };
		};
		
		new downloadApkThread().start();
	}
	
	private class downloadApkThread extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                {
                    String sdpath = Environment.getExternalStorageDirectory() + "/";
                    m_savePath = sdpath + "Download";
                    File file = new File(m_savePath);
                    if (!file.exists())
                    {
                        file.mkdir();
                    }
                    
                    File apkFile = new File(m_savePath, m_saveName);
                    
                    if (apkFile.exists())
                    {
                    	m_downloadHandler.sendEmptyMessage(DOWNLOAD_FINISH);
                    	return;
                    }
                    
                    File tmpFile = new File(m_savePath, m_saveName + ".tmp");
                    int loadedLength = (int)tmpFile.length();
                    
                    URL url = new URL(m_url);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("range", "bytes=" + loadedLength + "-");
                    conn.connect();
                    int length = conn.getContentLength();
                    length += loadedLength;
                    InputStream is = conn.getInputStream();

                    FileOutputStream fos = new FileOutputStream(tmpFile, tmpFile.exists());
                    int count = loadedLength;
                    byte buf[] = new byte[1024];
                    m_progress = 0;
                    m_downloadHandler.sendEmptyMessage(DOWNLOAD);
                    while (true)
                    {
                        int numread = is.read(buf);
                        count += numread;
                        int newProgress = (int) (((float) count / length) * 100);
                        if (newProgress > m_progress)
                        {
                        	m_progress = newProgress;
                        	m_downloadHandler.sendEmptyMessage(DOWNLOAD);
                        }
                        if (numread <= 0)
                        {
                            break;
                        }
                        fos.write(buf, 0, numread);
                    }
                    fos.close();
                    is.close();
                    
                    tmpFile.renameTo(apkFile);
                    m_downloadHandler.sendEmptyMessage(DOWNLOAD_FINISH);
                }
                else
                {
                	UnityPlayer.UnitySendMessage("AndroidSDKListener", "DownloadGameCallback", "-1");
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                UnityPlayer.UnitySendMessage("AndroidSDKListener", "DownloadGameCallback", "-1");
            }
        }
    };

	
	public void InstallApk()
	{
		File apkfile = new File(m_savePath, m_saveName);
        if (!apkfile.exists())
        {
            return;
        }
		
        // 解决Android N 上 报错：android.os.FileUriExposedException
        // https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
        // http://android.xsoftlab.net/reference/android/support/v4/content/FileProvider.html
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if(Build.VERSION.SDK_INT >= 24)
        {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(getBaseContext(), getBaseContext().getApplicationContext().getPackageName()  + ".fileProvider", apkfile);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        }
        else
        {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.parse("file://" + apkfile.toString()), "application/vnd.android.package-archive");
        }
        this.startActivity(intent);

        UnityPlayer.UnitySendMessage("AndroidSDKListener", "InstallApkCallback", "0");
	}
	
	public void StartWebView(String url)
	{
		Log.d("debug", "start open webview");
		Intent intent = new Intent(this, WebViewActivity.class);
		intent.putExtra("url", url);
		this.startActivity(intent);
	}

	public void TestChannelInit()
	{
		UnityPlayer.UnitySendMessage("AndroidSDKListener", "InitCallback", "This is a message from TestChannelSDK!!!");
	}
}
