package com.mylhyl.zxing.scanner.sample;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Surface;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ParsedResultType;
import com.mylhyl.zxing.scanner.OnScannerCompletionListener;
import com.mylhyl.zxing.scanner.ScannerView;

public class ScannerActivity extends AppCompatActivity implements OnScannerCompletionListener {
    public static final int REQUEST_CODE_SCANNER = 188;
    public static final String EXTRA_RETURN_SCANNER_RESULT = "return_scanner_result";
    public static final String EXTRA_SCANNER_RESULT_Text = "scanner_result_text";

    private ScannerView scannerView;
    private TextView statusView;
    private Result lastResult;
    private boolean returnScanResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scannerView.toggleLight(isChecked);
            }
        });

        scannerView = (ScannerView) findViewById(R.id.capture_view);
//        scannerView.setLaserFrameTopMargin(100);
//        scannerView.setLaserFrameSize(200,200);
//        scannerView.setLaserFrameCornerLength(25);//设置4角长度
        scannerView.setMediaResId(R.raw.beep);//设置扫描成功的声音
//        scannerView.setLaserLineHeight(5);//设置扫描线高度

        scannerView.setLaserLineResId(R.mipmap.wx_scan_line);//线图
//        scannerView.setLaserLineResId(R.mipmap.zfb_grid_scan_line, true);//网格图
//        scannerView.setLaserFrameBoundColor(0xFF26CEFF);//支付宝颜色

        scannerView.setOnScannerCompletionListener(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            returnScanResult = extras.getBoolean(EXTRA_RETURN_SCANNER_RESULT);
        }
    }

    @Override
    public void OnScannerCompletion(Result rawResult, ParsedResult parsedResult, Bitmap barcode) {
        lastResult = rawResult;

        ParsedResultType type = parsedResult.getType();
        switch (type) {
            case ADDRESSBOOK:
                break;
            case PRODUCT:
                break;
            case URI:
                break;
            case TEXT:
//                returnScanResult = true;
//                if (returnScanResult) {
//                    onReturnScanResult(rawResult);
//                }
                break;
            case GEO:
                break;
            case TEL:
                break;
            case SMS:
                break;
        }
        Toast.makeText(ScannerActivity.this, type.toString(), Toast.LENGTH_SHORT).show();
    }

    private void onReturnScanResult(Result rawResult) {
        Intent intent = getIntent();
        intent.putExtra(EXTRA_SCANNER_RESULT_Text, rawResult.getText());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        scannerView.onResume();
        lastResult = null;
        resetStatusView();
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

    private void restartPreviewAfterDelay(long delayMS) {
        scannerView.restartPreviewAfterDelay(delayMS);
        resetStatusView();
    }

    private void resetStatusView() {
        lastResult = null;
    }
}
