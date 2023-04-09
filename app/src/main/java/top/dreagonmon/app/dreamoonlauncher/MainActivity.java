package top.dreagonmon.app.dreamoonlauncher;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import top.dreagonmon.app.dreamoonlauncher.control.ActivityResultCallback;
import top.dreagonmon.app.dreamoonlauncher.control.ConfigControl;
import top.dreagonmon.app.dreamoonlauncher.control.PermissionControl;
import top.dreagonmon.app.dreamoonlauncher.control.SidebarControl;
import top.dreagonmon.app.dreamoonlauncher.server.LauncherServer;
import top.dreagonmon.app.dreamoonlauncher.utils.GlobalStorage;
import top.dreagonmon.app.dreamoonlauncher.window.WebViewWindow;

/**
 * WebView as Background test Launcher
 */
public class MainActivity extends AppCompatActivity {
    public static final String SP_NAME_APPLICATION = "application";
    private SparseArray<ActivityResultCallback> activityCallbackArray = new SparseArray<>();
    private MainHandler handler;
    private GlobalStorage global;
    private WebViewWindow web;
    private SidebarControl sidebar;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.handler = new MainHandler();
        this.global = new GlobalStorage();
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        // init global object
        global.permissionControl = new PermissionControl(this);
        global.configControl = new ConfigControl(this);
        // start server
        global.server = new LauncherServer(this, global.configControl.getServerPort());
        global.server.startServer();
        // init View
        FrameLayout webFrame = findViewById(R.id.web_container);
        this.web = new WebViewWindow(this);
        webFrame.addView(web.getWebView());
        this.web.loadURL(global.configControl.getHomePage());
        this.sidebar = new SidebarControl(this);
    }
    protected void onResume() {
        super.onResume();
        enterFullScreen();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        ActivityResultCallback cb = activityCallbackArray.get(requestCode);
        if (cb == null)
            super.onActivityResult(requestCode, resultCode, data);
        else
            cb.onActivityResult(resultCode, data);
    }
    @Override
    public void onBackPressed() {
        web.goBack();
    }
    @Override
    protected void onDestroy() {
        if (global.server != null && global.server.wasStarted()) {
            global.server.stopServer();
        }
        super.onDestroy();
    }

    /** MainActivity functions */
    public void registerActivityResultCallback(int requestCode, ActivityResultCallback callback){
        activityCallbackArray.put(requestCode, callback);
    }
    public void removeActivityResultCallback(int requestCode){
        activityCallbackArray.delete(requestCode);
    }
    public void runOnMainThread(Runnable func){
        Message msg = this.handler.obtainMessage();
        msg.what = 0;
        msg.obj = func;
        this.handler.sendMessage(msg);
    }
    public GlobalStorage getGlobalStorage(){
        return this.global;
    }
    public void enterFullScreen(){
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        });
    }

    static class MainHandler extends Handler{
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case 0:
                    // run on main thread
                    Runnable runnable = (Runnable) msg.obj;
                    runnable.run();
                    break;
                case 1:
                    break;
                default:
                    break;
            }
        }
    }
}
