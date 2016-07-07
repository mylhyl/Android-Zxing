package com.mylhyl.zxing.scanner.sample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.google.zxing.client.result.URIParsedResult;
import com.mylhyl.zxing.scanner.OnScannerCompletionListener;
import com.mylhyl.zxing.scanner.common.Scanner;

/**
 * Created by hupei on 2016/7/7.
 */
public abstract class BasicScannerActivity extends AppCompatActivity implements OnScannerCompletionListener {
    public static final int REQUEST_CODE_SCANNER = 188;
    public static final String EXTRA_RETURN_SCANNER_RESULT = "return_scanner_result";
    public static final String EXTRA_RETURN_SCANNER_RESULT_TEXT = "return_scanner_result_text";

    private boolean mReturnScanResult;

    abstract void onResultActivity(Result result, ParsedResultType type, Bundle bundle);

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mReturnScanResult = extras.getBoolean(EXTRA_RETURN_SCANNER_RESULT);
        }
    }

    @Override
    public void OnScannerCompletion(Result rawResult, ParsedResult parsedResult, Bitmap barcode) {
        if (rawResult == null) {
            Toast.makeText(this, "未发现二维码", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (mReturnScanResult) {
            onReturnScanResult(rawResult);
            return;
        }
        Bundle bundle = new Bundle();
        ParsedResultType type = parsedResult.getType();
        switch (type) {
            case ADDRESSBOOK:
                AddressBookParsedResult addressResult = (AddressBookParsedResult) parsedResult;
                bundle.putStringArray(Scanner.result.EXTRA_RESULT_ADDRESS_BOOK_NAME, addressResult.getNames());
                bundle.putStringArray(Scanner.result.EXTRA_RESULT_ADDRESS_BOOK_PHONE_NUMBER, addressResult.getPhoneNumbers());
                bundle.putStringArray(Scanner.result.EXTRA_RESULT_ADDRESS_BOOK_EMAIL, addressResult.getEmails());
                break;
            case PRODUCT:
                break;
            case URI:
                URIParsedResult uriParsedResult = (URIParsedResult) parsedResult;
                bundle.putString(Scanner.result.EXTRA_RESULT_URI, uriParsedResult.getURI());
                break;
            case TEXT:
                bundle.putString(Scanner.result.EXTRA_RESULT_TEXT, rawResult.getText());
                break;
            case GEO:
                break;
            case TEL:
                break;
            case SMS:
                break;
        }
        onResultActivity(rawResult, type, bundle);
    }

    private void onReturnScanResult(Result rawResult) {
        Intent intent = getIntent();
        intent.putExtra(EXTRA_RETURN_SCANNER_RESULT_TEXT, rawResult.getText());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}
