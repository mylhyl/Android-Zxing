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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.mylhyl.zxing.scanner.common.Contents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * This class does the work of decoding the user's request and extracting all
 * the data to be encoded in a barcode.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class QRCodeEncoder {

	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;

	private String contents;
	private String displayContents;
	private BarcodeFormat barcodeFormat;
	private final int dimension;
	private final boolean useVCard;

	QRCodeEncoder(BarcodeFormat format, String type, Object data, int dimension,
			boolean useVCard) throws WriterException {

		this.dimension = dimension;
		this.useVCard = useVCard;
		encodeContentsFromZXing(format, type, data);
	}

	String getContents() {
		return contents;
	}

	String getDisplayContents() {
		return displayContents;
	}

	boolean isUseVCard() {
		return useVCard;
	}

	// It would be nice if the string encoding lived in the core ZXing library,
	// but we use platform specific code like PhoneNumberUtils, so it can't.
	private boolean encodeContentsFromZXing(BarcodeFormat format, String type,
			Object data) {
		// Default to QR_CODE if no format given.
		barcodeFormat = null;
		if (format != null) {
			try {
				barcodeFormat = format;
			} catch (IllegalArgumentException iae) {
				// Ignore it then
			}
		}
		if (barcodeFormat == null || barcodeFormat == BarcodeFormat.QR_CODE) {
			if (type == null || type.isEmpty()) {
				return false;
			}
			this.barcodeFormat = BarcodeFormat.QR_CODE;
			encodeQRCodeContents(type, data);
		} else {
			if (data != null && data instanceof String) {
				contents = (String) data;
				displayContents = (String) data;
			}
		}
		return contents != null && !contents.isEmpty();
	}

	private void encodeQRCodeContents(String type, Object object) {
		switch (type) {
		case Contents.Type.TEXT:
			String textData = (String) object;
			if (textData != null && !textData.isEmpty()) {
				contents = textData;
				displayContents = textData;
			}
			break;

		case Contents.Type.EMAIL:
			String emailData = ContactEncoder.trim((String) object);
			if (emailData != null) {
				contents = "mailto:" + emailData;
				displayContents = emailData;
			}
			break;

		case Contents.Type.PHONE:
			String phoneData = ContactEncoder.trim((String) object);
			if (phoneData != null) {
				contents = "tel:" + phoneData;
				displayContents = PhoneNumberUtils.formatNumber(phoneData);
			}
			break;

		case Contents.Type.SMS:
			String smsData = ContactEncoder.trim((String) object);
			if (smsData != null) {
				contents = "sms:" + smsData;
				displayContents = PhoneNumberUtils.formatNumber(smsData);
			}
			break;

		case Contents.Type.CONTACT:
			Bundle contactBundle = (Bundle) object;
			if (contactBundle != null) {

				String name = contactBundle
						.getString(ContactsContract.Intents.Insert.NAME);
				String organization = contactBundle
						.getString(ContactsContract.Intents.Insert.COMPANY);
				String address = contactBundle
						.getString(ContactsContract.Intents.Insert.POSTAL);
				List<String> phones = getAllBundleValues(contactBundle,
						Contents.PHONE_KEYS);
				List<String> phoneTypes = getAllBundleValues(contactBundle,
						Contents.PHONE_TYPE_KEYS);
				List<String> emails = getAllBundleValues(contactBundle,
						Contents.EMAIL_KEYS);
				String url = contactBundle.getString(Contents.URL_KEY);
				List<String> urls = url == null ? null : Collections
						.singletonList(url);
				String note = contactBundle.getString(Contents.NOTE_KEY);

				ContactEncoder encoder = useVCard ? new VCardContactEncoder()
						: new MECARDContactEncoder();
				String[] encoded = encoder.encode(
						Collections.singletonList(name), organization,
						Collections.singletonList(address), phones, phoneTypes,
						emails, urls, note);
				// Make sure we've encoded at least one field.
				if (!encoded[1].isEmpty()) {
					contents = encoded[0];
					displayContents = encoded[1];
				}

			}
			break;

		case Contents.Type.LOCATION:
			Bundle locationBundle = (Bundle) object;
			if (locationBundle != null) {
				// These must use Bundle.getFloat(), not getDouble(), it's part
				// of the API.
				float latitude = locationBundle
						.getFloat("LAT", Float.MAX_VALUE);
				float longitude = locationBundle.getFloat("LONG",
						Float.MAX_VALUE);
				if (latitude != Float.MAX_VALUE && longitude != Float.MAX_VALUE) {
					contents = "geo:" + latitude + ',' + longitude;
					displayContents = latitude + "," + longitude;
				}
			}
			break;
		}
	}

	private static List<String> getAllBundleValues(Bundle bundle, String[] keys) {
		List<String> values = new ArrayList<>(keys.length);
		for (String key : keys) {
			Object value = bundle.get(key);
			values.add(value == null ? null : value.toString());
		}
		return values;
	}

	Bitmap encodeAsBitmap() throws WriterException {
		String contentsToEncode = contents;
		if (contentsToEncode == null) {
			return null;
		}
		Map<EncodeHintType, Object> hints = null;
		String encoding = guessAppropriateEncoding(contentsToEncode);
		if (encoding != null) {
			hints = new EnumMap<>(EncodeHintType.class);
			hints.put(EncodeHintType.CHARACTER_SET, encoding);
		}
		BitMatrix result;
		try {
			result = new MultiFormatWriter().encode(contentsToEncode, barcodeFormat,
					dimension, dimension, hints);
		} catch (IllegalArgumentException iae) {
			// Unsupported format
			return null;
		}
		int width = result.getWidth();
		int height = result.getHeight();
		int[] pixels = new int[width * height];
		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
		return bitmap;
	}

	private static String guessAppropriateEncoding(CharSequence contents) {
		// Very crude at the moment
		for (int i = 0; i < contents.length(); i++) {
			if (contents.charAt(i) > 0xFF) {
				return "UTF-8";
			}
		}
		return null;
	}

}
