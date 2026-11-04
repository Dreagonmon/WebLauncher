package top.dreagonmon.app.dreamoonlauncher.window;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.webkit.ServiceWorkerController;
import android.webkit.WebSettings;
import android.webkit.WebView;

import top.dreagonmon.app.dreamoonlauncher.BuildConfig;
import top.dreagonmon.app.dreamoonlauncher.MainActivity;

public class WebViewWindow {
    private WebView web;
    public WebViewWindow(MainActivity viewContext) {
        this.web = new WebView(viewContext);
        config(this.web, viewContext);
    }

    /*Config webView*/
    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void config(WebView web, MainActivity viewContext){
        web.setBackgroundColor(Color.TRANSPARENT);
        // Handle URL Loading
        web.setWebViewClient(new AppWebViewClient());
        // Handle Browser Event
        web.setWebChromeClient(new AppChromeClient(viewContext));
        // Enable Image and JavaScript
        WebSettings setting = web.getSettings();
        setting.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        setting.setLoadsImagesAutomatically(true);
        setting.setJavaScriptEnabled(true);
        setting.setJavaScriptCanOpenWindowsAutomatically(true);
        setting.setSupportMultipleWindows(true);
        setting.setDomStorageEnabled(true);
        // file:// 页面（默认桌面）fetch https://127.0.0.1 的本地 API 属于跨域，
        // WebView 默认禁止 file 页面发起这种请求，会导致 launcher API 请求
        // 根本发不出去（权限弹窗自然不出现）。放开 file 页面的跨域访问。
        setting.setAllowFileAccessFromFileURLs(true);
        setting.setAllowUniversalAccessFromFileURLs(true);
        // Set User Agent
        String ua = setting.getUserAgentString();
        ua = String.format("%1$s WebLauncher/%2$s (port %3$s)",
                ua,
                BuildConfig.VERSION_CODE,
                viewContext.getGlobalStorage().configControl.getServerPort()
        );
        setting.setUserAgentString(ua);
        // Allow Service Worker Rewrite Fetch
        ServiceWorkerController.getInstance().setServiceWorkerClient(new AppServiceWorkerClient());
        // Enable WebView Debug
        if (0 != (viewContext.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)){
            WebView.setWebContentsDebuggingEnabled(true);
        }
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

    public boolean canGoBack() {
        return web.canGoBack();
    }
}
