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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.mylhyl.zxing.scanner.camera.CameraManager;
import com.mylhyl.zxing.scanner.common.Scanner;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class ViewfinderView extends View {

    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;


    private CameraManager cameraManager;
    private final Paint paint;
    private Bitmap resultBitmap;

    private int animationDelay = 0;
    private Bitmap laserLineBitmap;

    private int maskColor = Scanner.color.VIEWFINDER_MASK;//扫描框以外区域半透明黑色
    private int resultColor = Scanner.color.RESULT_VIEW;//扫描成功后扫描框以外区域白色
    private int laserLineTop;// 扫描线最顶端位置

    private int laserLineHeight;//扫描线默认高度
    private int laserFrameCornerWidth;//扫描框4角宽
    private int laserFrameCornerLength;//扫描框4角高
    private int drawTextSize;//提示文字大小
    private int drawTextMargin;//提示文字与扫描框距离
    private ScannerOptions scannerOptions;

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    void setScannerOptions(ScannerOptions scannerOptions) {
        this.scannerOptions = scannerOptions;
        laserLineHeight = dp2px(scannerOptions.getLaserLineHeight());
        laserFrameCornerWidth = dp2px(scannerOptions.getLaserFrameCornerWidth());
        laserFrameCornerLength = dp2px(scannerOptions.getLaserFrameCornerLength());

        drawTextSize = Scanner.sp2px(getContext(), scannerOptions.getTipTextSize());
        drawTextMargin = dp2px(scannerOptions.getTipTextLaserFrameMargin());
    }

    private int dp2px(int dp) {
        return Scanner.dp2px(getContext(), dp);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return;
        }
        Rect frame = cameraManager.getFramingRect();//取扫描框
        //取屏幕预览
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        // 绘制扫描框以外4个区域
        drawMask(canvas, frame);
        // 如果有二维码结果的Bitmap，在扫取景框内绘制不透明的result Bitmap
        if (resultBitmap != null) {
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            drawFrame(canvas, frame);//绘制扫描框
            drawFrameCorner(canvas, frame);//绘制扫描框4角
            drawText(canvas, frame);// 画扫描框下面的字
            drawLaserLine(canvas, frame);//绘制扫描线
            moveLaserSpeed(frame);//计算移动位置
        }
    }

    private void moveLaserSpeed(Rect frame) {
        //初始化扫描线最顶端位置
        if (laserLineTop == 0) {
            laserLineTop = frame.top;
        }
        int laserMoveSpeed = scannerOptions.getLaserLineMoveSpeed();
        // 每次刷新界面，扫描线往下移动 LASER_VELOCITY
        laserLineTop += laserMoveSpeed;
        if (laserLineTop >= frame.bottom) {
            laserLineTop = frame.top;
        }
        if (animationDelay == 0) {
            animationDelay = (int) ((1.0f * 1000 * laserMoveSpeed) / (frame.bottom - frame.top));
        }

        // 只刷新扫描框的内容，其他地方不刷新
        postInvalidateDelayed(animationDelay, frame.left - POINT_SIZE, frame.top - POINT_SIZE
                , frame.right + POINT_SIZE, frame.bottom + POINT_SIZE);
    }

    /**
     * 画扫描框外区域
     *
     * @param canvas
     * @param frame
     */
    private void drawMask(Canvas canvas, Rect frame) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    }

    /**
     * 绘制提示文字
     *
     * @param canvas
     * @param frame
     */
    private void drawText(Canvas canvas, Rect frame) {
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(scannerOptions.getTipTextColor());
        textPaint.setTextSize(drawTextSize);

        float x = frame.left;//文字开始位置
        //根据 drawTextGravityBottom 文字在扫描框上方还是下文，默认下方
        float y = scannerOptions.isTipTextLaserFrameBottom() ? frame.bottom + drawTextMargin
                : frame.top - drawTextMargin;

        StaticLayout staticLayout = new StaticLayout(scannerOptions.getTipText(), textPaint, frame.width()
                , Layout.Alignment.ALIGN_CENTER, 1.0f, 0, false);
        canvas.save();
        canvas.translate(x, y);
        staticLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * 绘制扫描框4角
     *
     * @param canvas
     * @param frame
     */
    private void drawFrameCorner(Canvas canvas, Rect frame) {
        paint.setColor(scannerOptions.getLaserFrameCornerColor());
        paint.setStyle(Paint.Style.FILL);
        // 左上角
        canvas.drawRect(frame.left - laserFrameCornerWidth, frame.top, frame.left, frame.top
                + laserFrameCornerLength, paint);
        canvas.drawRect(frame.left - laserFrameCornerWidth, frame.top - laserFrameCornerWidth
                , frame.left + laserFrameCornerLength, frame.top, paint);
        // 右上角
        canvas.drawRect(frame.right, frame.top, frame.right + laserFrameCornerWidth,
                frame.top + laserFrameCornerLength, paint);
        canvas.drawRect(frame.right - laserFrameCornerLength, frame.top - laserFrameCornerWidth,
                frame.right + laserFrameCornerWidth, frame.top, paint);
        // 左下角
        canvas.drawRect(frame.left - laserFrameCornerWidth, frame.bottom - laserFrameCornerLength,
                frame.left, frame.bottom, paint);
        canvas.drawRect(frame.left - laserFrameCornerWidth, frame.bottom, frame.left
                + laserFrameCornerLength, frame.bottom + laserFrameCornerWidth, paint);
        // 右下角
        canvas.drawRect(frame.right, frame.bottom - laserFrameCornerLength, frame.right
                + laserFrameCornerWidth, frame.bottom, paint);
        canvas.drawRect(frame.right - laserFrameCornerLength, frame.bottom, frame.right
                + laserFrameCornerWidth, frame.bottom + laserFrameCornerWidth, paint);
    }

    /**
     * 画扫描框
     *
     * @param canvas
     * @param frame
     */
    private void drawFrame(Canvas canvas, Rect frame) {
        paint.setColor(Color.WHITE);//扫描边框白色
        paint.setStrokeWidth(1);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(frame, paint);
    }

    /**
     * 画扫描线
     *
     * @param canvas
     * @param frame
     */
    private void drawLaserLine(Canvas canvas, Rect frame) {
        if (scannerOptions.getLaserLineResId() == 0) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(scannerOptions.getLaserLineColor());// 设置扫描线颜色
            canvas.drawRect(frame.left, laserLineTop, frame.right
                    , laserLineTop + laserLineHeight, paint);
        } else {
            if (laserLineBitmap == null)//图片资源文件转为 Bitmap
                laserLineBitmap = BitmapFactory.decodeResource(getResources(), scannerOptions.getLaserLineResId());
            int height = laserLineBitmap.getHeight();//取原图高
            //网格图片
            if (scannerOptions.isLaserGridLine()) {
                RectF dstRectF = new RectF(frame.left, frame.top, frame.right, laserLineTop);
                Rect srcRect = new Rect(0, (int) (height - dstRectF.height())
                        , laserLineBitmap.getWidth(), height);
                canvas.drawBitmap(laserLineBitmap, srcRect, dstRectF, paint);
            }
            //线条图片
            else {
                //如果没有设置线条高度，则用图片原始高度
                if (laserLineHeight == dp2px(ScannerOptions.DEFAULT_LASER_LINE_HEIGHT)) {
                    laserLineHeight = laserLineBitmap.getHeight() / 2;
                }
                Rect laserRect = new Rect(frame.left, laserLineTop, frame.right
                        , laserLineTop + laserLineHeight);
                canvas.drawBitmap(laserLineBitmap, null, laserRect, paint);
            }
        }
    }

    void drawViewfinder() {
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
    void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    void laserLineBitmapRecycle() {
        if (laserLineBitmap != null) {
            laserLineBitmap.recycle();
            laserLineBitmap = null;
        }
    }
}
