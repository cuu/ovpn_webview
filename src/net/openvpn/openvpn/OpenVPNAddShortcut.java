/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.content.SharedPreferences
 *  android.os.Bundle
 *  android.preference.PreferenceManager
 *  android.text.Editable
 *  android.util.Log
 *  android.view.KeyEvent
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.widget.AdapterView
 *  android.widget.AdapterView$OnItemSelectedListener
 *  android.widget.Button
 *  android.widget.EditText
 *  android.widget.Spinner
 *  android.widget.TextView
 *  android.widget.TextView$OnEditorActionListener
 */
package net.openvpn.openvpn;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import net.openvpn.openvpn.OpenVPNClientBase;
import net.openvpn.openvpn.OpenVPNService;
import net.openvpn.openvpn.PrefUtil;
import net.openvpn.openvpn.SpinUtil;

public class OpenVPNAddShortcut
extends OpenVPNClientBase
implements View.OnClickListener,
AdapterView.OnItemSelectedListener,
TextView.OnEditorActionListener {
    private static final String TAG = "OpenVPNAddShortcut";
    private Button cancel_button;
    private Button create_button;
    private PrefUtil prefs;
    private Spinner profile_spin;
    private EditText shortcut_name_edit;

    private void set_shortcut_name(String string2) {
        if (string2 == null) return;
        this.shortcut_name_edit.setText((CharSequence)string2);
        this.shortcut_name_edit.selectAll();
        this.shortcut_name_edit.requestFocus();
    }

    private void stop() {
        this.doUnbindService();
    }

    public void onClick(View view) {
        Log.d((String)"OpenVPNAddShortcut", (String)"onClick");
        if (view.getId() == 2131361812) {
            this.createConnectShortcut(SpinUtil.get_spinner_selected_item(this.profile_spin), this.shortcut_name_edit.getText().toString());
        }
        this.finish();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(2130903042);
        this.prefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences((Context)this));
        this.profile_spin = (Spinner)this.findViewById(2131361809);
        this.shortcut_name_edit = (EditText)this.findViewById(2131361810);
        this.create_button = (Button)this.findViewById(2131361812);
        this.cancel_button = (Button)this.findViewById(2131361811);
        this.create_button.setOnClickListener((View.OnClickListener)this);
        this.cancel_button.setOnClickListener((View.OnClickListener)this);
        this.profile_spin.setOnItemSelectedListener((AdapterView.OnItemSelectedListener)this);
        this.shortcut_name_edit.setOnEditorActionListener((TextView.OnEditorActionListener)this);
        this.doBindService();
    }

    protected void onDestroy() {
        Log.d((String)"OpenVPNAddShortcut", (String)"onDestroy");
        this.stop();
        super.onDestroy();
    }

    public boolean onEditorAction(TextView textView, int n, KeyEvent keyEvent) {
        if (!this.action_enter(n, keyEvent)) return false;
        if (textView != this.shortcut_name_edit) return false;
        this.onClick((View)this.create_button);
        return true;
    }

    public void onItemSelected(AdapterView<?> adapterView, View view, int n, long l) {
        this.set_shortcut_name(SpinUtil.get_spinner_selected_item(this.profile_spin));
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    @Override
    protected void post_bind() {
        String string2;
        String[] arrstring;
        OpenVPNService.ProfileList profileList = this.profile_list();
        if (profileList != null && profileList.size() > 0) {
            string2 = this.current_profile().get_name();
            arrstring = profileList.profile_names();
        } else {
            this.profile_spin.setEnabled(false);
            this.shortcut_name_edit.setEnabled(false);
            this.create_button.setEnabled(false);
            string2 = this.resString(2131099799);
            arrstring = new String[]{string2};
        }
        SpinUtil.show_spinner((Context)this, this.profile_spin, arrstring);
        SpinUtil.set_spinner_selected_item(this.profile_spin, string2);
        this.set_shortcut_name(string2);
    }
}

