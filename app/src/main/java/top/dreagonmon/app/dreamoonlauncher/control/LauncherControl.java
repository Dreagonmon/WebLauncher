package top.dreagonmon.app.dreamoonlauncher.control;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.List;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;

public class LauncherControl {
    private final MainActivity viewContext;
    public LauncherControl(MainActivity context){
        this.viewContext = context;
    }
    /* launch application */
    public void launchApplication(String packageName, String activityName){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, activityName));
        viewContext.startActivity(intent);
    }
    public Drawable getActivityIcon(String packageName, String activityName) {
        PackageManager pm = this.viewContext.getPackageManager();
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(packageName, activityName));
        ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
        return resolveInfo == null ? null : resolveInfo.loadIcon(pm);
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
