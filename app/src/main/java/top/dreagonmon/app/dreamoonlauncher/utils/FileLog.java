package top.dreagonmon.app.dreamoonlauncher.utils;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 轻量文件日志：写到 Android/data/&lt;pkg&gt;/files/launcher.log，
 * 方便没连 adb 的手机上事后取日志排查。
 */
public class FileLog {
    public static void write(Context context, String msg) {
        try {
            File dir = context.getExternalFilesDir(null);
            if (dir == null)
                dir = context.getFilesDir();
            File log = new File(dir, "launcher.log");
            String line = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US)
                    .format(new Date()) + " " + msg + "\n";
            FileWriter fw = new FileWriter(log, true);
            fw.write(line);
            fw.close();
        } catch (Throwable ignore) {
            // 日志失败不影响功能
        }
    }

    public static void write(Context context, String msg, Throwable e) {
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            write(context, msg + "\n" + sw);
        } catch (Throwable ignore) {
        }
    }
}
