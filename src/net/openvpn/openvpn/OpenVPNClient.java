package net.openvpn.openvpn;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.text.method.*;

import net.openvpn.openvpn.OpenVPNService.ConnectionStats;
import net.openvpn.openvpn.OpenVPNService.EventMsg;
import net.openvpn.openvpn.OpenVPNService.Profile;
import net.openvpn.openvpn.OpenVPNService.ProfileList;
import net.openvpn.openvpn.OpenVPNService.Challenge;


public class OpenVPNClient extends OpenVPNClientBase implements OnClickListener, OnTouchListener, OnItemSelectedListener, OnEditorActionListener {
    private static final int REQUEST_IMPORT_PKCS12 = 3;
    private static final int REQUEST_IMPORT_PROFILE = 2;
    private static final int REQUEST_VPN_ACTOR_RIGHTS = 1;
    private static final boolean RETAIN_AUTH = false;
    private static final int S_BIND_CALLED = 1;
    private static final int S_ONSTART_CALLED = 2;
    private static final String TAG = "OpenVPNClient";
    private static final int UIF_PROFILE_SETTING_FROM_SPINNER = 262144; //0x40000
    private static final int UIF_REFLECTED = 131072; //0x20000
    private static final int UIF_RESET = 65536;//0x10000
    private static final boolean UI_OVERLOADED = false;
    private String autostart_profile_name;
    private View button_group;
    private TextView bytes_in_view;
    private TextView bytes_out_view;
    private TextView challenge_view;
    private View conn_details_group;
    private Button connect_button;
    private View cr_group;
    private TextView details_more_less;
    private Button disconnect_button;
    private TextView duration_view;
    private FinishOnConnect finish_on_connect;
    private View info_group;
    private boolean last_active;
    private TextView last_pkt_recv_view;
    private ScrollView main_scroll_view;
    private EditText password_edit;
    private View password_group;
    private CheckBox password_save_checkbox;
    private EditText pk_password_edit;
    private View pk_password_group;
    private CheckBox pk_password_save_checkbox;
    private View post_import_help_blurb;
    private PrefUtil prefs;
    private ImageButton profile_edit;
    private View profile_group;
    private Spinner profile_spin;
    private ProgressBar progress_bar;
    private ImageButton proxy_edit;
    private View proxy_group;
    private Spinner proxy_spin;
    private PasswordUtil pwds;
    private EditText response_edit;
    private View server_group;
    private Spinner server_spin;
    private int startup_state;
    private View stats_expansion_group;
    private View stats_group;
    private Handler stats_timer_handler;
    private Runnable stats_timer_task;
    private ImageView status_icon_view;
    private TextView status_view;
    private boolean stop_service_on_client_exit;
    private View[] textgroups;
    private TextView[] textviews;
    private Handler ui_reset_timer_handler;
    private Runnable ui_reset_timer_task;
    private EditText username_edit;
    private View username_group;

    /* renamed from: net.openvpn.openvpn.OpenVPNClient.3 */
    class AnonymousClass3 implements Runnable {
        final /* synthetic */ Activity val$self;

        AnonymousClass3(Activity activity) {
            this.val$self = activity;
        }

        public void run() {
            if (OpenVPNClient.this.finish_on_connect != FinishOnConnect.DISABLED) {
                this.val$self.finish();
            }
        }
    }

    /* renamed from: net.openvpn.openvpn.OpenVPNClient.4 */
    class AnonymousClass4 implements DialogInterface.OnClickListener {
        final /* synthetic */ EditText val$name_field;
        final /* synthetic */ String val$prof_name;

        AnonymousClass4(EditText editText, String str) {
            this.val$name_field = editText;
            this.val$prof_name = str;
        }

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case -1:
                    OpenVPNClient.this.createConnectShortcut(this.val$prof_name, this.val$name_field.getText().toString());
                default:
            }
        }
    }

    /* renamed from: net.openvpn.openvpn.OpenVPNClient.5 */
    class AnonymousClass5 implements DialogInterface.OnClickListener {
        final /* synthetic */ EditText val$name_field;
        final /* synthetic */ String val$orig_prof_name;

        AnonymousClass5(EditText editText, String str) {
            this.val$name_field = editText;
            this.val$orig_prof_name = str;
        }

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case -1:
                    OpenVPNClient.this.submitRenameProfileIntent(this.val$orig_prof_name, this.val$name_field.getText().toString());
                default:
            }
        }
    }

    /* renamed from: net.openvpn.openvpn.OpenVPNClient.6 */
    class AnonymousClass6 implements DialogInterface.OnClickListener {
        final /* synthetic */ ProxyList val$proxy_list;
        final /* synthetic */ String val$proxy_name;

        AnonymousClass6(ProxyList proxyList, String str) {
            this.val$proxy_list = proxyList;
            this.val$proxy_name = str;
        }

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case -1:
                    if (this.val$proxy_list != null) {
                        this.val$proxy_list.remove(this.val$proxy_name);
                        this.val$proxy_list.save();
                        OpenVPNClient.this.gen_ui_reset_event(OpenVPNClient.RETAIN_AUTH);
                    }
                default:
            }
        }
    }

    /* renamed from: net.openvpn.openvpn.OpenVPNClient.7 */
    class AnonymousClass7 implements DialogInterface.OnClickListener {
        final /* synthetic */ Context val$context;

        AnonymousClass7(Context context) {
            this.val$context = context;
        }

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case -1:
                    OpenVPNClient.this.pwds.regenerate(true);
                    ProfileList proflist = OpenVPNClient.this.profile_list();
                    if (proflist != null) {
                        proflist.forget_certs();
                    }
                    TrustMan.forget_certs(this.val$context);
                    OpenVPNImportProfile.forget_server_history(OpenVPNClient.this.prefs);
                    ProxyList proxy_list = OpenVPNClient.this.get_proxy_list();
                    if (proxy_list != null) {
                        proxy_list.forget_creds();
                        proxy_list.save();
                    }
                    OpenVPNClient.this.ui_setup(OpenVPNClient.this.is_active(), OpenVPNClient.UIF_RESET, null);
                default:
            }
        }
    }

    private enum FinishOnConnect {
        DISABLED,
        ENABLED,
        ENABLED_ACROSS_ONSTART
    }

    private enum ProfileSource {
        UNDEF,
        SERVICE,
        PRIORITY,
        PREFERENCES,
        SPINNER,
        LIST0
    }

    public OpenVPNClient() {
        this.stop_service_on_client_exit = RETAIN_AUTH;
        this.startup_state = 0;
        this.finish_on_connect = FinishOnConnect.DISABLED;
        this.last_active = RETAIN_AUTH;
        this.stats_timer_handler = new Handler();
        this.stats_timer_task = new Runnable() {
            public void run() {
                OpenVPNClient.this.show_stats();
                OpenVPNClient.this.schedule_stats();
            }
        };
        this.ui_reset_timer_handler = new Handler();
        this.ui_reset_timer_task = new Runnable() {
            public void run() {
                if (!OpenVPNClient.this.is_active()) {
                    OpenVPNClient.this.ui_setup(OpenVPNClient.RETAIN_AUTH, OpenVPNClient.UIF_RESET, null);
                }
            }
        };
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String str = TAG;
        Object[] objArr = new Object[S_BIND_CALLED];
        objArr[0] = intent.toString();
        Log.d(str, String.format("CLI: onCreate intent=%s", objArr));
        this.prefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences(this));
        this.pwds = new PasswordUtil(PreferenceManager.getDefaultSharedPreferences(this));
        init_default_preferences(this.prefs);
        setContentView(R.layout.form);
        load_ui_elements();
        doBindService();
        warn_app_expiration(this.prefs);
        new AppRate(this).setMinDaysUntilPrompt(14).setMinLaunchesUntilPrompt(10).init();
    }

    protected void onNewIntent(Intent intent) {
        String str = TAG;
        Object[] objArr = new Object[S_BIND_CALLED];
        objArr[0] = intent.toString();
        Log.d(str, String.format("CLI: onNewIntent intent=%s", objArr));
        setIntent(intent);
    }

    protected void post_bind() {
        Log.d(TAG, "CLI: post bind");
        this.startup_state |= S_BIND_CALLED;
        process_autostart_intent(is_active());
        render_last_event();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        ///getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public void event(EventMsg ev) {
        render_event(ev, RETAIN_AUTH, is_active(), RETAIN_AUTH);
    }

    private void render_last_event() {
        boolean active = is_active();
        EventMsg ev = get_last_event();
        if (ev != null) {
            render_event(ev, true, active, true);
        } else if (n_profiles_loaded() > 0) {
            render_event(EventMsg.disconnected(), true, active, true);
        } else {
            hide_status();
            ui_setup(active, UIF_RESET, null);
            show_progress(0, active);
        }
        EventMsg pev = get_last_event_prof_manage();
        if (pev != null) {
            render_event(pev, true, active, true);
        }
    }

    private boolean show_conn_info_field(String text, int field_id, int row_id) {
        boolean vis;
        int i = 0;
        if (text.length() > 0) {
            vis = true;
        } else {
            vis = RETAIN_AUTH;
        }
        TextView tv = (TextView) findViewById(field_id);
        View row = findViewById(row_id);
        tv.setText(text);
        if (!vis) {
            i = 8;
        }
        row.setVisibility(i);
        return vis;
    }

    private void reset_conn_info() {
        show_conn_info(new ClientAPI_ConnectionInfo());
    }

    private void show_conn_info(ClientAPI_ConnectionInfo ci) {
        this.info_group.setVisibility((((((((RETAIN_AUTH | show_conn_info_field(ci.getVpnIp4(), R.id.ipv4_addr, R.id.ipv4_addr_row)) | show_conn_info_field(ci.getVpnIp6(), R.id.ipv6_addr, R.id.ipv6_addr_row)) | show_conn_info_field(ci.getUser(), R.id.user, R.id.user_row)) | show_conn_info_field(ci.getClientIp(), R.id.client_ip, R.id.client_ip_row)) | show_conn_info_field(ci.getServerHost(), R.id.server_host, R.id.server_host_row)) | show_conn_info_field(ci.getServerIp(), R.id.server_ip, R.id.server_ip_row)) | show_conn_info_field(ci.getServerPort(), R.id.server_port, R.id.server_port_row)) | show_conn_info_field(ci.getServerProto(), R.id.server_proto, R.id.server_proto_row) ? 0 : 8);
        set_visibility_stats_expansion_group();
    }

    private void set_visibility_stats_expansion_group() {
        int i = 0;
        boolean expand_stats = this.prefs.get_boolean("expand_stats", RETAIN_AUTH);
        View view = this.stats_expansion_group;
        if (!expand_stats) {
            i = View.GONE;
        }
        view.setVisibility(i);
        this.details_more_less.setText(expand_stats ? R.string.touch_less : R.string.touch_more);
    }

    private void render_event(EventMsg ev, boolean reset, boolean active, boolean cached) {
        int flags = ev.flags;
        if (ev.is_reflected(this)) {
            flags |= UIF_REFLECTED;
        }
        if (reset || (flags & 8) != 0 || ev.profile_override != null) {
            ui_setup(active, UIF_RESET | flags, ev.profile_override);
        } else if (ev.res_id == R.string.core_thread_active) {
            active = true;
            ui_setup(active, flags, null);
        } else if (ev.res_id == R.string.core_thread_inactive) {
            active = RETAIN_AUTH;
            ui_setup(RETAIN_AUTH, flags, null);
        }
        switch (ev.res_id) {
            case R.string.connected:
                this.main_scroll_view.fullScroll(33);
                break;
            case R.string.tun_iface_create:
                if (!cached) {
                    ok_dialog(resString(R.string.tun_ko_title), resString(R.string.tun_ko_error));
                    break;
                }
                break;
            case R.string.tap_not_supported:
                if (!cached) {
                    ok_dialog(resString(R.string.tap_unsupported_title), resString(R.string.tap_unsupported_error));
                    break;
                }
                break;
        }
        if (ev.priority >= S_BIND_CALLED) {
            if (ev.icon_res_id >= 0) {
                show_status_icon(ev.icon_res_id);
            }
            if (ev.res_id == R.string.connected) {
                show_status(ev.res_id);
                if (ev.conn_info != null) {
                    show_conn_info(ev.conn_info);
                }
            } else if (ev.info.length() > 0) {
                Object[] objArr = new Object[S_ONSTART_CALLED];
                objArr[0] = resString(ev.res_id);
                objArr[S_BIND_CALLED] = ev.info;
                show_status(String.format("%s : %s", objArr));
            } else {
                show_status(ev.res_id);
            }
        }
        show_progress(ev.progress, active);
        show_stats();
        if (ev.res_id == R.string.connected && this.finish_on_connect != FinishOnConnect.DISABLED) {
            if (this.prefs.get_boolean("autostart_finish_on_connect", RETAIN_AUTH)) {
                new Handler().postDelayed(new AnonymousClass3(this), 1000);
                return;
            }
            this.finish_on_connect = FinishOnConnect.DISABLED;
        }
    }

    private void stop_service() {
        submitDisconnectIntent(true);
    }

    private void stop() {
        cancel_stats();
        doUnbindService();
        if (this.stop_service_on_client_exit) {
            Log.d(TAG, "CLI: stopping service");
            stop_service();
        }
    }

    protected void onStop() {
        Log.d(TAG, "CLI: onStop");
        cancel_stats();
        super.onStop();
    }

    protected void onStart() {
        super.onStart();
        Log.d(TAG, "CLI: onStart");
        this.startup_state |= S_ONSTART_CALLED;//S_ONSTART_CALLED=>2, startup_state =0, 0|2 = 2 
        if (this.finish_on_connect == FinishOnConnect.ENABLED) {
            this.finish_on_connect = FinishOnConnect.ENABLED_ACROSS_ONSTART;
        }
        boolean active = is_active();// from OpenVPNClientBase->is_active->OpenVPNService{ this.mBoundService == null || !this.mBoundService.is_active()) } 
        if (active) {
            schedule_stats();
        }
        if (process_autostart_intent(active)) {
            ui_setup(active, UIF_RESET, null);
        }
    }

    protected void onDestroy() {
        stop();
        Log.d(TAG, "CLI: onDestroy called");
        super.onDestroy();
    }

    private boolean process_autostart_intent(boolean active) {
				//onStart之后startup_state =2, 不和REQUEST_IMPORT_PKCS12 发生反应,故不运行下面的语句
        if ((this.startup_state & REQUEST_IMPORT_PKCS12) == REQUEST_IMPORT_PKCS12) 
	{
            Intent intent = getIntent();
            String apn_key = "net.openvpn.openvpn.AUTOSTART_PROFILE_NAME";
            String apn = intent.getStringExtra(apn_key);
            if (apn != null) {
                this.autostart_profile_name = null;
                String str = TAG;
                Object[] objArr = new Object[S_BIND_CALLED];
                objArr[0] = apn;
                Log.d(str, String.format("CLI: autostart: %s", objArr));
                intent.removeExtra(apn_key);
                if (!active) {
                    ProfileList proflist = profile_list();// ArrayList<P>
                    if (proflist == null || proflist.get_profile_by_name(apn) == null) {
                        ok_dialog(resString(R.string.profile_not_found), apn);
                    } else {
                        this.autostart_profile_name = apn;
                        return true;
                    }
                } else if (!current_profile().get_name().equals(apn)) {
                    this.autostart_profile_name = apn;
                    submitDisconnectIntent(RETAIN_AUTH);
                }
            }
        }
        return RETAIN_AUTH;
    }

    private void cancel_ui_reset() {
        this.ui_reset_timer_handler.removeCallbacks(this.ui_reset_timer_task);
    }

    private void schedule_ui_reset(long delay) {
        cancel_ui_reset();
        this.ui_reset_timer_handler.postDelayed(this.ui_reset_timer_task, delay);
    }

    private void hide_status() {
        this.status_view.setVisibility(View.GONE);
    }

    private void show_status(String text) {
        this.status_view.setVisibility(0);
        this.status_view.setText(text);
    }

    private void show_status(int res_id) {
        this.status_view.setVisibility(0);
        this.status_view.setText(res_id);
    }

    private void show_status_icon(int res_id) {
        this.status_icon_view.setImageResource(res_id);
    }

    private void show_progress(int progress, boolean active) {
        if (progress <= 0 || progress >= 99) {
            this.progress_bar.setVisibility(View.GONE);
            return;
        }
        this.progress_bar.setVisibility(0);
        this.progress_bar.setProgress(progress);
    }

    private void cancel_stats() {
        this.stats_timer_handler.removeCallbacks(this.stats_timer_task);
    }

    private void schedule_stats() {
        cancel_stats();
        this.stats_timer_handler.postDelayed(this.stats_timer_task, 1000);
    }

    private static String render_bandwidth(long bw) {
        String postfix;
        float div;
        Object[] objArr;
        float bwf = (float) bw;
        if (bwf >= 1.0E12f) {
            postfix = "TB";
            div = 1.09951163E12f;
        } else if (bwf >= 1.0E9f) {
            postfix = "GB";
            div = 1.07374182E9f;
        } else if (bwf >= 1000000.0f) {
            postfix = "MB";
            div = 1048576.0f;
        } else if (bwf >= 1000.0f) {
            postfix = "KB";
            div = 1024.0f;
        } else {
            objArr = new Object[S_BIND_CALLED];
            objArr[0] = Float.valueOf(bwf);
            return String.format("%.0f", objArr);
        }
        objArr = new Object[S_ONSTART_CALLED];
        objArr[0] = Float.valueOf(bwf / div);
        objArr[S_BIND_CALLED] = postfix;
        return String.format("%.2f %s", objArr);
    }

    private String render_last_pkt_recv(int sec) {
        if (sec >= 3600) {
            return resString(R.string.lpr_gt_1_hour_ago);
        }
        String resString;
        Object[] objArr;
        if (sec >= 120) {
            resString = resString(R.string.lpr_gt_n_min_ago);
            objArr = new Object[S_BIND_CALLED];
            objArr[0] = Integer.valueOf(sec / 60);
            return String.format(resString, objArr);
        } else if (sec >= S_ONSTART_CALLED) {
            resString = resString(R.string.lpr_n_sec_ago);
            objArr = new Object[S_BIND_CALLED];
            objArr[0] = Integer.valueOf(sec);
            return String.format(resString, objArr);
        } else if (sec == S_BIND_CALLED) {
            return resString(R.string.lpr_1_sec_ago);
        } else {
            if (sec == 0) {
                return resString(R.string.lpr_lt_1_sec_ago);
            }
            return "";
        }
    }

    private void show_stats() {
        if (is_active()) {
            ConnectionStats stats = get_connection_stats();// from OpenVPNService
            this.last_pkt_recv_view.setText(render_last_pkt_recv(stats.last_packet_received));
            this.duration_view.setText(OpenVPNClientBase.render_duration(stats.duration)); // 返回的都是String
            this.bytes_in_view.setText(render_bandwidth(stats.bytes_in));
            this.bytes_out_view.setText(render_bandwidth(stats.bytes_out));
        }
    }

    private void clear_stats() {
        this.last_pkt_recv_view.setText("");
        this.duration_view.setText("");
        this.bytes_in_view.setText("");
        this.bytes_out_view.setText("");
        reset_conn_info();
    }

    private int n_profiles_loaded() {
        ProfileList proflist = profile_list();
        if (proflist != null) {
            return proflist.size();
        }
        return 0;
    }

    private String selected_profile_name() {
        String ret = null;
        ProfileList proflist = profile_list();
        if (proflist != null && proflist.size() > 0) {
            ret = proflist.size() == S_BIND_CALLED ? ((Profile) proflist.get(0)).get_name() : SpinUtil.get_spinner_selected_item(this.profile_spin);
        }
        if (ret == null) {
            return "UNDEFINED_PROFILE";
        }
        return ret;
    }

    private Profile selected_profile() {
        ProfileList proflist = profile_list();
        if (proflist != null) {
            return proflist.get_profile_by_name(selected_profile_name());
        }
        return null;
    }

    private void clear_auth() {
        this.username_edit.setText("");
        this.pk_password_edit.setText("");
        this.password_edit.setText("");
        this.response_edit.setText("");
    }

/*
> 0x10000 UIF_RESET
65536
> 0x20000 UIF_REFLECTED
131072
> 0x30000
196608
> 0x40000 UIF_PROFILE_SETTING_FROM_SPINNER
262144
> 0x50000
327680
*/
/// copy from procyon version 
    private void ui_setup(final boolean last_active, final int n, String autostart_profile_name) {
        this.cancel_ui_reset();
        boolean b = false;
        /*Label_0931:*/ {
            if (( 0x10000 & n) == 0x0) {
                final boolean last_active2 = this.last_active;
                b = false;
                if (last_active == last_active2) {
                    //break Label_0931;
                    return;
                }
            }
            this.clear_auth();
            b = false;
            if (!last_active) {
                final String autostart_profile_name2 = this.autostart_profile_name;
                b = false;
                if (autostart_profile_name2 != null) {
                    b = true;
                    autostart_profile_name = this.autostart_profile_name;
                    this.autostart_profile_name = null;
                }
            }
            final ProfileList profile_list = this.profile_list();
            Object o;
            if (profile_list != null && profile_list.size() > 0) {
                ProfileSource profileSource = ProfileSource.UNDEF;
                SpinUtil.show_spinner((Context)this, this.profile_spin, profile_list.profile_names());
                o = null;
                if (last_active) {
                    profileSource = ProfileSource.SERVICE;
                    o = this.current_profile();
                }
                if (o == null && autostart_profile_name != null) {
                    profileSource = ProfileSource.PRIORITY;
                    o = profile_list.get_profile_by_name(autostart_profile_name);
                    if (o == null) {
                        Log.d("OpenVPNClient", "CLI: profile override not found");
                        b = false;
                    }
                }
                if (o == null) {
                    if ((0x40000 & n) != 0x0) {
                        profileSource = ProfileSource.SPINNER;
                        o = profile_list.get_profile_by_name(SpinUtil.get_spinner_selected_item(this.profile_spin));
                    }
                    else {
                        profileSource = ProfileSource.PREFERENCES;
                        o = profile_list.get_profile_by_name(this.prefs.get_string("profile"));
                    }
                }
                if (o == null) {
                    profileSource = ProfileSource.LIST0;
                    //o = ((ArrayList<__Null>)profile_list).get(0);
										o = profile_list.get(0);
                }
                if (profileSource != ProfileSource.PREFERENCES && (0x20000 & n) == 0x0) {
                    this.prefs.set_string("profile", ((OpenVPNService.Profile)o).get_name());
                    this.gen_ui_reset_event(true);
                }
                if (profileSource != ProfileSource.SPINNER) {
                    SpinUtil.set_spinner_selected_item(this.profile_spin, ((OpenVPNService.Profile)o).get_name());
                }
                this.profile_group.setVisibility(0);
                this.profile_spin.setEnabled(!last_active);
            }
            else {
                this.profile_group.setVisibility(View.GONE);
                o = null;
            }
            if (o != null) {
                if ((0x10000 & n) != 0x0) {
                    ((OpenVPNService.Profile)o).reset_dynamic_challenge();
                }
                EditText editText = null;
                if (!last_active && (n & 0x20) != 0x0) {
                    this.post_import_help_blurb.setVisibility(0);
                }
                else if (last_active) {
                    this.post_import_help_blurb.setVisibility(View.GONE);
                }
                final ProxyList get_proxy_list = this.get_proxy_list();
                if (!last_active && get_proxy_list.size() > 0) {
                    SpinUtil.show_spinner((Context)this, this.proxy_spin, get_proxy_list.get_name_list(true));
                    final String get_enabled = get_proxy_list.get_enabled(true);
                    if (get_enabled != null) {
                        SpinUtil.set_spinner_selected_item(this.proxy_spin, get_enabled);
                    }
                    this.proxy_group.setVisibility(0);
                }
                else {
                    this.proxy_group.setVisibility(View.GONE);
                }
                if (!last_active && ((OpenVPNService.Profile)o).server_list_defined()) {
                    SpinUtil.show_spinner((Context)this, this.server_spin, ((OpenVPNService.Profile)o).get_server_list().display_names());
                    final String get_string_by_profile = this.prefs.get_string_by_profile(((OpenVPNService.Profile)o).get_name(), "server");
                    if (get_string_by_profile != null) {
                        SpinUtil.set_spinner_selected_item(this.server_spin, get_string_by_profile);
                    }
                    this.server_group.setVisibility(0);
                }
                else {
                    this.server_group.setVisibility(View.GONE);
                }
                if (!last_active) {
                    final boolean userlocked_username_defined = ((OpenVPNService.Profile)o).userlocked_username_defined();
                    final boolean get_autologin = ((OpenVPNService.Profile)o).get_autologin();
                    final boolean get_private_key_password_required = ((OpenVPNService.Profile)o).get_private_key_password_required();
                    final boolean is_dynamic_challenge = ((OpenVPNService.Profile)o).is_dynamic_challenge();
                    if ((!get_autologin || (get_autologin && userlocked_username_defined)) && !is_dynamic_challenge) {
                        if (userlocked_username_defined) {
                            this.username_edit.setText((CharSequence)((OpenVPNService.Profile)o).get_userlocked_username());
                            this.set_enabled(this.username_edit, false);
                        }
                        else {
                            this.set_enabled(this.username_edit, true);
                            final String get_string_by_profile2 = this.prefs.get_string_by_profile(((OpenVPNService.Profile)o).get_name(), "username");
                            if (get_string_by_profile2 != null) {
                                this.username_edit.setText((CharSequence)get_string_by_profile2);
                                editText = null;
                            }
                            else {
                                editText = null;
                                if (!false) {
                                    editText = this.username_edit;
                                }
                            }
                        }
                        this.username_group.setVisibility(0);
                    }
                    else {
                        this.username_group.setVisibility(View.GONE);
                        editText = null;
                    }
                    if (get_private_key_password_required) {
                        final boolean get_boolean_by_profile = this.prefs.get_boolean_by_profile(((OpenVPNService.Profile)o).get_name(), "pk_password_save", false);
                        this.pk_password_group.setVisibility(0);
                        this.pk_password_save_checkbox.setChecked(get_boolean_by_profile);
                        CharSequence value = null;
                        if (get_boolean_by_profile) {
                            value = this.pwds.get("pk", ((OpenVPNService.Profile)o).get_name());
                        }
                        if (value != null) {
                            this.pk_password_edit.setText(value);
                        }
                        else if (editText == null) {
                            editText = this.pk_password_edit;
                        }
                    }
                    else {
                        this.pk_password_group.setVisibility(View.GONE);
                    }
                    if (!get_autologin && !is_dynamic_challenge) {
                        final boolean get_allow_password_save = ((OpenVPNService.Profile)o).get_allow_password_save();
                        final boolean checked = get_allow_password_save && this.prefs.get_boolean_by_profile(((OpenVPNService.Profile)o).get_name(), "auth_password_save", false);
                        this.password_group.setVisibility(0);
                        this.password_save_checkbox.setEnabled(get_allow_password_save);
                        this.password_save_checkbox.setChecked(checked);
                        CharSequence value2 = null;
                        if (checked) {
                            value2 = this.pwds.get("auth", ((OpenVPNService.Profile)o).get_name());
                        }
                        if (value2 != null) {
                            this.password_edit.setText(value2);
                        }
                        else if (editText == null) {
                            editText = this.password_edit;
                        }
                    }
                    else {
                        this.password_group.setVisibility(View.GONE);
                    }
                }
                else {
                    this.username_group.setVisibility(View.GONE);
                    this.pk_password_group.setVisibility(View.GONE);
                    this.password_group.setVisibility(View.GONE);
                    editText = null;
                }
                if (!last_active && !((OpenVPNService.Profile)o).get_autologin() && ((OpenVPNService.Profile)o).challenge_defined()) {
                    this.cr_group.setVisibility(0);
                    final Challenge get_challenge = ((OpenVPNService.Profile)o).get_challenge();
                    this.challenge_view.setText((CharSequence)get_challenge.get_challenge());
                    this.challenge_view.setVisibility(0);
                    if (get_challenge.get_response_required()) {
                        if (get_challenge.get_echo()) {
                            this.response_edit.setTransformationMethod((TransformationMethod)SingleLineTransformationMethod.getInstance());
                        }
                        else {
                            this.response_edit.setTransformationMethod((TransformationMethod)PasswordTransformationMethod.getInstance());
                        }
                        this.response_edit.setVisibility(0);
                        if (editText == null) {
                            editText = this.response_edit;
                        }
                    }
                    else {
                        this.response_edit.setVisibility(View.GONE);
                    }
                    if (((OpenVPNService.Profile)o).is_dynamic_challenge()) {
                        this.schedule_ui_reset(((OpenVPNService.Profile)o).get_dynamic_challenge_expire_delay());
                    }
                }
                else {
                    this.cr_group.setVisibility(View.GONE);
                }
		
                this.button_group.setVisibility(0);
                if (last_active) {
                    this.conn_details_group.setVisibility(0);
                    this.connect_button.setVisibility(View.GONE);
                    this.disconnect_button.setVisibility(0);
                }
                else {
                    this.conn_details_group.setVisibility(View.GONE);
                    this.connect_button.setVisibility(0);
                    this.disconnect_button.setVisibility(View.GONE);
                }
                if (editText != null) {
                    b = false;
                }
                this.req_focus(editText);
            }
            else {
                this.post_import_help_blurb.setVisibility(View.GONE);
                this.proxy_group.setVisibility(View.GONE);
                this.server_group.setVisibility(View.GONE);
                this.username_group.setVisibility(View.GONE);
                this.pk_password_group.setVisibility(View.GONE);
                final View password_group = this.password_group;
                this.cr_group.setVisibility(View.GONE);
                this.conn_details_group.setVisibility(View.GONE);
                this.button_group.setVisibility(View.GONE);
		
                this.show_status_icon(R.drawable.info);
                this.show_status(R.string.no_profiles_loaded);
            }
            if (last_active) {
                this.schedule_stats();
            }
            else {
                this.cancel_stats();
            }
        }// end Label_0931
        this.last_active = last_active;
        if (b && !this.last_active) {
            this.finish_on_connect = FinishOnConnect.ENABLED;
            this.start_connect();
        }
    }



/*
	/// when I can not sure which version of ui_setup can work, just a fake one
	private void ui_setup(boolean var1_1,int var2_2,String var3_3)
	{
		this.post_import_help_blurb.setVisibility(View.GONE);
    	this.proxy_group.setVisibility( View.GONE );

    	this.server_group.setVisibility(0);
    	this.username_group.setVisibility(0);
    	this.pk_password_group.setVisibility(0);
    	this.password_group.setVisibility(0);

    	this.cr_group.setVisibility( View.GONE);
    	this.conn_details_group.setVisibility( View.GONE);
    	this.button_group.setVisibility(0);

    	this.show_status_icon(R.drawable.info);
    	this.show_status(R.string.no_profiles_loaded);
   	}
*/
    private void set_enabled(EditText editText, boolean state) {
        editText.setEnabled(state);
        editText.setFocusable(state);
        editText.setFocusableInTouchMode(state);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about_menu:
                startActivityForResult(new Intent(this, OpenVPNAbout.class), 0);
                return true;
            case R.id.help_menu:
                startActivityForResult(new Intent(this, OpenVPNHelp.class), 0);
                return true;
            case R.id.import_private_tunnel_profile:
                startActivity(new Intent("android.intent.action.VIEW", Uri.parse(getText(R.string.privatetunnel_import).toString())));
                break;
            case R.id.import_profile_remote:
                startActivityForResult(new Intent(this, OpenVPNImportProfile.class), 0);
                return true;
            case R.id.import_profile:
                raise_file_selection_dialog(S_ONSTART_CALLED, R.string.select_profile);
                return true;
            case R.id.import_pkcs12:
                raise_file_selection_dialog(REQUEST_IMPORT_PKCS12, R.string.select_pkcs12);
                return true;
            case R.id.preferences:
                startActivityForResult(new Intent(this, OpenVPNPrefs.class), 0);
                return true;
            case R.id.add_proxy:
                String prefix = OpenVPNService.INTENT_PREFIX;
                startActivityForResult(new Intent(this, OpenVPNAddProxy.class), 0);
                return true;
            case R.id.add_shortcut_connect:
                startActivityForResult(new Intent(this, OpenVPNAddShortcut.class), 0);
                return true;
            case R.id.add_shortcut_disconnect:
                createDisconnectShortcut(resString(R.string.disconnect_shortcut_title));
                return true;
            case R.id.add_shortcut_app:
                createAppShortcut(resString(R.string.app_shortcut_title));
                return true;
            case R.id.show_log:
                startActivityForResult(new Intent(this, OpenVPNLog.class), 0);
                return true;
            case R.id.show_raw_stats:
                startActivityForResult(new Intent(this, OpenVPNStats.class), 0);
                return true;
            case R.id.forget_creds:
                forget_creds_with_confirm();
                return true;
            case R.id.exit_partial:
                finish();
                return true;
            case R.id.exit_full:
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        this.stop_service_on_client_exit = true;
        finish();
        return true;
    }


    public void onClick(View v) {
        cancel_ui_reset();
        this.autostart_profile_name = null;
        this.finish_on_connect = FinishOnConnect.DISABLED;
        int viewid = v.getId();
        if (viewid == R.id.connect) 
				{
						//要开始连接了,界面上的connect button 按下,由于本类是implments  OnClickListener 
            start_connect();
        } 
				else if (viewid == R.id.disconnect) 
				{
            submitDisconnectIntent(RETAIN_AUTH);
        } 
				else if (viewid == R.id.profile_edit || viewid == R.id.proxy_edit) {
            openContextMenu(v);
        }
    }

    private void start_connect() {
        cancel_ui_reset();
        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            try {
                Log.d(TAG, "CLI: requesting VPN actor rights");
                startActivityForResult(intent, S_BIND_CALLED); // ->  onActivityResult
                return;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "CLI: requesting VPN actor rights failed", e);
                ok_dialog(resString(R.string.vpn_permission_dialog_missing_title), resString(R.string.vpn_permission_dialog_missing_text));
                return;
            }
        }
        Log.d(TAG, "CLI: app is already authorized as VPN actor");
        resolve_epki_alias_then_connect();
    }

    public boolean onTouch(View v, MotionEvent event) {
        boolean new_expand_stats = RETAIN_AUTH;
        if (v.getId() != R.id.conn_details_boxed || event.getAction() != 0) {
            return RETAIN_AUTH;
        }
        if (!this.prefs.get_boolean("expand_stats", RETAIN_AUTH)) {
            new_expand_stats = true;
        }
        this.prefs.set_boolean("expand_stats", new_expand_stats);
        set_visibility_stats_expansion_group();
        return true;
    }

    public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
        cancel_ui_reset();
        int viewid = parent.getId();
        if (viewid == R.id.profile) {
						// 327680 == 0x50000
            ui_setup(is_active(), 327680, null);
        } else if (viewid == R.id.proxy) {
            ProxyList proxy_list = get_proxy_list();
            if (proxy_list != null) {
                proxy_list.set_enabled(SpinUtil.get_spinner_list_item(this.proxy_spin, position));
                proxy_list.save();
                gen_ui_reset_event(true);
            }
        } else if (viewid == R.id.server) {
            String server = SpinUtil.get_spinner_list_item(this.server_spin, position);
            this.prefs.set_string_by_profile(SpinUtil.get_spinner_selected_item(this.profile_spin), "server", server);
            gen_ui_reset_event(true);
        }
    }

    public void onNothingSelected(AdapterView<?> adapterView) {
    }

    private void menu_add(ContextMenu menu, int id, boolean enabled, String menu_key) {
        MenuItem item = menu.add(0, id, 0, id).setEnabled(enabled);
        if (menu_key != null) {
            item.setIntent(new Intent().putExtra("net.openvpn.openvpn.MENU_KEY", menu_key));
        }
    }

    private String get_menu_key(MenuItem item) {
        if (item != null) {
            Intent intent = item.getIntent();
            if (intent != null) {
                return intent.getStringExtra("net.openvpn.openvpn.MENU_KEY");
            }
        }
        return null;
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        boolean z = RETAIN_AUTH;
        Log.d(TAG, "CLI: onCreateContextMenu");
        super.onCreateContextMenu(menu, v, menuInfo);
        int viewid = v.getId();
        if (!is_active() && (viewid == R.id.profile || viewid == R.id.profile_edit)) {
            Profile prof = selected_profile();
            if (prof != null) {
                String profile_name = prof.get_name();
                menu.setHeaderTitle(profile_name);
                if (SpinUtil.get_spinner_count(this.profile_spin) > S_BIND_CALLED) {
                    z = true;
                }
                menu_add(menu, R.string.profile_context_menu_change_profile, z, null);
                menu_add(menu, R.string.profile_context_menu_create_shortcut, true, profile_name);
                menu_add(menu, R.string.profile_context_menu_delete, prof.is_deleteable(), profile_name);
                menu_add(menu, R.string.profile_context_menu_rename, prof.is_renameable(), profile_name);
                menu_add(menu, R.string.profile_context_forget_creds, true, profile_name);
            } else {
                menu.setHeaderTitle(R.string.profile_context_none_selected);
            }
            menu_add(menu, R.string.profile_context_cancel, true, null);
        } else if (!is_active()) {
            if (viewid == R.id.proxy || viewid == R.id.proxy_edit) {
                ProxyList proxy_list = get_proxy_list();
                if (proxy_list != null) {
                    boolean z2;
                    String proxy_name = proxy_list.get_enabled(true);
                    boolean is_none = proxy_list.is_none(proxy_name);
                    menu.setHeaderTitle(proxy_name);
                    if (SpinUtil.get_spinner_count(this.proxy_spin) > S_BIND_CALLED) {
                        z2 = true;
                    } else {
                        z2 = RETAIN_AUTH;
                    }
                    menu_add(menu, R.string.proxy_context_change_proxy, z2, null);
                    if (is_none) {
                        z2 = RETAIN_AUTH;
                    } else {
                        z2 = true;
                    }
                    menu_add(menu, R.string.proxy_context_edit, z2, proxy_name);
                    if (!is_none) {
                        z = true;
                    }
                    menu_add(menu, R.string.proxy_context_delete, z, proxy_name);
                    menu_add(menu, R.string.proxy_context_forget_creds, proxy_list.has_saved_creds(proxy_name), proxy_name);
                } else {
                    menu.setHeaderTitle(R.string.proxy_context_none_selected);
                }
                menu_add(menu, R.string.proxy_context_cancel, true, null);
            }
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        Log.d(TAG, "CLI: onContextItemSelected");
        String prof_name;
        String proxy_name;
        switch (item.getItemId()) {
            case R.string.profile_context_menu_change_profile:
                this.profile_spin.performClick();
                return true;
            case R.string.profile_context_menu_create_shortcut:
                prof_name = get_menu_key(item);
                if (prof_name == null) {
                    return true;
                }
                launch_create_profile_shortcut_dialog(prof_name);
                return true;
            case R.string.profile_context_menu_delete:
                prof_name = get_menu_key(item);
                if (prof_name == null) {
                    return true;
                }
                submitDeleteProfileIntentWithConfirm(prof_name);
                return true;
            case R.string.profile_context_menu_rename:
                prof_name = get_menu_key(item);
                if (prof_name == null) {
                    return true;
                }
                launch_rename_profile_dialog(prof_name);
                return true;
            case R.string.profile_context_forget_creds:
                ProfileList proflist = profile_list();
                if (proflist == null) {
                    return true;
                }
                Profile prof = proflist.get_profile_by_name(get_menu_key(item));
                if (prof == null) {
                    return true;
                }
                prof_name = prof.get_name();
                this.pwds.remove("pk", prof_name);
                this.pwds.remove("auth", prof_name);
                prof.forget_cert();
                ui_setup(is_active(), UIF_RESET, null);
                return true;
            case R.string.profile_context_cancel:
            case R.string.proxy_context_cancel:
                return true;
            case R.string.proxy_context_change_proxy:
                this.proxy_spin.performClick();
                return true;
            case R.string.proxy_context_edit:
                proxy_name = get_menu_key(item);
                if (proxy_name == null) {
                    return true;
                }
                startActivityForResult(new Intent(this, OpenVPNAddProxy.class).putExtra("net.openvpn.openvpn.PROXY_NAME", proxy_name), 0);
                return true;
            case R.string.proxy_context_delete:
                delete_proxy_with_confirm(get_menu_key(item));
                return true;
            case R.string.proxy_context_forget_creds:
                proxy_name = get_menu_key(item);
                ProxyList proxy_list = get_proxy_list();
                if (proxy_list == null) {
                    return true;
                }
                proxy_list.forget_creds(proxy_name);
                proxy_list.save();
                return true;
            default:
                return RETAIN_AUTH;
        }
    }

    private void launch_create_profile_shortcut_dialog(String prof_name) {
        View view = getLayoutInflater().inflate(R.layout.create_shortcut_dialog, null);
        EditText name_field = (EditText) view.findViewById(R.id.shortcut_name);
        name_field.setText(prof_name);
        name_field.selectAll();
        DialogInterface.OnClickListener dialogClickListener = new AnonymousClass4(name_field, prof_name);
        new Builder(this).setTitle(R.string.create_shortcut_title).setView(view).setPositiveButton(R.string.create_shortcut_yes, dialogClickListener).setNegativeButton(R.string.create_shortcut_cancel, dialogClickListener).show();
    }

    private void launch_rename_profile_dialog(String orig_prof_name) {
        View view = getLayoutInflater().inflate(R.layout.rename_profile_dialog, null);
        EditText name_field = (EditText) view.findViewById(R.id.rename_profile_name);
        name_field.setText(orig_prof_name);
        name_field.selectAll();
        DialogInterface.OnClickListener dialogClickListener = new AnonymousClass5(name_field, orig_prof_name);
        new Builder(this).setTitle(R.string.rename_profile_title).setView(view).setPositiveButton(R.string.rename_profile_yes, dialogClickListener).setNegativeButton(R.string.rename_profile_cancel, dialogClickListener).show();
    }

    private void delete_proxy_with_confirm(String proxy_name) {
        DialogInterface.OnClickListener dialogClickListener = new AnonymousClass6(get_proxy_list(), proxy_name);
        new Builder(this).setTitle(R.string.proxy_delete_confirm_title).setMessage(proxy_name).setPositiveButton(R.string.proxy_delete_confirm_yes, dialogClickListener).setNegativeButton(R.string.proxy_delete_confirm_cancel, dialogClickListener).show();
    }

    private void forget_creds_with_confirm() {
        DialogInterface.OnClickListener dialogClickListener = new AnonymousClass7(this);
        new Builder(this).setTitle(R.string.forget_creds_title).setMessage(R.string.forget_creds_message).setPositiveButton(R.string.forget_creds_yes, dialogClickListener).setNegativeButton(R.string.forget_creds_cancel, dialogClickListener).show();
    }

    public PendingIntent get_configure_intent(int requestCode) {
        return PendingIntent.getActivity(this, requestCode, getIntent(),  PendingIntent.FLAG_CANCEL_CURRENT); //268435456);
    }

    private void resolve_epki_alias_then_connect() {
	Log.i(TAG,"In resolve_epki_alias_then_connect");
	OpenVPNClient.this.do_connect("DISABLE_CLIENT_CERT");
	
	/*
        resolveExternalPkiAlias(selected_profile(), new EpkiPost() {
	    @Override
            public void post_dispatch(String alias) {
		Log.i(TAG,String.format("EpkiPost post_dispatch %s",alias));
                OpenVPNClient.this.do_connect(alias);
            }
        });
	*/
    }

    private void do_connect(String epki_alias) {
        String app_name = "net.openvpn.connect.android";
        String proxy_name = null;
        String server = null;
        String username = null;
        String password = null;
        String pk_password = null;
        String response = null;
        boolean is_auth_pwd_save = RETAIN_AUTH;
        String profile_name = selected_profile_name();
	Log.i(TAG,String.format("do_connect %s",epki_alias));
	
        if (this.proxy_group.getVisibility() == 0) {
            ProxyList proxy_list = get_proxy_list();
            if (proxy_list != null) {
                proxy_name = proxy_list.get_enabled(RETAIN_AUTH);
            }
        }
        if (this.server_group.getVisibility() == 0) {
            server = SpinUtil.get_spinner_selected_item(this.server_spin);
        }
	///username input 
        if (this.username_group.getVisibility() == 0) {
            username = this.username_edit.getText().toString();
            if (username.length() > 0) {
                this.prefs.set_string_by_profile(profile_name, "username", username);
            }
        }

	// private key password 
        if (this.pk_password_group.getVisibility() == 0) {
            pk_password = this.pk_password_edit.getText().toString();
            boolean is_pk_pwd_save = this.pk_password_save_checkbox.isChecked();
            this.prefs.set_boolean_by_profile(profile_name, "pk_password_save", is_pk_pwd_save);
            if (is_pk_pwd_save) {
                this.pwds.set("pk", profile_name, pk_password);
            } else {
                this.pwds.remove("pk", profile_name);
            }
        }
	// password input 
        if (this.password_group.getVisibility() == 0) {
            password = this.password_edit.getText().toString();
            is_auth_pwd_save = this.password_save_checkbox.isChecked();
            this.prefs.set_boolean_by_profile(profile_name, "auth_password_save", is_auth_pwd_save);
            if (is_auth_pwd_save) {
                this.pwds.set("auth", profile_name, password);
            } else {
                this.pwds.remove("auth", profile_name);
            }
        }
        if (this.cr_group.getVisibility() == 0) {
            response = this.response_edit.getText().toString();
        }
        clear_auth();
        String vpn_proto = this.prefs.get_string("vpn_proto");
        String conn_timeout = this.prefs.get_string("conn_timeout");
        String compression_mode = this.prefs.get_string("compression_mode");
        clear_stats();
	Log.i(TAG,"Before submitConnectIntent");
        submitConnectIntent(profile_name, server, vpn_proto, conn_timeout, username, password, is_auth_pwd_save, pk_password, response, epki_alias, compression_mode, proxy_name, null, null, true, get_gui_version(app_name)); // from OpenVPNClientBase
    }

    private void import_profile(String path) {
        submitImportProfileViaPathIntent(path);
    }

    protected void onActivityResult(int request, int result, Intent data) {
        String str = TAG;
        Object[] objArr = new Object[S_ONSTART_CALLED];
        objArr[0] = Integer.valueOf(request);
        objArr[S_BIND_CALLED] = Integer.valueOf(result);
        Log.d(str, String.format("CLI: onActivityResult request=%d result=%d", objArr));
        String path;
        switch (request) {
            case S_BIND_CALLED /*1*/:
                if (result == RESULT_OK ) {
		    Log.i(str,"S_BIND_CALLED RESULT_OK,resolve_epki_alias_then_connect");
                    resolve_epki_alias_then_connect(); //->do_connect
		    return;
                } else if (result != 0) {
                } else {
                    if (this.finish_on_connect == FinishOnConnect.ENABLED) {
                        finish();
                    } else if (this.finish_on_connect == FinishOnConnect.ENABLED_ACROSS_ONSTART) {
                        this.finish_on_connect = FinishOnConnect.ENABLED;
                        start_connect();
                    }
		    return;
                }
            case S_ONSTART_CALLED /*2*/:
                if (result == -1) {
                    path = data.getStringExtra(FileDialog.RESULT_PATH);
                    str = TAG;
                    objArr = new Object[S_BIND_CALLED];
                    objArr[0] = path;
                    Log.d(str, String.format("CLI: IMPORT_PROFILE: %s", objArr));
                    import_profile(path);
		    return;
                }
            case REQUEST_IMPORT_PKCS12 /*3*/:
                if (result == -1) {
                    path = data.getStringExtra(FileDialog.RESULT_PATH);
                    str = TAG;
                    objArr = new Object[S_BIND_CALLED];
                    objArr[0] = path;
                    Log.d(str, String.format("CLI: IMPORT_PKCS12: %s", objArr));
                    import_pkcs12(path);
		    return;
                }
            default:
                super.onActivityResult(request, result, data);// what if the parent does not have onActivityResult?
        }
    }

    private TextView last_visible_edittext() {
        for (int i = 0; i < this.textgroups.length; i += S_BIND_CALLED) {
            if (this.textgroups[i].getVisibility() == 0) {
                return this.textviews[i];
            }
        }
        return null;
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (v != last_visible_edittext()) {
            return RETAIN_AUTH;
        }
        if (action_enter(actionId, event) && this.connect_button.getVisibility() == View.VISIBLE) {
            onClick(this.connect_button);
        }
        return true;
    }

    private void req_focus(EditText editText) {
        boolean auto_keyboard = this.prefs.get_boolean("auto_keyboard", RETAIN_AUTH);
        if (editText != null) {
            editText.requestFocus();
            if (auto_keyboard) {
                raise_keyboard(editText);
                return;
            }
            return;
        }
        this.main_scroll_view.requestFocus();
        if (auto_keyboard) {
            dismiss_keyboard();
        }
    }

    private void raise_keyboard(EditText editText) {
        InputMethodManager mgr = (InputMethodManager) getSystemService("input_method");
        if (mgr != null) {
            mgr.showSoftInput(editText, S_BIND_CALLED);
        }
    }

    private void dismiss_keyboard() {
        InputMethodManager mgr = (InputMethodManager) getSystemService("input_method");
        if (mgr != null) {
            TextView[] arr$ = this.textviews;
            int len$ = arr$.length;
            for (int i$ = 0; i$ < len$; i$ += S_BIND_CALLED) {
                mgr.hideSoftInputFromWindow(arr$[i$].getWindowToken(), 0);
            }
        }
    }

    private void load_ui_elements() {
        this.main_scroll_view = (ScrollView) findViewById(R.id.main_scroll_view);
        this.post_import_help_blurb = findViewById(R.id.post_import_help_blurb);
        this.profile_group = findViewById(R.id.profile_group);
        this.proxy_group = findViewById(R.id.proxy_group);
        this.server_group = findViewById(R.id.server_group);
        this.username_group = findViewById(R.id.username_group);
        this.password_group = findViewById(R.id.password_group);
        this.pk_password_group = findViewById(R.id.pk_password_group);
        this.cr_group = findViewById(R.id.cr_group);
        this.conn_details_group = findViewById(R.id.conn_details_group);
        this.stats_group = findViewById(R.id.stats_group);
        this.stats_expansion_group = findViewById(R.id.stats_expansion_group);
        this.info_group = findViewById(R.id.info_group);
        this.button_group = findViewById(R.id.button_group);
        this.profile_spin = (Spinner) findViewById(R.id.profile);
        this.profile_edit = (ImageButton) findViewById(R.id.profile_edit);
        this.proxy_spin = (Spinner) findViewById(R.id.proxy);
        this.proxy_edit = (ImageButton) findViewById(R.id.proxy_edit);
        this.server_spin = (Spinner) findViewById(R.id.server);
        this.challenge_view = (TextView) findViewById(R.id.challenge);
        this.username_edit = (EditText) findViewById(R.id.username);
        this.password_edit = (EditText) findViewById(R.id.password);
        this.pk_password_edit = (EditText) findViewById(R.id.pk_password);
        this.response_edit = (EditText) findViewById(R.id.response);
        this.password_save_checkbox = (CheckBox) findViewById(R.id.password_save);
        this.pk_password_save_checkbox = (CheckBox) findViewById(R.id.pk_password_save);
        this.status_view = (TextView) findViewById(R.id.status);
        this.status_icon_view = (ImageView) findViewById(R.id.status_icon);
        this.progress_bar = (ProgressBar) findViewById(R.id.progress);
        this.connect_button = (Button) findViewById(R.id.connect);
        this.disconnect_button = (Button) findViewById(R.id.disconnect);
        this.details_more_less = (TextView) findViewById(R.id.details_more_less);
        this.last_pkt_recv_view = (TextView) findViewById(R.id.last_pkt_recv);
        this.duration_view = (TextView) findViewById(R.id.duration);
        this.bytes_in_view = (TextView) findViewById(R.id.bytes_in);
        this.bytes_out_view = (TextView) findViewById(R.id.bytes_out);
        this.connect_button.setOnClickListener(this); //连接按纽
        this.disconnect_button.setOnClickListener(this);
        this.profile_spin.setOnItemSelectedListener(this);
        this.proxy_spin.setOnItemSelectedListener(this);
        this.server_spin.setOnItemSelectedListener(this);
        registerForContextMenu(this.profile_spin);
        registerForContextMenu(this.proxy_spin);
        findViewById(R.id.conn_details_boxed).setOnTouchListener(this);
        this.profile_edit.setOnClickListener(this);
        registerForContextMenu(this.profile_edit);
        this.proxy_edit.setOnClickListener(this);
        registerForContextMenu(this.proxy_edit);
        this.username_edit.setOnEditorActionListener(this);
        this.password_edit.setOnEditorActionListener(this);
        this.pk_password_edit.setOnEditorActionListener(this);
        this.response_edit.setOnEditorActionListener(this);
        this.textgroups = new View[]{this.cr_group, this.password_group, this.pk_password_group, this.username_group};
        this.textviews = new EditText[]{this.response_edit, this.password_edit, this.pk_password_edit, this.username_edit};
    }
}
