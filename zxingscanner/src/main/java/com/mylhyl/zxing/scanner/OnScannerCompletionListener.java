package com.mylhyl.zxing.scanner;

import android.graphics.Bitmap;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;

/**
 * Created by hupei on 2016/7/1.
 */
public interface OnScannerCompletionListener {
    /**
     * 扫描成功后将调用
     * @param rawResult
     * @param parsedResult
     * @param barcode
     */
    void OnScannerCompletion(Result rawResult, ParsedResult parsedResult, Bitmap barcode);
}
