/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.os.Build
 *  android.os.Build$VERSION
 *  android.security.KeyChain
 *  android.util.Log
 */
package net.openvpn.openvpn;

import android.content.Context;
import android.os.Build;
import android.security.KeyChain;
import android.util.Log;
import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import net.openvpn.openvpn.JellyBeanHackBase;

public class JellyBeanHack extends JellyBeanHackBase {
    private static final String TAG = "JellyBeanHack";
    private String alias_;
    private PrivateKey pk_;
    private boolean rsa_sign_initialized = false;

    private JellyBeanHack() {
        this.resetPrivateKey();
        if (JellyBeanHack.rsa_sign_init() == 1) {
            this.rsa_sign_initialized = true;
        }
        Object[] arrobject = new Object[]{this.rsa_sign_initialized};
        Log.i((String)"JellyBeanHack", (String)String.format("JellyBeanHack: rsa_sign_initialized=%b", arrobject));
    }

    private PrivateKey get_pk(String string2) {
        synchronized (this) {
            if (this.alias_ == null) return null;
            if (!this.alias_.equals(string2)) return null;
            return this.pk_;
        }
    }

    public static JellyBeanHack newJellyBeanHack() {
        Object[] arrobject = new Object[]{Build.VERSION.SDK_INT};
        Log.i((String)"JellyBeanHack", (String)String.format("Build.VERSION.SDK_INT=%d", arrobject));
        if (Build.VERSION.SDK_INT != 16) return null;
        return new JellyBeanHack();
    }

    private static int openssl_pkey(PrivateKey privateKey) throws Exception {
        if (privateKey == null) return 0;
        Method method = privateKey.getClass().getSuperclass().getDeclaredMethod("getOpenSSLKey", new Class[0]);
        method.setAccessible(true);
        Object object = method.invoke(privateKey, new Object[0]);
        method.setAccessible(false);
        Method method2 = object.getClass().getDeclaredMethod("getPkeyContext", new Class[0]);
        method2.setAccessible(true);
        int n = (Integer)method2.invoke(object, new Object[0]);
        method2.setAccessible(false);
        return n;
    }

    private static native void pkey_retain(int var0);

    private static native byte[] rsa_sign(byte[] var0, int var1) throws InvalidKeyException;

    private static native int rsa_sign_init();

    private PrivateKey set_pk(String string2, PrivateKey privateKey) throws Exception {
        synchronized (this) {
            this.alias_ = null;
            this.pk_ = null;
            if (privateKey == null) return this.pk_;
            JellyBeanHack.pkey_retain(JellyBeanHack.openssl_pkey(privateKey));
            if (string2 == null) return this.pk_;
            if (string2.length() <= 0) return this.pk_;
            this.alias_ = string2;
            this.pk_ = privateKey;
            return this.pk_;
        }
    }

    public boolean enabled() {
        return this.rsa_sign_initialized;
    }

    public PrivateKey getPrivateKey(Context context, String string2) throws Exception {
        synchronized (this) {
            PrivateKey privateKey;
            PrivateKey privateKey2 = this.get_pk(string2);
            if (privateKey2 != null) return privateKey2;
            privateKey2 = privateKey = this.set_pk(string2, KeyChain.getPrivateKey((Context)context, (String)string2));
            return privateKey2;
        }
    }

    public void resetPrivateKey() {
        synchronized (this) {
            Log.i((String)"JellyBeanHack", (String)"JellyBeanHack: resetPrivateKey");
            this.alias_ = null;
            this.pk_ = null;
            return;
        }
    }

    public byte[] rsaSign(PrivateKey privateKey, byte[] arrby) throws Exception {
        synchronized (this) {
            return JellyBeanHack.rsa_sign(arrby, JellyBeanHack.openssl_pkey(privateKey));
        }
    }
}

