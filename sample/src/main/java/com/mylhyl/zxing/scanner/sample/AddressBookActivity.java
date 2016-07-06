package com.mylhyl.zxing.scanner.sample;

import android.os.Bundle;
import android.widget.TextView;

public class AddressBookActivity extends BasicActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_book);

        TextView textView = (TextView) findViewById(R.id.textView2);

        Bundle extras = getIntent().getExtras();

        String[] names = extras.getStringArray("name");
        String[] phoneNumbers = extras.getStringArray("phoneNumber");
        String[] emails = extras.getStringArray("email");

        StringBuffer sb = new StringBuffer();

        if (names != null && names.length > 0) {
            sb.append("姓名：").append(names[0]).append("\n");
        }

        if (phoneNumbers != null && phoneNumbers.length > 0) {
            sb.append("电话：").append(phoneNumbers[0]).append("\n");
        }

        if (emails != null && emails.length > 0) {
            sb.append("邮箱：").append(emails[0]);
        }

        textView.setText(sb.toString());
    }
}
