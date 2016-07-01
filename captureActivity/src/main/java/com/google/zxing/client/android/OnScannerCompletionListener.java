package com.google.zxing.client.android;

import android.graphics.Bitmap;

import com.google.zxing.Result;

/**
 * Created by hupei on 2016/7/1.
 */
public interface OnScannerCompletionListener {
    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult The contents of the barcode
     * @param barcode   A greyscale bitmap of the camera data which was decoded.
     */
    void OnCompletion(Result rawResult, Bitmap barcode);
}
