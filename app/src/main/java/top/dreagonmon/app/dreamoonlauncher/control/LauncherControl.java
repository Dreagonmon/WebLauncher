package top.dreagonmon.app.dreamoonlauncher.control;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;

/**
 * 应用枚举/图标/启动，走 LauncherApps（桌面正统 API）：
 * - 自动覆盖当前用户 + 可访问的 profile（工作资料、MIUI 分身等）
 * - 用户以 serial number 字符串对外暴露（协议里的 user 字段），
 *   内部经 UserManager.getUserForSerialNumber 映射回 UserHandle
 */
public class LauncherControl {
    private final MainActivity viewContext;
    private final LauncherApps launcherApps;
    private final UserManager userManager;

    public LauncherControl(MainActivity context){
        this.viewContext = context;
        this.launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        this.userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
    }

    /** serial -> UserHandle：当前用户 + 所有可访问 profile。 */
    public Map<Long, UserHandle> getUsers(){
        LinkedHashMap<Long, UserHandle> map = new LinkedHashMap<>();
        try {
            UserHandle self = Process.myUserHandle();
            map.put(userManager.getSerialNumberForUser(self), self);
        } catch (Exception ignore) {
        }
        try {
            for (UserHandle u : launcherApps.getProfiles()) {
                map.put(userManager.getSerialNumberForUser(u), u);
            }
        } catch (Exception e) {
            android.util.Log.w("LauncherControl", "getProfiles failed", e);
        }
        return map;
    }

    /** 协议里的 user（serial 字符串）→ UserHandle；缺省/非法回退当前用户。 */
    private UserHandle resolveUser(String serial){
        if (serial != null && !serial.isEmpty()) {
            try {
                UserHandle u = userManager.getUserForSerialNumber(Long.parseLong(serial));
                if (u != null) return u;
            } catch (Exception ignore) {
            }
        }
        return Process.myUserHandle();
    }

    /* launch application */
    public boolean launchApplication(String packageName, String activityName){
        return launchApplication(packageName, activityName, null) == null;
    }
    /** 启动应用。成功返回 null；失败返回原因（供 API 回给页面显示，手机无控制台）。 */
    public String launchApplication(String packageName, String activityName, String userSerial){
        ComponentName component = new ComponentName(packageName, activityName);
        UserHandle user = resolveUser(userSerial);
        StringBuilder err = new StringBuilder();
        try {
            // 桌面正统启动：系统负责 task 语义与跨 profile 权限
            launcherApps.startMainActivity(component, user, null, null);
            top.dreagonmon.app.dreamoonlauncher.utils.FileLog.write(
                    viewContext, "launch ok " + packageName + "/" + activityName
                            + " user=" + userSerial);
            return null;
        } catch (Exception e) {
            err.append("startMainActivity(").append(userSerial).append(") ")
                    .append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
            android.util.Log.w("LauncherControl",
                    "startMainActivity failed " + packageName + "/" + activityName, e);
            top.dreagonmon.app.dreamoonlauncher.utils.FileLog.write(
                    viewContext, "launch failed " + err, e);
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(component);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (!user.equals(Process.myUserHandle())) {
            // 跨 profile：试 startActivityAsUser（需系统对 HOME 角色放行，失败很常见）
            try {
                java.lang.reflect.Method m = android.content.Context.class.getMethod(
                        "startActivityAsUser", Intent.class, UserHandle.class);
                m.invoke(viewContext, intent, user);
                top.dreagonmon.app.dreamoonlauncher.utils.FileLog.write(
                        viewContext, "launch ok (asUser) " + packageName + " user=" + userSerial);
                return null;
            } catch (Exception e) {
                Throwable c = e.getCause() != null ? e.getCause() : e;
                err.append(" | startActivityAsUser ").append(c.getClass().getSimpleName())
                        .append(": ").append(c.getMessage());
                top.dreagonmon.app.dreamoonlauncher.utils.FileLog.write(viewContext, err.toString());
            }
            // 跨 profile 都失败：不再回退到当前用户（那会弹主/分身选择器，语义错）
            return err.toString();
        }
        // 当前用户：回退经典 Intent 路径（与真实桌面一致地构造 Intent，
        // MIUI 等系统应用会检查 action/category，光板 component-only 会自崩）
        try {
            viewContext.startActivity(intent);
            top.dreagonmon.app.dreamoonlauncher.utils.FileLog.write(
                    viewContext, "launch ok (fallback) " + packageName + "/" + activityName);
            return null;
        } catch (Exception e) {
            err.append(" | startActivity ").append(e.getClass().getSimpleName())
                    .append(": ").append(e.getMessage());
            top.dreagonmon.app.dreamoonlauncher.utils.FileLog.write(
                    viewContext, "launch failed (fallback) " + packageName + "/" + activityName, e);
            return err.toString();
        }
    }
    public void openWebpage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(this.viewContext.getPackageManager()) != null) {
            this.viewContext.startActivity(intent);
        }
    }

    public Drawable getActivityIcon(String packageName, String activityName){
        return getActivityIcon(packageName, activityName, null);
    }
    public Drawable getActivityIcon(String packageName, String activityName, String userSerial){
        UserHandle user = resolveUser(userSerial);
        if (!user.equals(Process.myUserHandle())) {
            // profile 用户（工作资料等）：LauncherApps 图标，带 profile 角标
            try {
                List<LauncherActivityInfo> list =
                        launcherApps.getActivityList(packageName, user);
                if (list != null) {
                    int density = this.viewContext.getResources().getDisplayMetrics().densityDpi;
                    for (LauncherActivityInfo ai : list) {
                        if (ai.getComponentName().getClassName().equals(activityName)) {
                            Drawable d = ai.getBadgedIcon(density);
                            if (d != null) return d;
                        }
                    }
                }
            } catch (Exception ignore) {
            }
            return null;
        }
        // 当前用户走 PackageManager：尊重厂商主题图标服务
        // （LauncherActivityInfo.getIcon 的 IconCache 在 HyperOS 上绕过主题，
        //  大量应用会回退成默认方块图标）
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
        ArrayList<ApplicationInfo> appsList = new ArrayList<>();
        for (Map.Entry<Long, UserHandle> entry : getUsers().entrySet()) {
            String serial = String.valueOf(entry.getKey());
            List<LauncherActivityInfo> list;
            try {
                list = launcherApps.getActivityList(null, entry.getValue());
            } catch (Exception e) {
                android.util.Log.w("LauncherControl",
                        "getActivityList failed user=" + serial, e);
                continue;
            }
            if (list == null) continue;
            for (LauncherActivityInfo ai : list) {
                ApplicationInfo app = new ApplicationInfo();
                app.label = String.valueOf(ai.getLabel());
                app.packageName = ai.getComponentName().getPackageName();
                app.activityName = ai.getComponentName().getClassName();
                app.user = serial;
                appsList.add(app);
            }
        }
        return appsList;
    }
    public static class ApplicationInfo{
        public String label;
        public String packageName;
        public String activityName;
        public String user;
    }
}
