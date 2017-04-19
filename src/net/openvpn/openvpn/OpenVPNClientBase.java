package net.openvpn.openvpn;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.util.Log;
import android.view.KeyEvent;
import java.io.IOException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.text.DateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import net.openvpn.openvpn.HttpsClient.AuthContext;
import net.openvpn.openvpn.HttpsClient.CancelDetect;
import net.openvpn.openvpn.HttpsClient.CancelDetect.I;
import net.openvpn.openvpn.HttpsClient.Task;
import net.openvpn.openvpn.OpenVPNService.ConnectionStats;
import net.openvpn.openvpn.OpenVPNService.EventMsg;
import net.openvpn.openvpn.OpenVPNService.EventReceiver;
import net.openvpn.openvpn.OpenVPNService.LocalBinder;
import net.openvpn.openvpn.OpenVPNService.LogMsg;
import net.openvpn.openvpn.OpenVPNService.MergedProfile;
import net.openvpn.openvpn.OpenVPNService.Profile;
import net.openvpn.openvpn.OpenVPNService.ProfileList;


import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;


public abstract class OpenVPNClientBase extends Activity implements EventReceiver {
    private static final String TAG = "OpenVPNClientBase";
    private OpenVPNService mBoundService;
    private ServiceConnection mConnection;

    protected interface EpkiPost {
        void post_dispatch(String str);
    }

    /* renamed from: net.openvpn.openvpn.OpenVPNClientBase.2 */
    class AnonymousClass2 implements OnClickListener {
        final /* synthetic */ String val$profile_name;

        AnonymousClass2(String str) {
            this.val$profile_name = str;
        }

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case -1:
                    OpenVPNClientBase.this.submitDeleteProfileIntent(this.val$profile_name);
                default:
            }
        }
    }

    /* renamed from: net.openvpn.openvpn.OpenVPNClientBase.3 */
    class AnonymousClass3 implements KeyChainAliasCallback {
        final /* synthetic */ Handler handler;
        final /* synthetic */ EpkiPost next_action;

        /* renamed from: net.openvpn.openvpn.OpenVPNClientBase.3.1 */
        class AnonymousClass1 implements Runnable {
            final /* synthetic */ String _alias;

            AnonymousClass1(String str) {
                this._alias = str;
            }

            public void run() {
                AnonymousClass3.this.next_action.post_dispatch(this._alias);
            }
        }

        AnonymousClass3(EpkiPost epkiPost, Handler handler) {
            this.next_action = epkiPost;
            this.handler = handler;
        }

        public void alias(String alias) {
            if (alias != null) {
                this.handler.post(new AnonymousClass1(alias));
            }
        }
    }

    /* renamed from: net.openvpn.openvpn.OpenVPNClientBase.4 */
    class AnonymousClass4 implements OnClickListener {
        final /* synthetic */ EpkiPost next_action;
        final /* synthetic */ KeyChainAliasCallback response;
        final /* synthetic */ OpenVPNClientBase topthis;

        AnonymousClass4(OpenVPNClientBase openVPNClientBase, KeyChainAliasCallback keyChainAliasCallback, EpkiPost epkiPost) {
            this.topthis = openVPNClientBase;
            this.response = keyChainAliasCallback;
            this.next_action = epkiPost;
        }

        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case -2:
                    this.next_action.post_dispatch("DISABLE_CLIENT_CERT");//This is like continue to connect without pki cert,
                case -1:
                    KeyChain.choosePrivateKeyAlias(this.topthis, this.response, new String[]{"RSA"}, null, null, -1, null);
                default:
            }
        }
    }

    /* renamed from: net.openvpn.openvpn.OpenVPNClientBase.7 */
    class AnonymousClass7 extends Task {
        final /* synthetic */ AuthContext val$ac;
        final /* synthetic */ CancelDetect val$cancel;
        final /* synthetic */ Handler val$handler;
        final /* synthetic */ Runnable val$on_success;
        final /* synthetic */ boolean val$prefer_autologin;
        final /* synthetic */ String val$save_as_filename;

        /* renamed from: net.openvpn.openvpn.OpenVPNClientBase.7.1 */
        class AnonymousClass71 implements Runnable {
            final /* synthetic */ String val$profile_content;

            AnonymousClass71(String str) {
                this.val$profile_content = str;
            }

            public void run() {
                if (!AnonymousClass7.this.val$cancel.is_canceled()) {
                    if (AnonymousClass7.this.val$save_as_filename != null) {
                        OpenVPNClientBase.this.submitImportProfileIntent(this.val$profile_content, AnonymousClass7.this.val$save_as_filename, true);
                    } else {
                        OpenVPNClientBase.this.submitImportProfileIntent(this.val$profile_content, "client.ovpn", true);
                    }
                    AnonymousClass7.this.val$handler.post(AnonymousClass7.this.val$on_success);
                }
            }
        }

        AnonymousClass7(AuthContext authContext, boolean z, CancelDetect cancelDetect, String str, Handler handler, Runnable runnable) {
            this.val$ac = authContext;
            this.val$prefer_autologin = z; //prefer_autologin
            this.val$cancel = cancelDetect;
            this.val$save_as_filename = str;
            this.val$handler = handler;
            this.val$on_success = runnable;
        }

        public void run() {
            try {
                get_session_id(this.val$ac);
                int pavail = profile_types_available(this.val$ac);
                if (this.val$prefer_autologin && (pavail & 1) != 0) {
                    dispatch_prof(get_profile(this.val$ac, "GetAutologin"));
                } else if ((pavail & 2) != 0) {
                    dispatch_prof(get_profile(this.val$ac, "GetUserlogin"));
                } else {
                    this.interact.error_dialog(R.string.profile_import_error, R.string.no_profile_types, null);
                }
                close_session(this.val$ac);
            } catch (ErrorDialogException e) {
                e.dispatch(this.interact);
            } catch (Exception e2) {
                Log.e(OpenVPNClientBase.TAG, "import_profile", e2);
                this.interact.error_dialog(R.string.profile_import_error, 0, e2);
            }
        }

        private void dispatch_prof(String profile_content) {
            this.val$handler.post(new AnonymousClass71(profile_content));
        }
    }

    public OpenVPNClientBase() {
        this.mBoundService = null;
        this.mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                OpenVPNClientBase.this.mBoundService = ((LocalBinder) service).getService();
                Log.d(OpenVPNClientBase.TAG, "CLIBASE: onServiceConnected: " + OpenVPNClientBase.this.mBoundService.toString());
                OpenVPNClientBase.this.mBoundService.client_attach(OpenVPNClientBase.this);
                OpenVPNClientBase.this.post_bind();
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.d(OpenVPNClientBase.TAG, "CLIBASE: onServiceDisconnected");
                OpenVPNClientBase.this.mBoundService = null;
            }
        };
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void doBindService() {
        bindService(new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_BIND), this.mConnection, Context.BIND_IMPORTANT| Context.BIND_AUTO_CREATE);//65); //0x41
	Log.i(TAG,"bind service");
    }

    protected void doUnbindService() {
        Log.d(TAG, "CLIBASE: doUnbindService");
        if (this.mBoundService != null) {
            this.mBoundService.client_detach(this);
            unbindService(this.mConnection);
            this.mBoundService = null;
        }
    }

    protected void post_bind() {
	Log.i(TAG,"post_bind");
    }

    public void event(EventMsg ev) {
    }

    public void log(LogMsg lm) {
    }

    public PendingIntent get_configure_intent(int requestCode) {
        return null;
    }

    protected boolean is_active() {
        if (this.mBoundService == null || !this.mBoundService.is_active()) {
            return false;
        }
        return true;
    }

    protected Profile current_profile() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_current_profile();
        }
        return null;
    }

    protected ProfileList profile_list() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_profile_list(); // OpenVPNService -> get_profile_list()
        }
        return null;
    }
    
    protected void refresh_profile_list(){
	if(this.mBoundService != null){
	    this.mBoundService.refresh_profile_list();
	}
    }
    
    protected long max_profile_size() {
        return OpenVPNService.max_profile_size();
    }

    protected MergedProfile merge_parse_profile(String basename, String profile_content) {
        if (this.mBoundService != null) {
            return this.mBoundService.merge_parse_profile(basename, profile_content);
        }
        return null;
    }

    protected ClientAPI_LLVector get_stat_values_full() {
        if (this.mBoundService != null) {
            return this.mBoundService.stat_values_full();
        }
        return null;
    }

    protected ConnectionStats get_connection_stats() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_connection_stats();
        }
        return null;
    }

    protected long get_tunnel_bytes_per_cpu_second() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_tunnel_bytes_per_cpu_second();
        }
        return 0;
    }

    protected EventMsg get_last_event() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_last_event();
        }
        return null;
    }

    protected EventMsg get_last_event_prof_manage() {
        if (this.mBoundService != null) {
            return this.mBoundService.get_last_event_prof_manage();
        }
        return null;
    }

    public ArrayDeque<LogMsg> log_history() {
        if (this.mBoundService != null) {
            return this.mBoundService.log_history();
        }
        return null;
    }

    protected void jellyBeanHackPurge() {
        if (this.mBoundService != null) {
            this.mBoundService.jellyBeanHackPurge();
        }
    }

    protected ProxyList get_proxy_list() {
        if (this.mBoundService != null) {
            return this.mBoundService.proxy_list;
        }
        return null;
    }

    public void gen_proxy_context_expired_event() {
        if (this.mBoundService != null) {
            this.mBoundService.gen_proxy_context_expired_event();
        }
    }

    protected void gen_ui_reset_event(boolean exclude_self) {
        if (this.mBoundService != null) {
            this.mBoundService.gen_ui_reset_event(exclude_self, this);
        }
    }

    protected String get_gui_version(String name) {
        String versionName = "0.0";
        int versionCode = 0;
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = pi.versionName;
            versionCode = pi.versionCode;
        } catch (Exception e) {
            Log.e(TAG, "cannot obtain version info", e);
        }
        return String.format("%s %s-%d", new Object[]{name, versionName, Integer.valueOf(versionCode)});
    }

    protected void submitConnectIntent(String profile_name, String server, String vpn_proto, String conn_timeout, String username, String password, boolean cache_password, String pk_password, String response, String epki_alias, String compression_mode, String proxy_name, String proxy_username, String proxy_password, boolean proxy_allow_creds_dialog, String gui_version) {
        String prefix = OpenVPNService.INTENT_PREFIX;
        Intent intent = new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_CONNECT).putExtra(prefix + ".PROFILE", profile_name).putExtra(prefix + ".GUI_VERSION", gui_version).putExtra(prefix + ".PROXY_NAME", proxy_name).putExtra(prefix + ".PROXY_USERNAME", proxy_username).putExtra(prefix + ".PROXY_PASSWORD", proxy_password).putExtra(prefix + ".PROXY_ALLOW_CREDS_DIALOG", proxy_allow_creds_dialog).putExtra(prefix + ".SERVER", server).putExtra(prefix + ".PROTO", vpn_proto).putExtra(prefix + ".CONN_TIMEOUT", conn_timeout).putExtra(prefix + ".USERNAME", username).putExtra(prefix + ".PASSWORD", password).putExtra(prefix + ".CACHE_PASSWORD", cache_password).putExtra(prefix + ".PK_PASSWORD", pk_password).putExtra(prefix + ".RESPONSE", response).putExtra(prefix + ".EPKI_ALIAS", epki_alias).putExtra(prefix + ".COMPRESSION_MODE", compression_mode);
	
        if (this.mBoundService != null) {
            this.mBoundService.client_attach(this);
        }
        ComponentName serv = startService(intent); //ComponentName belong to Android  , A Service is an application component that can perform long-running operations in the background, and it does not provide a user interface.
	// 然后 startService 跳转到 Intent(OpenVPNService.class )->onStartCommand()上
	//然后 onStartCommand 分析intent.action,发现 是PREFIX.CONNECT,跳转connect_action,然后跳转到do_connect_action
	//then read ovpn content,start_connection.... 
        Log.d(TAG, "CLI: submitConnectIntent: " + profile_name);
    }

    public static void autostart(Context context) {
        PrefUtil prefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences(context));
        String profile_name = prefs.get_string("autostart_profile_name");
        if (profile_name != null) {
            prefs.delete_key("autostart_profile_name");
            if (prefs.get_boolean("autostart", false)) {
								
                Intent intent = new Intent(context, OpenVPNClient.class)
									.addFlags( FLAG_ACTIVITY_NEW_TASK |  FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS /*276824064*/)
									.putExtra("net.openvpn.openvpn.AUTOSTART_PROFILE_NAME", profile_name);

                context.startActivity(intent);
                Log.d(TAG, "CLIBASE: autostart profile='" + profile_name + "' intent=" + intent.toString());
            }
        }
    }

    protected Intent buildDisconnectIntent(boolean stop_service) {
        return new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_DISCONNECT).putExtra(OpenVPNService.INTENT_PREFIX + ".STOP", stop_service);
    }

    protected void submitDisconnectIntent(boolean stop_service) {
        Log.d(TAG, "CLIBASE: submitDisconnectIntent");
        startService(buildDisconnectIntent(stop_service));
    }

    protected void submitImportProfileIntent(String profile_content, String save_as_filename, boolean merge) {
        String prefix = OpenVPNService.INTENT_PREFIX;
        ComponentName serv = startService(new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_IMPORT_PROFILE).putExtra(prefix + ".CONTENT", profile_content).putExtra(prefix + ".FILENAME", save_as_filename).putExtra(prefix + ".MERGE", merge));
    }

    protected void submitImportProfileViaPathIntent(String profile_path) {
        ComponentName serv = startService(new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_IMPORT_PROFILE_VIA_PATH).putExtra(OpenVPNService.INTENT_PREFIX + ".PATH", profile_path));
    }

    protected void submitDeleteProfileIntent(String profile_name) {
        ComponentName serv = startService(new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_DELETE_PROFILE).putExtra(OpenVPNService.INTENT_PREFIX + ".PROFILE", profile_name));
    }

    protected void submitDeleteProfileIntentWithConfirm(String profile_name) {
        OnClickListener dialogClickListener = new AnonymousClass2(profile_name);
        new Builder(this).setTitle(R.string.delete_profile_confirm_title).setMessage(profile_name).setPositiveButton(R.string.delete_profile_confirm_yes, dialogClickListener).setNegativeButton(R.string.delete_profile_confirm_cancel, dialogClickListener).show();
    }

    protected void submitRenameProfileIntent(String profile_name, String new_profile_name) {
        String prefix = OpenVPNService.INTENT_PREFIX;
        ComponentName serv = startService(new Intent(this, OpenVPNService.class).setAction(OpenVPNService.ACTION_RENAME_PROFILE).putExtra(prefix + ".PROFILE", profile_name).putExtra(prefix + ".NEW_PROFILE", new_profile_name));
    }

    protected void createConnectShortcut(String profile_name, String shortcut_name) {
        getApplicationContext().sendBroadcast(new Intent().setAction("com.android.launcher.action.INSTALL_SHORTCUT").putExtra("android.intent.extra.shortcut.INTENT", new Intent(this, OpenVPNClient.class).addFlags( FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS ).putExtra("net.openvpn.openvpn.AUTOSTART_PROFILE_NAME", profile_name)).putExtra("android.intent.extra.shortcut.NAME", shortcut_name).putExtra("android.intent.extra.shortcut.ICON_RESOURCE", ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.openvpn_connected)));
    }

    protected void createDisconnectShortcut(String shortcut_name) {
        getApplicationContext().sendBroadcast(new Intent().setAction("com.android.launcher.action.INSTALL_SHORTCUT").putExtra("android.intent.extra.shortcut.INTENT", new Intent(this, OpenVPNDisconnect.class).addFlags( FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS )).putExtra("android.intent.extra.shortcut.NAME", shortcut_name).putExtra("android.intent.extra.shortcut.ICON_RESOURCE", ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.openvpn_disconnected)));
    }

    protected void createAppShortcut(String shortcut_name) {
        getApplicationContext().sendBroadcast(new Intent().setAction("com.android.launcher.action.INSTALL_SHORTCUT").putExtra("android.intent.extra.shortcut.INTENT", new Intent(this, OpenVPNClient.class)).putExtra("android.intent.extra.shortcut.NAME", shortcut_name).putExtra("android.intent.extra.shortcut.ICON_RESOURCE", ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.icon)));
    }

    protected void resolveExternalPkiAlias(Profile prof, EpkiPost next_action)
    {
	Log.i(TAG,"before dialogClickListener = new AnonymousClass4");
        OnClickListener dialogClickListener = new AnonymousClass4(this, new AnonymousClass3(next_action, new Handler()), next_action);
        if (prof == null || !prof.need_external_pki_alias())
	{
	    if(prof == null)
	    {
		Log.i(TAG,"resolveExternalPkiAlias prof is null");
		
	    }else if (!prof.need_external_pki_alias())
	    {
		Log.i(TAG,"resolveExternalPkiAlias prof need_external_pki_alias false");
	    }
	    
	    // next_action.post_dispatch(null);
        } else if(prof !=null && prof.need_external_pki_alias())
	{
	    Log.i(TAG," prof is not null and prof need external pki alias");
            new Builder(this).setTitle(R.string.select_certificate_title).setMessage(R.string.select_certificate_message).setPositiveButton(R.string.select_certificate_yes, dialogClickListener).setNegativeButton(R.string.select_certificate_no, dialogClickListener).show();
        }

	next_action.post_dispatch(null);
    }

    protected void import_pkcs12(String path) {
        jellyBeanHackPurge();
        try {
            Intent intent = KeyChain.createInstallIntent();
            intent.putExtra("PKCS12", FileUtil.readFileByteArray(path, 262144));
            startActivity(intent);
        } catch (IOException e) {
            ok_dialog(String.format("[ %s ] %s", new Object[]{path, getText(R.string.file_read_error)}));
        }
    }

    protected static String render_duration(int duration) {
        int seconds = duration % 60;
        int minutes = (duration / 60) % 60;
        return String.format("%d:%02d:%02d", new Object[]{Integer.valueOf(duration / 3600), Integer.valueOf(minutes), Integer.valueOf(seconds)});
    }

    protected void ok_dialog(String text) {
        new Builder(this).setTitle(text).setPositiveButton(R.string.ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();
    }

    protected void ok_dialog(String title, String message) {
        new Builder(this).setTitle(title).setMessage(message).setPositiveButton(R.string.ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        }).show();
    }

    protected boolean action_enter(int actionId, KeyEvent event) {
        return actionId == 5 || actionId == 6 || (actionId == 0 && event != null && event.getAction() == 0 && event.getRepeatCount() == 0);
    }

    protected void raise_file_selection_dialog(int requestCode, int prompt_res_id) {
        startActivityForResult(new Intent(this, FileDialog.class).putExtra(FileDialog.START_PATH, "/sdcard").putExtra(FileDialog.CAN_SELECT_DIR, false).putExtra(FileDialog.SELECTION_MODE, 1).putExtra(FileDialog.OPTION_ONE_CLICK_SELECT, false).putExtra(FileDialog.OPTION_PROMPT, resString(prompt_res_id)), requestCode);
    }

    protected static String get_app_expire_string() {
        Date expire = OpenVPNService.get_app_expire();
        if (expire != null) {
            return DateFormat.getDateTimeInstance().format(expire);
        }
        return null;
    }

    protected void warn_app_expiration(PrefUtil prefs) {
        String expire_string = get_app_expire_string();
        if (expire_string != null && !expire_string.equals(prefs.get_string("app_expire_string"))) {
            ok_dialog(String.format(resString(R.string.beta_expire_warn), new Object[]{expire_string}));
            prefs.set_string("app_expire_string", expire_string);
        }
    }

    protected static String get_openvpn_core_platform() {
        return OpenVPNService.get_openvpn_core_platform();
    }

    protected void init_default_preferences(PrefUtil prefs) {
        if (!prefs.contains_key("vpn_proto")) {
            prefs.set_string("vpn_proto", "adaptive");
        }
        if (!prefs.contains_key("conn_timeout")) {
            prefs.set_string("conn_timeout", "60");
        }
        if (!prefs.contains_key("compression_mode")) {
            prefs.set_string("compression_mode", "yes");
        }
        if (!prefs.contains_key("tls_version_min_override")) {
            prefs.set_string("tls_version_min_override", "default");
        }
        if (!prefs.contains_key("auto_keyboard")) {
            prefs.set_boolean("auto_keyboard", true);
        }
        if (!prefs.contains_key("google_dns_fallback")) {
            prefs.set_boolean("google_dns_fallback", true);
        }
        if (!prefs.contains_key("autostart_finish_on_connect")) {
            prefs.set_boolean("autostart_finish_on_connect", true);
        }
    }

    protected String resString(int res_id) {
        return getResources().getString(res_id);
    }

    protected void importProfileRemote(AuthContext ac, boolean prefer_autologin, I cancel_source, Runnable on_success, Runnable on_fail, String save_as_filename, boolean enable_cert_dialog, boolean enable_trust_error_dialog) {
        AuthContext authContext = ac;
        boolean z = prefer_autologin;
        Task anonymousClass7 = new AnonymousClass7(authContext, z, new CancelDetect(cancel_source), save_as_filename, new Handler(), on_success);
        //HttpsClient.run_task(this, task, cancel_source, on_fail, enable_cert_dialog, enable_trust_error_dialog, max_profile_size());
        HttpsClient.run_task(this, anonymousClass7, cancel_source, on_fail, enable_cert_dialog, enable_trust_error_dialog, max_profile_size());
    }

    protected void dumpCryptoAlgs() {
        try {
            for (Provider provider : Security.getProviders()) {
                Log.d(TAG, "CRYPTO provider: " + provider.getName() + " ****************");
                for (Service service : provider.getServices()) {
                    Log.d(TAG, "CRYPTO algorithm: " + service.getAlgorithm());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "dumpCryptoAlgs", e);
        }
    }
}
