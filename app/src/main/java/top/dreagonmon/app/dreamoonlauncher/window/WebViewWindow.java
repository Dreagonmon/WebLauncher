package top.dreagonmon.app.dreamoonlauncher.window;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.MalformedURLException;
import java.net.URL;

import top.dreagonmon.app.dreamoonlauncher.BuildConfig;
import top.dreagonmon.app.dreamoonlauncher.MainActivity;
import top.dreagonmon.app.dreamoonlauncher.control.ChromeClient;

public class WebViewWindow {
    private final ChromeClient browserCore;
    private WebView web;
    public WebViewWindow(MainActivity viewContext) {
        this.browserCore = new ChromeClient(viewContext);
        this.web = new WebView(viewContext);
        config(this.web, viewContext);
    }

    /*Config webView*/
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void config(WebView web, MainActivity viewContext){
        web.setBackgroundColor(Color.TRANSPARENT);
        // Handle URL Loading
        web.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;//Allow all url
            }
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
        });
        // Handle Browser Event
        web.setWebChromeClient(browserCore);
        // Enable Image and JavaScript
        WebSettings setting = web.getSettings();
        setting.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        setting.setBlockNetworkImage(false);
        setting.setLoadsImagesAutomatically(true);
        setting.setJavaScriptEnabled(true);
        setting.setJavaScriptCanOpenWindowsAutomatically(true);
        setting.setSupportMultipleWindows(true);
        String ua = setting.getUserAgentString();
        ua = String.format("%1$s WebLauncher/%2$s (port %3$s)",
                ua,
                BuildConfig.VERSION_CODE,
                viewContext.getGlobalStorage().configControl.getServerPort()
        );
        setting.setUserAgentString(ua);
    }

    /*get WebView*/
    public WebView getWebView(){
        return web;
    }
    /*load url*/
    public void loadURL(String url){
        web.loadUrl(url);
    }

    public void goBack() {
        if (web.canGoBack())
            web.goBack();
    }
}
