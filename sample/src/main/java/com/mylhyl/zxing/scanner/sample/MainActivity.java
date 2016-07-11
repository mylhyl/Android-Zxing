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
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.zxing.client.result.ParsedResultType;
import com.mylhyl.zxing.scanner.common.Contents;
import com.mylhyl.zxing.scanner.common.Intents;
import com.mylhyl.zxing.scanner.encode.QREncode;

import java.io.ByteArrayOutputStream;

public class MainActivity extends BasicActivity {
    private static final int PICK_CONTACT = 1;
    private TextView tvResult;
    private ImageView imageView;
    private int laserMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvResult = (TextView) findViewById(R.id.textView);
        imageView = (ImageView) findViewById(R.id.imageView);

        final CheckBox checkBox = (CheckBox) findViewById(R.id.checkBox);
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioButton:
                        laserMode = EnCodeActivity.EXTRA_LASER_LINE_MODE_0;
                        break;
                    case R.id.radioButton2:
                        laserMode = EnCodeActivity.EXTRA_LASER_LINE_MODE_1;
                        break;
                    case R.id.radioButton3:
                        laserMode = EnCodeActivity.EXTRA_LASER_LINE_MODE_2;
                        break;
                }
            }
        });

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EnCodeActivity.gotoActivity(MainActivity.this,
                        checkBox.isChecked(), laserMode);
            }
        });

        final EditText editText = (EditText) findViewById(R.id.editText);

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String qrContent = editText.getText().toString();
                Bitmap bitmap = QREncode.encodeQR(MainActivity.this,
                        new QREncode.Builder()
                                //二维码颜色
                                .setColor(getResources().getColor(R.color.colorPrimary))
                                //二维码类型
                                .setParsedResultType(TextUtils.isEmpty(qrContent) ? ParsedResultType.URI : ParsedResultType.TEXT)
                                //二维码内容
                                .setContents(TextUtils.isEmpty(qrContent) ? "https://github.com/mylhyl" : qrContent)
                                .build());
                imageView.setImageBitmap(bitmap);
                tvResult.setText("单击识别图中二维码");

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

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setDrawingCacheEnabled(true);//step 1
                Bitmap bitmap = imageView.getDrawingCache();//step 2
                //step 3 转bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

                DeCodeActivity.gotoActivity(MainActivity.this, baos.toByteArray());//step 4
                imageView.setDrawingCacheEnabled(false);//step 5
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_CANCELED && resultCode == Activity.RESULT_OK) {
            if (requestCode == EnCodeActivity.REQUEST_CODE_SCANNER) {
                if (data != null) {
                    String stringExtra = data.getStringExtra(Intents.Scan.RESULT);
                    tvResult.setText(stringExtra);
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
        tvResult.setText("单击二维码图片识别");
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
