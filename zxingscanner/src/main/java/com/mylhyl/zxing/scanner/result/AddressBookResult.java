package com.mylhyl.zxing.scanner.result;

import com.google.zxing.client.result.AddressBookParsedResult;

/**
 * Created by hupei on 2016/8/12.
 */
public class AddressBookResult extends Result {
    private final String[] names;
    private final String[] nicknames;
    private final String pronunciation;
    private final String[] phoneNumbers;
    private final String[] phoneTypes;
    private final String[] emails;
    private final String[] emailTypes;
    private final String instantMessenger;
    private final String note;
    private final String[] addresses;
    private final String[] addressTypes;
    private final String org;
    private final String birthday;
    private final String title;
    private final String[] urls;
    private final String[] geo;

    public AddressBookResult(AddressBookParsedResult addressBookParsedResult) {
        this.names = addressBookParsedResult.getNames();
        this.nicknames = addressBookParsedResult.getNicknames();
        this.pronunciation = addressBookParsedResult.getPronunciation();
        this.phoneNumbers = addressBookParsedResult.getPhoneNumbers();
        this.phoneTypes = addressBookParsedResult.getPhoneTypes();
        this.emails = addressBookParsedResult.getEmails();
        this.emailTypes = addressBookParsedResult.getEmailTypes();
        this.instantMessenger = addressBookParsedResult.getInstantMessenger();
        this.note = addressBookParsedResult.getNote();
        this.addresses = addressBookParsedResult.getAddresses();
        this.addressTypes = addressBookParsedResult.getAddressTypes();
        this.org = addressBookParsedResult.getOrg();
        this.birthday = addressBookParsedResult.getBirthday();
        this.title = addressBookParsedResult.getTitle();
        this.urls = addressBookParsedResult.getURLs();
        this.geo = addressBookParsedResult.getGeo();
    }

    public String[] getNames() {
        return names;
    }

    public String[] getNicknames() {
        return nicknames;
    }

    public String getPronunciation() {
        return pronunciation;
    }

    public String[] getPhoneNumbers() {
        return phoneNumbers;
    }

    public String[] getPhoneTypes() {
        return phoneTypes;
    }

    public String[] getEmails() {
        return emails;
    }

    public String[] getEmailTypes() {
        return emailTypes;
    }

    public String getInstantMessenger() {
        return instantMessenger;
    }

    public String getNote() {
        return note;
    }

    public String[] getAddresses() {
        return addresses;
    }

    public String[] getAddressTypes() {
        return addressTypes;
    }

    public String getOrg() {
        return org;
    }

    public String getBirthday() {
        return birthday;
    }

    public String getTitle() {
        return title;
    }

    public String[] getUrls() {
        return urls;
    }

    public String[] getGeo() {
        return geo;
    }
}
