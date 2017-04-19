/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.os.Bundle
 *  android.util.Log
 *  android.view.View
 *  android.webkit.WebSettings
 *  android.webkit.WebView
 */
package net.openvpn.openvpn;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import java.io.IOException;
import java.util.Locale;
import net.openvpn.openvpn.FileUtil;
import net.openvpn.openvpn.OpenVPNClientBase;

public class OpenVPNHelp
extends OpenVPNClientBase {
    private static final String TAG = "OpenVPNHelp";

    @Override
    public void onCreate(Bundle bundle) {
        String string2;
        super.onCreate(bundle);
        String string3 = String.format("help/%s", Locale.getDefault().getLanguage());
        Log.d((String)"OpenVPNHelp", (String)String.format("Localized help directory: %s", string3));
        try {
            String string4;
            string2 = string4 = FileUtil.readAsset((Context)this, String.format("%s/index.html", string3));
        }
        catch (IOException var4_7) {
            string3 = "help/default";
            string2 = null;
        }
        if (string2 == null) {
            try {
                String string5;
                string2 = string5 = FileUtil.readAsset((Context)this, String.format("%s/index.html", string3));
            }
            catch (IOException var7_8) {
                Log.e((String)"OpenVPNHelp", (String)"error reading help file", (Throwable)var7_8);
                this.finish();
            }
        }
        WebView webView = new WebView((Context)this);
        webView.getSettings().setBuiltInZoomControls(true);
        this.setContentView((View)webView);
        webView.loadData(string2.replaceAll("\\n+", "%20"), "text/html", "UTF-8");
    }
}

