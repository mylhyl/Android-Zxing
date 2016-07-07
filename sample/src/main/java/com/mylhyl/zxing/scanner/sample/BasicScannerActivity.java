package com.mylhyl.zxing.scanner.sample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.URIParsedResult;
import com.mylhyl.zxing.scanner.OnScannerCompletionListener;
import com.mylhyl.zxing.scanner.ScannerView;

/**
 * Created by hupei on 2016/7/7.
 */
public abstract class BasicScannerActivity extends AppCompatActivity implements OnScannerCompletionListener {
    public static final int REQUEST_CODE_SCANNER = 188;
    public static final String EXTRA_RETURN_SCANNER_RESULT = "return_scanner_result";
    public static final String EXTRA_SCANNER_RESULT_Text = "scanner_result_text";

    ScannerView mScannerView;
    private Result lastResult;
    private boolean returnScanResult;

    abstract int findScannerViewId();

    abstract void gotoScannerActivity(ParsedResultType type, Bundle bundle);

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        mScannerView = (ScannerView) findViewById(findScannerViewId());
        mScannerView.setOnScannerCompletionListener(this);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            returnScanResult = extras.getBoolean(EXTRA_RETURN_SCANNER_RESULT);
        }
    }

    @Override
    public void OnScannerCompletion(Result rawResult, ParsedResult parsedResult, Bitmap barcode) {
        lastResult = rawResult;
        if (returnScanResult) {
            onReturnScanResult(rawResult);
            return;
        }
        Bundle bundle = new Bundle();
        ParsedResultType type = parsedResult.getType();
        switch (type) {
            case ADDRESSBOOK:
                AddressBookParsedResult addressResult = (AddressBookParsedResult) parsedResult;
                bundle.putStringArray("name", addressResult.getNames());
                bundle.putStringArray("phoneNumber", addressResult.getPhoneNumbers());
                bundle.putStringArray("email", addressResult.getEmails());
                break;
            case PRODUCT:
                break;
            case URI:
                URIParsedResult uriParsedResult = (URIParsedResult) parsedResult;
                bundle.putString("uri", uriParsedResult.getURI());
                break;
            case TEXT:
                bundle.putString("text", rawResult.getText());
                break;
            case GEO:
                break;
            case TEL:
                break;
            case SMS:
                break;
        }
        gotoScannerActivity(type, bundle);
    }

    private void onReturnScanResult(Result rawResult) {
        Intent intent = getIntent();
        intent.putExtra(EXTRA_SCANNER_RESULT_Text, rawResult.getText());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onResume() {
        mScannerView.onResume();
        resetStatusView();
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (lastResult != null) {
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void restartPreviewAfterDelay(long delayMS) {
        mScannerView.restartPreviewAfterDelay(delayMS);
        resetStatusView();
    }

    private void resetStatusView() {
        lastResult = null;
    }
}
