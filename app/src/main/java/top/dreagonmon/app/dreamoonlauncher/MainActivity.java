package top.dreagonmon.app.dreamoonlauncher;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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
    /** 等待中的 <input type=file> 回调（文件选择结果返回给它） */
    private ValueCallback<Uri[]> pendingFilePathCallback;
    /** 文件选择器：现代 Activity Result API，替代已弃用的 startActivityForResult */
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        ValueCallback<Uri[]> cb = pendingFilePathCallback;
                        pendingFilePathCallback = null;
                        if (cb != null)
                            cb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(
                                    result.getResultCode(), result.getData()));
                    });
    private MainHandler handler;
    /** 正在等待用户操作的对话框：Activity 销毁时统一 cancel，唤醒阻塞中的子线程 */
    private final java.util.List<android.app.AlertDialog> pendingDialogs =
            java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    public void registerPendingDialog(android.app.AlertDialog dialog) {
        pendingDialogs.add(dialog);
    }

    public void unregisterPendingDialog(android.app.AlertDialog dialog) {
        pendingDialogs.remove(dialog);
    }
    private GlobalStorage global;
    private WebViewWindow web;
    private SidebarControl sidebar;
    private Runnable drawerHandleFade;
    private DrawerLayout drawerLayout;
    private androidx.activity.OnBackPressedCallback backCallback;

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.handler = new MainHandler();
        try {
            doOnCreate(savedInstanceState);
        } catch (Throwable e) {
            // 启动崩溃：Toast 不随本进程死亡消失，没 adb 也能肉眼看到原因
            android.util.Log.e("WebLauncher", "onCreate crashed", e);
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            String msg = e + "\n" + sw.toString();
            try {
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show();
            } catch (Throwable ignore) {
            }
            throw e;
        }
    }

    private void doOnCreate(@Nullable Bundle savedInstanceState) {
        this.global = new GlobalStorage();
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        // 边到边显示：内容延伸到系统栏下方，配合 enterFullScreen 隐藏系统栏
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
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
        setupDrawerHandle();

        // 返回键：优先关抽屉 → WebView 后退 → 都没有才退出。
        // targetSdk 36 起预测式返回默认启用，onBackPressed 不再被调用，
        // 必须走 OnBackPressedDispatcher。
        backCallback = new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return;
                }
                if (web.canGoBack()) {
                    web.goBack();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
                setEnabled(true);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    /** 左边缘抽屉把手：点击唤出抽屉、显示 3 秒后淡出（ onResume 时重新计时）；并声明系统手势排除区避免与返回手势冲突。 */
    private void setupDrawerHandle() {
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        this.drawerLayout = drawer;
        final View handle = findViewById(R.id.drawer_handle);
        handle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawer.openDrawer(GravityCompat.START);
            }
        });
        // 3 秒后完全透明；用户触摸把手时提前取消
        drawerHandleFade = new Runnable() {
            @Override
            public void run() {
                handle.animate().alpha(0f).setDuration(500).start();
            }
        };
        handle.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                v.removeCallbacks(drawerHandleFade);
                return false;
            }
        });
        // 抽屉打开时把手立即隐藏；关闭后重新出现并重新计时
        drawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                handle.setAlpha(1f - Math.abs(slideOffset));
            }
            @Override
            public void onDrawerOpened(View drawerView) {
                handle.setAlpha(0f);
            }
            @Override
            public void onDrawerClosed(View drawerView) {
                showDrawerHandle();
            }
        });
        // Android 10+：在左边缘声明系统手势排除区，让边缘滑动优先给抽屉
        final View decor = getWindow().getDecorView();
        decor.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                applyGestureExclusion(v);
            }
        });
        applyGestureExclusion(decor);
    }

    /** 沿左边缘铺一段段约 200dp 高的手势排除矩形（系统对单个矩形高度有限制）。 */
    private void applyGestureExclusion(View decor) {
        if (Build.VERSION.SDK_INT < 29)
            return;
        int w = decor.getWidth();
        int h = decor.getHeight();
        if (w == 0 || h == 0)
            return;
        float density = getResources().getDisplayMetrics().density;
        int stripW = (int) (32 * density);
        int stripH = (int) (200 * density);
        java.util.List<Rect> rects = new java.util.ArrayList<>();
        for (int y = 0; y < h; y += stripH) {
            int bottom = Math.min(y + stripH, h);
            if (bottom - y >= (int) (24 * density))
                rects.add(new Rect(0, y, stripW, bottom));
        }
        decor.setSystemGestureExclusionRects(rects);
    }

    /** 把手立即重新显示，3 秒后自动淡出（进入 APP / 从其它 APP 返回 / 抽屉关闭时调用）。 */
    private void showDrawerHandle() {
        final View handle = findViewById(R.id.drawer_handle);
        if (handle == null || drawerHandleFade == null)
            return;
        handle.animate().cancel();
        handle.removeCallbacks(drawerHandleFade);
        handle.setAlpha(1f);
        handle.postDelayed(drawerHandleFade, 3000);
    }
    protected void onResume() {
        super.onResume();
        enterFullScreen();
        // 打开 APP / 从其它 APP 跳转回来：把手显示 3 秒后自动隐藏
        showDrawerHandle();
    }
    // 返回键逻辑见 onCreate 中注册的 backCallback（OnBackPressedDispatcher）

    @Override
    protected void onDestroy() {
        // 唤醒所有还在等对话框的子线程（dismiss 触发 onDismiss → latch 归零）
        synchronized (pendingDialogs) {
            for (android.app.AlertDialog d : pendingDialogs) {
                try {
                    d.dismiss();
                } catch (Exception ignore) {
                }
            }
            pendingDialogs.clear();
        }
        if (global.server != null && global.server.wasStarted()) {
            global.server.stopServer();
        }
        super.onDestroy();
    }

    /** MainActivity functions */
    /** 发起文件选择；结果经 filePickerLauncher 回调给 filePathCallback。 */
    public void launchFilePicker(Intent picker, ValueCallback<Uri[]> filePathCallback){
        // 上一次未完成的请求先置 null，避免 WebView 文件选择器卡死
        if (pendingFilePathCallback != null)
            pendingFilePathCallback.onReceiveValue(null);
        pendingFilePathCallback = filePathCallback;
        filePickerLauncher.launch(picker);
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
                // 新 API：WindowInsetsControllerCompat（兼容 API 26+，替代已废弃的
                // FLAG_FULLSCREEN + setSystemUiVisibility，Android 15 起强制边到边）
                WindowInsetsControllerCompat controller =
                        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                controller.hide(android.view.WindowInsets.Type.systemBars());
                // 对应原 SYSTEM_UI_FLAG_IMMERSIVE_STICKY：滑动边缘临时唤出后自动隐藏
                controller.setSystemBarsBehavior(
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        });
    }

    static class MainHandler extends Handler{
        MainHandler(){
            // 显式绑定主线程 Looper（无参 Handler() 构造已弃用）
            super(Looper.getMainLooper());
        }
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
