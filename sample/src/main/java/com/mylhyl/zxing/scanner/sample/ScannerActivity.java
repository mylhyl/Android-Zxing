package com.mylhyl.zxing.scanner.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.CompoundButton;
import android.widget.ToggleButton;

import com.google.zxing.client.result.ParsedResultType;
import com.mylhyl.zxing.scanner.ScannerView;

public class ScannerActivity extends BasicScannerActivity {

    @Override
    int findScannerViewId() {
        return R.id.capture_view;
    }

    @Override
    void gotoScannerActivity(ParsedResultType type, Bundle bundle) {
        switch (type) {
            case ADDRESSBOOK:
                startActivity(new Intent(ScannerActivity.this, AddressBookActivity.class).putExtras(bundle));
                break;
            case PRODUCT:
                break;
            case URI:
                startActivity(new Intent(ScannerActivity.this, UriActivity.class).putExtras(bundle));
                break;
            case TEXT:
                startActivity(new Intent(ScannerActivity.this, TextActivity.class).putExtras(bundle));
                break;
            case GEO:
                break;
            case TEL:
                break;
            case SMS:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        ToggleButton toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mScannerView.toggleLight(isChecked);
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
}
