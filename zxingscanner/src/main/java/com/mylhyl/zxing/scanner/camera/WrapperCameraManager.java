package com.mylhyl.zxing.scanner.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;

import com.mylhyl.zxing.scanner.camera.open.CameraFacing;

/**
 * Created by hupei on 2017/8/9.
 */
public class WrapperCameraManager extends CameraManager {

    private int laserFrameTopMargin;//扫描框离屏幕上方距离

    public WrapperCameraManager(Context context, CameraFacing cameraFacing) {
        super(context);
        requestedCameraId = cameraFacing == CameraFacing.BACK ? 0 : 1;
    }

    @Override
    public synchronized Rect getFramingRect() {
//        return super.getFramingRect();
        if (framingRect == null) {
            if (camera == null) {
                return null;
            }
            Point screenResolution = configManager.getScreenResolution();
            if (screenResolution == null) {
                // Called early, before init even finished
                return null;
            }
            int height;
            int width = findDesiredDimensionInRange(screenResolution.x, MIN_FRAME_WIDTH,
                    MAX_FRAME_WIDTH);
            //竖屏则为正方形
            if (context.getResources().getConfiguration().orientation == Configuration
                    .ORIENTATION_PORTRAIT) {
                height = width;
            } else {
                height = findDesiredDimensionInRange(screenResolution.y, MIN_FRAME_HEIGHT,
                        MAX_FRAME_HEIGHT);
            }
            int statusBarHeight = getStatusBarHeight();//状态栏高度
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            if (laserFrameTopMargin == 0)
                laserFrameTopMargin = topOffset - statusBarHeight;
            else {
                laserFrameTopMargin += statusBarHeight;
            }
            framingRect = new Rect(leftOffset, laserFrameTopMargin, leftOffset + width,
                    laserFrameTopMargin + height);
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
            int statusBarHeight = getStatusBarHeight();//状态栏高度
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2 - statusBarHeight;
            if (laserFrameTopMargin == 0)
                laserFrameTopMargin = topOffset;
            else {
                laserFrameTopMargin += statusBarHeight;
            }
            framingRect = new Rect(leftOffset, laserFrameTopMargin, leftOffset + width,
                    laserFrameTopMargin + height);
            //   Log.d(TAG, "Calculated manual framing rect: " + framingRect);
            framingRectInPreview = null;
        } else {
            requestedFramingRectWidth = width;
            requestedFramingRectHeight = height;
        }
    }

    /**
     * 获取状态栏高度
     *
     * @return
     */
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
