package top.dreagonmon.app.dreamoonlauncher.control;

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
        context.findViewById(R.id.side_btn_restart).setOnClickListener(buttonRestartLauncher);
    }

    View.OnClickListener buttonSetHomePage = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Thread t = new Thread(){
                @Override
                public void run() {
                    try {
                        SubThreadDialogWindow window = new SubThreadDialogWindow(viewContext);
                        String homePage = window.prompt(viewContext.getResources().getString(R.string.text_input_home_page));
                        if (!Pattern.matches("^https://.*$",homePage)){
                            throw new Exception();
                        }
                        viewContext.getGlobalStorage().configControl.setHomePage(homePage);
                        viewContext.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(viewContext, viewContext.getResources().getString(R.string.text_config_saved_restart), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e){
                        viewContext.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(viewContext, viewContext.getResources().getString(R.string.text_config_failed), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            };
            t.start();
        }
    };
    View.OnClickListener buttonSetServerPort = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Thread t = new Thread(){
                @Override
                public void run() {
                    try {
                        SubThreadDialogWindow window = new SubThreadDialogWindow(viewContext);
                        String portText = window.prompt(viewContext.getResources().getString(R.string.text_input_server_port));
                        int number = Integer.parseInt(portText);
                        if (number < 10000 || number > 65535){
                            throw new Exception();
                        }
                        viewContext.getGlobalStorage().configControl.setServerPort(number);
                        viewContext.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(viewContext, viewContext.getResources().getString(R.string.text_config_saved_restart), Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception e){
                        viewContext.runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(viewContext, viewContext.getResources().getString(R.string.text_config_failed), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            };
            t.start();
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
}
