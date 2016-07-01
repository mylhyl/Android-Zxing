package com.mylhyl.zxing.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.mylhyl.zxing.scanner.camera.CameraManager;
import com.mylhyl.zxing.scanner.common.Scanner;

import java.io.IOException;

/**
 * Created by hupei on 2016/7/1.
 */
public class ScannerView extends FrameLayout implements SurfaceHolder.Callback {

    private static final String TAG = ScannerView.class.getSimpleName();

    private SurfaceView mSurfaceView;
    private ViewfinderView mViewfinderView;

    private boolean hasSurface;
    private CameraManager mCameraManager;
    private ScannerViewHandler mScannerViewHandler;
    private BeepManager mBeepManager;
    private AmbientLightManager mAmbientLightManager;
    private OnScannerCompletionListener mScannerCompletionListener;

    public ScannerView(Context context) {
        this(context, null);
    }

    public ScannerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScannerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        hasSurface = false;
        mBeepManager = new BeepManager(context);
        mAmbientLightManager = new AmbientLightManager(context);

        mSurfaceView = new SurfaceView(context, attrs, defStyle);
        addView(mSurfaceView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        mViewfinderView = new ViewfinderView(context, attrs);
        addView(mViewfinderView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void onResume() {
        mCameraManager = new CameraManager(getContext());
        mViewfinderView.setCameraManager(mCameraManager);

        mScannerViewHandler = null;

        mBeepManager.updatePrefs();
        mAmbientLightManager.start(mCameraManager);
        SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
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

    public void onPause() {
        if (mScannerViewHandler != null) {
            mScannerViewHandler.quitSynchronously();
            mScannerViewHandler = null;
        }
        mAmbientLightManager.stop();
        mBeepManager.close();
        mCameraManager.closeDriver();
        // historyManager = null; // Keep for onActivityResult
        if (!hasSurface) {
            SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (mCameraManager.isOpen()) {
            Log.w(TAG,
                    "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            mCameraManager.openDriver(surfaceHolder);
            // Creating the mScannerViewHandler starts the preview, which can also throw a
            // RuntimeException.
            if (mScannerViewHandler == null) {
                mScannerViewHandler = new ScannerViewHandler(this, null,
                        null, null, mCameraManager);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
        }
    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        if (mScannerCompletionListener != null)
            mScannerCompletionListener.OnCompletion(rawResult, barcode);
        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            mBeepManager.playBeepSoundAndVibrate();
            drawResultPoints(barcode, scaleFactor, rawResult);
        }
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of
     * the barcode.
     *
     * @param barcode     A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult   The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor,
                                  Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(Scanner.color.RESULT_POINTS);
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4
                    && (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A || rawResult
                    .getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and
                // metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(),
                                scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a,
                                 ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(), scaleFactor * a.getY(),
                    scaleFactor * b.getX(), scaleFactor * b.getY(), paint);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            Log.e(TAG,
                    "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(surfaceHolder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        hasSurface = false;
    }

    public void setOnScannerCompletionListener(OnScannerCompletionListener listener) {
        this.mScannerCompletionListener = listener;
    }

    /**
     * 设置扫描完成播放声音
     *
     * @param mediaResId
     */
    public void setMediaResId(int mediaResId) {
        mBeepManager.setMediaResId(mediaResId);
    }

    /**
     * 切换闪光灯
     *
     * @param mode
     */
    public void toggleLight(boolean mode) {
        mCameraManager.setTorch(mode);
    }

    public void sendReplyMessage(int id, Object arg, long delayMS) {
        if (mScannerViewHandler != null) {
            Message message = Message.obtain(mScannerViewHandler, id, arg);
            if (delayMS > 0L) {
                mScannerViewHandler.sendMessageDelayed(message, delayMS);
            } else {
                mScannerViewHandler.sendMessage(message);
            }
        }
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (mScannerViewHandler != null) {
            mScannerViewHandler.sendEmptyMessageDelayed(Scanner.RESTART_PREVIEW, delayMS);
        }
    }

    public ViewfinderView getViewfinderView() {
        return mViewfinderView;
    }

    public void drawViewfinder() {
        mViewfinderView.drawViewfinder();
    }

    Handler getScannerViewHandler() {
        return mScannerViewHandler;
    }

    CameraManager getCameraManager() {
        return mCameraManager;
    }

}
