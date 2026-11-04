package top.dreagonmon.app.dreamoonlauncher.control;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import java.util.regex.Pattern;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;
import top.dreagonmon.app.dreamoonlauncher.R;
import top.dreagonmon.app.dreamoonlauncher.window.SubThreadDialogWindow;

public class SidebarControl {
    private final MainActivity viewContext;
    public SidebarControl(MainActivity context){
        this.viewContext = context;
        context.findViewById(R.id.side_btn_set_home_page).setOnClickListener(buttonSetHomePage);
        context.findViewById(R.id.side_btn_set_server_port).setOnClickListener(buttonSetServerPort);
        context.findViewById(R.id.side_btn_reset_permission).setOnClickListener(buttonResetPermission);
        context.findViewById(R.id.side_btn_app_list_permission).setOnClickListener(buttonAppListPermission);
        context.findViewById(R.id.side_btn_restart).setOnClickListener(buttonRestartLauncher);
    }

    View.OnClickListener buttonSetHomePage = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // UI 线程直接走回调式对话框，不再开线程阻塞等待
            SubThreadDialogWindow window = new SubThreadDialogWindow(viewContext);
            window.promptAsync(viewContext.getResources().getString(R.string.text_input_home_page), homePage -> {
                if (!Pattern.matches("^https://.*$", homePage)) {
                    Toast.makeText(viewContext, viewContext.getResources().getString(R.string.text_config_failed), Toast.LENGTH_SHORT).show();
                    return;
                }
                viewContext.getGlobalStorage().configControl.setHomePage(homePage);
                Toast.makeText(viewContext, viewContext.getResources().getString(R.string.text_config_saved_restart), Toast.LENGTH_SHORT).show();
            });
        }
    };
    View.OnClickListener buttonSetServerPort = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // UI 线程直接走回调式对话框，不再开线程阻塞等待
            SubThreadDialogWindow window = new SubThreadDialogWindow(viewContext);
            window.promptAsync(viewContext.getResources().getString(R.string.text_input_server_port), portText -> {
                int number;
                try {
                    number = Integer.parseInt(portText);
                } catch (NumberFormatException e) {
                    number = -1;
                }
                if (number < 10000 || number > 65535) {
                    Toast.makeText(viewContext, viewContext.getResources().getString(R.string.text_config_failed), Toast.LENGTH_SHORT).show();
                    return;
                }
                viewContext.getGlobalStorage().configControl.setServerPort(number);
                Toast.makeText(viewContext, viewContext.getResources().getString(R.string.text_config_saved_restart), Toast.LENGTH_SHORT).show();
            });
        }
    };
    View.OnClickListener buttonResetPermission = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            viewContext.getGlobalStorage().permissionControl.removeAllPermission();
            viewContext.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(viewContext, viewContext.getResources().getString(R.string.text_config_saved), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    View.OnClickListener buttonRestartLauncher = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            viewContext.recreate();
        }
    };
    /** 打开系统设置里本应用的"查看其他应用"权限页，帮助用户授予包可见性权限。 */
    View.OnClickListener buttonAppListPermission = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String pkg = viewContext.getPackageName();
            // Android 11+ 的"Apps that can see all other apps"权限页
            Intent intent = new Intent("android.intent.action.MANAGE_APP_PERMISSIONS");
            intent.putExtra("android.intent.extra.PACKAGE_NAME", pkg);
            intent.putExtra("hideInfoButton", true);
            try {
                viewContext.startActivity(intent);
                return;
            } catch (Exception ignore) {
                // 部分 ROM（含某些 HyperOS 版本）没有这个页面，退回应用详情页
            }
            try {
                viewContext.startActivity(new Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + pkg)));
            } catch (Exception e) {
                Toast.makeText(viewContext,
                        R.string.text_no_settings_page, Toast.LENGTH_SHORT).show();
            }
        }
    };
}
