package com.mylhyl.zxing.scanner.result;

import com.google.zxing.client.result.TextParsedResult;

/**
 * Created by hupei on 2016/8/12.
 */
public class TextResult extends Result {
    private final String text;
    private final String language;

    public TextResult(TextParsedResult textParsedResult) {
        this.text = textParsedResult.getText();
        this.language = textParsedResult.getLanguage();
    }

    public String getText() {
        return text;
    }

    public String getLanguage() {
        return language;
    }
}
