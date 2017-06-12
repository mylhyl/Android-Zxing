package com.mylhyl.zxing.scanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
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

//    private static final String TAG = ScannerView.class.getSimpleName();

    private SurfaceView mSurfaceView;
    private ViewfinderView mViewfinderView;

    private boolean hasSurface;
    private CameraManager mCameraManager;
    private ScannerViewHandler mScannerViewHandler;
    private BeepManager mBeepManager;
    private int mMediaResId;
    private OnScannerCompletionListener mScannerCompletionListener;

    private int laserFrameWidth, laserFrameHeight;//扫描框大小
    private int laserFrameTopMargin;//扫描框离屏幕上方距离

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

        mSurfaceView = new SurfaceView(context, attrs, defStyle);
        addView(mSurfaceView, new LayoutParams(LayoutParams.MATCH_PARENT
                , LayoutParams.MATCH_PARENT));

        mViewfinderView = new ViewfinderView(context, attrs);
        addView(mViewfinderView, new LayoutParams(LayoutParams.MATCH_PARENT
                , LayoutParams.MATCH_PARENT));
    }

    public void onResume() {
        mCameraManager = new CameraManager(getContext());
        mCameraManager.setLaserFrameTopMargin(laserFrameTopMargin);//扫描框与屏幕距离
        mViewfinderView.setCameraManager(mCameraManager);
        if (mBeepManager != null) mBeepManager.updatePrefs();

        mScannerViewHandler = null;

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
        if (mBeepManager != null) mBeepManager.close();
        mCameraManager.closeDriver();
        mViewfinderView.laserLineBitmapRecycle();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (mCameraManager.isOpen()) {
//            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
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
            //设置扫描框大小
            if (laserFrameWidth > 0 && laserFrameHeight > 0)
                mCameraManager.setManualFramingRect(laserFrameWidth, laserFrameHeight);
        } catch (IOException ioe) {
//            Log.w(TAG, ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
//            Log.w(TAG, "Unexpected error initializing camera", e);
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
        //扫描成功
        if (mScannerCompletionListener != null) {
            //转换结果
            mScannerCompletionListener.OnScannerCompletion(rawResult,
                    Scanner.parseResult(rawResult), barcode);
        }
        //设置扫描结果图片
        if (barcode != null) {
            mViewfinderView.drawResultBitmap(barcode);
        }
        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            if (mBeepManager == null) {
                mBeepManager = new BeepManager(getContext());
                mBeepManager.setMediaResId(mMediaResId);
            }
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
//        if (surfaceHolder == null) {
//            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
//        }
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
        if (!hasSurface && surfaceHolder != null) {
            surfaceHolder.removeCallback(this);
        }
    }

    public ScannerView setOnScannerCompletionListener(OnScannerCompletionListener listener) {
        this.mScannerCompletionListener = listener;
        return this;
    }

    /**
     * 设置扫描线颜色
     *
     * @param laserColor
     */
    public ScannerView setLaserColor(int laserColor) {
        mViewfinderView.setLaserColor(laserColor);
        return this;
    }

    /**
     * 设置线形扫描线资源
     *
     * @param laserLineResId resId
     */
    public ScannerView setLaserLineResId(int laserLineResId) {
        mViewfinderView.setLaserLineResId(laserLineResId);
        return this;
    }

    /**
     * 设置网格扫描线资源
     *
     * @param laserLineResId resId
     */
    public ScannerView setLaserGridLineResId(int laserLineResId) {
        mViewfinderView.setLaserGridLineResId(laserLineResId);
        return this;
    }

    /**
     * 设置扫描线高度
     *
     * @param laserLineHeight dp
     */
    public ScannerView setLaserLineHeight(int laserLineHeight) {
        mViewfinderView.setLaserLineHeight(laserLineHeight);
        return this;
    }

    /**
     * 设置扫描框4角颜色
     *
     * @param laserFrameBoundColor
     */
    public ScannerView setLaserFrameBoundColor(int laserFrameBoundColor) {
        mViewfinderView.setLaserFrameBoundColor(laserFrameBoundColor);
        return this;
    }

    /**
     * 设置扫描框4角长度
     *
     * @param laserFrameCornerLength dp
     */
    public ScannerView setLaserFrameCornerLength(int laserFrameCornerLength) {
        mViewfinderView.setLaserFrameCornerLength(laserFrameCornerLength);
        return this;
    }

    /**
     * 设置扫描框4角宽度
     *
     * @param laserFrameCornerWidth dp
     */
    public ScannerView setLaserFrameCornerWidth(int laserFrameCornerWidth) {
        mViewfinderView.setLaserFrameCornerWidth(laserFrameCornerWidth);
        return this;
    }

    /**
     * 设置文字颜色
     *
     * @param textColor 文字颜色
     */
    public ScannerView setDrawTextColor(int textColor) {
        mViewfinderView.setDrawTextColor(textColor);
        return this;
    }

    /**
     * 设置文字大小
     *
     * @param textSize 文字大小 sp
     */
    public ScannerView setDrawTextSize(int textSize) {
        mViewfinderView.setDrawTextSize(textSize);
        return this;
    }

    /**
     * 设置文字
     *
     * @param text
     * @param isBottom 是否在扫描框下方
     */
    public ScannerView setDrawText(String text, boolean isBottom) {
        return setDrawText(text, isBottom, 0);
    }

    /**
     * 设置文字
     *
     * @param text
     * @param isBottom   是否在扫描框下方
     * @param textMargin 离扫描框间距 dp
     */
    public ScannerView setDrawText(String text, boolean isBottom, int textMargin) {
        return setDrawText(text, 0, 0, isBottom, textMargin);
    }

    /**
     * 设置文字
     *
     * @param text
     * @param textSize   文字大小 sp
     * @param textColor  文字颜色
     * @param isBottom   是否在扫描框下方
     * @param textMargin 离扫描框间距 dp
     */
    public ScannerView setDrawText(String text, int textSize, int textColor
            , boolean isBottom, int textMargin) {
        mViewfinderView.setDrawText(text, textSize, textColor, isBottom, textMargin);
        return this;
    }

    /**
     * 设置扫描完成播放声音
     *
     * @param mediaResId
     */
    public ScannerView setMediaResId(int mediaResId) {
        this.mMediaResId = mediaResId;
        return this;
    }

    /**
     * 切换闪光灯
     *
     * @param mode true开；false关
     */
    public ScannerView toggleLight(boolean mode) {
        if (mCameraManager != null) mCameraManager.setTorch(mode);
        return this;
    }

    /**
     * 设置扫描框大小
     *
     * @param width  dp
     * @param height dp
     */
    public ScannerView setLaserFrameSize(int width, int height) {
        this.laserFrameWidth = Scanner.dp2px(getContext(), width);
        this.laserFrameHeight = Scanner.dp2px(getContext(), height);
        return this;
    }

    /**
     * 设置扫描框与屏幕距离
     *
     * @param laserFrameTopMargin
     */
    public ScannerView setLaserFrameTopMargin(int laserFrameTopMargin) {
        this.laserFrameTopMargin = Scanner.dp2px(getContext(), laserFrameTopMargin);
        return this;
    }

    /**
     * 重新扫描，支持延时
     *
     * @param delayMS 毫秒
     */
    public void restartPreviewAfterDelay(long delayMS) {
        if (mScannerViewHandler != null) {
            mScannerViewHandler.sendEmptyMessageDelayed(Scanner.RESTART_PREVIEW, delayMS);
        }
    }

    ViewfinderView getViewfinderView() {
        return mViewfinderView;
    }

    void drawViewfinder() {
        mViewfinderView.drawViewfinder();
    }
}
