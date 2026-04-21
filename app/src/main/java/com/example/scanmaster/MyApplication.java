package com.example.scanmaster;

import android.app.Application;
import android.util.Log;

import org.opencv.android.OpenCVLoader;

public class MyApplication extends Application {
    private static final String TAG = "ScanMaster";

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化OpenCV远程依赖库
        if (OpenCVLoader.initLocal()) {
            Log.d(TAG, "OpenCV 初始化成功");
        } else {
            Log.e(TAG, "OpenCV 初始化失败");
        }
    }
}
