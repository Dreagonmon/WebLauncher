package top.dreagonmon.app.dreamoonlauncher.window;

import android.webkit.ServiceWorkerClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import androidx.annotation.Nullable;

public class AppServiceWorkerClient extends ServiceWorkerClient {
    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
        return null;
    }
}
