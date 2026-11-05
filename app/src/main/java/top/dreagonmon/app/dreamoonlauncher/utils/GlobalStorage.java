/* 应用程序的全局对象池，由MainActivity持有唯一的实例 */
package top.dreagonmon.app.dreamoonlauncher.utils;

import top.dreagonmon.app.dreamoonlauncher.control.ConfigControl;
import top.dreagonmon.app.dreamoonlauncher.control.PermissionControl;
import top.dreagonmon.app.dreamoonlauncher.server.LauncherServer;

public class GlobalStorage {
    public LauncherServer server;
    public PermissionControl permissionControl;
    public ConfigControl configControl;
    /** 进程内事件总线：SSE /api/events 订阅，事件源（应用变更回调等）广播 */
    public final EventBus eventBus = new EventBus();
}
