package top.dreagonmon.app.dreamoonlauncher.window;

import android.annotation.SuppressLint;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.MalformedURLException;
import java.net.URL;

public class AppWebViewClient extends WebViewClient {
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return false;//Allow all url
    }
    @SuppressLint("WebViewClientOnReceivedSslError")
    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        // Allow Localhost https
        try {
            URL url = new URL(error.getUrl());
            if ("127.0.0.1".equals(url.getHost()) || "localhost".equals(url.getHost())){
                handler.proceed();
                return;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        handler.cancel();
    }
}
