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
 *  android.widget.ArrayAdapter
 *  android.widget.AutoCompleteTextView
 *  android.widget.Button
 *  android.widget.CheckBox
 *  android.widget.EditText
 *  android.widget.ListAdapter
 *  android.widget.ProgressBar
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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.openvpn.openvpn.HttpsClient;
import net.openvpn.openvpn.OpenVPNClientBase;
import net.openvpn.openvpn.OpenVPNDebug;
import net.openvpn.openvpn.PrefUtil;

public class OpenVPNImportProfile extends OpenVPNClientBase implements View.OnClickListener, TextView.OnEditorActionListener,HttpsClient.CancelDetect.I {
    private static final String TAG = "OpenVPNImportProfile";
    private int generation;
    private PrefUtil prefs;
    private Set<String> server_history;

    static /* synthetic */ int access108(OpenVPNImportProfile openVPNImportProfile) {
        int n = openVPNImportProfile.generation;
        openVPNImportProfile.generation = n + 1;
        return n;
    }

    private void add_to_server_history(String string2) {
        this.server_history.add(string2);
        this.prefs.set_string_set("import_server_history", this.server_history);
    }

    private void clear_auth() {
        EditText editText = (EditText)this.findViewById(R.id.password);
        if (editText == null) return;
        editText.setText((CharSequence)"");
    }

    public static void forget_server_history(PrefUtil prefUtil) {
        prefUtil.delete_key("import_server_history");
    }

    private void set_server_history_autocomplete() {
        Object[] arrobject = (String[])Arrays.copyOf(this.server_history.toArray(), this.server_history.size(), String[].class);
        ///this.findViewById(R.id.import_server).setAdapter((ListAdapter)new ArrayAdapter((Context)this, R.layout.import_server_item, arrobject));
    }

    private void set_ui_state(boolean bl) {
        Button button = (Button)this.findViewById(R.id.import_button);
        ProgressBar progressBar = (ProgressBar)this.findViewById(R.id.import_progress);
        if (bl) {
            button.setEnabled(false);
            progressBar.setVisibility(0);
            return;
        }
        button.setEnabled(true);
        progressBar.setVisibility(8);
    }

    private void stop() {
        this.generation = 1 + this.generation;
        this.clear_auth();
        this.doUnbindService();
    }

    public int cancel_generation() {
        return this.generation;
    }

    public void onClick(View view) {
        Log.d((String)"OpenVPNImportProfile", (String)"onClick");
        int n = view.getId();
        if (n == R.id.import_button) {
            this.generation = 1 + this.generation;
            EditText editText = (EditText)this.findViewById(R.id.import_server);
            EditText editText2 = (EditText)this.findViewById(R.id.username);
            EditText editText3 = (EditText)this.findViewById(R.id.password);
            CheckBox checkBox = (CheckBox)this.findViewById(R.id.import_autologin_checkbox);
            final String string2 = editText.getText().toString();
            String string3 = editText2.getText().toString();
            HttpsClient.AuthContext authContext = new HttpsClient.AuthContext(string2, string3, OpenVPNDebug.pw_repl(string3, editText3.getText().toString()));
            if (string2.length() <= 0) return;
            if (string3.length() <= 0) return;
            this.importProfileRemote(authContext, checkBox.isChecked(), this, new Runnable(){

                @Override
                public void run() {
                    OpenVPNImportProfile.this.add_to_server_history(string2);
                    OpenVPNImportProfile.this.finish();
                }
            }, new Runnable(){

                @Override
                public void run() {
                    OpenVPNImportProfile.access108(OpenVPNImportProfile.this);
                    OpenVPNImportProfile.this.set_ui_state(false);
                }
            }, null, true, true);
            this.set_ui_state(true);
            return;
        }
        if (n != R.id.import_cancel_button) return;
        this.generation = 1 + this.generation;
        this.finish();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.generation = 1;
        this.setContentView(R.layout.import_profile);
        this.prefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences((Context)this));
        this.server_history = this.prefs.get_string_set("import_server_history");
        if (this.server_history == null) {
            this.server_history = new HashSet<String>();
        }
        Button button = (Button)this.findViewById(R.id.import_button);
        Button button2 = (Button)this.findViewById(R.id.import_cancel_button);
        button.setOnClickListener((View.OnClickListener)this);
        button2.setOnClickListener((View.OnClickListener)this);
        ((TextView)this.findViewById(R.id.password)).setOnEditorActionListener((TextView.OnEditorActionListener)this);
        this.set_ui_state(false);
        this.set_server_history_autocomplete();
        this.doBindService();
    }

    protected void onDestroy() {
        Log.d((String)"OpenVPNImportProfile", (String)"onDestroy");
        this.stop();
        super.onDestroy();
    }

    public boolean onEditorAction(TextView textView, int n, KeyEvent keyEvent) {
        TextView textView2 = (TextView)this.findViewById(R.id.password);
        if (!this.action_enter(n, keyEvent)) return false;
        if (textView != textView2) return false;
        this.onClick((View)((Button)this.findViewById(R.id.import_button)));
        return true;
    }

    @Override
    protected void post_bind() {
        Log.d((String)"OpenVPNImportProfile", (String)"post_bind");
    }

}

