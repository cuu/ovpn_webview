/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.ComponentName
 *  android.content.Context
 *  android.content.Intent
 *  android.content.SharedPreferences
 *  android.os.Bundle
 *  android.os.Handler
 *  android.os.SystemClock
 *  android.preference.PreferenceManager
 *  android.text.Editable
 *  android.util.Log
 *  android.view.KeyEvent
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.Button
 *  android.widget.CheckBox
 *  android.widget.EditText
 *  android.widget.TextView
 *  android.widget.TextView$OnEditorActionListener
 */
package net.openvpn.openvpn;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import net.openvpn.openvpn.OpenVPNClientBase;
import net.openvpn.openvpn.OpenVPNService;
import net.openvpn.openvpn.PrefUtil;

public class OpenVPNProxyCreds
extends OpenVPNClientBase
implements View.OnClickListener,
TextView.OnEditorActionListener {
    private static final String TAG = "OpenVPNProxyCreds";
    Button cancel_button;
    Button ok_button;
    EditText password_edit;
    private PrefUtil prefs;
    TextView prev_creds_not_accepted_textview;
    String profile_name;
    String proxy_name;
    TextView proxy_title_textview;
    CheckBox remember_creds_checkbox;
    private Handler ui_reset_timer_handler = new Handler();
    private Runnable ui_reset_timer_task;
    EditText username_edit;

    public OpenVPNProxyCreds() {
        this.ui_reset_timer_task = new Runnable(){

            @Override
            public void run() {
                OpenVPNProxyCreds.this.gen_proxy_context_expired_event();
                OpenVPNProxyCreds.this.finish();
            }
        };
    }

    private void cancel_ui_reset() {
        this.ui_reset_timer_handler.removeCallbacks(this.ui_reset_timer_task);
    }

    private void schedule_ui_reset(long l) {
        this.cancel_ui_reset();
        this.ui_reset_timer_handler.postDelayed(this.ui_reset_timer_task, l);
    }

    private void stop() {
        this.doUnbindService();
    }

    public void onClick(View view) {
        Log.d((String)"OpenVPNProxyCreds", (String)"onClick");
        int n = view.getId();
        if (n != R.id.proxy_ok_button) {
            if (n != R.id.proxy_cancel_button) return;
            this.finish();
            return;
        }
        if (this.proxy_name != null && this.profile_name != null) {
        	String prefix = OpenVPNService.INTENT_PREFIX;
            this.startService(new Intent((Context)this, (Class)OpenVPNService.class).setAction(OpenVPNService.ACTION_SUBMIT_PROXY_CREDS).putExtra(prefix + ".PROFILE", this.profile_name).putExtra(prefix + ".PROXY_NAME", this.proxy_name).putExtra(prefix + ".PROXY_USERNAME", this.username_edit.getText().toString()).putExtra(prefix + ".PROXY_PASSWORD", this.password_edit.getText().toString()).putExtra(prefix + ".PROXY_REMEMBER_CREDS", this.remember_creds_checkbox.isChecked()));
        }
        this.finish();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(R.layout.proxy_creds);
        this.prefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences((Context)this));
        this.prev_creds_not_accepted_textview = (TextView)this.findViewById(R.id.prev_creds_not_accepted);
        this.proxy_title_textview = (TextView)this.findViewById(R.id.proxy_creds_title);
        this.username_edit = (EditText)this.findViewById(R.id.proxy_username);
        this.password_edit = (EditText)this.findViewById(R.id.proxy_password);
        this.remember_creds_checkbox = (CheckBox)this.findViewById(R.id.proxy_remember_creds);
        this.ok_button = (Button)this.findViewById(R.id.proxy_ok_button);
        this.cancel_button = (Button)this.findViewById(R.id.proxy_cancel_button);
        this.ok_button.setOnClickListener((View.OnClickListener)this);
        this.cancel_button.setOnClickListener((View.OnClickListener)this);
        this.password_edit.setOnEditorActionListener((TextView.OnEditorActionListener)this);
        this.doBindService();
    }

    protected void onDestroy() {
        Log.d((String)"OpenVPNProxyCreds", (String)"onDestroy");
        this.cancel_ui_reset();
        this.stop();
        super.onDestroy();
    }

    public boolean onEditorAction(TextView textView, int n, KeyEvent keyEvent) {
        if (!this.action_enter(n, keyEvent)) return false;
        if (textView != this.password_edit) return false;
        this.onClick((View)this.ok_button);
        return true;
    }

    @Override
    protected void post_bind() {
        long expire;
        Intent intent = this.getIntent();
        if (intent == null) return;
        this.profile_name = intent.getStringExtra("net.openvpn.openvpn.PROFILE");
        this.proxy_name = intent.getStringExtra("net.openvpn.openvpn.PROXY_NAME");
        if (this.proxy_name == null) {
            this.finish();
            return;
        }
        String string2 = this.resString(R.string.proxy_creds_title);
        Object[] arrobject = new Object[]{this.proxy_name};
        String string3 = String.format(string2, arrobject);
        this.proxy_title_textview.setText((CharSequence)string3);
        if (intent.getIntExtra("net.openvpn.openvpn.N_RETRIES", 0) > 0) {
            this.prev_creds_not_accepted_textview.setVisibility(0);
        }
        if ((expire = intent.getLongExtra("net.openvpn.openvpn.EXPIRES", 0)) <= 0) return;
        long remaining_time = expire - SystemClock.elapsedRealtime();
        if (remaining_time > 0) {
            this.schedule_ui_reset(remaining_time);
            return;
        }
        this.finish();
    }

}

