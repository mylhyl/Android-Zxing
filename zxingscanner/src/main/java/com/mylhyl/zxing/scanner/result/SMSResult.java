package com.mylhyl.zxing.scanner.result;

import com.google.zxing.client.result.SMSParsedResult;

/**
 * Created by hupei on 2016/8/12.
 */
public class SMSResult extends Result {
    private final String[] numbers;
    private final String[] vias;
    private final String subject;
    private final String body;

    public SMSResult(SMSParsedResult smsParsedResult) {
        this.numbers = smsParsedResult.getNumbers();
        this.vias = smsParsedResult.getVias();
        this.subject = smsParsedResult.getSubject();
        this.body = smsParsedResult.getBody();
    }

    public String[] getNumbers() {
        return numbers;
    }

    public String[] getVias() {
        return vias;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}
