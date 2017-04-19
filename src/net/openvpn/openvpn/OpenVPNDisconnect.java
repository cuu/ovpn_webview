/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.os.Bundle
 *  android.util.Log
 */
package net.openvpn.openvpn;

import android.os.Bundle;
import android.util.Log;
import net.openvpn.openvpn.OpenVPNClientBase;

public class OpenVPNDisconnect extends OpenVPNClientBase {
    private static final String TAG = "OpenVPNDisconnect";

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d((String)"OpenVPNDisconnect", (String)"disconnect");
        this.submitDisconnectIntent(false);
        this.finish();
    }
}

