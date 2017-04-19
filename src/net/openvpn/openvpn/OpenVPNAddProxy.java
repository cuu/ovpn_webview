/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.content.Intent
 *  android.content.SharedPreferences
 *  android.os.Bundle
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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import net.openvpn.openvpn.PrefUtil;
import net.openvpn.openvpn.ProxyList;

public class OpenVPNAddProxy
extends OpenVPNClientBase
implements View.OnClickListener,
TextView.OnEditorActionListener {
    private static final String TAG = "OpenVPNAddProxy";
    CheckBox allow_cleartext_auth_checkbox;
    Button cancel_button;
    EditText friendly_name_edit;
    EditText host_edit;
    String mod_proxy_name;
    EditText port_edit;
    private PrefUtil prefs;
    Button save_button;
    TextView title_textview;

    private void stop() {
        this.doUnbindService();
    }

    public void onClick(View view) {
        Log.d((String)"OpenVPNAddProxy", (String)"onClick");
        int n = view.getId();
        if (n != R.id.proxy_save_button) {
            if (n != R.id.proxy_cancel_button) return;
            this.finish();
            return;
        }
        ProxyList proxyList = this.get_proxy_list();
        if (proxyList == null) {
            Log.d((String)"OpenVPNAddProxy", (String)"proxy_list is null on save!");
            this.finish();
            return;
        }
        ProxyList.Item item = new ProxyList.Item();
        String string2 = this.friendly_name_edit.getText().toString().trim();
        if (string2.length() > 0) {
            item.friendly_name = string2;
        }
        item.host = this.host_edit.getText().toString().trim();
        item.port = this.port_edit.getText().toString().trim();
        item.allow_cleartext_auth = this.allow_cleartext_auth_checkbox.isChecked();
        if (!item.is_valid()) return;
        String string3 = item.name();
        if (!string3.equals(this.mod_proxy_name)) {
            proxyList.remove(this.mod_proxy_name);
        }
        proxyList.put(item);
        proxyList.set_enabled(string3);
        proxyList.save();
        this.gen_ui_reset_event(false);
        this.finish();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(R.layout.add_proxy);
        this.prefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences((Context)this));
        this.title_textview = (TextView)this.findViewById(R.id.proxy_title);
        this.friendly_name_edit = (EditText)this.findViewById(R.id.proxy_friendly_name);
        this.host_edit = (EditText)this.findViewById(R.id.proxy_host);
        this.port_edit = (EditText)this.findViewById(R.id.proxy_port);
        this.allow_cleartext_auth_checkbox = (CheckBox)this.findViewById(R.id.proxy_allow_cleartext_auth_checkbox);
        this.save_button = (Button)this.findViewById(R.id.proxy_save_button);
        this.cancel_button = (Button)this.findViewById(R.id.proxy_cancel_button);
        this.save_button.setOnClickListener((View.OnClickListener)this);
        this.cancel_button.setOnClickListener((View.OnClickListener)this);
        this.port_edit.setOnEditorActionListener((TextView.OnEditorActionListener)this);
        this.doBindService();
    }

    protected void onDestroy() {
        Log.d((String)"OpenVPNAddProxy", (String)"onDestroy");
        this.stop();
        super.onDestroy();
    }

    public boolean onEditorAction(TextView textView, int n, KeyEvent keyEvent) {
        if (!this.action_enter(n, keyEvent)) return false;
        if (textView != this.port_edit) return false;
        this.onClick((View)this.save_button);
        return true;
    }

    @Override
    protected void post_bind() {
        Intent intent = this.getIntent();
        if (intent == null) return;
        this.mod_proxy_name = intent.getStringExtra("net.openvpn.openvpn.PROXY_NAME");
        if (this.mod_proxy_name != null) {
            this.title_textview.setText(R.string.proxy_title_modify);
        }
        ProxyList proxyList = this.get_proxy_list();
        if (this.mod_proxy_name == null) return;
        if (proxyList == null) return;
        ProxyList.Item item = proxyList.get(this.mod_proxy_name);
        if (item == null) return;
        if (item.friendly_name != null) {
            this.friendly_name_edit.setText((CharSequence)item.friendly_name);
        }
        this.host_edit.setText((CharSequence)item.host);
        this.port_edit.setText((CharSequence)item.port);
        this.allow_cleartext_auth_checkbox.setChecked(item.allow_cleartext_auth);
    }
}

