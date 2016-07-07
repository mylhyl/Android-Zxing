package com.mylhyl.zxing.scanner.sample;

import android.app.Application;

import com.bumptech.glide.request.target.ViewTarget;

/**
 * Created by hupei on 2016/7/7.
 */
public class APP extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //因为Glide加载图片的源码中也使用了setTag和getTag模式而
        //https://github.com/bumptech/glide/issues/370
        ViewTarget.setTagId(R.string.app_name);
    }
}
