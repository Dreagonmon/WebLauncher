package top.dreagonmon.app.dreamoonlauncher.utils;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.InputStream;
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
            InputStream in = am.open("server.bks");
            char[] key = "123456".toCharArray();
            this.ks = KeyStore.getInstance(KeyStore.getDefaultType());
            this.ks.load(in, key);
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
