package com.mylhyl.zxing.scanner.sample;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.client.result.ParsedResultType;
import com.mylhyl.zxing.scanner.common.Contents;
import com.mylhyl.zxing.scanner.encode.QREncode;

public class MainActivity extends BasicActivity implements CompoundButton.OnCheckedChangeListener {
    private static final int PICK_CONTACT = 1;
    private ImageView imageView;
    private CheckBox checkBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.imageView);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MainActivity.this, ScannerActivity.class)
                        .putExtra(ScannerActivity.EXTRA_RETURN_SCANNER_RESULT, checkBox.isChecked()), ScannerActivity.REQUEST_CODE_SCANNER);
            }
        });
        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = QREncode.encodeQR(MainActivity.this,
                        new QREncode.Builder().setParsedResultType(ParsedResultType.URI)
                                .setColor(getResources().getColor(R.color.colorPrimary))
                                .setContents("https://github.com/mylhyl").build());
                imageView.setImageBitmap(bitmap);
            }
        });
        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                startActivityForResult(intent, PICK_CONTACT);
            }
        });
        checkBox = (CheckBox) findViewById(R.id.checkBox);
        checkBox.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == checkBox) {

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_CANCELED && resultCode == Activity.RESULT_OK) {
            if (requestCode == ScannerActivity.REQUEST_CODE_SCANNER) {
                if (data != null) {
                    String stringExtra = data.getStringExtra(ScannerActivity.EXTRA_SCANNER_RESULT_Text);
                    ((TextView) findViewById(R.id.textView)).setText(stringExtra);
                }
            } else if (requestCode == PICK_CONTACT) {
                // Data field is content://contacts/people/984
                showContactAsBarcode(data.getData());
            }
        }
    }

    /**
     * @param contactUri content://contacts/people/17
     */
    private void showContactAsBarcode(Uri contactUri) {
        if (contactUri == null) return;
        ContentResolver resolver = getContentResolver();
        Cursor cursor;
        try {
            cursor = resolver.query(contactUri, null, null, null, null);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        if (cursor == null) {
            return;
        }
        String id;
        String name;
        boolean hasPhone;
        try {
            if (!cursor.moveToFirst()) {
                return;
            }
            id = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
            name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            hasPhone = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0;

        } finally {
            cursor.close();
        }

        Bundle bundle = new Bundle();
        if (name != null && !name.isEmpty()) {
            bundle.putString(ContactsContract.Intents.Insert.NAME, massageContactData(name));
        }

        if (hasPhone) {
            Cursor phonesCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + '=' + id,
                    null,
                    null);
            if (phonesCursor != null) {
                try {
                    int foundPhone = 0;
                    int phonesNumberColumn = phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int phoneTypeColumn = phonesCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
                    while (phonesCursor.moveToNext() && foundPhone < Contents.PHONE_KEYS.length) {
                        String number = phonesCursor.getString(phonesNumberColumn);
                        if (number != null && !number.isEmpty()) {
                            bundle.putString(Contents.PHONE_KEYS[foundPhone], massageContactData(number));
                        }
                        int type = phonesCursor.getInt(phoneTypeColumn);
                        bundle.putInt(Contents.PHONE_TYPE_KEYS[foundPhone], type);
                        foundPhone++;
                    }
                } finally {
                    phonesCursor.close();
                }
            }
        }

        Cursor methodsCursor = resolver.query(ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID + '=' + id,
                null,
                null);
        if (methodsCursor != null) {
            try {
                if (methodsCursor.moveToNext()) {
                    String data = methodsCursor.getString(
                            methodsCursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
                    if (data != null && !data.isEmpty()) {
                        bundle.putString(ContactsContract.Intents.Insert.POSTAL, massageContactData(data));
                    }
                }
            } finally {
                methodsCursor.close();
            }
        }

        Cursor emailCursor = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + '=' + id,
                null,
                null);
        if (emailCursor != null) {
            try {
                int foundEmail = 0;
                int emailColumn = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA);
                while (emailCursor.moveToNext() && foundEmail < Contents.EMAIL_KEYS.length) {
                    String email = emailCursor.getString(emailColumn);
                    if (email != null && !email.isEmpty()) {
                        bundle.putString(Contents.EMAIL_KEYS[foundEmail], massageContactData(email));
                    }
                    foundEmail++;
                }
            } finally {
                emailCursor.close();
            }
        }

        Bitmap bitmap = QREncode.encodeQR(MainActivity.this, new QREncode.Builder()
                .setParsedResultType(ParsedResultType.ADDRESSBOOK)
                .setBundle(bundle).build());
        imageView.setImageBitmap(bitmap);
    }

    private static String massageContactData(String data) {
        if (data.indexOf('\n') >= 0) {
            data = data.replace("\n", " ");
        }
        if (data.indexOf('\r') >= 0) {
            data = data.replace("\r", " ");
        }
        return data;
    }
}
