package com.mylhyl.zxing.scanner.sample;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.WindowManager;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResultType;
import com.mylhyl.zxing.scanner.decode.QRDecode;

public class DeCodeActivity extends BasicScannerActivity {
    @Override
    void onResultActivity(Result result, ParsedResultType type, Bundle bundle) {
        switch (type) {
            case ADDRESSBOOK:
                AddressBookActivity.gotoActivity(DeCodeActivity.this, bundle);
                break;
            case PRODUCT:
                break;
            case URI:
                UriActivity.gotoActivity(DeCodeActivity.this, bundle);
                break;
            case TEXT:
                TextActivity.gotoActivity(DeCodeActivity.this, bundle);
                break;
            case GEO:
                break;
            case TEL:
                break;
            case SMS:
                break;
        }
        progressDialog.dismiss();
        finish();
    }

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("请稍候...");
        progressDialog.show();

//        Bundle extras = getIntent().getExtras();
//        if (extras != null) {
//            Bitmap bitmap = extras.getParcelable("bitmap");
//            if (bitmap != null) {
//                QRDecode.decodeQR(bitmap, this);
//            }
//        }
    }

    public static void gotoActivity(Activity activity, Bitmap bitmap) {
        activity.startActivity(new Intent(activity, DeCodeActivity.class).putExtra("bitmap", bitmap));
    }
}
