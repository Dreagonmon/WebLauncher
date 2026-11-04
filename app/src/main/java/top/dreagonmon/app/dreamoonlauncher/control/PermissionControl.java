package top.dreagonmon.app.dreamoonlauncher.control;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;
import top.dreagonmon.app.dreamoonlauncher.R;
import top.dreagonmon.app.dreamoonlauncher.window.SubThreadDialogWindow;

public class PermissionControl {
    private static final String SP_PERMISSION = "permission";
    private static final String NAME_ALLOW = "allow";
    private static final String NAME_TOKENS = "tokens";
    public static final String PERMISSION_LAUNCHER = "launcher";
    private final MainActivity viewContext;
    private final SharedPreferences sharedPreferences;
    private JSONObject permissionJson;
    public PermissionControl(MainActivity context){
        this.viewContext = context;
        this.sharedPreferences = context.getSharedPreferences(MainActivity.SP_NAME_APPLICATION, Context.MODE_PRIVATE);
        String json = this.sharedPreferences.getString(SP_PERMISSION, "{}");
        JSONObject obj;
        try {
            obj = new JSONObject(json);
        } catch (JSONException e) {
            obj = new JSONObject();
        }
        this.permissionJson = obj;
    }

    private void save(){
        SharedPreferences.Editor editor = this.sharedPreferences.edit();
        editor.putString(SP_PERMISSION, this.permissionJson.toString());
        editor.apply();
    }
    private String getPermissionDisplayName(String name){
        switch (name){
            case PERMISSION_LAUNCHER:
                return viewContext.getResources().getString(R.string.word_permission_launcher);
            default: return null;
        }
    }

    public synchronized void addPermission(String name, String origin){
        try {
            if (!permissionJson.has(name)){
                permissionJson.put(name, new JSONObject());
            }
            JSONObject perm = permissionJson.getJSONObject(name);
            if (!perm.has(NAME_ALLOW)){
                perm.put(NAME_ALLOW, new JSONArray());
            }
            JSONArray list = perm.getJSONArray(NAME_ALLOW);
            boolean isNew = true;
            for (int x=0; x<list.length(); x++){
                if (list.getString(x).equals(origin)){
                    isNew = false;
                    break;
                }
            }
            if (isNew){
                list.put(origin);
            }
            // 每个 origin 一个稳定 token：img 请求带不了自定义头，
            // icon 等接口靠 URL 里的 token 鉴权
            if (!perm.has(NAME_TOKENS)){
                perm.put(NAME_TOKENS, new JSONObject());
            }
            JSONObject tokens = perm.getJSONObject(NAME_TOKENS);
            if (!tokens.has(origin)){
                tokens.put(origin, java.util.UUID.randomUUID().toString().replace("-", ""));
            }
            this.save();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /** 授权成功时返回该 origin 的 token。addPermission 幂等且会补发
     *  token（覆盖旧版本已授权但无 token 字段的情况）。 */
    public synchronized String issueToken(String name, String origin){
        addPermission(name, origin);
        try {
            JSONObject perm = permissionJson.getJSONObject(name);
            return perm.getJSONObject(NAME_TOKENS).getString(origin);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** token 是否属于该权限下任一已授权 origin。 */
    public synchronized boolean hasToken(String name, String token){
        if (token == null || token.isEmpty()) return false;
        try {
            JSONObject perm = permissionJson.getJSONObject(name);
            JSONObject tokens = perm.getJSONObject(NAME_TOKENS);
            java.util.Iterator<String> it = tokens.keys();
            while (it.hasNext()){
                if (tokens.getString(it.next()).equals(token)){
                    return true;
                }
            }
        } catch (Exception e) {
            // 无该权限或无 tokens 字段
        }
        return false;
    }

    public synchronized void removePermission(String name, String origin){
        try {
            if (!permissionJson.has(name)){
                permissionJson.put(name, new JSONObject());
            }
            JSONObject perm = permissionJson.getJSONObject(name);
            if (!perm.has(NAME_ALLOW)){
                perm.put(NAME_ALLOW, new JSONArray());
            }
            JSONArray list = perm.getJSONArray(NAME_ALLOW);
            for (int x=0; x<list.length(); x++){
                String org = list.getString(x);
                if (org.equals(origin)){
                    list.remove(x);
                    break;
                }
            }
            this.save();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void removeAllPermission(){
        this.permissionJson = new JSONObject();
        this.save();
    }
    public synchronized boolean hasPermission(String name, String origin){
        try {
            if (!permissionJson.has(name)){
                permissionJson.put(name, new JSONObject());
            }
            JSONObject perm = permissionJson.getJSONObject(name);
            if (!perm.has(NAME_ALLOW)){
                perm.put(NAME_ALLOW, new JSONArray());
            }
            JSONArray list = perm.getJSONArray(NAME_ALLOW);
            for (int x=0; x<list.length(); x++){
                String org = list.getString(x);
                if (org.equals(origin)){
                    return true;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }
    public synchronized boolean requestPermission(String name, String origin){
        String permissionName = getPermissionDisplayName(name);
        if (permissionName == null){
            return false; // not support permission
        }
        if (hasPermission(name, origin)){
            return true; // already had permission
        }
        String text = viewContext.getResources().getString(R.string.text_permission_request)
                .replace("{{name}}", permissionName)
                .replace("{{origin}}", origin);
        SubThreadDialogWindow window = new SubThreadDialogWindow(this.viewContext);
        boolean allowed = window.confirm(text);
        if (!allowed) {
            return false; //user denied
        }
        addPermission(name, origin);
        return true;
    }
}
