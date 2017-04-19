/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.app.Application
 *  android.content.Context
 */
package net.openvpn.openvpn;

import android.app.Application;
import android.content.Context;

public class OpenVPNApplication
extends Application {
    public static Context context;

    public static String resString(int n) {
        return context.getString(n);
    }

    public void onCreate() {
        super.onCreate();
        context = this.getApplicationContext();
    }
}

