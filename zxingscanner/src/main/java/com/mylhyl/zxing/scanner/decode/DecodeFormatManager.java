package com.mylhyl.zxing.scanner.decode;

import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.mylhyl.zxing.scanner.common.Scanner;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by hupei on 2017/7/7.
 */

public final class DecodeFormatManager {

    static final Set<BarcodeFormat> PRODUCT_FORMATS;
    static final Set<BarcodeFormat> INDUSTRIAL_FORMATS;
    private static final Set<BarcodeFormat> ONE_D_FORMATS;
    static final Set<BarcodeFormat> QR_CODE_FORMATS = EnumSet.of(BarcodeFormat.QR_CODE);
    static final Set<BarcodeFormat> DATA_MATRIX_FORMATS = EnumSet.of(BarcodeFormat.DATA_MATRIX);

    static {
        PRODUCT_FORMATS = EnumSet.of(
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.RSS_14,
                BarcodeFormat.RSS_EXPANDED);
        INDUSTRIAL_FORMATS = EnumSet.of(
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.CODE_128,
                BarcodeFormat.ITF,
                BarcodeFormat.CODABAR);
        ONE_D_FORMATS = EnumSet.copyOf(PRODUCT_FORMATS);
        ONE_D_FORMATS.addAll(INDUSTRIAL_FORMATS);
    }

    private static final Map<String, Set<BarcodeFormat>> FORMATS_FOR_MODE;

    static {
        FORMATS_FOR_MODE = new HashMap<>();
        FORMATS_FOR_MODE.put(Scanner.ScanMode.ONE_D_MODE, ONE_D_FORMATS);
        FORMATS_FOR_MODE.put(Scanner.ScanMode.PRODUCT_MODE, PRODUCT_FORMATS);
        FORMATS_FOR_MODE.put(Scanner.ScanMode.QR_CODE_MODE, QR_CODE_FORMATS);
        FORMATS_FOR_MODE.put(Scanner.ScanMode.DATA_MATRIX_MODE, DATA_MATRIX_FORMATS);
    }

    private DecodeFormatManager() {
    }

    public static Set<BarcodeFormat> parseDecodeFormats(BarcodeFormat... scanFormats) {
        if (scanFormats != null) {
            Set<BarcodeFormat> formats = EnumSet.noneOf(BarcodeFormat.class);
            try {
                for (BarcodeFormat format : scanFormats) {
                    formats.add(format);
                }
                return formats;
            } catch (IllegalArgumentException iae) {
                // ignore it then
            }
        }
        return null;
    }

    public static Set<BarcodeFormat> parseDecodeFormats(String decodeMode) {
        if (!TextUtils.isEmpty(decodeMode)) {
            return FORMATS_FOR_MODE.get(decodeMode);
        }
        return null;
    }
}
