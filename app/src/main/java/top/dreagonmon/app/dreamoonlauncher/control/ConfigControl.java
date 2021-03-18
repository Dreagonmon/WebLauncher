package top.dreagonmon.app.dreamoonlauncher.control;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;

public class ConfigControl {
    private static final String SP_CONFIG = "config";
    private static final String NAME_HOMEPAGE = "homepage";
    private static final String DEF_HOMEPAGE = "file:///android_asset/wallpaper/simple/index.html";
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
        return this.sharedPreferences.getString(NAME_HOMEPAGE, DEF_HOMEPAGE);
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
