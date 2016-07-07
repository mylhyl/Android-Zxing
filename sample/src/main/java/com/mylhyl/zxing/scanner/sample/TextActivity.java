package com.mylhyl.zxing.scanner.sample;

import android.os.Bundle;
import android.widget.TextView;

public class TextActivity extends BasicActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        Bundle extras = getIntent().getExtras();
        if (extras != null)
            ((TextView) findViewById(R.id.textView3)).setText(extras.getString("text"));
    }
}
