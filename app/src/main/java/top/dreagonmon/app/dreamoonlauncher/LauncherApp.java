package top.dreagonmon.app.dreamoonlauncher;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 全局崩溃记录：把未捕获异常写入 getExternalFilesDir 下的 crash.log，
 * 方便在没连 adb 的手机上事后取日志（Android/data/top.dreagonmon.app.dreamoonlauncher/files/crash.log）。
 */
public class LauncherApp extends Application {
    private static final String TAG = "WebLauncherCrash";

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        install(base);
    }

    private void install(final Context context) {
        final Thread.UncaughtExceptionHandler prev =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                try {
                    File dir = context.getExternalFilesDir(null);
                    if (dir == null)
                        dir = context.getFilesDir();
                    File log = new File(dir, "crash.log");
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    pw.printf("=== %s ===%n", new SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
                    pw.printf("Thread: %s%n", t.getName());
                    pw.printf("Model: %s %s, Android %s (API %d)%n",
                            android.os.Build.MANUFACTURER, android.os.Build.MODEL,
                            android.os.Build.VERSION.RELEASE, android.os.Build.VERSION.SDK_INT);
                    e.printStackTrace(pw);
                    pw.flush();
                    String text = sw.toString();
                    FileWriter fw = new FileWriter(log, true);
                    fw.write(text);
                    fw.close();
                    Log.e(TAG, text);
                } catch (Throwable ignore) {
                    // 记日志本身再炸就只能放弃
                }
                if (prev != null)
                    prev.uncaughtException(t, e);
            }
        });
    }
}
