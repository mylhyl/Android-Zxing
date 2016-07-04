/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mylhyl.zxing.scanner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import com.google.zxing.ResultPoint;
import com.mylhyl.zxing.scanner.camera.CameraManager;
import com.mylhyl.zxing.scanner.common.Scanner;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {
    static final String TAG = ViewfinderView.class.getSimpleName();
    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;
    private static final int DEFAULT_LASER_LINE_HEIGHT = 6;//扫描线默认高度
    private static final int DEFAULT_LASER_VELOCITY = 5;// 扫描线默认移动距离

    private CameraManager cameraManager;
    private final Paint paint;
    private Bitmap resultBitmap;
    private final int maskColor;//扫描框以外区域半透明黑色
    private final int resultColor;//扫描成功后扫描框以外区域白色
    private int laserColor;//扫描线颜色
    private final int resultPointColor;//聚焦扫描线中聚焦点红色
    private int laserFrameBoundColor;//扫描框4角颜色

    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;

    private int laserLineTop;// 扫描线最顶端位置
    private int laserLineHeight = DEFAULT_LASER_LINE_HEIGHT;
    private int laserVelocity = DEFAULT_LASER_VELOCITY;
    //扫描框4角宽与高
    private int laserFrameBoundsWidth = 10;
    private int laserFrameBoundsLength = 30;
    private int laserLineResId;
    private Drawable laserLineDrawable;
    private float density;
    private String drawText = "将二维码放入框内，即可自动扫描";
    private int drawTextSize = 16;
    private int drawTextColor = Color.WHITE;
    private boolean drawTextGravityBottom = true;
    private int drawTextMargin = 80;
    private int laserFrameWidth;
    private int laserFrameHeight;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = context.getResources().getDisplayMetrics().density;
        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskColor = Scanner.color.VIEWFINDER_MASK;
        resultColor = Scanner.color.RESULT_VIEW;
        laserColor = Scanner.color.VIEWFINDER_LASER;
        resultPointColor = Scanner.color.POSSIBLE_RESULT_POINTS;
        laserFrameBoundColor = laserColor;
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;

    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        //扫描框大小
        if (laserFrameWidth > 0 && laserFrameHeight > 0)
            cameraManager.setManualFramingRect(laserFrameWidth, laserFrameHeight);

        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // 绘制扫描框以外4个区域
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        if (resultBitmap != null) {
            // 如果有二维码结果的Bitmap，在扫取景框内绘制不透明的result Bitmap
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            drawFrameBounds(canvas, frame);//绘制扫描框4角

            //初始化扫描线最顶端位置
            if (laserLineTop == 0) {
                laserLineTop = frame.top;
            }
            // 每次刷新界面，扫描线往下移动 LASER_VELOCITY
            if (laserLineTop >= frame.bottom) {
                laserLineTop = frame.top;
            } else {
                laserLineTop += laserVelocity;
            }

            if (laserLineResId == 0) {
                drawLaserLine(canvas, frame);//绘制扫描线
            } else {
                drawLaserDrawable(canvas, frame);//绘制扫描图片
            }

            // 画扫描框下面的字
            drawText(canvas, frame, width);

            float scaleX = frame.width() / (float) previewFrame.width();
            float scaleY = frame.height() / (float) previewFrame.height();

            List<ResultPoint> currentPossible = possibleResultPoints;
            List<ResultPoint> currentLast = lastPossibleResultPoints;
            int frameLeft = frame.left;
            int frameTop = frame.top;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new ArrayList<>(5);
                lastPossibleResultPoints = currentPossible;
                paint.setAlpha(CURRENT_POINT_OPACITY);
                paint.setColor(resultPointColor);
                synchronized (currentPossible) {
                    for (ResultPoint point : currentPossible) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                                frameTop + (int) (point.getY() * scaleY),
                                POINT_SIZE, paint);
                    }
                }
            }
            if (currentLast != null) {
                paint.setAlpha(CURRENT_POINT_OPACITY / 2);
                paint.setColor(resultPointColor);
                synchronized (currentLast) {
                    float radius = POINT_SIZE / 2.0f;
                    for (ResultPoint point : currentLast) {
                        canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                                frameTop + (int) (point.getY() * scaleY),
                                radius, paint);
                    }
                }
            }

            // 只刷新扫描框的内容，其他地方不刷新
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left - POINT_SIZE,
                    frame.top - POINT_SIZE,
                    frame.right + POINT_SIZE,
                    frame.bottom + POINT_SIZE);
        }
    }

    public void setDrawText(String text, int textSize, int textColor, boolean isBottom, int textMargin) {
        if (!TextUtils.isEmpty(text))
            drawText = text;
        if (textSize > 0)
            drawTextSize = textSize;
        if (textColor > 0)
            drawTextColor = textColor;
        drawTextGravityBottom = isBottom;
        if (textMargin > 0)
            drawTextMargin = textMargin;
    }

    private void drawText(Canvas canvas, Rect frame, int width) {
        paint.setColor(drawTextColor);
        paint.setTextSize(drawTextSize * density);
        paint.setAlpha(SCANNER_ALPHA[2]);
        drawText = "将二维码放入框内，即可自动扫描";
        final float textWidth = paint.measureText(drawText);
        float x = (width - textWidth) / 2;
        float y = drawTextGravityBottom ? frame.bottom + drawTextMargin : frame.top - drawTextMargin;
        canvas.drawText(drawText, x, y, paint);
    }

    /**
     * 绘制扫描框4角样式
     *
     * @param canvas
     * @param frame
     */
    private void drawFrameBounds(Canvas canvas, Rect frame) {
        paint.setColor(Color.WHITE);//扫描框白色
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(frame, paint);

        paint.setColor(laserFrameBoundColor);//4角框颜色与扫描线颜色一至
        paint.setStyle(Paint.Style.FILL);

        // 左上角
        canvas.drawRect(frame.left - laserFrameBoundsWidth, frame.top, frame.left, frame.top
                + laserFrameBoundsLength, paint);
        canvas.drawRect(frame.left - laserFrameBoundsWidth, frame.top - laserFrameBoundsWidth, frame.left
                + laserFrameBoundsLength, frame.top, paint);
        // 右上角
        canvas.drawRect(frame.right, frame.top, frame.right + laserFrameBoundsWidth,
                frame.top + laserFrameBoundsLength, paint);
        canvas.drawRect(frame.right - laserFrameBoundsLength, frame.top - laserFrameBoundsWidth,
                frame.right + laserFrameBoundsWidth, frame.top, paint);
        // 左下角
        canvas.drawRect(frame.left - laserFrameBoundsWidth, frame.bottom - laserFrameBoundsLength,
                frame.left, frame.bottom, paint);
        canvas.drawRect(frame.left - laserFrameBoundsWidth, frame.bottom, frame.left
                + laserFrameBoundsLength, frame.bottom + laserFrameBoundsWidth, paint);
        // 右下角
        canvas.drawRect(frame.right, frame.bottom - laserFrameBoundsLength, frame.right
                + laserFrameBoundsWidth, frame.bottom, paint);
        canvas.drawRect(frame.right - laserFrameBoundsLength, frame.bottom, frame.right
                + laserFrameBoundsWidth, frame.bottom + laserFrameBoundsWidth, paint);
    }

    private void drawLaserLine(Canvas canvas, Rect frame) {
        Log.d(TAG, "drawLaserLine");
        paint.setColor(laserColor);// 设置扫描线颜色
        Rect laserRect = new Rect(frame.left, laserLineTop, frame.right, laserLineTop + laserLineHeight);
        canvas.drawRect(laserRect, paint);
    }


    private void drawLaserDrawable(Canvas canvas, Rect frame) {
        Log.d(TAG, "drawLaserDrawable");
        if (laserLineDrawable == null)
            laserLineDrawable = getResources().getDrawable(laserLineResId);
        //如果没有手动设置扫描线高度，则高度为图片高度
        int intrinsicHeight = laserLineDrawable.getIntrinsicHeight() / 2;
        if (laserLineHeight == DEFAULT_LASER_LINE_HEIGHT) {
            laserLineHeight = intrinsicHeight;
        } else {
            laserLineHeight = Math.min(intrinsicHeight, laserLineHeight);
        }
        Log.d(TAG, "laserLineHeight = " + laserLineHeight);
        Rect laserRect = new Rect(frame.left, laserLineTop, frame.right, laserLineTop + laserLineHeight);
        laserLineDrawable.setBounds(laserRect);
        laserLineDrawable.draw(canvas);
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

    public void setLaserColor(int laserColor) {
        this.laserColor = laserColor;
    }

    public void setLaserLineResId(int laserLineResId) {
        this.laserLineResId = laserLineResId;
    }

    public void setLaserLineHeight(int laserLineHeight) {
        this.laserLineHeight = laserLineHeight;
    }

    public void setLaserFrameBoundColor(int laserFrameBoundColor) {
        this.laserFrameBoundColor = laserFrameBoundColor;
    }

    public void setLaserFrameBoundsLength(int laserFrameBoundsLength) {
        this.laserFrameBoundsLength = laserFrameBoundsLength;
    }

    public void setLaserFrameBoundsWidth(int laserFrameBoundsWidth) {
        this.laserFrameBoundsWidth = laserFrameBoundsWidth;
    }

    public void setLaserFrameSize(int width, int height) {
        this.laserFrameWidth = width;
        this.laserFrameHeight = height;
    }
}
