package com.rh.materialdemo.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rh.materialdemo.R;
import com.rh.materialdemo.Util.MyToast;
import com.rh.materialdemo.Util.NetworkUtils;
import com.rh.materialdemo.Util.StorageSpaceUtils;
import com.rh.materialdemo.service.DownloadService;

/**
 * @author RH
 */
public class DownloadActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = "DownloadActivity";
    private EditText textUrl;
    private DownloadService.DownloadBinder downloadBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //设置toolbar替代原ActionBar
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //显示导航按钮
            actionBar.setDisplayHomeAsUpEnabled(true);
            //actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_black_24dp);//设置导航按钮图标，默认为一个返回箭头
        }
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());
        init();
    }

    private void init() {
        initView();
        Intent intent = new Intent(this, DownloadService.class);
        //开启服务
        startService(intent);
        //绑定服务
        bindService(intent, connection, BIND_AUTO_CREATE);
        //获取写入外部存储的权限
        if (ContextCompat.checkSelfPermission(DownloadActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(DownloadActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void initView() {
        textUrl = (EditText) findViewById(R.id.download_url);
        Button startDownload = (Button) findViewById(R.id.start_download);
        Button pauseDownload = (Button) findViewById(R.id.pause_download);
        Button cancelDownload = (Button) findViewById(R.id.cancel_download);
        startDownload.setOnClickListener(this);
        pauseDownload.setOnClickListener(this);
        cancelDownload.setOnClickListener(this);
        TextView space = (TextView) findViewById(R.id.StorageSpaces);

        String availInternalSize= StorageSpaceUtils.formateFileSize(StorageSpaceUtils.getAvailableInternalMemorySize(),false);
        String totalInternalSize= StorageSpaceUtils.formateFileSize(StorageSpaceUtils.getTotalInternalMemorySize(),false);
        space.setText(String.format("内部存储空间剩余：%s/%s", availInternalSize, totalInternalSize));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    MyToast.show("拒绝权限将无法下载");
                    finish();
                }
                break;
            default:
        }
    }

    @Override
    public void onClick(View v) {
        if (downloadBinder == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.start_download:
                String url = textUrl.getText().toString();
                if (!"".equals(url)) {
                    if (NetworkUtils.isNetworkConnected(this)) {
                        downloadBinder.startDownload(url);
                    }else {
                        MyToast.show("当前网络不可用，请检查网络设置！");
                    }
                } else {
                    MyToast.show("请输入下载链接");
                }
                break;
            case R.id.pause_download:
                downloadBinder.pauseDownload();
                break;
            case R.id.cancel_download:
                downloadBinder.cancelDownload();
                break;
            default:
                break;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}