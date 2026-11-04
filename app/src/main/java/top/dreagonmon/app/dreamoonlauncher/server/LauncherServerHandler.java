package top.dreagonmon.app.dreamoonlauncher.server;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.util.IHandler;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;
import top.dreagonmon.app.dreamoonlauncher.control.LauncherControl;
import top.dreagonmon.app.dreamoonlauncher.control.PermissionControl;

public class LauncherServerHandler implements IHandler<IHTTPSession, Response> {
    private static final int ICON_SIZE = 128;
    private final MainActivity viewContext;
    private final LauncherControl control;
    public LauncherServerHandler(MainActivity context){
        this.viewContext = context;
        this.control = new LauncherControl(context);
    }
    private PermissionControl getPermissionControl(){
        return this.viewContext.getGlobalStorage().permissionControl;
    }
    // response serve
    @Override
    public Response handle(IHTTPSession session) {
        if (session.getMethod().toString().toLowerCase().equals("options")){
            return doCORS(session);
        }
        Response rsp = route(session);
        addCORSHeader(session, rsp);
        return rsp;
    }

    private void addCORSHeader(IHTTPSession session, Response rsp){
        Map<String, String> header = session.getHeaders();
        if (header.containsKey("origin")){
            String origin = header.get("origin");
            rsp.addHeader("Access-Control-Allow-Origin", origin);
        }
        if (header.containsKey("access-control-request-method")){
            String accessMethod = header.get("access-control-request-method");
            rsp.addHeader("Access-Control-Allow-Methods", accessMethod);
        }
        if (header.containsKey("access-control-request-headers")){
            String accessHeader = header.get("access-control-request-headers");
            rsp.addHeader("Access-Control-Allow-Headers", accessHeader);
        }
        // Private Network Access：公网 https 页面 fetch 本机 127.0.0.1 时，
        // 新版 Chromium 预检要求此头，缺失则请求被直接拦截（弹窗请求根本到不了这里）
        if (header.containsKey("access-control-request-private-network")){
            rsp.addHeader("Access-Control-Allow-Private-Network", "true");
        }
        rsp.addHeader("Access-Control-Max-Age", "86400");
    }
    /** 解析请求方 origin。同源 GET 时 Chromium 不发 Origin 头，
     *  此时用 Host 头重建（服务器只有 https，直接拼 https://host）。
     *  file:// 页面发的是 "null"，归一为 "file://"。 */
    private static String resolveOrigin(IHTTPSession session){
        String origin = session.getHeaders().get("origin");
        if (origin == null || origin.equals("null") || origin.equals("file://")){
            String host = session.getHeaders().get("host");
            if (origin != null) return "file://"; // file 页面显式发了 "null"
            if (host != null && !host.isEmpty()) return "https://" + host;
            return "file://";
        }
        return origin;
    }
    private boolean hasPermission(String name, IHTTPSession session){
        try{
            // CSRF 防线：鉴权接口必须携带自定义头 X-Requested-With。
            // no-cors fetch / <img> / iframe 这类简单请求会被浏览器静默
            // 剥掉非白名单自定义头，且这类请求不带 Origin（会被 Host
            // 重建成本地 origin 而骗过鉴权）；而 cors fetch 带自定义头会
            // 触发预检，预检必带真实 Origin，鉴权即回到正轨。
            String xrw = session.getHeaders().get("x-requested-with");
            if (!"WebLauncher".equals(xrw)){
                top.dreagonmon.app.dreamoonlauncher.utils.FileLog.write(
                        viewContext, "permission denied (no XRW header) " + session.getUri());
                return false;
            }
            return getPermissionControl().hasPermission(name, resolveOrigin(session));
        }catch (Exception e){
            return false;
        }
    }
    private Response RSP_200(){
        return Response.newFixedLengthResponse(Status.OK,"text/plain","200 OK");
    }
    private Response RSP_404(){
        return Response.newFixedLengthResponse(Status.NOT_FOUND,"text/plain","404 Not Found");
    }
    private Response RSP_403(){
        return Response.newFixedLengthResponse(Status.FORBIDDEN,"text/plain","403 Forbidden");
    }
    private Response RSP_500(){
        return Response.newFixedLengthResponse(Status.INTERNAL_ERROR,"text/plain","500 Internal Server Error");
    }

    private Response doCORS(IHTTPSession session){
        Response rsp = Response.newFixedLengthResponse(Status.NO_CONTENT, "", "");
        addCORSHeader(session, rsp);
        return rsp;
    }
    private Response route(IHTTPSession session){
        String[] path = session.getUri().split("/");
        // path[0] is empty
        if (path.length < 3 || !path[1].equals("api")){
            // 非 /api/ 路径：从 assets 提供静态页面，让默认桌面跑在
            // https://127.0.0.1 同源下（file:// 页面 fetch 跨域在新版 Chromium 已不可用）
            return serveAsset(session);
        }
        try {
            switch (path[2]) {
                case "app-list-all":
                    return doGetApplicationList(session);
                case "icon":
                    return doGetApplicationIcon(session);
                case "launch":
                    return doLaunchApplication(session);
                case "open-webpage":
                    return doOpenWebpage(session);
                case "request-permission":
                    return doRequestPermission(session);
            }
        }catch (Exception e){
            e.printStackTrace();
            top.dreagonmon.app.dreamoonlauncher.utils.FileLog.write(
                    viewContext, "route error " + session.getUri(), e);
            return RSP_500();
        }
        return RSP_404();
    }

    /* Static assets: serve /wallpaper/simple pages over https so the default
       launcher page runs same-origin with the API (file:// cross-origin fetch
       is unreliable in new Chromium). */
    private Response serveAsset(IHTTPSession session){
        String uri = session.getUri();
        int q = uri.indexOf('?');
        if (q >= 0) uri = uri.substring(0, q);
        uri = android.net.Uri.decode(uri);
        if (uri.isEmpty() || uri.equals("/")) uri = "/index.html";
        if (uri.contains("..")) return RSP_404();
        String assetName = "wallpaper/simple" + uri;
        try (java.io.InputStream in = viewContext.getAssets().open(assetName)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bos.write(buf, 0, n);
            return Response.newFixedLengthResponse(Status.OK, guessMime(assetName), bos.toByteArray());
        } catch (Exception e) {
            return RSP_404();
        }
    }
    private static String guessMime(String name){
        if (name.endsWith(".html")) return "text/html";
        if (name.endsWith(".js")) return "application/javascript";
        if (name.endsWith(".css")) return "text/css";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    /* Launcher Router */
    private Response doGetApplicationList(IHTTPSession session) throws JSONException {
        if (!hasPermission(PermissionControl.PERMISSION_LAUNCHER, session)){
            return RSP_403();
        }
        List<LauncherControl.ApplicationInfo> lst = control.getApplicationList();
        JSONArray array = new JSONArray();
        for (LauncherControl.ApplicationInfo info:lst){
            JSONObject app = new JSONObject();
            app.put("name", info.label);
            app.put("package", info.packageName);
            app.put("activity", info.activityName);
            array.put(app);
        }
        String text = array.toString(2);
        return Response.newFixedLengthResponse(Status.OK, "application/json", text);
    }
    private Response doGetApplicationIcon(IHTTPSession session) throws RuntimeException {
        // icon don`t need permission
        // TODO: require permission, and cache icon for better speed.
        Map<String, List<String>> params = session.getParameters();
        String packageName = params.get("package").get(0);
        String activityName = params.get("activity").get(0);
        int size;
        if (params.containsKey("size")){
            size = Integer.parseInt(params.get("size").get(0));
        }else{
            size = ICON_SIZE;
        }
        Drawable dw = control.getActivityIcon(packageName, activityName);
        Bitmap icon = Bitmap.createBitmap(size,size, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(icon);
        icon.prepareToDraw();
        dw.setBounds(0,0,size,size);
        dw.draw(cv);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG,100,bos);
        byte[] data = bos.toByteArray();
        return Response.newFixedLengthResponse(Status.OK, "image/png", data);
    }
    private Response doLaunchApplication(IHTTPSession session){
        if (!hasPermission(PermissionControl.PERMISSION_LAUNCHER, session)){
            return RSP_403();
        }
        Map<String, List<String>> params = session.getParameters();
        String packageName = params.get("package").get(0);
        String activityName = params.get("activity").get(0);
        control.launchApplication(packageName, activityName);
        return RSP_200();
    }
    private Response doOpenWebpage(IHTTPSession session){
        if (!hasPermission(PermissionControl.PERMISSION_LAUNCHER, session)){
            return RSP_403();
        }
        Map<String, List<String>> params = session.getParameters();
        String href = params.get("href").get(0);
        control.openWebpage(href);
        return RSP_200();
    }
    private Response doRequestPermission(IHTTPSession session){
        // 同样要求 X-Requested-With，否则 no-cors/img 可无头触发授权弹窗骚扰
        String xrw = session.getHeaders().get("x-requested-with");
        if (!"WebLauncher".equals(xrw)){
            return RSP_403();
        }
        Map<String, List<String>> params = session.getParameters();
        String name = params.get("name").get(0);
        String origin = resolveOrigin(session);
        Log.d("server", "request-permission name=" + name + " origin=" + origin);
        top.dreagonmon.app.dreamoonlauncher.utils.FileLog.write(
                viewContext, "request-permission name=" + name + " origin=" + origin);
        boolean result = getPermissionControl().requestPermission(name, origin);
        Log.d("server", String.valueOf(result));
        if (result) {
            return RSP_200();
        }else{
            return RSP_403();
        }
    }
}
