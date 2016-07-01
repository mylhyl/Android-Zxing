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

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultMetadataType;
import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.result.ResultButtonListener;
import com.google.zxing.client.android.result.ResultHandler;
import com.google.zxing.client.android.result.ResultHandlerFactory;
import com.google.zxing.client.android.result.supplement.SupplementalInfoRetriever;
import com.mylhyl.zxing.scanner.OnScannerCompletionListener;
import com.mylhyl.zxing.scanner.ScannerView;
import com.mylhyl.zxing.scanner.common.Intents;
import com.mylhyl.zxing.scanner.common.Scanner;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity implements OnScannerCompletionListener {

    private static final String TAG = CaptureActivity.class.getSimpleName();

    private static final long DEFAULT_INTENT_RESULT_DURATION_MS = 1500L;
    private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;

    private static final String[] ZXING_URLS = {
            "http://zxing.appspot.com/scan", "zxing://scan/"};

    private static final Collection<ResultMetadataType> DISPLAYABLE_METADATA_TYPES = EnumSet
            .of(ResultMetadataType.ISSUE_NUMBER,
                    ResultMetadataType.SUGGESTED_PRICE,
                    ResultMetadataType.ERROR_CORRECTION_LEVEL,
                    ResultMetadataType.POSSIBLE_COUNTRY);
    private ScannerView scannerView;
    private TextView statusView;
    private View resultView;
    private Result lastResult;
    private IntentSource source;
    private String sourceUrl;
    private ScanFromWebPageManager scanFromWebPageManager;
    private InactivityTimer inactivityTimer;


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.capture);

        inactivityTimer = new InactivityTimer(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        scannerView = (ScannerView) findViewById(R.id.capture_view);
        resultView = findViewById(R.id.result_view);
        statusView = (TextView) findViewById(R.id.status_view);
        
        scannerView.setMediaResId(R.raw.beep);
        scannerView.setOnScannerCompletionListener(this);

        scannerView.onResume();

        lastResult = null;

        setCurrentOrientation();

        resetStatusView();

        inactivityTimer.onResume();

        source = IntentSource.NONE;
        sourceUrl = null;
        scanFromWebPageManager = null;
    }

    private void setCurrentOrientation() {
        // 不自动旋转
        boolean autoOrientation = true;
        if (autoOrientation) {
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                switch (rotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_90:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    default:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
            } else {
                switch (rotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_270:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    default:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                }
            }
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
    }

    private static boolean isZXingURL(String dataString) {
        if (dataString == null) {
            return false;
        }
        for (String url : ZXING_URLS) {
            if (dataString.startsWith(url)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        scannerView.onPause();
        inactivityTimer.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (source == IntentSource.NATIVE_APP_INTENT) {
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                }
                if ((source == IntentSource.NONE || source == IntentSource.ZXING_LINK)
                        && lastResult != null) {
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                scannerView.toggleLight(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                scannerView.toggleLight(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void OnCompletion(Result rawResult, Bitmap barcode) {
        inactivityTimer.onActivity();
        lastResult = rawResult;
        ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(
                this, rawResult);

        boolean fromLiveScan = barcode != null;

        switch (source) {
            case NATIVE_APP_INTENT:
            case PRODUCT_SEARCH_LINK:
                handleDecodeExternally(rawResult, resultHandler, barcode);
                break;
            case ZXING_LINK:
                if (scanFromWebPageManager == null
                        || !scanFromWebPageManager.isScanFromWebPage()) {
                    handleDecodeInternally(rawResult, resultHandler, barcode);
                } else {
                    handleDecodeExternally(rawResult, resultHandler, barcode);
                }
                break;
            case NONE:
                // 批量扫描模式
                boolean bulkMode = false;
                if (fromLiveScan && bulkMode) {
                    Toast.makeText(
                            getApplicationContext(),
                            getResources()
                                    .getString(R.string.msg_bulk_mode_scanned)
                                    + " (" + rawResult.getText() + ')',
                            Toast.LENGTH_SHORT).show();
                    // Wait a moment or else it will scan the same barcode
                    // continuously about 3 times
                    restartPreviewAfterDelay(BULK_MODE_SCAN_DELAY_MS);
                } else {
                    handleDecodeInternally(rawResult, resultHandler, barcode);
                }
                break;
        }
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of
     * the barcode.
     *
     * @param barcode     A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult   The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor,
                                  Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4
                    && (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A || rawResult
                    .getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and
                // metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(),
                                scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a,
                                 ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(), scaleFactor * a.getY(),
                    scaleFactor * b.getX(), scaleFactor * b.getY(), paint);
        }
    }

    // Put up our own UI for how to handle the decoded contents.
    private void handleDecodeInternally(Result rawResult,
                                        ResultHandler resultHandler, Bitmap barcode) {

        CharSequence displayContents = resultHandler.getDisplayContents();

        // 自动打开网页
        boolean autoOpenWeb = false;
        if (resultHandler.getDefaultButtonID() != null && autoOpenWeb) {
            resultHandler.handleButtonPress(resultHandler.getDefaultButtonID());
            return;
        }

        statusView.setVisibility(View.GONE);
        scannerView.getViewfinderView().setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);

        ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
        if (barcode == null) {
            barcodeImageView.setImageBitmap(BitmapFactory.decodeResource(
                    getResources(), R.drawable.launcher_icon));
        } else {
            barcodeImageView.setImageBitmap(barcode);
        }

        TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
        formatTextView.setText(rawResult.getBarcodeFormat().toString());

        TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
        typeTextView.setText(resultHandler.getType().toString());

        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                DateFormat.SHORT);
        TextView timeTextView = (TextView) findViewById(R.id.time_text_view);
        timeTextView
                .setText(formatter.format(new Date(rawResult.getTimestamp())));

        TextView metaTextView = (TextView) findViewById(R.id.meta_text_view);
        View metaTextViewLabel = findViewById(R.id.meta_text_view_label);
        metaTextView.setVisibility(View.GONE);
        metaTextViewLabel.setVisibility(View.GONE);
        Map<ResultMetadataType, Object> metadata = rawResult
                .getResultMetadata();
        if (metadata != null) {
            StringBuilder metadataText = new StringBuilder(20);
            for (Map.Entry<ResultMetadataType, Object> entry : metadata
                    .entrySet()) {
                if (DISPLAYABLE_METADATA_TYPES.contains(entry.getKey())) {
                    metadataText.append(entry.getValue()).append('\n');
                }
            }
            if (metadataText.length() > 0) {
                metadataText.setLength(metadataText.length() - 1);
                metaTextView.setText(metadataText);
                metaTextView.setVisibility(View.VISIBLE);
                metaTextViewLabel.setVisibility(View.VISIBLE);
            }
        }

        TextView contentsTextView = (TextView) findViewById(R.id.contents_text_view);
        contentsTextView.setText(displayContents);
        int scaledSize = Math.max(22, 32 - displayContents.length() / 4);
        contentsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

        TextView supplementTextView = (TextView) findViewById(R.id.contents_supplement_text_view);
        supplementTextView.setText("");
        supplementTextView.setOnClickListener(null);
        // 检索更多信息
        boolean supplemental = true;
        if (supplemental) {
            SupplementalInfoRetriever.maybeInvokeRetrieval(supplementTextView,
                    resultHandler.getResult(), this);
        }

        int buttonCount = resultHandler.getButtonCount();
        ViewGroup buttonView = (ViewGroup) findViewById(R.id.result_button_view);
        buttonView.requestFocus();
        for (int x = 0; x < ResultHandler.MAX_BUTTON_COUNT; x++) {
            TextView button = (TextView) buttonView.getChildAt(x);
            if (x < buttonCount) {
                button.setVisibility(View.VISIBLE);
                button.setText(resultHandler.getButtonText(x));
                button.setOnClickListener(new ResultButtonListener(
                        resultHandler, x));
            } else {
                button.setVisibility(View.GONE);
            }
        }

    }

    // Briefly show the contents of the barcode, then handle the result outside
    // Barcode Scanner.
    private void handleDecodeExternally(Result rawResult,
                                        ResultHandler resultHandler, Bitmap barcode) {

        if (barcode != null) {
            scannerView.getViewfinderView().drawResultBitmap(barcode);
        }

        long resultDurationMS;
        if (getIntent() == null) {
            resultDurationMS = DEFAULT_INTENT_RESULT_DURATION_MS;
        } else {
            resultDurationMS = getIntent().getLongExtra(
                    Intents.Scan.RESULT_DISPLAY_DURATION_MS,
                    DEFAULT_INTENT_RESULT_DURATION_MS);
        }

        if (resultDurationMS > 0) {
            String rawResultString = String.valueOf(rawResult);
            if (rawResultString.length() > 32) {
                rawResultString = rawResultString.substring(0, 32) + " ...";
            }
            statusView.setText(getString(resultHandler.getDisplayTitle())
                    + " : " + rawResultString);
        }

        if (source == IntentSource.NATIVE_APP_INTENT) {

            // Hand back whatever action they requested - this can be changed to
            // Intents.Scan.ACTION when
            // the deprecated intent is retired.
            Intent intent = new Intent(getIntent().getAction());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
            intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult
                    .getBarcodeFormat().toString());
            byte[] rawBytes = rawResult.getRawBytes();
            if (rawBytes != null && rawBytes.length > 0) {
                intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
            }
            Map<ResultMetadataType, ?> metadata = rawResult.getResultMetadata();
            if (metadata != null) {
                if (metadata.containsKey(ResultMetadataType.UPC_EAN_EXTENSION)) {
                    intent.putExtra(Intents.Scan.RESULT_UPC_EAN_EXTENSION,
                            metadata.get(ResultMetadataType.UPC_EAN_EXTENSION)
                                    .toString());
                }
                Number orientation = (Number) metadata
                        .get(ResultMetadataType.ORIENTATION);
                if (orientation != null) {
                    intent.putExtra(Intents.Scan.RESULT_ORIENTATION,
                            orientation.intValue());
                }
                String ecLevel = (String) metadata
                        .get(ResultMetadataType.ERROR_CORRECTION_LEVEL);
                if (ecLevel != null) {
                    intent.putExtra(Intents.Scan.RESULT_ERROR_CORRECTION_LEVEL,
                            ecLevel);
                }
                @SuppressWarnings("unchecked")
                Iterable<byte[]> byteSegments = (Iterable<byte[]>) metadata
                        .get(ResultMetadataType.BYTE_SEGMENTS);
                if (byteSegments != null) {
                    int i = 0;
                    for (byte[] byteSegment : byteSegments) {
                        intent.putExtra(
                                Intents.Scan.RESULT_BYTE_SEGMENTS_PREFIX + i,
                                byteSegment);
                        i++;
                    }
                }
            }
            sendReplyMessage(Scanner.RETURN_SCAN_RESULT, intent, resultDurationMS);

        } else if (source == IntentSource.PRODUCT_SEARCH_LINK) {

            // Reformulate the URL which triggered us into a query, so that the
            // request goes to the same
            // TLD as the scan URL.
            int end = sourceUrl.lastIndexOf("/scan");
            String replyURL = sourceUrl.substring(0, end) + "?q="
                    + resultHandler.getDisplayContents() + "&source=zxing";
            sendReplyMessage(Scanner.LAUNCH_PRODUCT_QUERY, replyURL,
                    resultDurationMS);

        } else if (source == IntentSource.ZXING_LINK) {

            if (scanFromWebPageManager != null
                    && scanFromWebPageManager.isScanFromWebPage()) {
                String replyURL = scanFromWebPageManager.buildReplyURL(
                        rawResult, resultHandler);
                scanFromWebPageManager = null;
                sendReplyMessage(Scanner.LAUNCH_PRODUCT_QUERY, replyURL,
                        resultDurationMS);
            }

        }
    }

    private void sendReplyMessage(int id, Object arg, long delayMS) {
        scannerView.sendReplyMessage(id, arg, delayMS);
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        scannerView.restartPreviewAfterDelay(delayMS);
        resetStatusView();
    }

    private void resetStatusView() {
        resultView.setVisibility(View.GONE);
        statusView.setText(R.string.msg_default_status);
        statusView.setVisibility(View.VISIBLE);
        scannerView.getViewfinderView().setVisibility(View.VISIBLE);
        lastResult = null;
    }

}
