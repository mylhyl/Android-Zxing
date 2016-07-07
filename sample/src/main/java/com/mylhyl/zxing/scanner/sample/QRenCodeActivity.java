package com.mylhyl.zxing.scanner.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResultType;
import com.mylhyl.zxing.scanner.ScannerView;
import com.mylhyl.zxing.scanner.decode.QRDecode;
import com.mylhyl.zxing.scanner.sample.picture.PickPictureTotalActivity;

public class QRenCodeActivity extends BasicScannerActivity {

    @Override
    void onResultActivity(Result result, ParsedResultType type, Bundle bundle) {
        mLastResult = result;
        switch (type) {
            case ADDRESSBOOK:
                AddressBookActivity.gotoActivity(QRenCodeActivity.this, bundle);
                break;
            case PRODUCT:
                break;
            case URI:
                UriActivity.gotoActivity(QRenCodeActivity.this, bundle);
                break;
            case TEXT:
                TextActivity.gotoActivity(QRenCodeActivity.this, bundle);
                break;
            case GEO:
                break;
            case TEL:
                break;
            case SMS:
                break;
        }
        finish();
    }

    private ScannerView mScannerView;
    private Result mLastResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_encode);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mScannerView = (ScannerView) findViewById(R.id.capture_view);
        mScannerView.setOnScannerCompletionListener(this);

        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mScannerView.toggleLight(isChecked);
            }
        });

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PickPictureTotalActivity.gotoActivity(QRenCodeActivity.this);
            }
        });

//        scannerView.setLaserFrameTopMargin(100);
//        scannerView.setLaserFrameSize(200,200);
//        scannerView.setLaserFrameCornerLength(25);//设置4角长度
        mScannerView.setMediaResId(R.raw.beep);//设置扫描成功的声音
//        scannerView.setLaserLineHeight(5);//设置扫描线高度

        mScannerView.setLaserLineResId(R.mipmap.wx_scan_line);//线图
//        scannerView.setLaserLineResId(R.mipmap.zfb_grid_scan_line, true);//网格图
//        scannerView.setLaserFrameBoundColor(0xFF26CEFF);//支付宝颜色
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
                if (mLastResult != null) {
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
        mLastResult = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_CANCELED && resultCode == Activity.RESULT_OK) {
            if (requestCode == PickPictureTotalActivity.REQUEST_CODE_SELECT_PICTURE) {
                String picturePath = data.getStringExtra(PickPictureTotalActivity.EXTRA_PICTURE_PATH);
                QRDecode.decodeQR(picturePath, this);
            }
        }
    }
}
