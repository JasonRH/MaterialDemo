package com.rh.materialdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.rh.materialdemo.Util.ActivityCollector;
import com.rh.materialdemo.activity.DownloadActivity;
import com.squareup.leakcanary.LeakCanary;
import org.litepal.LitePalApplication;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 *
 * @author RH
 * @date 2017/11/4
 */

public class MyApplication extends Application {
    /**
     * 全局context
     */
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        /*LitePalApplication.initialize(context)的调用原则是尽可能早，
         *比如合适的调用位置是在Application的onCreate()里调用。
         *调用时传递的参数是Application的context，
         *不要使用任何activity或service的实例作为参数，否则可能发生内存泄漏。
         *
         * 也可以在AndroidManifest.xml，配置application的参数android:name="org.litepal.LitePalApplication"
         * 然后MyApplication继承LitePalApplication
         */
        LitePalApplication.initialize(context);

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

    }

    public static Context getContext() {
        return context;
    }

    public static void exit() {
        ActivityCollector.removeAllActivity();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);

    }


}
