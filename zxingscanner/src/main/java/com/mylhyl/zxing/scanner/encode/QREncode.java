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

package com.mylhyl.zxing.scanner.encode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.result.ParsedResultType;
import com.mylhyl.zxing.scanner.common.Contents;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * This class encodes data from an Intent into a QR code, and then displays it
 * full screen so that another person can scan it with their device.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class QREncode {
    private QREncode() {
    }

    /**
     * @param context
     * @param codeEncoder {@linkplain Builder#build() QREncode.Builder().build()}
     * @return
     */
    public static Bitmap encodeQR(Context context, QRCodeEncoder codeEncoder) {
        // This assumes the view is full screen, which is a good assumption
        int smallerDimension = getSmallerDimension(context.getApplicationContext());
        try {
            return codeEncoder.encodeAsBitmap(smallerDimension);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static int getSmallerDimension(Context context) {
        WindowManager manager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        int width = displaySize.x;
        int height = displaySize.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 7 / 8;
        return smallerDimension;
    }

    public static class Builder {
        private BarcodeFormat barcodeFormat;
        private ParsedResultType parsedResultType;
        private Bundle bundle;
        private String contents;//原内容
        private String encodeContents;//编码内容
        private int color;
        private boolean useVCard;

        BarcodeFormat getBarcodeFormat() {
            return barcodeFormat;
        }

        public Builder setBarcodeFormat(BarcodeFormat barcodeFormat) {
            this.barcodeFormat = barcodeFormat;
            return this;
        }

        ParsedResultType getParsedResultType() {
            return parsedResultType;
        }

        public Builder setParsedResultType(ParsedResultType parsedResultType) {
            this.parsedResultType = parsedResultType;
            return this;
        }

        Bundle getBundle() {
            return bundle;
        }

        public Builder setBundle(Bundle bundle) {
            this.bundle = bundle;
            return this;
        }

        String getContents() {
            return contents;
        }

        /**
         * 二维码内容
         *
         * @param contents tel、email等不需要前缀
         * @return
         */
        public Builder setContents(String contents) {
            this.contents = contents;
            return this;
        }

        String getEncodeContents() {
            return encodeContents;
        }

        Builder setEncodeContents(String encodeContents) {
            this.encodeContents = encodeContents;
            return this;
        }

        int getColor() {
            return color;
        }

        public Builder setColor(int color) {
            this.color = color;
            return this;
        }

        boolean isUseVCard() {
            return useVCard;
        }

        public Builder setUseVCard(boolean useVCard) {
            this.useVCard = useVCard;
            return this;
        }

        public QRCodeEncoder build() {
            if (parsedResultType == null) {
                throw new IllegalArgumentException("parsedResultType no found...");
            }
            if (parsedResultType != ParsedResultType.ADDRESSBOOK && parsedResultType != ParsedResultType.GEO && contents == null) {
                throw new IllegalArgumentException("parsedResultType not" +
                        " ParsedResultType.ADDRESSBOOK and ParsedResultType.GEO, contents can not null");
            }
            if ((parsedResultType == ParsedResultType.ADDRESSBOOK || parsedResultType == ParsedResultType.GEO) && bundle == null) {
                throw new IllegalArgumentException("parsedResultType yes" +
                        " ParsedResultType.ADDRESSBOOK and ParsedResultType.GEO, bundle can not null");
            }
            return new QRCodeEncoder(this);
        }
    }
}
