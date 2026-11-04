package top.dreagonmon.app.dreamoonlauncher.control;

import android.content.Context;
import android.content.SharedPreferences;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;

public class ConfigControl {
    private static final String SP_CONFIG = "config";
    private static final String NAME_HOMEPAGE = "homepage";
    /** 旧默认页（file://）。file 页面跨域 fetch 本地 API 在新版 Chromium 不可靠，
     *  未自定义主页时一律迁移到本地服务器的 https 同源页面 */
    private static final String OLD_DEF_HOMEPAGE = "file:///android_asset/wallpaper/simple/index.html";
    private static final String NAME_SERVER_PORT = "server_port";
    private static final int DEF_SERVER_PORT = 10801;
    private final MainActivity viewContext;
    private final SharedPreferences sharedPreferences;
    public ConfigControl(MainActivity context){
        this.viewContext = context;
        this.sharedPreferences = context.getSharedPreferences(SP_CONFIG, Context.MODE_PRIVATE);
    }
    private void setConfig(String name, String value){
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.putString(name, value);
        editor.commit();
    }private void setConfig(String name, int value){
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.putInt(name, value);
        editor.commit();
    }
    public String getHomePage(){
        String saved = this.sharedPreferences.getString(NAME_HOMEPAGE, null);
        if (saved == null || saved.equals(OLD_DEF_HOMEPAGE) || saved.startsWith("file://")){
            // 默认页由本地服务器提供：与 API 同源，且证书已被 network_security_config 信任
            return "https://127.0.0.1:" + getServerPort() + "/index.html";
        }
        return saved;
    }
    public void setHomePage(String url){
        setConfig(NAME_HOMEPAGE, url);
    }
    public int getServerPort(){
        return this.sharedPreferences.getInt(NAME_SERVER_PORT, DEF_SERVER_PORT);
    }
    public void setServerPort(int port){
        setConfig(NAME_SERVER_PORT, port);
    }
}
