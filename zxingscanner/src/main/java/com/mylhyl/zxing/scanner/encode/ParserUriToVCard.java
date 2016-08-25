package com.mylhyl.zxing.scanner.encode;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.ContactsContract;

/**
 * Created by hupei on 2016/8/25.
 */
public class ParserUriToVCard {

    public static final String URL_KEY = "URL_KEY";
    public static final String NOTE_KEY = "NOTE_KEY";

    public static final String[] PHONE_KEYS = {
            ContactsContract.Intents.Insert.PHONE,
            ContactsContract.Intents.Insert.SECONDARY_PHONE,
            ContactsContract.Intents.Insert.TERTIARY_PHONE
    };

    public static final String[] PHONE_TYPE_KEYS = {
            ContactsContract.Intents.Insert.PHONE_TYPE,
            ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE,
            ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE
    };

    public static final String[] EMAIL_KEYS = {
            ContactsContract.Intents.Insert.EMAIL,
            ContactsContract.Intents.Insert.SECONDARY_EMAIL,
            ContactsContract.Intents.Insert.TERTIARY_EMAIL
    };

    public ParserUriToVCard() {
    }

    public Bundle parserUri(Context context, Uri contactUri) {
        if (context == null || contactUri == null) return null;
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor;
        try {
            cursor = resolver.query(contactUri, null, null, null, null);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        if (cursor == null) return null;

        String id;
        String name;
        boolean hasPhone;
        try {
            if (!cursor.moveToFirst()) return null;

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
                    while (phonesCursor.moveToNext() && foundPhone < PHONE_KEYS.length) {
                        String number = phonesCursor.getString(phonesNumberColumn);
                        if (number != null && !number.isEmpty()) {
                            bundle.putString(PHONE_KEYS[foundPhone], massageContactData(number));
                        }
                        int type = phonesCursor.getInt(phoneTypeColumn);
                        bundle.putInt(PHONE_TYPE_KEYS[foundPhone], type);
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
                while (emailCursor.moveToNext() && foundEmail < EMAIL_KEYS.length) {
                    String email = emailCursor.getString(emailColumn);
                    if (email != null && !email.isEmpty()) {
                        bundle.putString(EMAIL_KEYS[foundEmail], massageContactData(email));
                    }
                    foundEmail++;
                }
            } finally {
                emailCursor.close();
            }
        }
        if (bundle.isEmpty()) return null;
        return bundle;
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
