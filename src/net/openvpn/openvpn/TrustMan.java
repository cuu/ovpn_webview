/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.util.Log
 */
package net.openvpn.openvpn;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class TrustMan implements X509TrustManager {
    private static final String KEYSTORE_FILE = "trusted-certs.keystore";
    private static final String TAG = "TrustMan";
    private static int generation = 0;
    private KeyStore appKeyStore;
    private X509TrustManager appTrustManager;
    private int current_generation;
    private X509TrustManager defaultTrustManager;
    private File keyStoreFile;
    private Callback parent;

    public TrustMan(Context context) throws Error {
        File file = context.getFilesDir();
        this.keyStoreFile = new File(file + File.separator + "trusted-certs.keystore");
        this.reload();
    }

    private void callOnTrustSucceed(boolean bl) {
        if (this.parent == null) return;
        this.parent.onTrustSucceed(bl);
    }

    /*
     * Enabled unnecessary exception pruning
     */
    private void checkCertTrusted(X509Certificate[] arrx509Certificate, String string2, boolean bl) throws CertificateException {
        Log.d((String)"TrustMan", (String)("checkCertTrusted(" + arrx509Certificate + ", " + string2 + ", " + bl + ")"));
        this.check_reload();
        try {
            Log.d((String)"TrustMan", (String)"checkCertTrusted: trying appTrustManager");
            if (bl) {
                this.appTrustManager.checkServerTrusted(arrx509Certificate, string2);
            } else {
                this.appTrustManager.checkClientTrusted(arrx509Certificate, string2);
            }
            this.callOnTrustSucceed(true);
            return;
        }
        catch (CertificateException var5_4) {
            if (this.isExpiredException(var5_4)) {
                Log.d((String)"TrustMan", (String)"checkCertTrusted: accepting expired certificate from keystore");
                this.callOnTrustSucceed(true);
                return;
            }
            if (this.isCertKnown(arrx509Certificate[0])) {
                Log.d((String)"TrustMan", (String)"checkCertTrusted: accepting cert already stored in keystore");
                this.callOnTrustSucceed(true);
                return;
            }
            try {
                Log.d((String)"TrustMan", (String)"checkCertTrusted: trying defaultTrustManager");
                if (bl) {
                    this.defaultTrustManager.checkServerTrusted(arrx509Certificate, string2);
                } else {
                    this.defaultTrustManager.checkClientTrusted(arrx509Certificate, string2);
                }
                this.callOnTrustSucceed(false);
                return;
            }
            catch (CertificateException var6_5) {
                TrustContext trustContext = new TrustContext();
                trustContext.chain = arrx509Certificate;
                trustContext.authType = string2;
                trustContext.excep = var6_5;
                if (this.parent == null) throw new TrustFail(var6_5);
                this.parent.onTrustFail(trustContext);
                throw new TrustFail(var6_5);
            }
        }
    }

    private void check_reload() {
        try {
            if (this.current_generation == generation) return;
            this.reload();
            return;
        }
        catch (Error var1_1) {
            Log.e((String)"TrustMan", (String)"check_reload", (Throwable)var1_1);
            return;
        }
    }

    public static void forget_certs(Context context) {
        boolean bl = context.deleteFile("trusted-certs.keystore");
        generation = 1 + generation;
        Object[] arrobject = new Object[]{"trusted-certs.keystore", bl, generation};
        Log.d((String)"TrustMan", (String)String.format("forget certs: fn=%s status=%b gen=%d", arrobject));
    }

    private X509TrustManager getTrustManager(KeyStore keyStore, String string2) {
        TrustManager[] arrtrustManager;
        int n;
        int n2;
        block4 : {
            try {
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
                trustManagerFactory.init(keyStore);
                arrtrustManager = trustManagerFactory.getTrustManagers();
                n2 = arrtrustManager.length;
                n = 0;
                break block4;
            }
            catch (Exception var3_9) {
                Log.e((String)"TrustMan", (String)("getTrustManager(" + keyStore + "," + string2 + ")"), (Throwable)var3_9);
            }
            return null;
        }
        while (n < n2) {
            TrustManager trustManager = arrtrustManager[n];
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager)trustManager;
            }
            ++n;
        }
        return null;
    }

    private boolean isCertKnown(X509Certificate x509Certificate) {
        try {
            String string2 = this.appKeyStore.getCertificateAlias(x509Certificate);
            boolean bl = false;
            if (string2 == null) return bl;
            return true;
        }
        catch (KeyStoreException var2_4) {
            return false;
        }
    }

    private boolean isExpiredException(Throwable throwable) {
        do {
            if (!(throwable instanceof CertificateExpiredException)) continue;
            return true;
        } while ((throwable = throwable.getCause()) != null);
        return false;
    }

    public static boolean isTrustFail(Exception exception) {
        Throwable throwable = exception;
        while (throwable != null) {
            if (throwable instanceof TrustFail) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    private KeyStore loadAppKeyStore() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        }
        catch (KeyStoreException var1_2) {
            Log.e((String)"TrustMan", (String)"loadAppKeyStore()", (Throwable)var1_2);
            return null;
        }
        try {
            keyStore.load(null, null);
            keyStore.load(new FileInputStream(this.keyStoreFile), "OpenVPN".toCharArray());
            return keyStore;
        }
        catch (FileNotFoundException var6_3) {
            Log.d((String)"TrustMan", (String)("loadAppKeyStore(" + this.keyStoreFile + ") - file does not exist"));
            return keyStore;
        }
        catch (Exception var4_4) {
            Log.e((String)"TrustMan", (String)("loadAppKeyStore(" + this.keyStoreFile + ")"), (Throwable)var4_4);
            return keyStore;
        }
    }

    private void reload() throws Error {
        Object[] arrobject = new Object[]{this.current_generation, generation};
        Log.d((String)"TrustMan", (String)String.format("reload certs: gen=%d/%d", arrobject));
        KeyStore keyStore = this.loadAppKeyStore();
        if (keyStore == null) {
            throw new Error("could not load appKeyStore");
        }
        X509TrustManager x509TrustManager = this.getTrustManager(null, "default");
        if (x509TrustManager == null) {
            throw new Error("could not load defaultTrustManager");
        }
        X509TrustManager x509TrustManager2 = this.getTrustManager(keyStore, "app-init");
        if (x509TrustManager2 == null) {
            throw new Error("could not load appTrustManager");
        }
        this.current_generation = generation;
        this.appKeyStore = keyStore;
        this.defaultTrustManager = x509TrustManager;
        this.appTrustManager = x509TrustManager2;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] arrx509Certificate, String string2) throws CertificateException {
        this.checkCertTrusted(arrx509Certificate, string2, false);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] arrx509Certificate, String string2) throws CertificateException {
        this.checkCertTrusted(arrx509Certificate, string2, true);
    }

    public void clearCallback() {
        this.parent = null;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        this.check_reload();
        return this.defaultTrustManager.getAcceptedIssuers();
    }

    public void setCallback(Callback callback) {
        this.parent = callback;
    }

    public void trustCert(TrustContext trustContext) {
        block4 : {
            Log.d((String)"TrustMan", (String)("trust cert: " + trustContext.toString()));
            try {
                this.appKeyStore.setCertificateEntry(trustContext.chain[0].getSubjectDN().toString(), trustContext.chain[0]);
                X509TrustManager x509TrustManager = this.getTrustManager(this.appKeyStore, "app-reload");
                if (x509TrustManager == null) break block4;
                this.appTrustManager = x509TrustManager;
            }
            catch (KeyStoreException var3_4) {
                Log.e((String)"TrustMan", (String)("trustCert(" + trustContext.chain + ")"), (Throwable)var3_4);
                return;
            }
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(this.keyStoreFile);
            this.appKeyStore.store(fileOutputStream, "OpenVPN".toCharArray());
            fileOutputStream.close();
            return;
        }
        catch (Exception var7_5) {
            Log.e((String)"TrustMan", (String)("trustCert(" + this.keyStoreFile + ")"), (Throwable)var7_5);
            return;
        }
    }

    public static interface Callback {
        public void onTrustFail(TrustContext var1);

        public void onTrustSucceed(boolean var1);
    }

    public static class Error
    extends Exception {
        public Error(String string2) {
            super("TrustMan: " + string2);
        }
    }

    public static class TrustContext {
        public String authType;
        public X509Certificate[] chain;
        public CertificateException excep;

        public String toString() {
            return "TrustContext chain=" + this.chain + " authType=" + this.authType + " excep=" + this.excep;
        }
    }

    public static class TrustFail
    extends CertificateException {
        TrustFail(CertificateException certificateException) {
            super(certificateException);
        }
    }

}

