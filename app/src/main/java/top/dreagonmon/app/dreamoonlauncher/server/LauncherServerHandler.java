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
        rsp.addHeader("Access-Control-Max-Age", "86400");
    }
    private boolean hasPermission(String name, IHTTPSession session){
        try{
            String origin = session.getHeaders().get("origin");
            // unknown origin don`t have permission
            if (origin == null){
                return false;
            }
            return getPermissionControl().hasPermission(name, origin);
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
            return  RSP_404();
        }
        try {
            switch (path[2]) {
                case "app-list-all":
                    return doGetApplicationList(session);
                case "icon":
                    return doGetApplicationIcon(session);
                case "launch":
                    return doLaunchApplication(session);
                case "request-permission":
                    return doRequestPermission(session);
            }
        }catch (Exception e){
            e.printStackTrace();
            return RSP_500();
        }
        return RSP_404();
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
    private Response doRequestPermission(IHTTPSession session){
        Map<String, List<String>> params = session.getParameters();
        String name = params.get("name").get(0);
        Log.d("server", "doRequestPermission: reading origin...");
        String origin = session.getHeaders().get("origin");
        Log.d("server", String.valueOf(origin));
        boolean result = getPermissionControl().requestPermission(name, origin);
        Log.d("server", String.valueOf(result));
        if (result) {
            return RSP_200();
        }else{
            return RSP_403();
        }
    }
}
