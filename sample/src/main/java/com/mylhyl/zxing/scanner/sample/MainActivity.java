package com.mylhyl.zxing.scanner.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, ScannerActivity.class)
                        .putExtra(ScannerActivity.EXTRA_RETURN_SCANNER_RESULT, true), ScannerActivity.REQUEST_CODE_SCANNER);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_CANCELED && requestCode == ScannerActivity.REQUEST_CODE_SCANNER
                && resultCode == Activity.RESULT_OK && data != null) {
            String stringExtra = data.getStringExtra(ScannerActivity.EXTRA_SCANNER_RESULT_Text);
            ((TextView) findViewById(R.id.textView)).setText(stringExtra);
        }
    }
}
