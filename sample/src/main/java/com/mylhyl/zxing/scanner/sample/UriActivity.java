package com.mylhyl.zxing.scanner.sample;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mylhyl.crlayout.SwipeRefreshWebView;

public class UriActivity extends BasicActivity {
    private SwipeRefreshWebView swipeRefreshWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_uri);
        String uri = getIntent().getStringExtra("uri");
        swipeRefreshWebView = (SwipeRefreshWebView) findViewById(R.id.webView);
        swipeRefreshWebView.getScrollView().loadUrl(uri);
        swipeRefreshWebView.getScrollView().setWebViewClient(new SampleWebViewClient());
    }

    private class SampleWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            swipeRefreshWebView.showProgressView();
            return true;
        }
    }
}
