package top.dreagonmon.app.dreamoonlauncher.control;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;

public class LauncherControl {
    private final MainActivity viewContext;
    public LauncherControl(MainActivity context){
        this.viewContext = context;
    }
    /* launch application */
    public boolean launchApplication(String packageName, String activityName){
        // 与真实桌面一致地构造 Intent：MIUI 等系统应用会检查 action/category，
        // 光板 component-only Intent 会被拉起后自崩（如 HyperOS 计算器）
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(packageName, activityName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            viewContext.startActivity(intent);
            top.dreagonmon.app.dreamoonlauncher.utils.FileLog.write(
                    viewContext, "launch ok " + packageName + "/" + activityName);
            return true;
        } catch (Exception e) {
            // 个别系统应用（activity alias/未导出）从外部启动会抛异常
            android.util.Log.w("LauncherControl", "launch failed " + packageName + "/" + activityName, e);
            top.dreagonmon.app.dreamoonlauncher.utils.FileLog.write(
                    viewContext, "launch failed " + packageName + "/" + activityName, e);
            return false;
        }
    }
    public void openWebpage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(this.viewContext.getPackageManager()) != null) {
            this.viewContext.startActivity(intent);
        }
    }
    public Drawable getActivityIcon(String packageName, String activityName) {
        PackageManager pm = this.viewContext.getPackageManager();
        // 逐级回退：activity 图标 → 应用图标 → null（调用方出 404）
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, activityName));
            ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
            if (resolveInfo != null) {
                Drawable d = resolveInfo.loadIcon(pm);
                if (d != null) return d;
            }
        } catch (Exception ignore) {
        }
        try {
            return pm.getApplicationIcon(packageName);
        } catch (Exception e) {
            return null;
        }
    }
    public List<ApplicationInfo> getApplicationList(){
        PackageManager pm = viewContext.getPackageManager();
        ArrayList<ApplicationInfo> appsList = new ArrayList<>();
        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> allApps = pm.queryIntentActivities(i, 0);
        for(ResolveInfo ri:allApps) {
            ApplicationInfo app = new ApplicationInfo();
            app.label = ri.loadLabel(pm).toString();
            app.packageName = ri.activityInfo.packageName;
            app.activityName = ri.activityInfo.name;
            appsList.add(app);
        }
        return appsList;
    }
    public static class ApplicationInfo{
        public String label;
        public String packageName;
        public String activityName;
    }
}
