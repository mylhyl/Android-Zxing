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

package com.mylhyl.zxing.scanner.decode;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.mylhyl.zxing.scanner.camera.CameraManager;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * This thread does all the heavy lifting of decoding the images.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class DecodeThread extends Thread {

    public static final String BARCODE_BITMAP = "barcode_bitmap";
    public static final String BARCODE_SCALED_FACTOR = "barcode_scaled_factor";

    private final CameraManager cameraManager;
    private final Handler scannerViewHandler;
    private final Map<DecodeHintType, Object> hints;
    private DecodeHandler handler;
    private final CountDownLatch handlerInitLatch;
    private boolean bundleThumbnail = false;

    public DecodeThread(CameraManager cameraManager, Handler scannerViewHandler,
                        Collection<BarcodeFormat> decodeFormats, boolean bundleThumbnail) {
        this.cameraManager = cameraManager;
        this.scannerViewHandler = scannerViewHandler;
        this.bundleThumbnail = bundleThumbnail;
        handlerInitLatch = new CountDownLatch(1);

        hints = new EnumMap<>(DecodeHintType.class);

        // The prefs can't change while the thread is running, so pick them up
        // once here.
        if (decodeFormats == null || decodeFormats.isEmpty()) {

            decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
            // 一维码：商品
            boolean decode1DProduct = true;
            if (decode1DProduct) {
                decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
            }
            // 一维码：工业
            boolean decode1DIndustrial = true;
            if (decode1DIndustrial) {
                decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
            }
            // 二维码
            boolean decodeQR = true;
            if (decodeQR) {
                decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
            }
            // Data Matrix
            boolean decodeDataMatrix = true;
            if (decodeDataMatrix) {
                decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
            }
        }
        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);

        Log.i("DecodeThread", "Hints: " + hints);
    }

    public Handler getHandler() {
        try {
            handlerInitLatch.await();
        } catch (InterruptedException ie) {
            // continue?
        }
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new DecodeHandler(cameraManager, scannerViewHandler, hints, bundleThumbnail);
        handlerInitLatch.countDown();
        Looper.loop();
    }

}
