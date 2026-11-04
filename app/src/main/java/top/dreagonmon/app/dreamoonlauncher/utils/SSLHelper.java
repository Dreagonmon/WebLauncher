package top.dreagonmon.app.dreamoonlauncher.utils;

import android.content.Context;
import android.content.res.AssetManager;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;

import javax.net.ssl.KeyManagerFactory;

public class SSLHelper {
    private KeyStore ks;
    private KeyManagerFactory fc;
    public SSLHelper(Context context){
        try {
            AssetManager am = context.getAssets();
            char[] key = "123456".toCharArray();
            // 显式尝试 BKS（server.bks 是 BKS v1 格式）。Android 10+ 的
            // getDefaultType() 返回 PKCS12，直接用它加载会抛异常被吞掉，
            // HTTPS 静默失效。保留 PKCS12 回退以防证书被换成 p12。
            this.ks = null;
            for (String type : new String[]{"BKS", "PKCS12"}) {
                try {
                    KeyStore ks = KeyStore.getInstance(type);
                    ks.load(am.open("server.bks"), key);
                    this.ks = ks;
                    break;
                } catch (Exception ignore) {
                    // try next type
                }
            }
            if (this.ks == null)
                throw new Exception("cannot load server.bks as BKS/PKCS12");
            this.fc = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            this.fc.init(this.ks, key);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public KeyStore getKeyStore(){return this.ks;}
    public KeyManagerFactory getKeyManagerFactory(){return this.fc;}
    public Certificate getCert(){
        try {
            return this.ks.getCertificate("launcher");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return null;
    }
}
