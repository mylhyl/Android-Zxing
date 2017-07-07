package com.mylhyl.zxing.scanner.common;

import android.content.Context;
import android.util.TypedValue;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;

/**
 * Created by hupei on 2016/7/1.
 */
public final class Scanner {
    public static final int RESTART_PREVIEW = 0;
    public static final int DECODE_SUCCEEDED = 1;
    public static final int DECODE_FAILED = 2;
    public static final int RETURN_SCAN_RESULT = 3;
    public static final int LAUNCH_PRODUCT_QUERY = 4;
    public static final int DECODE = 5;
    public static final int QUIT = 6;

    public static class color {
        public static final int VIEWFINDER_MASK = 0x60000000;
        public static final int RESULT_VIEW = 0xb0000000;
        public static final int VIEWFINDER_LASER = 0xff00ff00;
        public static final int POSSIBLE_RESULT_POINTS = 0xc0ffbd21;
        public static final int RESULT_POINTS = 0xc099cc00;
    }

    public static class Scan {
        public static final String ACTION = "com.mylhyl.zxing.scanner.client.android.SCAN";

        public static final String RESULT = "SCAN_RESULT";
    }

    public static class ScanMode {

        /**
         * Decode only UPC and EAN barcodes. This is the right choice for shopping apps which get
         * prices, reviews, etc. for products.
         */
        public static final String PRODUCT_MODE = "PRODUCT_MODE";

        /**
         * Decode only 1D barcodes.
         */
        public static final String ONE_D_MODE = "ONE_D_MODE";

        /**
         * Decode only QR codes.
         */
        public static final String QR_CODE_MODE = "QR_CODE";

        /**
         * Decode only Data Matrix codes.
         */
        public static final String DATA_MATRIX_MODE = "DATA_MATRIX_MODE";
    }

    public static ParsedResult parseResult(Result rawResult) {
        if (rawResult == null) return null;
        return ResultParser.parseResult(rawResult);
    }

    public static int dp2px(Context context, float dpValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue
                , context.getResources().getDisplayMetrics());
    }

    public static int sp2px(Context context, float spValue) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, spValue
                , context.getResources().getDisplayMetrics());
    }
}
