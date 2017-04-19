/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.os.Bundle
 *  android.text.method.MovementMethod
 *  android.text.method.ScrollingMovementMethod
 *  android.util.Log
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.Button
 *  android.widget.ScrollView
 *  android.widget.TextView
 */
package net.openvpn.openvpn;

import android.os.Bundle;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import net.openvpn.openvpn.OpenVPNClientBase;
import net.openvpn.openvpn.OpenVPNService;

public class OpenVPNLog
extends OpenVPNClientBase
implements View.OnClickListener {
    private static final String TAG = "OpenVPNClientLog";
    private Button mPause;
    private Button mResume;
    private ScrollView mScrollView;
    private TextView mTextView;
    private ArrayList<OpenVPNService.LogMsg> pause_buffer;

    private void refresh_log_view() {
        ArrayDeque<OpenVPNService.LogMsg> arrayDeque = this.log_history();
        if (arrayDeque == null) return;
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<OpenVPNService.LogMsg> iterator = arrayDeque.iterator();
        do {
            if (!iterator.hasNext()) {
                this.mTextView.setText((CharSequence)stringBuilder.toString());
                this.scroll_textview_to_bottom();
                return;
            }
            stringBuilder.append(iterator.next().line);
        } while (true);
    }

    private void scroll_textview_to_bottom() {
        this.mScrollView.post(new Runnable(){

            @Override
            public void run() {
                OpenVPNLog.this.mScrollView.smoothScrollTo(0, OpenVPNLog.this.mTextView.getBottom());
            }
        });
    }

    private void set_pause_state(boolean bl) {
        if (bl) {
            this.mPause.setVisibility(8);
            this.mResume.setVisibility(0);
            this.pause_buffer = new ArrayList();
            return;
        }
        this.mPause.setVisibility(0);
        this.mResume.setVisibility(8);
        if (this.pause_buffer == null) return;
        Iterator<OpenVPNService.LogMsg> iterator = this.pause_buffer.iterator();
        do {
            if (!iterator.hasNext()) {
                this.scroll_textview_to_bottom();
                this.pause_buffer = null;
                return;
            }
            OpenVPNService.LogMsg logMsg = iterator.next();
            this.mTextView.append((CharSequence)logMsg.line);
        } while (true);
    }

    private void stop() {
        this.doUnbindService();
    }

    @Override
    public void log(OpenVPNService.LogMsg logMsg) {
        if (this.pause_buffer == null) {
            this.mTextView.append((CharSequence)logMsg.line);
            this.scroll_textview_to_bottom();
            return;
        }
        this.pause_buffer.add(logMsg);
    }

    public void onClick(View view) {
        Log.d((String)"OpenVPNClientLog", (String)"LOG: onClick");
        int n = view.getId();
        if (n == R.id.log_pause) {
            Log.d((String)"OpenVPNClientLog", (String)"LOG: onClick PAUSE");
            this.set_pause_state(true);
            return;
        }
        if (n != R.id.log_resume) return;
        Log.d((String)"OpenVPNClientLog", (String)"LOG: onClick RESUME");
        this.set_pause_state(false);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(R.layout.log);
        this.mTextView = (TextView)this.findViewById(R.id.log_textview);
        this.mScrollView = (ScrollView)this.findViewById(R.id.log_scrollview);
        this.mPause = (Button)this.findViewById(R.id.log_pause);
        this.mResume = (Button)this.findViewById(R.id.log_resume);
        this.mPause.setOnClickListener((View.OnClickListener)this);
        this.mResume.setOnClickListener((View.OnClickListener)this);
        this.mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
        this.doBindService();
    }

    protected void onDestroy() {
        Log.d((String)"OpenVPNClientLog", (String)"LOG: onDestroy");
        this.stop();
        super.onDestroy();
    }

    @Override
    protected void post_bind() {
        Log.d((String)"OpenVPNClientLog", (String)"LOG: post_bind");
        this.refresh_log_view();
        this.set_pause_state(false);
    }

}

