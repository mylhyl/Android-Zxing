package com.mylhyl.zxing.scanner.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.mylhyl.zxing.scanner.common.Scanner;

public class AddressBookActivity extends BasicActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_book);

        TextView textView = (TextView) findViewById(R.id.textView2);

        Bundle extras = getIntent().getExtras();

        String[] names = extras.getStringArray(Scanner.result.EXTRA_RESULT_ADDRESS_BOOK_NAME);
        String[] phoneNumbers = extras.getStringArray(Scanner.result.EXTRA_RESULT_ADDRESS_BOOK_PHONE_NUMBER);
        String[] emails = extras.getStringArray(Scanner.result.EXTRA_RESULT_ADDRESS_BOOK_EMAIL);

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

    public static void gotoActivity(Activity activity, Bundle bundle) {
        activity.startActivity(new Intent(activity, AddressBookActivity.class).putExtras(bundle));
    }
}
