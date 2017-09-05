package com.mylhyl.zxing.scanner.sample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Window;
import android.widget.Toast;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;
import com.mylhyl.zxing.scanner.OnScannerCompletionListener;
import com.mylhyl.zxing.scanner.ScannerView;
import com.mylhyl.zxing.scanner.common.Scanner;

public class TestMainActivity extends Activity implements OnScannerCompletionListener {
    public static void gotoActivity(Activity activity) {
        activity.startActivity(new Intent(activity,TestMainActivity.class));
    }
    private ScannerView mScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_test_main);

        mScannerView = (ScannerView) findViewById(R.id.scanner_view);

        mScannerView.setLaserFrameSize(256, 256)
                .setLaserFrameCornerLength(22)
                .setLaserFrameCornerWidth(2)
                .setLaserFrameBoundColor(0xff06c1ae)
                .setLaserColor(0xff06c1ae)
                .setLaserLineHeight(8)
                .toggleLight(false)
                .setDrawText("请联系其它已添加该设备用户获取二维码", 12, 0x000000, true, 19)
                .setLaserFrameTopMargin(10);


        mScannerView.setOnScannerCompletionListener(this);
    }


    @Override
    protected void onResume() {
        mScannerView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mScannerView.onPause();
        super.onPause();
    }

    @Override
    public void OnScannerCompletion(Result rawResult, ParsedResult parsedResult, Bitmap barcode) {
        Toast.makeText(this, rawResult.getText(), Toast.LENGTH_SHORT).show();
    }
}
