/* 应用程序的全局对象池，由MainActivity持有唯一的实例 */
package top.dreagonmon.app.dreamoonlauncher.utils;

import top.dreagonmon.app.dreamoonlauncher.control.ConfigControl;
import top.dreagonmon.app.dreamoonlauncher.control.PermissionControl;
import top.dreagonmon.app.dreamoonlauncher.server.LauncherServer;

public class GlobalStorage {
    public LauncherServer server;
    public PermissionControl permissionControl;
    public ConfigControl configControl;
}
