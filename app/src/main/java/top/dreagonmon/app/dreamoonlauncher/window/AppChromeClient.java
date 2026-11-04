package top.dreagonmon.app.dreamoonlauncher.window;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;

public class AppChromeClient extends WebChromeClient {
    private final MainActivity viewContext;
    public AppChromeClient(Context viewContext){
        this.viewContext = (MainActivity) viewContext;
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        // 现代 Activity Result API：结果由 MainActivity.filePickerLauncher 回调
        Intent filePicker = fileChooserParams.createIntent();
        viewContext.launchFilePicker(filePicker, filePathCallback);
        return true;
    }
}
