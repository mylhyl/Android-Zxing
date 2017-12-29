package com.mylhyl.zxing.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.zxing.Result;
import com.mylhyl.zxing.scanner.camera.CameraManager;
import com.mylhyl.zxing.scanner.common.Scanner;

import java.io.IOException;

/**
 * Created by hupei on 2017/12/13.
 */

class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, ScannerViewHandler.HandleDecodeListener {
    private static final String TAG = CameraSurfaceView.class.getSimpleName();

    private ScannerView mScannerView;
    private boolean hasSurface;
    private CameraManager mCameraManager;
    private ScannerViewHandler mScannerViewHandler;

    private boolean lightMode = false;//闪光灯，默认关闭
    private ScannerOptions mScannerOptions;

    CameraSurfaceView(Context context, ScannerView scannerView) {
        super(context);
        this.mScannerView = scannerView;
        hasSurface = false;
    }

    void onResume(ScannerOptions scannerOptions) {
        this.mScannerOptions = scannerOptions;
        this.mCameraManager = new CameraManager(getContext(), mScannerOptions);
        this.mScannerViewHandler = null;

        SurfaceHolder surfaceHolder = getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            surfaceHolder.addCallback(this);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (mCameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            mCameraManager.openDriver(surfaceHolder);
            requestLayout();
            mCameraManager.setTorch(lightMode);
            // Creating the mScannerViewHandler starts the preview, which can also throw a
            // RuntimeException.
            if (mScannerViewHandler == null) {
                mScannerViewHandler = new ScannerViewHandler(mScannerOptions, mCameraManager,this);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
        }
    }

    void onPause() {
        if (mScannerViewHandler != null) {
            mScannerViewHandler.quitSynchronously();
            mScannerViewHandler = null;
        }
        mCameraManager.closeDriver();
    }

    void setTorch(boolean mode) {
        this.lightMode = mode;
        if (mCameraManager != null) mCameraManager.setTorch(lightMode);
    }

    void restartPreviewAfterDelay(long delayMS) {
        if (mScannerViewHandler != null)
            mScannerViewHandler.sendEmptyMessageDelayed(Scanner.RESTART_PREVIEW, delayMS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
//        if (surfaceHolder == null) {
//            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
//        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(surfaceHolder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        hasSurface = false;
        if (!hasSurface && surfaceHolder != null) {
            surfaceHolder.removeCallback(this);
        }
    }

    CameraManager getCameraManager() {
        return mCameraManager;
    }

    @Override
    public void restartPreview() {
        mScannerView.drawViewfinder();
    }

    @Override
    public void decodeSucceeded(Result rawResult, Bitmap barcode, float scaleFactor) {
        mScannerView.handleDecode(rawResult, barcode, scaleFactor);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        boolean portrait = true;
        if (mCameraManager != null) {
            portrait = mCameraManager.isPortrait();
            if (portrait && mCameraManager.getCameraResolution() != null) {
                Point cameraResolution = mCameraManager.getCameraResolution();
                int cameraPreviewWidth = cameraResolution.y;
                int cameraPreviewHeight = cameraResolution.x;
                if (width * 1f / height < cameraPreviewWidth * 1f / cameraPreviewHeight) {
                    float ratio = cameraPreviewHeight * 1f / cameraPreviewWidth;
                    width = (int) (height / ratio + 0.5f);
                } else {
                    float ratio = cameraPreviewWidth * 1f / cameraPreviewHeight;
                    height = (int) (width / ratio + 0.5f);
                }
            }
        }
        if (portrait) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }
}
