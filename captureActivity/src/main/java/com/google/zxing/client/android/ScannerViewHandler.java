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

package com.google.zxing.client.android;

import java.util.Collection;
import java.util.Map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraManager;

/**
 * This class handles all the messaging which comprises the state machine for
 * capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ScannerViewHandler extends Handler {

	private final ScannerView scannerView;
	private final DecodeThread decodeThread;
	private State state;
	private final CameraManager cameraManager;

	private enum State {
		PREVIEW, SUCCESS, DONE
	}

	ScannerViewHandler(ScannerView scannerView,
					   Collection<BarcodeFormat> decodeFormats,
					   Map<DecodeHintType, ?> baseHints, String characterSet,
					   CameraManager cameraManager) {
		this.scannerView = scannerView;
		decodeThread = new DecodeThread(scannerView, decodeFormats, baseHints,
				characterSet, new ViewfinderResultPointCallback(
						scannerView.getViewfinderView()));
		decodeThread.start();
		state = State.SUCCESS;

		// Start ourselves capturing previews and decoding.
		this.cameraManager = cameraManager;
		cameraManager.startPreview();
		restartPreviewAndDecode();
	}

	@Override
	public void handleMessage(Message message) {
		switch (message.what) {
		case Scanner.RESTART_PREVIEW:
			restartPreviewAndDecode();
			break;
		case Scanner.DECODE_SUCCEEDED:
			state = State.SUCCESS;
			Bundle bundle = message.getData();
			Bitmap barcode = null;
			float scaleFactor = 1.0f;
			if (bundle != null) {
				byte[] compressedBitmap = bundle
						.getByteArray(DecodeThread.BARCODE_BITMAP);
				if (compressedBitmap != null) {
					barcode = BitmapFactory.decodeByteArray(compressedBitmap,
							0, compressedBitmap.length, null);
					// Mutable copy:
					barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
				}
				scaleFactor = bundle
						.getFloat(DecodeThread.BARCODE_SCALED_FACTOR);
			}
			scannerView
					.handleDecode((Result) message.obj, barcode, scaleFactor);
			break;
		case Scanner.DECODE_FAILED:
			// We're decoding as fast as possible, so when one decode fails,
			// start another.
			state = State.PREVIEW;
			cameraManager.requestPreviewFrame(decodeThread.getHandler(),
					Scanner.DECODE);
			break;
		case Scanner.RETURN_SCAN_RESULT:
			break;
		case Scanner.LAUNCH_PRODUCT_QUERY:
			break;
		}
	}

	public void quitSynchronously() {
		state = State.DONE;
		cameraManager.stopPreview();
		Message quit = Message.obtain(decodeThread.getHandler(), Scanner.QUIT);
		quit.sendToTarget();
		try {
			// Wait at most half a second; should be enough time, and onPause()
			// will timeout quickly
			decodeThread.join(500L);
		} catch (InterruptedException e) {
			// continue
		}

		// Be absolutely sure we don't send any queued up messages
		removeMessages(Scanner.DECODE_SUCCEEDED);
		removeMessages(Scanner.DECODE_FAILED);
	}

	private void restartPreviewAndDecode() {
		if (state == State.SUCCESS) {
			state = State.PREVIEW;
			cameraManager.requestPreviewFrame(decodeThread.getHandler(),
					Scanner.DECODE);
			scannerView.drawViewfinder();
		}
	}

}
