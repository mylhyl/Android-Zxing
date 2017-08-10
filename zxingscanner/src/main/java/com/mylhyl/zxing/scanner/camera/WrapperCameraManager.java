package com.mylhyl.zxing.scanner.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import com.google.zxing.PlanarYUVLuminanceSource;
import com.mylhyl.zxing.scanner.camera.open.CameraFacing;

/**
 * Created by hupei on 2017/8/9.
 */
public class WrapperCameraManager extends CameraManager {
    private final int statusBarHeight;//状态栏高度
    private int laserFrameTopMargin;//扫描框离屏幕上方距离

    public WrapperCameraManager(Context context, CameraFacing cameraFacing) {
        super(context);
        statusBarHeight = getStatusBarHeight();
        setManualCameraId(cameraFacing == CameraFacing.BACK ? 0 : 1);
    }

//    @Override
//    public synchronized Rect getFramingRectInPreview() {
//        return super.getFramingRectInPreview();
//    }
//
//    @Override
//    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
//        return super.buildLuminanceSource(data, width, height);
//    }

    @Override
    public synchronized Rect getFramingRect() {
        if (framingRect == null) {
            if (camera == null) {
                return null;
            }
            Point screenResolution = configManager.getScreenResolution();
            if (screenResolution == null) {
                return null;
            }
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.6);
            int height = (int) (width * 0.9);
            newFramingRect(width, height, screenResolution);
        }
        return framingRect;
    }

    @Override
    public synchronized void setManualFramingRect(int width, int height) {
        if (initialized) {
            Point screenResolution = configManager.getScreenResolution();
            if (width > screenResolution.x) {
                width = screenResolution.x;
            }
            if (height > screenResolution.y) {
                height = screenResolution.y;
            }

            newFramingRect(width, height, screenResolution);
            framingRectInPreview = null;
        } else {
            requestedFramingRectWidth = width;
            requestedFramingRectHeight = height;
        }
    }

    private void newFramingRect(int width, int height, Point screenResolution) {
        int leftOffset = (screenResolution.x - width) / 2;
        int topOffset = (screenResolution.y - height) / 2;
        int topMargin = laserFrameTopMargin;
        if (topMargin == 0) {
            topMargin = topOffset - statusBarHeight;
        } else {
            topMargin += statusBarHeight;
        }
        framingRect = new Rect(leftOffset, topMargin, leftOffset + width, topMargin + height);
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen",
                "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * 设置扫描框与屏幕上方距离
     *
     * @param laserFrameTopMargin
     */
    public void setLaserFrameTopMargin(int laserFrameTopMargin) {
        this.laserFrameTopMargin = laserFrameTopMargin;
    }
}
