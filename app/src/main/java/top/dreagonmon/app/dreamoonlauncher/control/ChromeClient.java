package top.dreagonmon.app.dreamoonlauncher.control;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.Nullable;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;

public class ChromeClient extends WebChromeClient {
    private final int REQUEST_PICK_FILE = 0x10;
    private final MainActivity viewContext;
    public ChromeClient(Context viewContext){
        this.viewContext = (MainActivity) viewContext;
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        Intent filePicker = fileChooserParams.createIntent();
        viewContext.registerActivityResultCallback(REQUEST_PICK_FILE,new FileChooseCallback(filePathCallback));
        viewContext.startActivityForResult(filePicker,REQUEST_PICK_FILE);
        return true;
    }

    private class FileChooseCallback implements ActivityResultCallback{
        private final ValueCallback<Uri[]> filePathCallback;
        public FileChooseCallback(ValueCallback<Uri[]> filePathCallback){
            this.filePathCallback = filePathCallback;
        }
        @Override
        public void onActivityResult(int resultCode, @Nullable Intent data) {
            viewContext.removeActivityResultCallback(REQUEST_PICK_FILE);
            Uri[] result = FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(result);
        }
    }
}
