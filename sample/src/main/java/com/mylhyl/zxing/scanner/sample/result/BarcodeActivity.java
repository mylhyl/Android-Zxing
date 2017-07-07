package com.mylhyl.zxing.scanner.sample.result;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.litesuits.http.LiteHttp;
import com.litesuits.http.exception.HttpException;
import com.litesuits.http.listener.HttpListener;
import com.litesuits.http.request.AbstractRequest;
import com.litesuits.http.request.StringRequest;
import com.litesuits.http.request.param.HttpMethods;
import com.litesuits.http.response.Response;
import com.mylhyl.zxing.scanner.common.Scanner;
import com.mylhyl.zxing.scanner.result.ISBNResult;
import com.mylhyl.zxing.scanner.result.ProductResult;
import com.mylhyl.zxing.scanner.sample.BasicActivity;
import com.mylhyl.zxing.scanner.sample.R;
import com.mylhyl.zxing.scanner.sample.entities.Barcode;

import java.io.Serializable;

public class BarcodeActivity extends BasicActivity {
    private static final String TAG = "BarcodeActivity";
    private ImageView mImageView;
    private TextView mTextView4, mTextView5, mTextView6, mTextView7, mTextView8, mTextView9;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        mTextView4 = (TextView) findViewById(R.id.textView4);
        mTextView5 = (TextView) findViewById(R.id.textView5);
        mTextView6 = (TextView) findViewById(R.id.textView6);
        mTextView7 = (TextView) findViewById(R.id.textView7);
        mTextView8 = (TextView) findViewById(R.id.textView8);
        mTextView9 = (TextView) findViewById(R.id.textView9);
        mImageView = (ImageView) findViewById(R.id.imageView2);

        Bundle extras = getIntent().getExtras();
        if (extras == null) finish();

        Serializable serializable = extras.getSerializable(Scanner.Scan.RESULT);
        if (serializable == null) finish();

        String productID = "";
        if (serializable instanceof ProductResult) {
            ProductResult productResult = (ProductResult) serializable;
            productID = productResult.getProductID();
        } else if (serializable instanceof ISBNResult) {
            ISBNResult isbnResult = (ISBNResult) serializable;
            productID = isbnResult.getISBN();
        }

        LiteHttp liteHttp = LiteHttp.build(this)
                .setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .create();
        executeAsync(liteHttp, productID);

    }

    private void executeAsync(LiteHttp liteHttp, String productID) {
        //如果查询失败，请到 http://apistore.baidu.com/apiworks/servicedetail/1477.html 填写自己的apikey
        StringRequest stringRequest = new StringRequest("http://apis.baidu.com/3023/barcode/barcode")
                .setMethod(HttpMethods.Get)
                .addHeader("apikey", "3cec8d8175109823b986b178803738db")
                .addUrlParam("barcode", productID)
                .setHttpListener(new HttpListener<String>() {
                    @Override
                    public void onLoading(AbstractRequest<String> request, long total, long len) {
                        super.onLoading(request, total, len);
                    }

                    @Override
                    public void onSuccess(String s, Response<String> response) {
                        String result = response.getResult();
                        if (!TextUtils.isEmpty(result)) onSuccessDone(result);
                    }

                    @Override
                    public void onFailure(HttpException e, Response<String> response) {
                        String result = response.getResult();
                        Toast.makeText(BarcodeActivity.this, result, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onStart(AbstractRequest<String> request) {
                        super.onStart(request);
                    }

                    @Override
                    public void onUploading(AbstractRequest<String> request, long total, long len) {
                        super.onUploading(request, total, len);
                    }
                });
        liteHttp.executeAsync(stringRequest);
    }

    private void onSuccessDone(String result) {
        Log.i(TAG, "onSuccessDone: " + result);
        Barcode barcode = JSON.parseObject(result, Barcode.class);
        if (barcode == null) {
            Log.e(TAG, "product is null");
            return;
        }
        mTextView4.setText("名称：" + barcode.name);
        mTextView5.setText("价格：" + barcode.price);
        mTextView6.setText("规格：" + barcode.spec);
        mTextView7.setText("品牌：" + barcode.brand);
        mTextView8.setText("国家：" + barcode.country);
        mTextView9.setText("公司：" + barcode.company);
        Glide.with(this).load(barcode.gtin).into(mImageView);
    }

    public static void gotoActivity(Activity activity, Bundle bundle) {
        activity.startActivity(new Intent(activity, BarcodeActivity.class).putExtras(bundle));
    }
}
