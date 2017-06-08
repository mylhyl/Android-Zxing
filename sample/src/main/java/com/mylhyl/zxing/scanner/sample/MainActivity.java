package com.mylhyl.zxing.scanner.sample;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.zxing.client.result.ParsedResultType;
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
                        laserMode = ScannerActivity.EXTRA_LASER_LINE_MODE_0;
                        break;
                    case R.id.radioButton2:
                        laserMode = ScannerActivity.EXTRA_LASER_LINE_MODE_1;
                        break;
                    case R.id.radioButton3:
                        laserMode = ScannerActivity.EXTRA_LASER_LINE_MODE_2;
                        break;
                }
            }
        });

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    //权限还没有授予，需要在这里写申请权限的代码
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, 60);
                } else {
                    //权限已经被授予，在这里直接写要执行的相应方法即可
                    ScannerActivity.gotoActivity(MainActivity.this,
                            checkBox.isChecked(), laserMode);
                }
            }
        });

        final EditText editText = (EditText) findViewById(R.id.editText);

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String qrContent = editText.getText().toString();
                Bitmap bitmap = QREncode.encodeQR(new QREncode.Builder(MainActivity.this)
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
            if (requestCode == ScannerActivity.REQUEST_CODE_SCANNER) {
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
        //可以自己组装bundle;
//        ParserUriToVCard parserUriToVCard = new ParserUriToVCard();
//        Bundle bundle = parserUriToVCard.parserUri(this, contactUri);
//        if (bundle != null) {
//            Bitmap bitmap = QREncode.encodeQR(new QREncode.Builder(this)
//                    .setParsedResultType(ParsedResultType.ADDRESSBOOK)
//                    .setBundle(bundle).build());
//            imageView.setImageBitmap(bitmap);
//            tvResult.setText("单击二维码图片识别");
//        } else tvResult.setText("联系人Uri错误");

        //只传Uri
        Bitmap bitmap = QREncode.encodeQR(new QREncode.Builder(this)
                .setParsedResultType(ParsedResultType.ADDRESSBOOK)
                .setAddressBookUri(contactUri).build());
        imageView.setImageBitmap(bitmap);
        tvResult.setText("单击二维码图片识别");
    }
}
