package com.mylhyl.zxing.scanner.result;

import com.google.zxing.client.result.URIParsedResult;

/**
 * Created by hupei on 2016/8/12.
 */
public class URIResult extends Result {
    private final String uri;
    private final String title;

    public URIResult(URIParsedResult uriParsedResult) {
        this.uri = uriParsedResult.getURI();
        this.title = uriParsedResult.getTitle();
    }

    public String getUri() {
        return uri;
    }

    public String getTitle() {
        return title;
    }
}
