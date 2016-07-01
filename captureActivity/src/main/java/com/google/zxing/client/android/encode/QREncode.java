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

package com.google.zxing.client.android.encode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;

/**
 * This class encodes data from an Intent into a QR code, and then displays it
 * full screen so that another person can scan it with their device.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class QREncode {

	private static final String TAG = QREncode.class.getSimpleName();

	private static final int MAX_BARCODE_FILENAME_LENGTH = 24;
	private static final Pattern NOT_ALPHANUMERIC = Pattern
			.compile("[^A-Za-z0-9]");

	private QRCodeEncoder qrCodeEncoder;

	/**
	 * 
	 * @param context
	 * @param format
	 *            {@linkplain BarcodeFormat BarcodeFormat}
	 * @param type
	 *            {@linkplain Contents.Type Contents.Type}
	 * @param data
	 * @param useVCard
	 */
	public QREncode(Context context, String type, Object data,
			boolean useVCard) {
		// This assumes the view is full screen, which is a good assumption
		WindowManager manager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		Point displaySize = new Point();
		display.getSize(displaySize);
		int width = displaySize.x;
		int height = displaySize.y;
		int smallerDimension = width < height ? width : height;
		smallerDimension = smallerDimension * 7 / 8;

		try {
			qrCodeEncoder = new QRCodeEncoder(null, type, data,
					smallerDimension, useVCard);
			Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
			if (bitmap == null) {
				Log.w(TAG, "Could not encode barcode");
				qrCodeEncoder = null;
				return;
			}
		} catch (WriterException e) {
			Log.w(TAG, "Could not encode barcode", e);
			qrCodeEncoder = null;
		}
	}

	public Bitmap encodeAsBitmap() {
		QRCodeEncoder encoder = qrCodeEncoder;
		if (encoder == null) { // Odd
			Log.w(TAG, "No existing barcode to send?");
			return null;
		}

		String contents = encoder.getContents();
		if (contents == null) {
			Log.w(TAG, "No existing barcode to send?");
			return null;
		}

		try {
			return encoder.encodeAsBitmap();
		} catch (WriterException we) {
			Log.w(TAG, we);
			return null;
		}
	}

	public void saveQR() {
		QRCodeEncoder encoder = qrCodeEncoder;
		if (encoder == null) { // Odd
			Log.w(TAG, "No existing barcode to send?");
			return;
		}

		String contents = encoder.getContents();
		if (contents == null) {
			Log.w(TAG, "No existing barcode to send?");
			return;
		}

		Bitmap bitmap;
		try {
			bitmap = encoder.encodeAsBitmap();
		} catch (WriterException we) {
			Log.w(TAG, we);
			return;
		}
		if (bitmap == null) {
			return;
		}

		File bsRoot = new File(Environment.getExternalStorageDirectory(),
				"BarcodeScanner");
		File barcodesRoot = new File(bsRoot, "Barcodes");
		if (!barcodesRoot.exists() && !barcodesRoot.mkdirs()) {
			Log.w(TAG, "Couldn't make dir " + barcodesRoot);
			return;
		}
		File barcodeFile = new File(barcodesRoot, makeBarcodeFileName(contents)
				+ ".png");
		if (!barcodeFile.delete()) {
			Log.w(TAG, "Could not delete " + barcodeFile);
			// continue anyway
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(barcodeFile);
			bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);
		} catch (FileNotFoundException fnfe) {
			Log.w(TAG, "Couldn't access file " + barcodeFile + " due to "
					+ fnfe);
			return;
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ioe) {
					// do nothing
				}
			}
		}
	}

	private static CharSequence makeBarcodeFileName(CharSequence contents) {
		String fileName = NOT_ALPHANUMERIC.matcher(contents).replaceAll("_");
		if (fileName.length() > MAX_BARCODE_FILENAME_LENGTH) {
			fileName = fileName.substring(0, MAX_BARCODE_FILENAME_LENGTH);
		}
		return fileName;
	}
}
