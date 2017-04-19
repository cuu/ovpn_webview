/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.app.AlertDialog
 *  android.app.AlertDialog$Builder
 *  android.content.Context
 *  android.content.DialogInterface
 *  android.content.DialogInterface$OnCancelListener
 *  android.content.DialogInterface$OnClickListener
 *  android.os.Handler
 *  android.text.format.DateFormat
 *  android.util.Log
 *  android.view.LayoutInflater
 *  android.view.View
 *  android.view.ViewGroup
 *  android.widget.TextView
 */
package net.openvpn.openvpn;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import javax.security.auth.x500.X500Principal;

abstract class CertWarn
implements DialogInterface.OnCancelListener,
DialogInterface.OnClickListener {
    public static final int RESPONSE_ACCEPT = 1;
    public static final int RESPONSE_REJECT = 0;
    private static final String TAG = "CertWarn";

    public CertWarn(Context context, X509Certificate x509Certificate, String string2) {
        Runnable runnable = new Runnable(){

            @Override
            public void run() {
                CertWarn.this.done(0);
            }
        };
        try {
            new AlertDialog.Builder(context).setTitle(R.string.cert_warn_title).setView(this.inflateCertificateView(context, x509Certificate, string2)).setPositiveButton(R.string.cert_warn_accept, (DialogInterface.OnClickListener)this).setNegativeButton(R.string.cert_warn_reject, (DialogInterface.OnClickListener)this).setOnCancelListener((DialogInterface.OnCancelListener)this).create().show();
            return;
        }
        catch (Exception var5_5) {
            Log.e((String)"CertWarn", (String)"AlertDialog error", (Throwable)var5_5);
            new Handler().postDelayed(runnable, 0);
            return;
        }
    }

    private static final String fingerprint(byte[] arrby) {
        if (arrby == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        int n = 0;
        while (n < arrby.length) {
            byte by = arrby[n];
            Object[] arrobject = new Object[]{Byte.valueOf(by)};
            stringBuilder.append(String.format("%02X", arrobject));
            if (n + 1 != arrby.length) {
                stringBuilder.append(':');
            }
            ++n;
        }
        return stringBuilder.toString();
    }

    private static String formatCertificateDate(Date date, java.text.DateFormat dateFormat) {
        if (date != null) return dateFormat.format(date);
        return "";
    }

    private static String getDigest(X509Certificate x509Certificate, String string2) {
        if (x509Certificate == null) {
            return "";
        }
        try {
            byte[] arrby = x509Certificate.getEncoded();
            return CertWarn.fingerprint(MessageDigest.getInstance(string2).digest(arrby));
        }
        catch (CertificateEncodingException var3_4) {
            return "";
        }
        catch (NoSuchAlgorithmException var2_5) {
            return "";
        }
    }

    private static String getSerialNumber(X509Certificate x509Certificate) {
        if (x509Certificate == null) {
            return "";
        }
        BigInteger bigInteger = x509Certificate.getSerialNumber();
        if (bigInteger != null) return CertWarn.fingerprint(bigInteger.toByteArray());
        return "";
    }

    private View inflateCertificateView(Context context, X509Certificate x509Certificate, String string2) {
        View view = LayoutInflater.from((Context)context).inflate(R.layout.cert_warn, null);
        java.text.DateFormat dateFormat = DateFormat.getDateFormat((Context)context);
        ((TextView)view.findViewById(R.id.cert_error)).setText((CharSequence)string2);
        HashMap<String, String> hashMap = CertWarn.parse_dn(x509Certificate.getIssuerX500Principal());
        HashMap<String, String> hashMap2 = CertWarn.parse_dn(x509Certificate.getSubjectX500Principal());
        ((TextView)view.findViewById(R.id.to_common)).setText((CharSequence)hashMap2.get("CN"));
        ((TextView)view.findViewById(R.id.to_org)).setText((CharSequence)hashMap2.get("O"));
        ((TextView)view.findViewById(R.id.to_org_unit)).setText((CharSequence)hashMap2.get("OU"));
        ((TextView)view.findViewById(R.id.by_common)).setText((CharSequence)hashMap.get("CN"));
        ((TextView)view.findViewById(R.id.by_org)).setText((CharSequence)hashMap.get("O"));
        ((TextView)view.findViewById(R.id.by_org_unit)).setText((CharSequence)hashMap.get("OU"));
        ((TextView)view.findViewById(R.id.serial_number)).setText((CharSequence)CertWarn.getSerialNumber(x509Certificate));
        String string3 = CertWarn.formatCertificateDate(x509Certificate.getNotBefore(), dateFormat);
        ((TextView)view.findViewById(R.id.issued_on)).setText((CharSequence)string3);
        String string4 = CertWarn.formatCertificateDate(x509Certificate.getNotAfter(), dateFormat);
        ((TextView)view.findViewById(R.id.expires_on)).setText((CharSequence)string4);
        ((TextView)view.findViewById(R.id.sha256_fingerprint)).setText((CharSequence)CertWarn.getDigest(x509Certificate, "SHA256"));
        ((TextView)view.findViewById(R.id.sha1_fingerprint)).setText((CharSequence)CertWarn.getDigest(x509Certificate, "SHA1"));
        return view;
    }

    private static HashMap<String, String> parse_dn(String string2) {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        StringBuilder[] arrstringBuilder = new StringBuilder[]{new StringBuilder(), new StringBuilder()};
        int n = 0;
        boolean bl = false;
        int n2 = 0;
        do {
            if (n2 >= string2.length()) {
                if (arrstringBuilder[0].length() <= 0) return hashMap;
                if (arrstringBuilder[1].length() <= 0) return hashMap;
                hashMap.put(arrstringBuilder[0].toString(), arrstringBuilder[1].toString());
                return hashMap;
            }
            char c = string2.charAt(n2);
            if (!bl && c == '\\') {
                bl = true;
            } else if (!bl && c == '=') {
                n = 1;
            } else if (!bl && c == ',') {
                if (arrstringBuilder[0].length() > 0 && arrstringBuilder[1].length() > 0) {
                    hashMap.put(arrstringBuilder[0].toString(), arrstringBuilder[1].toString());
                    arrstringBuilder[0].setLength(0);
                    arrstringBuilder[1].setLength(0);
                }
                n = 0;
            } else {
                StringBuilder stringBuilder = arrstringBuilder[n];
                if (stringBuilder.length() > 0 || c != ' ') {
                    stringBuilder.append(c);
                }
                bl = false;
            }
            ++n2;
        } while (true);
    }

    private static HashMap<String, String> parse_dn(X500Principal x500Principal) {
        return CertWarn.parse_dn(x500Principal.getName("RFC2253"));
    }

    protected abstract void done(int var1);

    public void onCancel(DialogInterface dialogInterface) {
        this.done(0);
    }

    public void onClick(DialogInterface dialogInterface, int n) {
        dialogInterface.dismiss();
        switch (n) {
            default: {
                this.done(0);
                return;
            }
            case -1: 
        }
        this.done(1);
    }

}

