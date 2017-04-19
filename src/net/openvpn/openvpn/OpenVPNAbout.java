/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.content.pm.PackageInfo
 *  android.content.pm.PackageManager
 *  android.os.Bundle
 *  android.util.Log
 *  android.view.View
 *  android.widget.TextView
 */
package net.openvpn.openvpn;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.io.IOException;
import net.openvpn.openvpn.FileUtil;
import net.openvpn.openvpn.OpenVPNClientBase;

public class OpenVPNAbout
extends OpenVPNClientBase {
    private static final String TAG = "OpenVPNAbout";

    private TextView get_text_view(int n) {
        return (TextView)this.findViewById(n);
    }

    @Override
    public void onCreate(Bundle bundle) {
        int n;
        super.onCreate(bundle);
        this.setContentView(2130903040);
        String string2 = "0.0";
        try {
            PackageInfo packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            string2 = packageInfo.versionName;
            n = packageInfo.versionCode;
        }
        catch (Exception var3_11) {
            Log.e((String)"OpenVPNAbout", (String)"cannot obtain version info", (Throwable)var3_11);
            n = 0;
        }
        String string3 = this.resString(2131099653);
        Object[] arrobject = new Object[]{string2, n};
        String string4 = String.format(string3, arrobject);
        this.get_text_view(2131361792).setText((CharSequence)string4);
        this.get_text_view(2131361793).setText((CharSequence)OpenVPNAbout.get_openvpn_core_platform());
        String string5 = OpenVPNAbout.get_app_expire_string();
        if (string5 != null) {
            String string6 = String.format(this.resString(2131099654), string5);
            this.get_text_view(2131361795).setText((CharSequence)string6);
        } else {
            this.findViewById(2131361794).setVisibility(8);
        }
        try {
            String string7 = FileUtil.readAsset((Context)this, "about.txt");
            this.get_text_view(2131361797).setText((CharSequence)string7);
            return;
        }
        catch (IOException var10_12) {
            Log.e((String)"OpenVPNAbout", (String)"Error opening about.txt", (Throwable)var10_12);
            return;
        }
    }
}

