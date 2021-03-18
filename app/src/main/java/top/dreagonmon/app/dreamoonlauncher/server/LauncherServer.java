package top.dreagonmon.app.dreamoonlauncher.server;

import android.app.AlertDialog;
import android.util.Log;

import java.io.IOException;
import java.util.Map;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.util.IHandler;

import top.dreagonmon.app.dreamoonlauncher.MainActivity;
import top.dreagonmon.app.dreamoonlauncher.R;
import top.dreagonmon.app.dreamoonlauncher.utils.SSLHelper;

public class LauncherServer extends NanoHTTPD {
    private final MainActivity viewContext;
    public LauncherServer(MainActivity context, int port){
        super("0.0.0.0",port);
        this.viewContext = context;
        SSLHelper hp = new SSLHelper(context);
        try {
            this.makeSecure(NanoHTTPD.makeSSLSocketFactory(hp.getKeyStore(),hp.getKeyManagerFactory()),null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LauncherServerHandler handler = new LauncherServerHandler(context);
        this.setHTTPHandler(handler);
    }
    public void startServer(){
        try {
            super.start(NanoHTTPD.SOCKET_READ_TIMEOUT,false);
        } catch (IOException e) {
            e.printStackTrace();
            new AlertDialog.Builder(this.viewContext)
                    .setMessage(R.string.text_server_start_failed)
                    .setPositiveButton(R.string.dialog_confirm, null)
                    .setCancelable(false)
                    .show();
        }
    }
    public void stopServer(){
        super.stop();
    }
}
