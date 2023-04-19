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
        setting.setBlockNetworkImage(false);
        setting.setLoadsImagesAutomatically(true);
        setting.setJavaScriptEnabled(true);
        setting.setJavaScriptCanOpenWindowsAutomatically(true);
        setting.setSupportMultipleWindows(true);
        setting.setDomStorageEnabled(true);
        setting.setDatabaseEnabled(true);
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
}
