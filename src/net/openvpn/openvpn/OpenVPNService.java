/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.app.Application
 *  android.app.Notification
 *  android.app.Notification$Builder
 *  android.app.PendingIntent
 *  android.content.BroadcastReceiver
 *  android.content.Context
 *  android.content.Intent
 *  android.content.IntentFilter
 *  android.content.SharedPreferences
 *  android.content.res.Resources
 *  android.net.VpnService
 *  android.net.VpnService$Builder
 *  android.os.Binder
 *  android.os.Build
 *  android.os.Build$VERSION
 *  android.os.Handler
 *  android.os.Handler$Callback
 *  android.os.IBinder
 *  android.os.Message
 *  android.os.ParcelFileDescriptor
 *  android.os.SystemClock
 *  android.preference.PreferenceManager
 *  android.security.KeyChain
 *  android.util.Base64
 *  android.util.Log
 *  android.widget.Toast
 */
package net.openvpn.openvpn;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;

import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONTokener;

import javax.crypto.Cipher;
import java.security.PrivateKey;

import net.openvpn.openvpn.CPUUsage;
import net.openvpn.openvpn.ClientAPI_Config;
import net.openvpn.openvpn.ClientAPI_ConnectionInfo;
import net.openvpn.openvpn.ClientAPI_DynamicChallenge;
import net.openvpn.openvpn.ClientAPI_EvalConfig;
import net.openvpn.openvpn.ClientAPI_Event;
import net.openvpn.openvpn.ClientAPI_ExternalPKICertRequest;
import net.openvpn.openvpn.ClientAPI_ExternalPKISignRequest;
import net.openvpn.openvpn.ClientAPI_InterfaceStats;
import net.openvpn.openvpn.ClientAPI_LLVector;
import net.openvpn.openvpn.ClientAPI_LogInfo;
import net.openvpn.openvpn.ClientAPI_MergeConfig;
import net.openvpn.openvpn.ClientAPI_OpenVPNClient;
import net.openvpn.openvpn.ClientAPI_ProvideCreds;
import net.openvpn.openvpn.ClientAPI_ServerEntry;
import net.openvpn.openvpn.ClientAPI_ServerEntryVector;
import net.openvpn.openvpn.ClientAPI_Status;
import net.openvpn.openvpn.ClientAPI_TransportStats;
import net.openvpn.openvpn.FileUtil;
import net.openvpn.openvpn.JellyBeanHack;
import net.openvpn.openvpn.OpenVPNClientThread;
import net.openvpn.openvpn.OpenVPNDebug;
import net.openvpn.openvpn.OpenVPNProxyCreds;
import net.openvpn.openvpn.PasswordUtil;
import net.openvpn.openvpn.PrefUtil;
import net.openvpn.openvpn.ProxyList;

public class OpenVPNService extends VpnService implements Handler.Callback, OpenVPNClientThread.EventReceiver {
    public static final String ACTION_BASE = "net.openvpn.openvpn.";
    public static final String ACTION_BIND = "net.openvpn.openvpn.BIND";
    public static final String ACTION_CONNECT = "net.openvpn.openvpn.CONNECT";
    public static final String ACTION_DELETE_PROFILE = "net.openvpn.openvpn.DELETE_PROFILE";
    public static final String ACTION_DISCONNECT = "net.openvpn.openvpn.DISCONNECT";
    public static final String ACTION_IMPORT_PROFILE = "net.openvpn.openvpn.IMPORT_PROFILE";
    public static final String ACTION_IMPORT_PROFILE_VIA_PATH = "net.openvpn.openvpn.ACTION_IMPORT_PROFILE_VIA_PATH";
    public static final String ACTION_RENAME_PROFILE = "net.openvpn.openvpn.RENAME_PROFILE";
    public static final String ACTION_SUBMIT_PROXY_CREDS = "net.openvpn.openvpn.ACTION_SUBMIT_PROXY_CREDS";
    public static final int EV_PRIO_HIGH = 3;
    public static final int EV_PRIO_INVISIBLE = 0;
    public static final int EV_PRIO_LOW = 1;
    public static final int EV_PRIO_MED = 2;
    public static final String PREFS_NAME = "VPNList";// from profile.dat ,store in SharedPreferences
    public static final int PROFILE_DAT_SKIP=8;
    
    private static final int GCI_REQ_ESTABLISH = 0;
    private static final int GCI_REQ_NOTIFICATION = 1;
    public static final String INTENT_PREFIX = "net.openvpn.openvpn";
    private static final int MSG_EVENT = 1;
    private static final int MSG_LOG = 2;
    private static final int NOTIFICATION_ID = 1024;
    private static final String TAG = "OpenVPNService";
    public  static final String PROFILE_DAT = "profile.dat";
    
    public static final int log_deque_max = 250;
    private boolean active = false;
    private ArrayDeque<EventReceiver> clients = new ArrayDeque();
    private CPUUsage cpu_usage;
    private Profile current_profile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private boolean enable_notifications;
    private HashMap event_info;
    private JellyBeanHack jellyBeanHack;
    private EventMsg last_event;
    private EventMsg last_event_prof_manage;
    private ArrayDeque<LogMsg> log_deque = new ArrayDeque();
    private final IBinder mBinder;
    private ConnectivityReceiver mConnectivityReceiver;
    private Handler mHandler;
    Notification.Builder mNotifyBuilder;
    private OpenVPNClientThread mThread;
    private PrefUtil prefs;
    private ProfileList profile_list;
    public ProxyList proxy_list;
    private PasswordUtil pwds;
    private boolean shutdown_pending = false;
    private long thread_started = 0;

    static {

    		try 
		{
		    System.loadLibrary("ovpncli");
    		} catch (UnsatisfiedLinkError e) 
		{
 		    System.err.println("Native code library failed to load.\n" + e);
		    Log.e("WestgalaxyVPNService","Native code library failed to load.\n" + e);
		    System.exit(1);
    		}

        ClientAPI_OpenVPNClient.init_process();
        Log.d(TAG, (String)ClientAPI_OpenVPNClient.crypto_self_test());
    }

    public OpenVPNService() {
        this.mBinder = new LocalBinder();
    }

    private String cert_format_pem(X509Certificate x509Certificate) throws CertificateEncodingException {
        byte[] arrby = x509Certificate.getEncoded();
        Object[] arrobject = new Object[]{Base64.encodeToString((byte[])arrby, (int)0)};
        return String.format("-----BEGIN CERTIFICATE-----%n%s-----END CERTIFICATE-----%n", arrobject);
    }

    private boolean connect_action(final String string2, final Intent intent, final boolean bl) {
        if (this.active) {
            this.stop_thread();
            new Handler().postDelayed(new Runnable(){

                @Override
                public void run() {
                    OpenVPNService.this.do_connect_action(string2, intent, bl);
                }
            }, 2000);
            return true;
        }
        this.do_connect_action(string2, intent, bl);
        return true;
    }

    private void crypto_self_test() {
        String string2 = ClientAPI_OpenVPNClient.crypto_self_test();
        if (string2.length() <= 0) return;
        Log.d(TAG, (String)String.format("SERV: crypto_self_test\n%s", string2));
    }

    private boolean delete_profile_action(String string2, Intent intent) {
        String string3 = intent.getStringExtra(string2 + ".PROFILE");
        this.get_profile_list();
        Profile profile = this.profile_list.get_profile_by_name(string3);
        if (profile == null) {
            return false;
        }
        if (!profile.is_deleteable()) {
            this.gen_event(1, "PROFILE_DELETE_FAILED", string3);
            return false;
        }
        if (this.active && profile == this.current_profile) {
            this.stop_thread();
        }
        if (!this.deleteFile(profile.get_filename())) {
            this.gen_event(1, "PROFILE_DELETE_FAILED", profile.get_name());
            return false;
        }
        this.pwds.remove("auth", string3);
        this.pwds.remove("pk", string3);
        this.refresh_profile_list();
        this.gen_event(0, "PROFILE_DELETE_SUCCESS", profile.get_name());
        return true;
    }

    private void disconnect_action(String string2, Intent intent) {
        boolean bl = intent.getBooleanExtra(string2 + ".STOP", false);
        this.stop_thread();
        if (!bl) return;
        this.stopSelf();
    }

    private boolean do_connect_action_buffer(String string2, Intent intent, boolean bl) {
        String string3;
        ProxyContext proxyContext;
        String string4     = intent.getStringExtra(string2 + ".PROFILE");

        String string5 = intent.getStringExtra(string2 + ".GUI_VERSION");
        String string6 = intent.getStringExtra(string2 + ".PROXY_NAME");
        String string7 = intent.getStringExtra(string2 + ".PROXY_USERNAME");
        String string8 = intent.getStringExtra(string2 + ".PROXY_PASSWORD");
        boolean bl2 = intent.getBooleanExtra(string2 + ".PROXY_ALLOW_CREDS_DIALOG", false);
        String string9 = intent.getStringExtra(string2 + ".SERVER");
        String string10 = intent.getStringExtra(string2 + ".PROTO");
        String string11 = intent.getStringExtra(string2 + ".CONN_TIMEOUT");
        String string12 = intent.getStringExtra(string2 + ".USERNAME");
        String string13 = intent.getStringExtra(string2 + ".PASSWORD");
        boolean bl3 = intent.getBooleanExtra(string2 + ".CACHE_PASSWORD", false);
        String string14 = intent.getStringExtra(string2 + ".PK_PASSWORD");
        String string15 = intent.getStringExtra(string2 + ".RESPONSE");
        String string16 = intent.getStringExtra(string2 + ".EPKI_ALIAS");
        String string17 = intent.getStringExtra(string2 + ".COMPRESSION_MODE");
        String string18 = OpenVPNDebug.pw_repl(string12, string13);
	
        Profile profile = this.locate_profile(string4);/// based on the profile name,
	/// searching in memory of ProfileList,an ArrayList, so works well on profile.dat Method ,too
	
        if (profile == null) {
            return false;
        }
        if (string6 != null) {// PROXY_NAME
            proxyContext = profile.get_proxy_context(true); // will try to new a proxy context
            proxyContext.new_connection(intent, string4, string6, string7, string8, bl2, this.proxy_list, bl); //just setup ,not to connect
        } else {
            profile.reset_proxy_context();
            proxyContext = null;
        }
        String string19 = profile.get_location();
        String string20 = profile.get_filename();// return orig_name or encodef utf8 filename 
        try {
	    // look, is get_filename, not the "name",the "name" is like complex, xxxx [bundled:test] ,
            string3 = this.read_file(string19, string20);
        }
        catch (IOException var25_27) {
            this.gen_event(1, "PROFILE_NOT_FOUND", String.format("%s/%s", string19, string20));
            return false;
        }
        Object[] arrobject = new Object[]{string3.length()};
        Log.d((String)"OpenVPNService", (String)String.format("SERV: profile file len=%d", arrobject));
        return this.start_connection(profile, string3, string5, proxyContext, string9, string10, string11, string12, string18, bl3, string14, string15, string16, string17);
    }

    
    private boolean do_connect_action(String string2, Intent intent, boolean bl) {
        String string3;
        ProxyContext proxyContext;
        String string4 = intent.getStringExtra(string2 + ".PROFILE");
        String string5 = intent.getStringExtra(string2 + ".GUI_VERSION");
        String string6 = intent.getStringExtra(string2 + ".PROXY_NAME");
        String string7 = intent.getStringExtra(string2 + ".PROXY_USERNAME");
        String string8 = intent.getStringExtra(string2 + ".PROXY_PASSWORD");
        boolean bl2 = intent.getBooleanExtra(string2 + ".PROXY_ALLOW_CREDS_DIALOG", false);
        String string9 = intent.getStringExtra(string2 + ".SERVER");
        String string10 = intent.getStringExtra(string2 + ".PROTO");
        String string11 = intent.getStringExtra(string2 + ".CONN_TIMEOUT");
        String string12 = intent.getStringExtra(string2 + ".USERNAME");
        String string13 = intent.getStringExtra(string2 + ".PASSWORD");
        boolean bl3 = intent.getBooleanExtra(string2 + ".CACHE_PASSWORD", false);
        String string14 = intent.getStringExtra(string2 + ".PK_PASSWORD");
        String string15 = intent.getStringExtra(string2 + ".RESPONSE");
        String string16 = intent.getStringExtra(string2 + ".EPKI_ALIAS");
        String string17 = intent.getStringExtra(string2 + ".COMPRESSION_MODE");
        String string18 = OpenVPNDebug.pw_repl(string12, string13);
	
        Profile profile = this.locate_profile(string4);//get the profile by name,not by file,so it works well  on profile.dat method,too
	
        if (profile == null) {
            return false;
        }
        if (string6 != null) {// PROXY_NAME
            proxyContext = profile.get_proxy_context(true); // will try to new a proxy context
            proxyContext.new_connection(intent, string4, string6, string7, string8, bl2, this.proxy_list, bl); //just setup ,not to connect
        } else {
            profile.reset_proxy_context();
            proxyContext = null;
        }
        String string19 = profile.get_location();
        String string20 = profile.get_filename();
        try {
	    
            string3 = this.read_file(string19, string20);//  here I need to set "virtual" method to read virtual .ovpn
	    // all virtual read from SharedPreferences , 
        }
        catch (IOException var25_27) {
            this.gen_event(1, "PROFILE_NOT_FOUND", String.format("%s/%s", string19, string20));
            return false;
        }
        Object[] arrobject = new Object[]{string3.length()};
        Log.d(TAG, (String)String.format("SERV: profile file len=%d", arrobject));
        return this.start_connection(profile, string3, string5, proxyContext, string9, string10, string11, string12, string18, bl3, string14, string15, string16, string17);
    }

    private void gen_event(int n, String string2, String string3) {
        this.gen_event(n, string2, string3, null, null);
    }

    private void gen_event(int n, String string2, String string3, String string4) {
        this.gen_event(n, string2, string3, string4, null);
    }

    private void gen_event(int n, String string2, String string3, String string4, EventReceiver eventReceiver) {
	Log.i(TAG,"in gen_event");
        EventInfo eventInfo = (EventInfo)this.event_info.get(string2);
        EventMsg eventMsg = new EventMsg();
        eventMsg.flags = n | 2;
        if (eventInfo != null) {
            eventMsg.progress = eventInfo.progress;
            eventMsg.priority = eventInfo.priority;
            eventMsg.res_id = eventInfo.res_id;
            eventMsg.icon_res_id = eventInfo.icon_res_id;
            eventMsg.sender = eventReceiver;
            eventMsg.flags |= eventInfo.flags;
        } else {
            eventMsg.res_id = R.string.unknown;
        }
        eventMsg.name = string2;
        eventMsg.info = string3 != null ? string3 : "";
        if ((4 & eventMsg.flags) != 0) {
            eventMsg.expires = 60000 + SystemClock.elapsedRealtime();
        }
        eventMsg.profile_override = string4;
        Message message = this.mHandler.obtainMessage(1, (Object)eventMsg);
        this.mHandler.sendMessage(message);//handleMessage
    }

    protected static Date get_app_expire() {
        int n = ClientAPI_OpenVPNClient.app_expire();
        if (n <= 0) return null;
        return new Date(1000 * (long)n);
    }

    private PendingIntent get_configure_intent(int n) {
        PendingIntent pendingIntent;
        Iterator<EventReceiver> iterator = this.clients.iterator();
        do {
            if (!iterator.hasNext()) return null;
        } while ((pendingIntent = iterator.next().get_configure_intent(n)) == null);
        return pendingIntent;
    }

    protected static String get_openvpn_core_platform() {
        return ClientAPI_OpenVPNClient.platform();
    }

    private boolean import_profile(String string2, String string3, boolean bl) {
        if (!ProfileFN.has_ovpn_ext(string3) || FileUtil.dirname(string3) != null) {
            this.gen_event(1, "PROFILE_FILENAME_ERROR", string3);
            return false;
        }
        if (bl) {
            ClientAPI_MergeConfig clientAPI_MergeConfig = ClientAPI_OpenVPNClient.merge_config_string_static(string2);
            String string4 = "PROFILE_" + clientAPI_MergeConfig.getStatus();
            if (!string4.equals("PROFILE_MERGE_SUCCESS")) {
                this.gen_event(1, string4, clientAPI_MergeConfig.getErrorText());
                return false;
            }
            string2 = clientAPI_MergeConfig.getProfileContent();
        }
        ClientAPI_Config clientAPI_Config = new ClientAPI_Config();
        clientAPI_Config.setContent(string2);
        ClientAPI_EvalConfig clientAPI_EvalConfig = ClientAPI_OpenVPNClient.eval_config_static(clientAPI_Config);
        if (clientAPI_EvalConfig.getError()) {
            Object[] arrobject = new Object[]{string3, clientAPI_EvalConfig.getMessage()};
            this.gen_event(1, "PROFILE_PARSE_ERROR", String.format("%s : %s", arrobject));
            return false;
        }
        Profile profile = new Profile("imported", string3, false, clientAPI_EvalConfig);
        try {
            FileUtil.writeFileAppPrivate((Context)this, profile.get_filename(), string2);
            String string5 = profile.get_name();
            this.pwds.remove("auth", string5);
            this.pwds.remove("pk", string5);
            this.refresh_profile_list();
            this.gen_event(0, "PROFILE_IMPORT_SUCCESS", string5, string5);
            return true;
        }
        catch (IOException var7_11) {
            this.gen_event(1, "PROFILE_WRITE_ERROR", string3);
            return false;
        }
    }

    private boolean import_profile_action(String string2, Intent intent) {
        return this.import_profile(intent.getStringExtra(string2 + ".CONTENT"), intent.getStringExtra(string2 + ".FILENAME"), intent.getBooleanExtra(string2 + ".MERGE", false));
    }

    private boolean import_profile_via_path_action(String string2, Intent intent) {
        ClientAPI_MergeConfig clientAPI_MergeConfig = ClientAPI_OpenVPNClient.merge_config_static(intent.getStringExtra(string2 + ".PATH"), true);
        String string3 = "PROFILE_" + clientAPI_MergeConfig.getStatus();
        if (string3.equals("PROFILE_MERGE_SUCCESS")) {
            return this.import_profile(clientAPI_MergeConfig.getProfileContent(), clientAPI_MergeConfig.getBasename(), false);
        }
        this.gen_event(1, string3, clientAPI_MergeConfig.getErrorText());
        return false;
    }

    private Profile locate_profile(String string2) {
        this.get_profile_list();
        Profile profile = this.profile_list.get_profile_by_name(string2);
        if (profile != null) return profile;
        this.gen_event(1, "PROFILE_NOT_FOUND", string2);
        return null;
    }

    private void log_message(String string2) {
        LogMsg logMsg = new LogMsg();
        logMsg.line = string2 + "\n";
        this.log_message(logMsg);
    }

    private void log_message(LogMsg logMsg) {
        Object[] arrobject = new Object[]{this.dateFormat.format(new Date()), logMsg.line};
        logMsg.line = String.format("%s -- %s", arrobject);
        this.log_deque.addLast(logMsg);
        while (this.log_deque.size() > 250) {
            this.log_deque.removeFirst();
        }
        Iterator<EventReceiver> iterator = this.clients.iterator();
        while (iterator.hasNext()) {
            iterator.next().log(logMsg);
        }
    }

    private void log_stats() {
        if (!this.active) return;
        String[] arrstring = OpenVPNService.stat_names();
        ClientAPI_LLVector clientAPI_LLVector = this.stat_values_full();
        if (clientAPI_LLVector == null) return;
        int n = 0;
        while (n < arrstring.length) {
            String string2 = arrstring[n];
            long l = clientAPI_LLVector.get(n);
            if (l > 0) {
                Object[] arrobject = new Object[]{string2, l};
                Log.i(TAG, (String)String.format("STAT %s=%s", arrobject));
            }
            ++n;
        }
    }

    public static long max_profile_size() {
        return ClientAPI_OpenVPNClient.max_profile_size();
    }

    private void populate_event_info_map() {
        this.event_info = new HashMap();
	//res_id,icon_res_id,progress,priority,flags
        this.event_info.put("RECONNECTING", new EventInfo(R.string.reconnecting, R.drawable.connecting, 20, 2, 0));
        this.event_info.put("RESOLVE", new EventInfo(R.string.resolve, R.drawable.connecting, 30, 1, 0));
        this.event_info.put("WAIT_PROXY", new EventInfo(R.string.wait_proxy, R.drawable.connecting, 40, 1, 0));
        this.event_info.put("WAIT", new EventInfo(R.string.wait, R.drawable.connecting, 50, 1, 0));
        this.event_info.put("CONNECTING", new EventInfo(R.string.connecting, R.drawable.connecting, 60, 1, 0));
        this.event_info.put("GET_CONFIG", new EventInfo(R.string.get_config, R.drawable.connecting, 70, 1, 0));
        this.event_info.put("ASSIGN_IP", new EventInfo(R.string.assign_ip, R.drawable.connecting, 80, 1, 0));
        this.event_info.put("ADD_ROUTES", new EventInfo(R.string.add_routes, R.drawable.connecting, 90, 1, 0));
        this.event_info.put("CONNECTED", new EventInfo(R.string.connected, R.drawable.connected, 100, 3, 0));
        this.event_info.put("DISCONNECTED", new EventInfo(R.string.disconnected, R.drawable.disconnected, 0, 2, 0));
        this.event_info.put("AUTH_FAILED", new EventInfo(R.string.auth_failed, R.drawable.error, 0, 3, 0));
        this.event_info.put("PEM_PASSWORD_FAIL", new EventInfo(R.string.pem_password_fail, R.drawable.error, 0, 3, 0));
        this.event_info.put("CERT_VERIFY_FAIL", new EventInfo(R.string.cert_verify_fail, R.drawable.error, 0, 3, 0));
        this.event_info.put("TLS_VERSION_MIN", new EventInfo(R.string.tls_version_min, R.drawable.error, 0, 3, 0));
        this.event_info.put("DYNAMIC_CHALLENGE", new EventInfo(R.string.dynamic_challenge, R.drawable.error, 0, 2, 0));
        this.event_info.put("TUN_SETUP_FAILED", new EventInfo(R.string.tun_setup_failed, R.drawable.error, 0, 3, 0));
        this.event_info.put("TUN_IFACE_CREATE", new EventInfo(R.string.tun_iface_create, R.drawable.error, 0, 3, 0));
        this.event_info.put("TAP_NOT_SUPPORTED", new EventInfo(R.string.tap_not_supported, R.drawable.error, 0, 3, 0));
        this.event_info.put("PROFILE_NOT_FOUND", new EventInfo(R.string.profile_not_found, R.drawable.error, 0, 3, 0));
        this.event_info.put("CONFIG_FILE_PARSE_ERROR", new EventInfo(R.string.config_file_parse_error, R.drawable.error, 0, 3, 0));
        this.event_info.put("NEED_CREDS_ERROR", new EventInfo(R.string.need_creds_error, R.drawable.error, 0, 3, 0));
        this.event_info.put("CREDS_ERROR", new EventInfo(R.string.creds_error, R.drawable.error, 0, 3, 0));
        this.event_info.put("CONNECTION_TIMEOUT", new EventInfo(R.string.connection_timeout, R.drawable.error, 0, 3, 0));
        this.event_info.put("INACTIVE_TIMEOUT", new EventInfo(R.string.inactive_timeout, R.drawable.error, 0, 3, 0));
        this.event_info.put("PROXY_NEED_CREDS", new EventInfo(R.string.proxy_need_creds, R.drawable.error, 0, 3, 0));
        this.event_info.put("PROXY_ERROR", new EventInfo(R.string.proxy_error, R.drawable.error, 0, 3, 0));
        this.event_info.put("PROXY_CONTEXT_EXPIRED", new EventInfo(R.string.proxy_context_expired, R.drawable.error, 0, 3, 0));
        this.event_info.put("EPKI_ERROR", new EventInfo(R.string.epki_error, R.drawable.error, 0, 3, 0));
        this.event_info.put("EPKI_INVALID_ALIAS", new EventInfo(R.string.epki_invalid_alias, R.drawable.error, 0, 0, 0));
        this.event_info.put("PAUSE", new EventInfo(R.string.pause, R.drawable.pause, 0, 3, 0));
        this.event_info.put("RESUME", new EventInfo(R.string.resume, R.drawable.connecting, 0, 2, 0));
        this.event_info.put("CORE_THREAD_ACTIVE", new EventInfo(R.string.core_thread_active, R.drawable.connecting, 10, 1, 0));
        this.event_info.put("CORE_THREAD_INACTIVE", new EventInfo(R.string.core_thread_inactive, -1, 0, 0, 0));
        this.event_info.put("CORE_THREAD_ERROR", new EventInfo(R.string.core_thread_error, R.drawable.error, 0, 3, 0));
        this.event_info.put("CORE_THREAD_ABANDONED", new EventInfo(R.string.core_thread_abandoned, R.drawable.error, 0, 3, 0));
        this.event_info.put("CLIENT_HALT", new EventInfo(R.string.client_halt, R.drawable.error, 0, 3, 0));
        this.event_info.put("CLIENT_RESTART", new EventInfo(R.string.client_restart, R.drawable.connecting, 0, 2, 0));
        this.event_info.put("PROFILE_IMPORT_SUCCESS", new EventInfo(R.string.profile_import_success, R.drawable.rightarrow, 0, 2, 44));
        this.event_info.put("PROFILE_DELETE_SUCCESS", new EventInfo(R.string.profile_delete_success, R.drawable.delete, 0, 2, 12));
        this.event_info.put("PROFILE_DELETE_FAILED", new EventInfo(R.string.profile_delete_failed, R.drawable.error, 0, 2, 4));
        this.event_info.put("PROFILE_PARSE_ERROR", new EventInfo(R.string.profile_parse_error, R.drawable.error, 0, 3, 4));
        this.event_info.put("PROFILE_CONFLICT", new EventInfo(R.string.profile_conflict, R.drawable.error, 0, 3, 4));
        this.event_info.put("PROFILE_WRITE_ERROR", new EventInfo(R.string.profile_write_error, R.drawable.error, 0, 3, 4));
        this.event_info.put("PROFILE_FILENAME_ERROR", new EventInfo(R.string.profile_filename_error, R.drawable.error, 0, 3, 4));
        this.event_info.put("PROFILE_RENAME_SUCCESS", new EventInfo(R.string.profile_rename_success, R.drawable.rightarrow, 0, 2, 12));
        this.event_info.put("PROFILE_RENAME_FAILED", new EventInfo(R.string.profile_rename_failed, R.drawable.error, 0, 2, 4));
        this.event_info.put("PROFILE_MERGE_EXCEPTION", new EventInfo(R.string.profile_merge_exception, R.drawable.error, 0, 2, 4));
        this.event_info.put("PROFILE_MERGE_OVPN_EXT_FAIL", new EventInfo(R.string.profile_merge_ovpn_ext_fail, R.drawable.error, 0, 2, 4));
        this.event_info.put("PROFILE_MERGE_OVPN_FILE_FAIL", new EventInfo(R.string.profile_merge_ovpn_file_fail, R.drawable.error, 0, 2, 4));
        this.event_info.put("PROFILE_MERGE_REF_FAIL", new EventInfo(R.string.profile_merge_ref_fail, R.drawable.error, 0, 2, 4));
        this.event_info.put("PROFILE_MERGE_MULTIPLE_REF_FAIL", new EventInfo(R.string.profile_merge_multiple_ref_fail, R.drawable.error, 0, 2, 4));
        this.event_info.put("UI_RESET", new EventInfo(R.string.ui_reset, R.drawable.rightarrow, 0, 0, 8));
    }

    private void register_connectivity_receiver() {
        this.mConnectivityReceiver = new ConnectivityReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
	
        this.registerReceiver((BroadcastReceiver)this.mConnectivityReceiver, intentFilter);
    }

    private boolean rename_profile_action(String string2, Intent intent) {
        Object[] arrobject;
        String string3;
        String string4 = intent.getStringExtra(string2 + ".PROFILE");
        String string5 = intent.getStringExtra(string2 + ".NEW_PROFILE");
        this.get_profile_list();
        Profile profile = this.profile_list.get_profile_by_name(string4);
        if (profile == null) {
            return false;
        }
        if (!profile.is_renameable() || string5 == null || string5.length() == 0) {
            Log.d((String)"OpenVPNService", (String)"PROFILE_RENAME_FAILED: rename preliminary checks");
            this.gen_event(1, "PROFILE_RENAME_FAILED", string4);
            return false;
        }
        File file = this.getFilesDir();
        Object[] arrobject2 = new Object[]{file.getPath(), profile.orig_filename};
        String string6 = String.format("%s/%s", arrobject2);
        if (!FileUtil.renameFile(string6, string3 = String.format("%s/%s", arrobject = new Object[]{file.getPath(), ProfileFN.encode_profile_fn(string5)}))) {
            Log.d(TAG, (String)String.format("PROFILE_RENAME_FAILED: rename operation from='%s' to='%s'", string6, string3));
            this.gen_event(1, "PROFILE_RENAME_FAILED", string4);
            return false;
        }
        this.refresh_profile_list();
        Profile profile2 = this.profile_list.get_profile_by_name(string5);
        if (profile2 == null) {
            Log.d(TAG, (String)"PROFILE_RENAME_FAILED: post-rename profile get");
            this.gen_event(1, "PROFILE_RENAME_FAILED", string4);
            return false;
        }
        this.pwds.remove("auth", string4);
        this.pwds.remove("pk", string4);
        this.gen_event(0, "PROFILE_RENAME_SUCCESS", profile2.get_name(), profile2.get_name());
        return true;
    }

    private String resString(int n) {
        return this.getResources().getString(n);
    }

    private boolean start_connection(Profile profile, String string2, String string3, ProxyContext proxyContext, String string4, String string5, String string6, String string7, String string8, boolean bl, String string9, String string10, String string11, String string12) {
        ClientAPI_EvalConfig clientAPI_EvalConfig;
        boolean bl2;
        if (this.active) {
            return false;
        }
        this.enable_notifications = this.prefs.get_boolean("enable_notifications", false);
        OpenVPNClientThread openVPNClientThread = new OpenVPNClientThread();
        ClientAPI_Config clientAPI_Config = new ClientAPI_Config();
        clientAPI_Config.setContent(string2);
        if (string4 != null) {
            clientAPI_Config.setServerOverride(string4);
        }
        if (string5 != null) {
            clientAPI_Config.setProtoOverride(string5);
        }
        if (string6 != null) {
            int n;
            try {
                int n2;
                n = n2 = Integer.parseInt(string6);
            }
            catch (NumberFormatException var26_26) {
                n = 0;
            }
            clientAPI_Config.setConnTimeout(n);
        }
        if (string12 != null) {
            clientAPI_Config.setCompressionMode(string12);
        }
        if (string9 != null) {
            clientAPI_Config.setPrivateKeyPassword(string9);
        }
        if ((bl2 = this.prefs.get_boolean("tun_persist", false)) && Build.VERSION.SDK_INT == 19) {
            Log.i(TAG, (String)"Seamless Tunnel disabled for KitKat 4.4 - 4.4.2");
            bl2 = false;
        }
        clientAPI_Config.setTunPersist(bl2);
        clientAPI_Config.setGoogleDnsFallback(this.prefs.get_boolean("google_dns_fallback", false));
        clientAPI_Config.setForceAesCbcCiphersuites(this.prefs.get_boolean("force_aes_cbc_ciphersuites_v2", false));
        clientAPI_Config.setAltProxy(this.prefs.get_boolean("alt_proxy", false));
        String string13 = this.prefs.get_string("tls_version_min_override");
        if (string13 != null) {
            clientAPI_Config.setTlsVersionMinOverride(string13);
        }
        if (string3 != null) {
            clientAPI_Config.setGuiVersion(string3);
        }
        if (profile.get_epki()) {
            if (string11 != null) {
                profile.persist_epki_alias(string11);
            } else {
                string11 = profile.get_epki_alias();
            }
            if (string11 != null) {
                if (string11.equals("DISABLE_CLIENT_CERT")) {
                    clientAPI_Config.setDisableClientCert(true);
                } else {
                    clientAPI_Config.setExternalPkiAlias(string11);
                }
            }
        }
        if (proxyContext != null) {
            proxyContext.client_api_config(clientAPI_Config);// 修改结构体clientAPI_Config中的帐密而已
        }
        if ((clientAPI_EvalConfig = openVPNClientThread.eval_config(clientAPI_Config)).getError()) { // OpenVPNClientThread是继承ClientAPI_OpenVPNClient ,eval_config源自libovpncli
            this.gen_event(1, "CONFIG_FILE_PARSE_ERROR", clientAPI_EvalConfig.getMessage());
            return false;
        }
        ClientAPI_ProvideCreds clientAPI_ProvideCreds = new ClientAPI_ProvideCreds();
        if (profile.is_dynamic_challenge()) {
            if (string10 != null) {
                clientAPI_ProvideCreds.setResponse(string10);
            }
            clientAPI_ProvideCreds.setDynamicChallengeCookie( profile.dynamic_challenge.cookie );
            profile.reset_dynamic_challenge();
        } else {
            if (!clientAPI_EvalConfig.getAutologin() && string7 != null && string7.length() == 0) {
                this.gen_event(1, "NEED_CREDS_ERROR", null);
                return false;
            }
            if (string7 != null) {
                clientAPI_ProvideCreds.setUsername(string7);
            }
            if (string8 != null) {
                clientAPI_ProvideCreds.setPassword(string8);
            }
            if (string10 != null) {
                clientAPI_ProvideCreds.setResponse(string10);
            }
        }
        clientAPI_ProvideCreds.setCachePassword(bl);
        clientAPI_ProvideCreds.setReplacePasswordWithSessionID(true);
        ClientAPI_Status clientAPI_Status = openVPNClientThread.provide_creds(clientAPI_ProvideCreds);
        if (clientAPI_Status.getError()) {
            this.gen_event(1, "CREDS_ERROR", clientAPI_Status.getMessage());
            return false;
        }
        Object[] arrobject = new Object[9];
        arrobject[0] = profile.name;
        arrobject[1] = string7;
        String string14 = proxyContext != null ? proxyContext.name() : "undef";
        arrobject[2] = string14;
        arrobject[3] = string4;
        arrobject[4] = string5;
        arrobject[5] = string6;
        arrobject[6] = string10;
        arrobject[7] = string11;
        arrobject[8] = string12;
        Log.i(TAG, (String)String.format("SERV: CONNECT prof=%s user=%s proxy=%s serv=%s proto=%s to=%s resp=%s epki_alias=%s comp=%s", arrobject));
        this.current_profile = profile;
        this.set_autostart_profile_name(profile.get_name());
        this.start_notification();
        this.gen_event(0, "CORE_THREAD_ACTIVE", null);
        openVPNClientThread.connect(this);
        this.mThread = openVPNClientThread;
        this.thread_started = SystemClock.elapsedRealtime();
        this.cpu_usage = new CPUUsage();
        this.active = true;
        return true;
    }

    private void start_notification() {
        if (this.mNotifyBuilder != null) return;
        if (this.current_profile == null) return;
        this.mNotifyBuilder = new Notification.Builder((Context)this).setContentIntent(this.get_configure_intent(1)).setSmallIcon(R.drawable.icon).setContentTitle((CharSequence)this.current_profile.get_name()+" Westgalaxy").setContentText((CharSequence)this.resString(R.string.notification_initial_content)).setOnlyAlertOnce(true).setOngoing(true).setWhen(new Date().getTime());
	
        this.startForeground(NOTIFICATION_ID, this.mNotifyBuilder.getNotification());
    }

    public static String[] stat_names() {
        int n = ClientAPI_OpenVPNClient.stats_n();
        String[] arrstring = new String[n];
        int n2 = 0;
        while (n2 < n) {
            arrstring[n2] = ClientAPI_OpenVPNClient.stats_name(n2);
            ++n2;
        }
        return arrstring;
    }

    private void stop_notification() {
        if (this.mNotifyBuilder == null) return;
        this.mNotifyBuilder = null;
        this.stopForeground(true);
    }

    private void stop_thread() {
        if (!this.active) return;
        this.mThread.stop();
        this.mThread.wait_thread_short();
        Log.d((String)"OpenVPNService", (String)"SERV: stop_thread succeeded");
    }

    private boolean submit_proxy_creds_action(String string2, Intent intent) {
        ProxyContext proxyContext;
        Intent intent2;
        Profile profile = this.locate_profile(intent.getStringExtra(string2 + ".PROFILE"));
        if (profile != null && (proxyContext = profile.get_proxy_context(false)) != null && (intent2 = proxyContext.submit_proxy_creds(intent.getStringExtra(string2 + ".PROXY_NAME"), intent.getStringExtra(string2 + ".PROXY_USERNAME"), intent.getStringExtra(string2 + ".PROXY_PASSWORD"), intent.getBooleanExtra(string2 + ".PROXY_REMEMBER_CREDS", false), this.proxy_list)) != null) {
            this.connect_action(string2, intent2, true);
            return true;
        }
        this.gen_event(1, "PROXY_CONTEXT_EXPIRED", null);
        return false;
    }

    private void unregister_connectivity_receiver() {
        this.unregisterReceiver((BroadcastReceiver)this.mConnectivityReceiver);
    }

    private void update_notification_event(EventMsg eventMsg) {
        if (this.mNotifyBuilder == null) return;
        if (eventMsg.priority < 1) return;
        switch (eventMsg.icon_res_id) {
            default: {
                this.mNotifyBuilder.setSmallIcon(R.drawable.icon);
                break;
            }
            case R.drawable.connecting: {
                this.mNotifyBuilder.setSmallIcon(R.drawable.openvpn_connecting);
                break;
            }
            case R.drawable.connected: {
                this.mNotifyBuilder.setSmallIcon(R.drawable.openvpn_connected);
                break;
            }
            case R.drawable.error: {
                this.mNotifyBuilder.setSmallIcon(R.drawable.openvpn_disconnected);
            }
        }
        this.mNotifyBuilder.setContentText((CharSequence)this.resString(eventMsg.res_id));
        this.startForeground(NOTIFICATION_ID, this.mNotifyBuilder.getNotification());
    }

    public void client_attach(EventReceiver eventReceiver) {
        this.clients.remove(eventReceiver);
        this.clients.addFirst(eventReceiver);
        Object[] arrobject = new Object[]{this.clients.size()};
        Log.d(OpenVPNService.TAG, (String)String.format("SERV: client attach n_clients=%d", arrobject));
    }

    public void client_detach(EventReceiver eventReceiver) {
        this.clients.remove(eventReceiver);
        Object[] arrobject = new Object[]{this.clients.size()};
        Log.d(OpenVPNService.TAG, (String)String.format("SERV: client detach n_clients=%d", arrobject));
    }

    @Override
    public void done(ClientAPI_Status clientAPI_Status) {
        boolean bl = clientAPI_Status.getError();
        String string2 = clientAPI_Status.getMessage();
        Object[] arrobject = new Object[]{bl, string2};
        Log.d(OpenVPNService.TAG, (String)String.format("EXIT: connect() exited, err=%b, msg='%s'", arrobject));
        this.log_stats();
        if (bl) {
            if (string2 != null && string2.equals("CORE_THREAD_ABANDONED")) {
                this.gen_event(1, "CORE_THREAD_ABANDONED", null);
            } else {
                String string3 = clientAPI_Status.getStatus();
                if (string3.length() == 0) {
                    string3 = "CORE_THREAD_ERROR";
                }
                this.gen_event(1, string3, string2);
            }
        }
        this.gen_event(0, "CORE_THREAD_INACTIVE", null);
        this.active = false;
    }

    @Override
    public void event(ClientAPI_Event clientAPI_Event) {
	Log.d(TAG,"in event");
	
        EventMsg eventMsg = new EventMsg();
        if (clientAPI_Event.getError()) {
            eventMsg.flags = 1 | eventMsg.flags;
        }
        eventMsg.name = clientAPI_Event.getName();
        eventMsg.info = clientAPI_Event.getInfo();
        EventInfo eventInfo = (EventInfo)this.event_info.get(eventMsg.name);
        if (eventInfo != null) {
	    /// copy value from eventInfo to eventMsg ....
            eventMsg.progress = eventInfo.progress;
            eventMsg.priority = eventInfo.priority;
            eventMsg.res_id = eventInfo.res_id;
            eventMsg.icon_res_id = eventInfo.icon_res_id;
            eventMsg.flags |= eventInfo.flags;
            if (eventInfo.res_id == R.string.connected && this.mThread != null) {
                eventMsg.conn_info = this.mThread.connection_info();
            }
        } else {
            eventMsg.res_id = R.string.unknown;
        }
        Message message = this.mHandler.obtainMessage(1, (Object)eventMsg);
        this.mHandler.sendMessage(message);
    }

    @Override
    public void external_pki_cert_request(ClientAPI_ExternalPKICertRequest req) {
        try {
            X509Certificate[] chain = KeyChain.getCertificateChain(this, req.getAlias());
            if (chain == null) {
                req.setError(true);
                req.setInvalidAlias(true);
            } else if (chain.length >= MSG_EVENT) {
                req.setCert(cert_format_pem(chain[GCI_REQ_ESTABLISH]));
                if (chain.length >= MSG_LOG) {
                    StringBuilder builder = new StringBuilder();
                    for (int i = MSG_EVENT; i < chain.length; i += MSG_EVENT) {
                        builder.append(cert_format_pem(chain[i]));
                    }
                    req.setSupportingChain(builder.toString());
                }
            } else {
                req.setError(true);
                req.setInvalidAlias(true);
                req.setErrorText(resString(R.string.epki_missing_cert));
            }
        } catch (Exception e) {
            Log.e(TAG, "EPKI error in external_pki_cert_request", e);
            req.setError(true);
            req.setInvalidAlias(true);
            req.setErrorText(e.toString());
        }
    }

    /*
     * Exception decompiling
     */
    @Override
    public void external_pki_sign_request(ClientAPI_ExternalPKISignRequest req) {
        try {
            String sig_type = req.getSigType();
            String errfmt = "EPKI error in external_pki_sign_request: %s";
            byte[] data_bytes = Base64.decode(req.getData(), GCI_REQ_ESTABLISH);
            byte[] sig_bytes = null;
            PrivateKey pk;
            Object[] objArr;
            String err;
            String str;
            if (this.jellyBeanHack == null) {
                Log.d(TAG, "EPKI: normal mode");
                pk = KeyChain.getPrivateKey(this, req.getAlias());
                if (pk == null) {
                    req.setError(true);
                    req.setInvalidAlias(true);
                } else if (sig_type.equals("RSA_RAW")) {
                    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
                    cipher.init(MSG_EVENT, pk);
                    sig_bytes = cipher.doFinal(data_bytes);
                } else {
                    objArr = new Object[MSG_EVENT];
                    objArr[GCI_REQ_ESTABLISH] = sig_type;
                    err = String.format("unknown signature type: %s", objArr);
                    str = TAG;
                    objArr = new Object[MSG_EVENT];
                    objArr[GCI_REQ_ESTABLISH] = err;
                    Log.e(str, String.format(errfmt, objArr));
                    req.setError(true);
                    req.setInvalidAlias(true);
                    req.setErrorText(err);
                    return;
                }
            }
            Log.d(TAG, "EPKI: Jelly bean mode");
            if (!this.jellyBeanHack.enabled()) {
                err = "Android OpenSSL not accessible";
                str = TAG;
                objArr = new Object[MSG_EVENT];
                objArr[GCI_REQ_ESTABLISH] = err;
                Log.e(str, String.format(errfmt, objArr));
                req.setError(true);
                req.setInvalidAlias(true);
                req.setErrorText(err);
                return;
            } else if (sig_type.equals("RSA_RAW")) {
                pk = this.jellyBeanHack.getPrivateKey(this, req.getAlias());
                if (pk != null) {
                    sig_bytes = this.jellyBeanHack.rsaSign(pk, data_bytes);
                } else {
                    req.setError(true);
                    req.setInvalidAlias(true);
                }
            } else {
                objArr = new Object[MSG_EVENT];
                objArr[GCI_REQ_ESTABLISH] = sig_type;
                err = String.format("unknown signature type: %s (Jelly Bean)", objArr);
                str = TAG;
                objArr = new Object[MSG_EVENT];
                objArr[GCI_REQ_ESTABLISH] = err;
                Log.e(str, String.format(errfmt, objArr));
                req.setError(true);
                req.setInvalidAlias(true);
                req.setErrorText(err);
                return;
            }
            if (sig_bytes != null) {
                req.setSig(Base64.encodeToString(sig_bytes, MSG_LOG));
            }
        } catch (Exception e) {
            Log.e(TAG, "EPKI error in external_pki_sign_request", e);
            req.setError(true);
            req.setInvalidAlias(true);
            req.setErrorText(e.toString());
        }
    }

    public void gen_proxy_context_expired_event() {
        this.gen_event(0, "PROXY_CONTEXT_EXPIRED", null);
    }

    public void gen_ui_reset_event(boolean bl, EventReceiver eventReceiver) {
        int n = 0;
        if (bl) {
            n = 0 | 16;
        }
        this.gen_event(n, "UI_RESET", null, null, eventReceiver);
    }

    public ConnectionStats get_connection_stats() {
        ConnectionStats connectionStats = new ConnectionStats();
        ClientAPI_TransportStats clientAPI_TransportStats = this.mThread.transport_stats();
        connectionStats.last_packet_received = -1;
        if (!this.active) {
            connectionStats.duration = 0;
            connectionStats.bytes_in = 0;
            connectionStats.bytes_out = 0;
            return connectionStats;
        }
        connectionStats.duration = (int)(SystemClock.elapsedRealtime() - this.thread_started) / 1000;
        if (connectionStats.duration < 0) {
            connectionStats.duration = 0;
        }
        connectionStats.bytes_in = clientAPI_TransportStats.getBytesIn();
        connectionStats.bytes_out = clientAPI_TransportStats.getBytesOut();
        int n = clientAPI_TransportStats.getLastPacketReceived();
        if (n < 0) return connectionStats;
        connectionStats.last_packet_received = n >> 10;
        return connectionStats;
    }

    public Profile get_current_profile() {
        if (this.current_profile != null) {
            return this.current_profile;
        }
        ProfileList profileList = this.get_profile_list();
        if (profileList.size() < 1) return null;
        return (Profile)profileList.get(0);
    }

    public EventMsg get_last_event() {
        if (this.last_event == null) return null;
        if (this.last_event.is_expired()) return null;
        return this.last_event;
    }

    public EventMsg get_last_event_prof_manage() {
        if (this.last_event_prof_manage == null) return null;
        if (this.last_event_prof_manage.is_expired()) return null;
        return this.last_event_prof_manage;
    }

    public ProfileList get_profile_list() {
        if (this.profile_list != null) return this.profile_list;
        this.refresh_profile_list();
        return this.profile_list;
    }

    public long get_tunnel_bytes_per_cpu_second() {
        if (this.cpu_usage == null) return 0;
        double d = this.cpu_usage.usage();
        if (d <= 0.0) return 0;
        ClientAPI_InterfaceStats clientAPI_InterfaceStats = this.mThread.tun_stats();
        return (long)((double)(clientAPI_InterfaceStats.getBytesIn() + clientAPI_InterfaceStats.getBytesOut()) / d);
    }
    
    // 处理 Handler.sendMessage 发过来的消息
    public boolean handleMessage(Message message) {

	Log.i(TAG,"in handleMessage");
        EventMsg eventMsg = this.get_last_event();
        switch (message.what) {
            default: {
                Log.d(OpenVPNService.TAG, (String)"SERV: unhandled message");
                return true;
            }
            case 1: {
                EventMsg eventMsg2 = (EventMsg)message.obj;
                switch (eventMsg2.res_id) {
                    case R.string.disconnected: {
                        if (eventMsg == null) break;
                        if ((1 & eventMsg.flags) != 0) {
                            eventMsg2.priority = 0;
                        }
                        if (this.current_profile == null || eventMsg.res_id == R.string.proxy_need_creds || eventMsg.res_id == R.string.dynamic_challenge) break;
                        this.current_profile.reset_proxy_context();
                        break;
                    }
                    case R.string.connected: {
                        if (this.current_profile == null) break;
                        this.current_profile.reset_proxy_context();
                        break;
                    }
                    case R.string.proxy_need_creds: {
                        ProxyContext proxyContext;
                        if (this.current_profile == null || (proxyContext = this.current_profile.get_proxy_context(false)) == null || !proxyContext.should_launch_creds_dialog()) break;
                        proxyContext.invalidate_proxy_creds(this.proxy_list);
                        Intent intent = new Intent(this.getBaseContext(), (Class)OpenVPNProxyCreds.class).addFlags(268435456);
                        proxyContext.configure_creds_dialog_intent(intent);
                        this.getApplication().startActivity(intent);
                        break;
                    }
                    case R.string.dynamic_challenge: {
                        ClientAPI_DynamicChallenge clientAPI_DynamicChallenge;
                        if (this.current_profile == null || !ClientAPI_OpenVPNClient.parse_dynamic_challenge(eventMsg2.info, clientAPI_DynamicChallenge = new ClientAPI_DynamicChallenge())) break;
                        DynamicChallenge dynamicChallenge = new DynamicChallenge();
                        dynamicChallenge.expires = 60000 + SystemClock.elapsedRealtime();
                        dynamicChallenge.cookie = eventMsg2.info;
                        dynamicChallenge.challenge.challenge = clientAPI_DynamicChallenge.getChallenge();
                        dynamicChallenge.challenge.echo = clientAPI_DynamicChallenge.getEcho();
                        dynamicChallenge.challenge.response_required = clientAPI_DynamicChallenge.getResponseRequired();
                        this.current_profile.dynamic_challenge = dynamicChallenge;
                        eventMsg2.info = "";
                        break;
                    }
                    case R.string.auth_failed: {
			Log.i(TAG,"R.string.auth_failed");
                        if (this.current_profile == null) break;
                        String string2 = this.current_profile.get_name();
                        this.pwds.remove("auth", string2);
                        break;
                    }
                    case R.string.pem_password_fail: {
                        eventMsg2.info = "";
                        if (this.current_profile == null) break;
                        String string3 = this.current_profile.get_name();
                        this.pwds.remove("pk", string3);
                        break;
                    }
                    case R.string.core_thread_inactive: {
			Log.i(TAG,"in case R.string.core_thread_inactive");
                        if (this.cpu_usage != null) {
                            this.cpu_usage.stop();
                        }
                        this.stop_notification();
                        if (this.shutdown_pending) break;
                        this.set_autostart_profile_name(null);
                    }
                }
		
                if (eventMsg2.res_id == R.string.epki_invalid_alias && this.profile_list != null) {
                    this.profile_list.invalidate_epki_alias(eventMsg2.info);
                }
                if (this.enable_notifications) {
                    if (eventMsg2.priority == 2) {
                        Toast.makeText((Context)this, (int)eventMsg2.res_id, (int)0).show();
                    } else if (eventMsg2.priority == 3) {
                        Toast.makeText((Context)this, (int)eventMsg2.res_id, (int)1).show();
                    }
                }
                if (eventMsg2.res_id == R.string.connected && (eventMsg == null || eventMsg.res_id != R.string.connected)) {
                    eventMsg2.transition = EventMsg.Transition.TO_CONNECTED;
                } else if (eventMsg2.res_id != R.string.connected && eventMsg != null && eventMsg.res_id == R.string.connected) {
                    eventMsg2.transition = EventMsg.Transition.TO_DISCONNECTED;
                }
                if ((4 & eventMsg2.flags) != 0) {
                    this.last_event_prof_manage = eventMsg2;
                } else if (eventMsg2.priority >= 2) {
                    this.last_event = eventMsg2;
                }
		
                int n = eventMsg2.res_id;
                String string4 = null;
		string4 = eventMsg2.toString();
                if (n != R.string.ui_reset && string4 != null) {
                    Log.i(TAG, (String)string4);
                }
                if (eventMsg2.res_id == R.string.core_thread_active) {
		    Log.i(TAG,"------ OpenVPN Start -------");
                    this.log_message("----- OpenVPN Start -----");
                }
                if (string4 != null) {
                    this.log_message(string4);
                }
                if (eventMsg2.res_id == R.string.core_thread_inactive) {
                    Object[] arrobject = new Object[]{this.get_tunnel_bytes_per_cpu_second()};
                    this.log_message(String.format("Tunnel bytes per CPU second: %d", arrobject));
                    this.log_message("----- OpenVPN Stop -----");
		    Log.i(TAG,"----- OpenVPN Stop -----");
                }
		Log.i(TAG,String.format("EventMsg flag: %d",eventMsg2.flags));
		
		      
                this.update_notification_event(eventMsg2);// for mNotifyBuilder set resource 
                Iterator<EventReceiver> iterator = this.clients.iterator(); //clients => ArrayDeque
                while (iterator.hasNext()) {
                    EventReceiver eventReceiver = iterator.next();
                    if ((16 & eventMsg2.flags) != 0 && eventReceiver == eventMsg2.sender) continue;
                    eventReceiver.event(eventMsg2);// to the class of implements Eventreceiver, which in this case is OpenVPNClientBase, and also the child ,IndexClient.event();
                }
                return true;
            }
	case 2:break; 
        }
        LogMsg logMsg = (LogMsg)message.obj;
        Object[] arrobject = new Object[]{logMsg.line};
        Log.i(OpenVPNService.TAG, (String)String.format("LOG: %s", arrobject));
        this.log_message(logMsg);
        return true;
    }

    public boolean is_active() {
        return this.active;
    }

    public void jellyBeanHackPurge() {
        if (this.jellyBeanHack == null) return;
        this.jellyBeanHack.resetPrivateKey();
    }

    @Override
    public void log(ClientAPI_LogInfo clientAPI_LogInfo) {
        LogMsg logMsg = new LogMsg();
        logMsg.line = clientAPI_LogInfo.getText();
        Message message = this.mHandler.obtainMessage(2, (Object)logMsg);
        this.mHandler.sendMessage(message);
    }

    public ArrayDeque<LogMsg> log_history() {
        return this.log_deque;
    }

    public MergedProfile merge_parse_profile(String string2, String string3) {
        if (string2 == null) return null;
        if (string3 == null) return null;
        ClientAPI_MergeConfig clientAPI_MergeConfig = ClientAPI_OpenVPNClient.merge_config_string_static(string3);
        String string4 = "PROFILE_" + clientAPI_MergeConfig.getStatus();
        if (string4.equals("PROFILE_MERGE_SUCCESS")) {
            String string5 = clientAPI_MergeConfig.getProfileContent();
            ClientAPI_Config clientAPI_Config = new ClientAPI_Config();
            clientAPI_Config.setContent(string5);
            MergedProfile mergedProfile = new MergedProfile("imported", string2, false, ClientAPI_OpenVPNClient.eval_config_static(clientAPI_Config));
            mergedProfile.profile_content = string5;
            return mergedProfile;
        }
        ClientAPI_EvalConfig clientAPI_EvalConfig = new ClientAPI_EvalConfig();
        EventInfo eventInfo = (EventInfo)this.event_info.get(string4);
        if (eventInfo != null) {
            string4 = this.resString(eventInfo.res_id);
        }
        clientAPI_EvalConfig.setError(true);
        clientAPI_EvalConfig.setMessage(string4 + " : " + clientAPI_MergeConfig.getErrorText());
        return new MergedProfile("imported", string2, false, clientAPI_EvalConfig);
    }

    public void network_pause() {
        if (!this.active) return;
        this.mThread.pause("");
    }

    public void network_reconnect(int n) {
        if (!this.active) return;
        this.mThread.reconnect(n);
    }

    public void network_resume() {
        if (!this.active) return;
        this.mThread.resume();
    }

    public IBinder onBind(Intent intent) {
        if (intent != null && intent.getAction().equals("net.openvpn.openvpn.BIND")) {
            Log.d((String)"OpenVPNService", (String)String.format("SERV: onBind intent=%s", new Object[]{intent}));
            return this.mBinder;
        }
        Log.d((String)"OpenVPNService", (String)String.format("SERV: onBind SUPER intent=%s", new Object[]{intent}));
        return super.onBind(intent);
    }

    public void onCreate() {
        super.onCreate();
        Log.d((String)"OpenVPNService", (String)"SERV: Service onCreate called");
        this.crypto_self_test();
        this.mHandler = new Handler((Handler.Callback)this);
        this.populate_event_info_map();
        this.register_connectivity_receiver();
        this.prefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences((Context)this));
        this.pwds = new PasswordUtil(PreferenceManager.getDefaultSharedPreferences((Context)this));
        this.jellyBeanHack = JellyBeanHack.newJellyBeanHack();
        this.proxy_list = new ProxyList(this.resString(R.string.proxy_none));
        this.proxy_list.set_backing_file((Context)this, "proxies.json");
        this.proxy_list.load();
    }

    public void onDestroy() {
        Log.d((String)"OpenVPNService", (String)"SERV: onDestroy called");
        this.shutdown_pending = true;
        this.stop_thread();
        this.unregister_connectivity_receiver();
        super.onDestroy();
    }

    public void onRevoke() {
        Log.d((String)"OpenVPNService", (String)"SERV: onRevoke called");
        this.stop_thread();
    }

    public int onStartCommand(Intent intent, int n, int n2) {
        if (intent == null) return 1;
        String string2 = intent.getAction();
        Log.d((String)"OpenVPNService", (String)String.format("SERV: onStartCommand action=%s", string2));
        if (string2.equals("net.openvpn.openvpn.CONNECT")) {
            this.connect_action("net.openvpn.openvpn", intent, false);
            return 1;
        }
        if (string2.equals("net.openvpn.openvpn.ACTION_SUBMIT_PROXY_CREDS")) {
            this.submit_proxy_creds_action("net.openvpn.openvpn", intent);
            return 1;
        }
        if (string2.equals("net.openvpn.openvpn.DISCONNECT")) {
            this.disconnect_action("net.openvpn.openvpn", intent);
            return 1;
        }
        if (string2.equals("net.openvpn.openvpn.IMPORT_PROFILE")) {
            this.import_profile_action("net.openvpn.openvpn", intent);
            return 1;
        }
        if (string2.equals("net.openvpn.openvpn.ACTION_IMPORT_PROFILE_VIA_PATH")) {
            this.import_profile_via_path_action("net.openvpn.openvpn", intent);
            return 1;
        }
        if (string2.equals("net.openvpn.openvpn.DELETE_PROFILE")) {
            this.delete_profile_action("net.openvpn.openvpn", intent);
            return 1;
        }
        if (!string2.equals("net.openvpn.openvpn.RENAME_PROFILE")) return 1;
        this.rename_profile_action("net.openvpn.openvpn", intent);
        return 1;
    }

    public boolean onUnbind(Intent intent) {
        Object[] arrobject = new Object[]{intent.toString()};
        Log.d(OpenVPNService.TAG, (String)String.format("SERV: onUnbind called intent=%s", arrobject));
        return super.onUnbind(intent);
    }

    @Override
    public boolean pause_on_connection_timeout() {
        ConnectivityReceiver connectivityReceiver = this.mConnectivityReceiver;
        boolean bl = false;
        if (connectivityReceiver != null) {
            boolean bl2 = this.mConnectivityReceiver.screen_on_defined;
            bl = false;
            if (bl2) {
                boolean bl3 = this.mConnectivityReceiver.screen_on;
                bl = false;
                if (!bl3) {
                    bl = true;
                }
            }
        }
        Object[] arrobject = new Object[]{bl};
        Log.d((String)"OpenVPNService", (String)String.format("pause_on_connection_timeout %b", arrobject));
        return bl;
    }

    private String get_in_share_preference(String key){
	SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return sharedPreferences.getString(key,"");	
    }

    
    public String read_profile_dat(String location,String dat_name,int  offset) throws IOException{
	if( location.equals("virtual")){
	    return FileUtil.readProfileDat((Context)this,dat_name, offset);
	}
	if(location.equals("internal")){
	    return FileUtil.readInternalProfileDat((Context)this,dat_name,offset);
	}
	return "";
    }
    
    public String read_file(String string2, String string3) throws IOException {
        if (string2.equals("bundled")) {
            return FileUtil.readAsset((Context)this, string3);
        }
	if (string2.equals("virtual")){
	    /// here  FileUtil.readAsset(this,PROFILE_DAT);
	    /// then uncompress then
	    return get_in_share_preference(string3);
	}
	
        if (!string2.equals("imported")) throw new InternalError();
        return FileUtil.readFileAppPrivate((Context)this, string3);
    }
    
    public String[] internal_file_list() {
	String dst = this.getApplicationContext().getFilesDir()+"/";
	try{
	    File f = new File(dst);
	    File[] ff = f.listFiles();
	    String[] result = new String[ff.length];
	    
	    for(int i=0;i<ff.length;i++){
		result[i] = ff[i].getAbsolutePath();
	    }
	    return result;
	}catch(Exception e){
	    e.printStackTrace();
	    return null;
	}
    }
    
    public void refresh_profile_list() {
        ProfileList profileList = new ProfileList();
        profileList.load_profiles("bundled"); // 就是assets 里的
        //profileList.load_profiles("imported");// 导出 ovpn 列表,不管是从哪儿来的
	
	profileList.load_profiles("virtual");// Now as profie.dat

	profileList.load_profiles("internal");// from internal storage,profile.dat

	//	profileList.load_profiles("internalV2");// from interna storage,but *.ovpn
	
        profileList.sort();
        Log.d((String)"OpenVPNService", (String)"SERV: refresh profiles:");
        Iterator iterator = profileList.iterator();
        do {
            if (!iterator.hasNext()) {
                this.profile_list = profileList;
                return;
            }
            Profile profile = (Profile)iterator.next();
            Object[] arrobject = new Object[]{profile.toString()};
            Log.d((String)"OpenVPNService", (String)String.format("SERV: %s", arrobject));
        } while (true);
    }

    public void set_autostart_profile_name(String string2) {
        if (string2 != null) {
            this.prefs.set_string("autostart_profile_name", string2);
            return;
        }
        this.prefs.delete_key("autostart_profile_name");
    }

    @Override
    public boolean socket_protect(int n) {
        boolean bl = this.protect(n);
        Object[] arrobject = new Object[]{n, bl};
        Log.d((String)"OpenVPNService", (String)String.format("SOCKET PROTECT: fd=%d protected status=%b", arrobject));
        return bl;
    }

    public ClientAPI_LLVector stat_values_full() {
        if (this.mThread == null) return null;
        return this.mThread.stats_bundle();
    }

    @Override
    public OpenVPNClientThread.TunBuilder tun_builder_new() {
        return new TunBuilder();
    }

    public static class Challenge {
        private String challenge;
        private boolean echo;
        private boolean response_required;

        public String get_challenge() {
            return this.challenge;
        }

        public boolean get_echo() {
            return this.echo;
        }

        public boolean get_response_required() {
            return this.response_required;
        }

        public String toString() {
            Object[] arrobject = new Object[]{this.challenge, this.echo, this.response_required};
            return String.format("%s/%b/%b", arrobject);
        }
    }

    public static class ConnectionStats {
        public long bytes_in;
        public long bytes_out;
        public int duration;
        public int last_packet_received;
    }

    private class ConnectivityReceiver
    extends BroadcastReceiver {
        private final int ANTI_FLAP_PERIOD;
        public boolean conn_on;
        public boolean conn_on_defined;
        private boolean initialized;
        private long last_action_time;
        private boolean last_ok;
        public boolean screen_on;
        public boolean screen_on_defined;

        private ConnectivityReceiver() {
            this.ANTI_FLAP_PERIOD = 10000;
            this.screen_on_defined = false;
            this.screen_on = false;
            this.conn_on_defined = false;
            this.conn_on = false;
            this.last_ok = true;
            this.initialized = false;
        }

        private long time_since_last_action() {
            return SystemClock.elapsedRealtime() - this.last_action_time;
        }

        private void update_last_action_time() {
            this.last_action_time = SystemClock.elapsedRealtime();
        }

        public void onReceive(Context context, Intent intent) {
            boolean bl;
            boolean bl2 = false;
            boolean bl3 = false;
            String string2 = intent.getAction();
            boolean bl4 = OpenVPNService.this.prefs.get_boolean("pause_vpn_on_blanked_screen", false);
            if ("android.intent.action.SCREEN_ON".equals(string2)) {
                Object[] arrobject = new Object[]{bl4};
                Log.i(OpenVPNService.TAG, (String)String.format("ConnectivityReceiver: SCREEN_ON pvbs=%b", arrobject));
                this.screen_on = true;
                this.screen_on_defined = bl = true;
            } else if ("android.intent.action.SCREEN_OFF".equals(string2)) {
		/// put some anonymous hack
                Object[] arrobject = new Object[]{bl4};
                Log.i(OpenVPNService.TAG, (String)String.format("ConnectivityReceiver: SCREEN_OFF pvbs=%b", arrobject));
                this.screen_on = false;
                this.screen_on_defined = bl = true;
                bl2 = false;
                bl3 = false;
            } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(string2)) {
                boolean bl5 = !intent.getBooleanExtra("noConnectivity", false);
                this.conn_on = bl5;
                bl3 = intent.getBooleanExtra("isFailover", false);
                this.conn_on_defined = bl2 = true;
                Object[] arrobject = new Object[]{this.conn_on, bl3};
                Log.i(OpenVPNService.TAG, (String)String.format("ConnectivityReceiver: CONNECTIVITY_ACTION conn=%b fo=%b", arrobject));
                bl = false;
            } else {
                Object[] arrobject = new Object[]{intent.toString()};
                Log.i(OpenVPNService.TAG, (String)String.format("ConnectivityReceiver: UNKNOWN INTENT: %s", arrobject));
                bl2 = false;
                bl3 = false;
                bl = false;
            }
            if (!bl) {
                if (!bl2) return;
            }
            boolean bl6 = !(bl4 && this.screen_on_defined && !this.screen_on || this.conn_on_defined && !this.conn_on);
            if (OpenVPNService.this.active) {
                if (bl6 != this.last_ok) {
                    if (bl6) {
                        Log.i(OpenVPNService.TAG, (String)"ConnectivityReceiver: triggering VPN resume");
                        OpenVPNService.this.network_resume();
                        this.last_ok = bl6;
                    } else {
                        Log.i(OpenVPNService.TAG, (String)"ConnectivityReceiver: triggering VPN pause");
                        OpenVPNService.this.network_pause();
                        this.last_ok = bl6;
                    }
                } else if (bl && this.screen_on && bl6 && !bl4) {
                    Log.i(OpenVPNService.TAG, (String)"ConnectivityReceiver: triggering special VPN resume");
                    OpenVPNService.this.network_resume();
                } else if (bl2 && bl3 && bl6 && this.initialized && this.time_since_last_action() > 10000) {
                    Log.i(OpenVPNService.TAG, (String)"ConnectivityReceiver: triggering VPN reconnect");
                    OpenVPNService.this.network_reconnect(2);
                }
            }
            if (!bl2) return;
            this.initialized = true;
            this.update_last_action_time();
        }
    }

    private static class DynamicChallenge {
        public Challenge challenge = new Challenge();
        public String cookie;
        public long expires;

        private DynamicChallenge() {
        }

        public long expire_delay() {
            return this.expires - SystemClock.elapsedRealtime();
        }

        public boolean is_expired() {
            if (SystemClock.elapsedRealtime() <= this.expires) return false;
            return true;
        }

        public String toString() {
            Object[] arrobject = new Object[]{this.challenge.toString(), this.cookie, this.expires};
            return String.format("%s/%s/%s", arrobject);
        }
    }

    private static class EventInfo {
        public int flags;
        public int icon_res_id;
        public int priority;
        public int progress;
        public int res_id;

        public EventInfo(int n, int n2, int n3, int n4, int n5) {
            this.res_id = n;
            this.icon_res_id = n2;
            this.progress = n3;
            this.priority = n4;
            this.flags = n5;
        }
    }

    public static class EventMsg {
        public static final int F_ERROR = 1;
        public static final int F_EXCLUDE_SELF = 16;
        public static final int F_FROM_JAVA = 2;
        public static final int F_PROF_IMPORT = 32;
        public static final int F_PROF_MANAGE = 4;
        public static final int F_UI_RESET = 8;
        public ClientAPI_ConnectionInfo conn_info;
        public long expires = 0;
        public int flags = 0;
        public int icon_res_id = -1;
        public String info;
        public String name;
        public int priority = 1;
        public String profile_override;
        public int progress = 0;
        public int res_id = -1;
        public EventReceiver sender;
        public Transition transition = Transition.NO_CHANGE;

        public static EventMsg disconnected() {
            EventMsg eventMsg = new EventMsg();
            eventMsg.flags = 2;
            eventMsg.res_id = R.string.disconnected;
            eventMsg.icon_res_id = R.drawable.disconnected;
            eventMsg.name = "DISCONNECTED";
            eventMsg.info = "";
            return eventMsg;
        }

        public boolean is_expired() {
            if (this.expires == 0) {
                return false;
            }
            if (SystemClock.elapsedRealtime() <= this.expires) return false;
            return true;
        }

        public boolean is_reflected(EventReceiver eventReceiver) {
            boolean bl = true;
            if (this.sender == null) {
                return false;
            }
            if ((16 & this.flags) != 0) return bl;
            if (this.sender != eventReceiver) return bl;
            return false;
        }

        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            Object[] arrobject = new Object[]{this.name};
            stringBuffer.append(String.format("EVENT: %s", arrobject));
            if (this.info.length() > 0) {
                Object[] arrobject2 = new Object[]{this.info};
                stringBuffer.append(String.format(" info='%s'", arrobject2));
            }
            if (this.transition == Transition.NO_CHANGE) return stringBuffer.toString();
            Object[] arrobject3 = new Object[]{this.transition};
            stringBuffer.append(String.format(" trans=%s", arrobject3));
            return stringBuffer.toString();
        }

        public String toStringFull() {
            Object[] arrobject = new Object[]{this.name, this.info, this.transition, this.flags, this.progress, this.priority, this.res_id};
            return String.format("EVENT: name=%s info='%s' trans=%s flags=%d progress=%d prio=%d res=%d", arrobject);
        }

		public enum Transition{
			NO_CHANGE,
			TO_CONNECTED,
			TO_DISCONNECTED
		}

    }

    public static interface EventReceiver {
        public void event(EventMsg var1);

        public PendingIntent get_configure_intent(int var1);

        public void log(LogMsg var1);
    }

    public static class InternalError
    extends RuntimeException {
    }

    public class LocalBinder
    extends Binder {
        OpenVPNService getService() {
            return OpenVPNService.this;
        }
    }

    public static class LogMsg {
        String line;
    }

    public class MergedProfile
    extends Profile {
        public String profile_content;

        private MergedProfile(String string2, String string3, boolean bl, ClientAPI_EvalConfig clientAPI_EvalConfig) {
            super(string2, string3, bl, clientAPI_EvalConfig);
        }
    }

    /// this Class Profile, does not store the content of .ovpn !!
    /// 
    public class Profile {
        private boolean allow_password_save;
        private boolean autologin;
        private DynamicChallenge dynamic_challenge;
        private String errorText;
        private boolean external_pki;
        private String external_pki_alias;
        public String location;
        private String name;
        public String orig_filename;
        private boolean private_key_password_required;
        private ProxyContext proxy_context;
        private ServerList server_list;
        private Challenge static_challenge;
        private String userlocked_username;

	//profile.dat



	
        private Profile(String string2, String string3, boolean bl, ClientAPI_EvalConfig clientAPI_EvalConfig) {
            this.location = string2;
            this.orig_filename = string3;//maybe cjk filename
            if (bl) {// filename_is_url_encoded_profile_name
                this.name = string3;//filename 
                if (ProfileFN.has_ovpn_ext(this.name)) {
                    this.name = ProfileFN.strip_ovpn_ext(this.name);
                }
		
                try {
                    this.name = URLDecoder.decode(this.name, "UTF-8");
                }
                catch (UnsupportedEncodingException var36_6) {
                    Log.e((String)"OpenVPNService", (String)"UnsupportedEncodingException when decoding profile filename", (Throwable)var36_6);
                }
            } else {
                this.name = string3;
            }
            if (clientAPI_EvalConfig.getError()) {
                this.errorText = clientAPI_EvalConfig.getMessage();
                return;
            }
            this.userlocked_username = clientAPI_EvalConfig.getUserlockedUsername();
            this.autologin = clientAPI_EvalConfig.getAutologin();
            this.external_pki = clientAPI_EvalConfig.getExternalPki();
            this.private_key_password_required = clientAPI_EvalConfig.getPrivateKeyPasswordRequired();
            this.allow_password_save = clientAPI_EvalConfig.getAllowPasswordSave();
            String string4 = clientAPI_EvalConfig.getStaticChallenge();
            if (string4.length() > 0) {
                Challenge challenge = new Challenge();
                challenge.challenge = string4;
                challenge.echo = clientAPI_EvalConfig.getStaticChallengeEcho();
                challenge.response_required = true;
                this.static_challenge = challenge;
            }
	    
            if (!bl) {
		/// to became like xxxxxx [bundled:test] 
                String string5;
                String string6 = clientAPI_EvalConfig.getProfileName();
                String string7 = clientAPI_EvalConfig.getFriendlyName();
                String string8 = this.location;
                String string9 = null;
                if (string8 != null) {
                    boolean bl2 = this.location.equals("imported");
                    string9 = null;
                    if (!bl2) {
                        string9 = this.location;
                    }
                }
                String string10 = string6;
                int n = string7.length();
                boolean bl3 = false;
                if (n > 0) {
                    string10 = string7;
                    bl3 = true;
                }
                if ((string5 = string3) != null && string5.equalsIgnoreCase("client.ovpn")) {
                    string5 = null;
                }
                if (ProfileFN.has_ovpn_ext(string5) && (string5 = ProfileFN.strip_ovpn_ext(string5)) != null && string10 != null && string5.equals(string10)) {
                    string5 = null;
                }
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append(string10);
                if (this.autologin && !bl3 && string5 == null) {
                    stringBuffer.append(OpenVPNService.this.getText(R.string.autologin_suffix).toString());
                }
                if (string9 != null || string5 != null) {
                    stringBuffer.append(" [");
                    if (string9 != null) {
                        stringBuffer.append(string9);
                        stringBuffer.append(":");
                    }
                    if (string5 != null) {
                        stringBuffer.append(string5);
                    }
                    stringBuffer.append("]");
                }
                this.name = stringBuffer.toString();
            }
            this.server_list = new ServerList();
            ClientAPI_ServerEntryVector clientAPI_ServerEntryVector = clientAPI_EvalConfig.getServerList();
            int n = (int)clientAPI_ServerEntryVector.size();
            int n2 = 0;
            do {
                if (n2 >= n) {
                    this.external_pki_alias = OpenVPNService.this.prefs.get_string_by_profile(this.name, "epki_alias");
                    return;
                }
                ClientAPI_ServerEntry clientAPI_ServerEntry = clientAPI_ServerEntryVector.get(n2);
                ServerEntry serverEntry = new ServerEntry();
                serverEntry.server = clientAPI_ServerEntry.getServer();
                serverEntry.friendly_name = clientAPI_ServerEntry.getFriendlyName();
                this.server_list.list.add(serverEntry);
                ++n2;
            } while (true);
        }

        private void expire_dynamic_challenge() {
            if (this.dynamic_challenge == null) return;
            if (!this.dynamic_challenge.is_expired()) return;
            this.dynamic_challenge = null;
        }

        private boolean get_epki() {
            return this.external_pki;
        }

        private String get_epki_alias() {
            return this.external_pki_alias;
        }

        private void invalidate_epki_alias(String string2) {
            if (this.external_pki_alias == null) return;
            if (!this.external_pki_alias.equals(string2)) return;
            this.external_pki_alias = null;
            OpenVPNService.this.prefs.delete_key_by_profile(this.name, "epki_alias");
            OpenVPNService.this.jellyBeanHackPurge();
        }

        private void persist_epki_alias(String string2) {
            this.external_pki_alias = string2;
            OpenVPNService.this.prefs.set_string_by_profile(this.name, "epki_alias", string2);
            OpenVPNService.this.jellyBeanHackPurge();
        }

        public boolean challenge_defined() {
            this.expire_dynamic_challenge();
            if (this.static_challenge != null) return true;
            if (this.dynamic_challenge == null) return false;
            return true;
        }

        public void forget_cert() {
            if (this.external_pki_alias == null) return;
            this.external_pki_alias = null;
            OpenVPNService.this.prefs.delete_key_by_profile(this.name, "epki_alias");
            OpenVPNService.this.jellyBeanHackPurge();
        }

        public boolean get_allow_password_save() {
            return this.allow_password_save;
        }

        public boolean get_autologin() {
            return this.autologin;
        }

        public Challenge get_challenge() {
            this.expire_dynamic_challenge();
            if (this.dynamic_challenge == null) return this.static_challenge;
            return this.dynamic_challenge.challenge;
        }

        public long get_dynamic_challenge_expire_delay() {
            if (!this.is_dynamic_challenge()) return 0;
            return this.dynamic_challenge.expire_delay();
        }

        public String get_error() {
            return this.errorText;
        }

        public String get_filename() {
            if (this.location != null && this.location.equals("bundled")) {
                return this.orig_filename;
            }
	    if (this.location != null && this.location.equals("virtual")) {
		String str3 = ProfileFN.encode_utf8_fn(this.name);
		if(str3 != null) return str3;
                return this.orig_filename;
            }
	    
            String string2 = ProfileFN.encode_profile_fn(this.name);
            if (string2 != null) return string2;
            return this.orig_filename;
        }

        public String get_location() {
            return this.location;
        }

        public String get_name() {
            return this.name;
        }

        public boolean get_private_key_password_required() {
            return this.private_key_password_required;
        }

        public ProxyContext get_proxy_context(boolean bl) {
            if (this.proxy_context != null && !this.proxy_context.is_expired()) {
                return this.proxy_context;
            }
            if (bl) {
                this.proxy_context = new ProxyContext();
                return this.proxy_context;
            }
            this.proxy_context = null;
            return this.proxy_context;
        }

        public ServerList get_server_list() {
            return this.server_list;
        }

        public String get_type_string() {
            if (this.get_autologin()) {
                return OpenVPNService.this.getText(R.string.profile_type_autologin).toString();
            }
            if (!this.get_epki()) return OpenVPNService.this.getText(R.string.profile_type_standard).toString();
            return OpenVPNService.this.getText(R.string.profile_type_epki).toString();
        }

        public String get_userlocked_username() {
            return this.userlocked_username;
        }

        public boolean have_external_pki_alias() {
            if (!this.external_pki) return false;
            if (this.external_pki_alias == null) return false;
            return true;
        }

        public boolean is_deleteable() {
            if (this.location == null) return false;
            if (this.location.equals("bundled")) return false;
            return true;
        }

        public boolean is_dynamic_challenge() {
            this.expire_dynamic_challenge();
            if (this.dynamic_challenge == null) return false;
            return true;
        }

        public boolean is_renameable() {
            return this.is_deleteable();
        }

        public boolean need_external_pki_alias() {
            if (!this.external_pki) return false;
            if (this.external_pki_alias != null) return false;
            return true;
        }

        public void reset_dynamic_challenge() {
            this.dynamic_challenge = null;
        }

        public void reset_proxy_context() {
            this.proxy_context = null;
        }

        public boolean server_list_defined() {
            if (this.server_list.list.size() <= 0) return false;
            return true;
        }

        public String toString() {
            Object[] arrobject = new Object[9];
            arrobject[0] = this.name;
            arrobject[1] = this.orig_filename;
            arrobject[2] = this.userlocked_username;
            arrobject[3] = this.autologin;
            arrobject[4] = this.external_pki;
            arrobject[5] = this.external_pki_alias;
            arrobject[6] = this.server_list.toString();
            String string2 = this.static_challenge != null ? this.static_challenge.toString() : "null";
            arrobject[7] = string2;
            String string3 = this.dynamic_challenge != null ? this.dynamic_challenge.toString() : "null";
            arrobject[8] = string3;
            return String.format("Profile name='%s' ofn='%s' userlock=%s auto=%b epki=%b/%s sl=%s sc=%s dc=%s", arrobject);
        }

        public boolean userlocked_username_defined() {
            if (this.userlocked_username.length() <= 0) return false;
            return true;
        }
    }

    private static class ProfileFN {
        private ProfileFN() {
        }

        public static String encode_profile_fn(String string2) {
            try {
                return URLEncoder.encode(string2, "UTF-8") + ".ovpn";
            }
            catch (UnsupportedEncodingException var1_2) {
                Log.e(OpenVPNService.TAG, (String)"UnsupportedEncodingException when encoding profile filename", (Throwable)var1_2);
                return null;
            }
        }

        public static String encode_utf8_fn(String string2) {
            try {
                return URLEncoder.encode(string2, "UTF-8");
            }
            catch (UnsupportedEncodingException e) {
                Log.e(OpenVPNService.TAG, (String)"UnsupportedEncodingException when encoding profile filename", (Throwable)e);
                return null;
            }
        }

        public static boolean has_ovpn_ext(String string2) {
            boolean bl = false;
            if (string2 == null) return bl;
            if (string2.endsWith(".ovpn")) return true;
            boolean bl2 = string2.endsWith(".OVPN");
            bl = false;
            if (!bl2) return bl;
            return true;
        }

        public static boolean has_dat_ext(String string2) {
            boolean bl = false;
            if (string2 == null) return bl;
            if (string2.endsWith(".dat")) return true;
            boolean bl2 = string2.endsWith(".DAT");
            bl = false;
            if (!bl2) return bl;
            return true;
        }
	
        public static String strip_ovpn_ext(String string2) {
            if (string2 == null) return string2;
            if (!ProfileFN.has_ovpn_ext(string2)) return string2;
            return string2.substring(0, -5 + string2.length());
        }
    }

    public class ProfileList
	extends ArrayList<Profile> {
	


        private void invalidate_epki_alias(String string2) {
            Iterator iterator = this.iterator();
            while (iterator.hasNext()) {
                ((Profile)iterator.next()).invalidate_epki_alias(string2);
            }
        }
	
	//make a profile from a String buffer
	private void add_profile_from_string(String fname,String profile_content){
	    boolean filename_is_url_encoded_profile_name = true;
	    String location ="virtual";

	    
	    try {
		ClientAPI_Config config = new ClientAPI_Config();
		config.setContent(profile_content);
		ClientAPI_EvalConfig ec = ClientAPI_OpenVPNClient.eval_config_static(config);
		if (ec.getError()) {
		    Object[] objArr = new Object[OpenVPNService.MSG_LOG];
		    objArr[OpenVPNService.GCI_REQ_ESTABLISH] = fname;
		    objArr[OpenVPNService.MSG_EVENT] = ec.getMessage();
		    Log.i(OpenVPNService.TAG, String.format("PROFILE: error evaluating %s: %s", objArr));
		} else {
		    Log.i(TAG,String.format("in add_profile_from_string %s",fname));
		    add(new Profile(location, fname, filename_is_url_encoded_profile_name, ec));
		}
	    } catch (Exception e2) {
		Log.e(OpenVPNService.TAG, "PROFILE: error enumerating assets of add_profile_from_string", e2);
		return;
	    }
	    
	}

	private void save_in_share_preference(String key ,String value){
	    SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
	    SharedPreferences.Editor editor = sharedPreferences.edit();
	    editor.putString(key, value);
	    
	    //保存key-value对到文件中
	    editor.commit();
	
	}
        private void load_profiles(String location) {
            String storage_title;
            String[] fnlist;
            boolean filename_is_url_encoded_profile_name;
	    int i;
            if (location.equals("bundled")){
                storage_title = "assets";
                try {
                	fnlist = OpenVPNService.this.getResources().getAssets().list("");
                	filename_is_url_encoded_profile_name = false;
            	}catch (IOException e4){
                   	Log.e(OpenVPNService.TAG, "PROFILE: error in get assets list", e4);
            		return;
            	}
            	
            } else {
                if (location.equals("imported")) {
                    storage_title = "app private storage";
                    fnlist = OpenVPNService.this.fileList();
                    filename_is_url_encoded_profile_name = true;
                }else if (location.equals("virtual")){
		    storage_title = "app gziped assets";
		    try{
			fnlist = OpenVPNService.this.getResources().getAssets().list("");
			filename_is_url_encoded_profile_name = true;
		    }catch (IOException e){
			Log.e(OpenVPNService.TAG,"PROFILE: error in get assets list gziped",e);
			return;
		    }
		    
		    String profile_content;
		    for (i=0;i<fnlist.length;i++) {
			String file = fnlist[i];
			if (file.equals(OpenVPNService.PROFILE_DAT) ){
			    try{
				profile_content = OpenVPNService.this.read_profile_dat(location,file,OpenVPNService.PROFILE_DAT_SKIP);
			    }catch (IOException e){
				Log.e(OpenVPNService.TAG,String.format("error in read %s",file),e);
				return;
			    }
			    try{
				JSONObject data = new JSONObject(new JSONTokener(profile_content));
				JSONArray profiles = data.getJSONArray("profiles"); // json key words
				int count = profiles.length();
				
				SharedPreferences sharedPreferences = getSharedPreferences(OpenVPNService.PREFS_NAME, MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPreferences.edit();
				JSONObject profileData;
				String name;
				String content;
				
				for (i=0; i<count; i++) {
				    try {
					profileData = profiles.getJSONObject(i);
					name = profileData.getString("name");
					content = profileData.getString("content");
					add_profile_from_string(name,content);
					editor.putString(name,content);
				    } catch (JSONException e) {
					Log.e(OpenVPNService.TAG,"parse profiles json error", e);
				    }				    
				}
				editor.commit();
			    }catch (JSONException e){
				Log.e(OpenVPNService.TAG,"error in JSONTokener",e);	
			    }

			    break;/// only one profile.dat can be there
			}

		    }
		    /*
		    try{
			String profile_dat = OpenVPNService.this.getResources().getAssets()+"/"+OpenVPNService.PROFILE_DAT;
			//InputStream is = getAssets().open(OpenVPNService.PROFILE_DAT);// directly open it
			filename_is_url_encoded_profile_name = true;
			/// next ,unzip it ,read it as json, and iterated to add to the LIST
			//json format
			
		    }catch(IOException e9){
			Log.e(OpenVPNService.TAG,"PROFILE.DAT: error in get profile.dat",e9);
			return;
		    }
		    */
		    //HERE to do the unzip and parse job
		    // virtual method does not go down
		    return;
		    
		}else if (location.equals("internal")){
		    storage_title = "internal profile.dat";
		    //fnlist = OpenVPNService.this.internal_file_list();
		    fnlist = OpenVPNService.this.fileList();
		    String profile_content;
		    for(i=0;i<fnlist.length;i++){
			String fn = fnlist[i];
			Log.i(OpenVPNService.TAG,String.format("internal %s",fn));
			if(fn.endsWith(OpenVPNService.PROFILE_DAT)){
			    
			    try{
				profile_content = OpenVPNService.this.read_profile_dat(location,fn,OpenVPNService.PROFILE_DAT_SKIP);
			    }catch (IOException e){
				Log.e(OpenVPNService.TAG,String.format("error in read %s",fn),e);
				return;
			    }
			    
			    try{
				JSONObject data = new JSONObject(new JSONTokener(profile_content));
				JSONArray profiles = data.getJSONArray("profiles"); // json key words
				int count = profiles.length();
				
				SharedPreferences sharedPreferences = getSharedPreferences(OpenVPNService.PREFS_NAME, MODE_PRIVATE);
				SharedPreferences.Editor editor = sharedPreferences.edit();
				JSONObject profileData;
				String name;
				String content;
				
				for (i=0; i<count; i++) {
				    try {
					profileData = profiles.getJSONObject(i);
					name = profileData.getString("name");
					content = profileData.getString("content");
					add_profile_from_string(name,content);
					editor.putString(name,content);
				    } catch (JSONException e) {
					Log.e(OpenVPNService.TAG,"parse profiles json error", e);
				    }				    
				}
				editor.commit();
			    }catch (JSONException e){
				Log.e(OpenVPNService.TAG,"error in JSONTokener",e);	
			    }
			    
			    break;// end internal 
			}
		    }
		    return;
		}
		else if (location.equals("internalV2")){
		    storage_title = "internal .ovpn";
		    
		    return;
		}else {
		    throw new InternalError();
		}
	    }
	    /// all to get the fnlist 
            String[] arr$ = fnlist;
            int len$ = arr$.length;
            for (int i$ = OpenVPNService.GCI_REQ_ESTABLISH; i$ < len$; i$ += OpenVPNService.MSG_EVENT) {
                String fn = arr$[i$];
                if (ProfileFN.has_ovpn_ext(fn)) {
                    String profile_content;
                    String str;
                    Object[] objArr;
                    profile_content = "";
                    try {
                        profile_content = OpenVPNService.this.read_file(location, fn);
                    } catch (IOException e) {
                        str = OpenVPNService.TAG;
                        objArr = new Object[OpenVPNService.MSG_LOG];
                        objArr[OpenVPNService.GCI_REQ_ESTABLISH] = fn;
                        objArr[OpenVPNService.MSG_EVENT] = storage_title;
                        Log.i(str, String.format("PROFILE: error reading %s from %s", objArr));
                    }
                    try {
                        ClientAPI_Config config = new ClientAPI_Config();
                        config.setContent(profile_content);
                        ClientAPI_EvalConfig ec = ClientAPI_OpenVPNClient.eval_config_static(config);
                        if (ec.getError()) {
                            str = OpenVPNService.TAG;
                            objArr = new Object[OpenVPNService.MSG_LOG];
                            objArr[OpenVPNService.GCI_REQ_ESTABLISH] = fn;
                            objArr[OpenVPNService.MSG_EVENT] = ec.getMessage();
                            Log.i(str, String.format("PROFILE: error evaluating %s: %s", objArr));
                        } else {
                            add(new Profile(location, fn, filename_is_url_encoded_profile_name, ec));
                        }
                    } catch (Exception e2) {
                        Log.e(OpenVPNService.TAG, "PROFILE: error enumerating assets", e2);
                        return;
                    }
                }
            }
        }

        private void sort() {
            Collections.sort(this, new CustomComparator());
        }

        public void forget_certs() {
            OpenVPNService.this.jellyBeanHackPurge();
            Iterator iterator = this.iterator();
            while (iterator.hasNext()) {
                ((Profile)iterator.next()).forget_cert();
            }
        }

        public Profile get_profile_by_name(String string2) {
            Profile profile;
            if (string2 == null) return null;
            Iterator iterator = this.iterator();
            do {
                if (!iterator.hasNext()) return null;
            } while (!string2.equals( (profile = (Profile)iterator.next()).name)  );
            return profile;
        }

        public String[] profile_names() {
            String[] arrstring = new String[this.size()];
            int n = 0;
            while (n < this.size()) {
                arrstring[n] = ((Profile)this.get(n)).name;
                ++n;
            }
            return arrstring;
        }

        private class CustomComparator implements Comparator<Profile> {
            private CustomComparator() {
            }

            @Override
            public int compare(Profile profile, Profile profile2) {
                return profile.name.compareTo(profile2.name);
            }
        }

    }

    private static class ProxyContext {
        private boolean allow_creds_dialog;
        private Intent connect_intent;
        private long expires;
        private boolean explicit_creds;
        private int n_retries;
        private String profile_name;
        private ProxyList.Item proxy;
        private String proxy_password;
        private String proxy_username;

        private ProxyContext() {
        }

        private void reset() {
            this.profile_name = null;
            this.proxy = null;
            this.connect_intent = null;
            this.expires = 0;
            this.explicit_creds = false;
            this.proxy_username = null;
            this.proxy_password = null;
            this.allow_creds_dialog = false;
            this.n_retries = 0;
        }

        public void client_api_config(ClientAPI_Config clientAPI_Config) {
            if (this.proxy == null) return;
            clientAPI_Config.setProxyHost(this.proxy.host);
            clientAPI_Config.setProxyPort(this.proxy.port);
            if (this.proxy_username != null && this.proxy_password != null) {
                clientAPI_Config.setProxyUsername(this.proxy_username);
                clientAPI_Config.setProxyPassword(this.proxy_password);
            }
            clientAPI_Config.setProxyAllowCleartextAuth(this.proxy.allow_cleartext_auth);
        }

        public void configure_creds_dialog_intent(Intent intent) {
            if (this.proxy == null) return;
            if (this.profile_name == null) return;
            intent.putExtra("net.openvpn.openvpn.PROFILE", this.profile_name);
            intent.putExtra("net.openvpn.openvpn.PROXY_NAME", this.proxy.name());
            intent.putExtra("net.openvpn.openvpn.N_RETRIES", this.n_retries);
            intent.putExtra("net.openvpn.openvpn.EXPIRES", this.expires);
        }

        public void invalidate_proxy_creds(ProxyList proxyList) {
            if (this.proxy != null && this.proxy.invalidate_creds()) {
                proxyList.put(this.proxy);
                proxyList.save();
            }
            this.proxy_username = null;
            this.proxy_password = null;
        }

        public boolean is_expired() {
            if (this.expires == 0) {
                return false;
            }
            if (SystemClock.elapsedRealtime() <= this.expires) return false;
            return true;
        }

        public String name() {
            if (this.proxy == null) return null;
            return this.proxy.name();
        }

        public void new_connection(Intent intent, String string2, String string3, String string4, String string5, boolean bl, ProxyList proxyList, boolean bl2) {
            if (bl2) return;
            ProxyList.Item item = proxyList.get(string3);
            if (item == null) {
                this.reset();
                return;
            }
            this.proxy = item;
            this.profile_name = string2;
            this.connect_intent = intent;
            this.allow_creds_dialog = bl;
            this.n_retries = 0;
            this.expires = 120000 + SystemClock.elapsedRealtime();
            if (this.explicit_creds) return;
            if (string4 != null && string5 != null) {
                this.proxy_username = string4;
                this.proxy_password = string5;
                return;
            }
            this.proxy_username = item.username;
            this.proxy_password = item.password;
        }

        public boolean should_launch_creds_dialog() {
            if (this.proxy == null) return false;
            if (!this.allow_creds_dialog) return false;
            return true;
        }

        public Intent submit_proxy_creds(String string2, String string3, String string4, boolean bl, ProxyList proxyList) {
            if (this.proxy == null) return null;
            if (!this.proxy.name().equals(string2)) return null;
            if (string3 == null) return null;
            if (string4 == null) return null;
            this.proxy_username = string3;
            this.proxy_password = string4;
            this.explicit_creds = true;
            if (bl) {
                this.proxy.username = string3;
                this.proxy.password = string4;
                this.proxy.remember_creds = bl;
                proxyList.put(this.proxy);
                proxyList.save();
            }
            this.n_retries = 1 + this.n_retries;
            return this.connect_intent;
        }
    }

    public static class ServerEntry {
        private String friendly_name;
        private String server;

        public String display_name() {
            if (this.friendly_name.length() <= 0) return this.server;
            return this.friendly_name;
        }

        public String toString() {
            Object[] arrobject = new Object[]{this.server, this.friendly_name};
            return String.format("%s/%s", arrobject);
        }
    }

    public static class ServerList {
        private ArrayList<ServerEntry> list = new ArrayList();

        public String[] display_names() {
            int n = this.list.size();
            String[] arrstring = new String[n];
            int n2 = 0;
            while (n2 < n) {
                arrstring[n2] = this.list.get(n2).display_name();
                ++n2;
            }
            return arrstring;
        }

        public String toString() {
            StringBuffer stringBuffer = new StringBuffer();
            Iterator<ServerEntry> iterator = this.list.iterator();
            while (iterator.hasNext()) {
                ServerEntry serverEntry = iterator.next();
                stringBuffer.append(serverEntry.toString() + ",");
            }
            return stringBuffer.toString();
        }
    }

    private class TunBuilder extends VpnService.Builder  implements OpenVPNClientThread.TunBuilder {
        private TunBuilder() {
            super();
        }

        private void log_error(String string2, Exception exception) {
            Object[] arrobject = new Object[]{string2, exception.toString()};
            Log.d(OpenVPNService.TAG, (String)String.format("BUILDER_ERROR: %s %s", arrobject));
        }

        @Override
        public boolean tun_builder_add_address(String string2, int n, String string3, boolean bl, boolean bl2) {
            try {
                Object[] arrobject = new Object[]{string2, n, string3, bl, bl2};
                Log.d(OpenVPNService.TAG, (String)String.format("BUILDER: add_address %s/%d %s ipv6=%b net30=%b", arrobject));
                this.addAddress(string2, n);
                return true;
            }
            catch (Exception var6_7) {
                this.log_error("tun_builder_add_address", var6_7);
                return false;
            }
        }

        @Override
        public boolean tun_builder_add_dns_server(String string2, boolean bl) {
            try {
                Object[] arrobject = new Object[]{string2, bl};
                Log.d((String)"OpenVPNService", (String)String.format("BUILDER: add_dns_server %s ipv6=%b", arrobject));
                this.addDnsServer(string2);
                return true;
            }
            catch (Exception var3_4) {
                this.log_error("tun_builder_add_dns_server", var3_4);
                return false;
            }
        }

        @Override
        public boolean tun_builder_add_route(String string2, int n, boolean bl) {
            try {
                Object[] arrobject = new Object[]{string2, n, bl};
                Log.d((String)"OpenVPNService", (String)String.format("BUILDER: add_route %s/%d ipv6=%b", arrobject));
                this.addRoute(string2, n); // from  Android VpnService.Builder addRoute (InetAddress address, int prefixLength)

                return true;
            }
            catch (Exception var4_5) {
                this.log_error("tun_builder_add_route", var4_5);
                return false;
            }
        }

        @Override
        public boolean tun_builder_add_search_domain(String string2) {
            try {
                Log.d(OpenVPNService.TAG, (String)String.format("BUILDER: add_search_domain %s", string2));
                this.addSearchDomain(string2);
                return true;
            }
            catch (Exception var2_2) {
                this.log_error("tun_builder_add_search_domain", var2_2);
                return false;
            }
        }

        @Override
        public int tun_builder_establish() {
            try {
                Log.d(OpenVPNService.TAG, (String)"BUILDER: establish");
                PendingIntent pendingIntent = OpenVPNService.this.get_configure_intent(0);
                if (pendingIntent == null) return this.establish().detachFd();
                this.setConfigureIntent(pendingIntent);
                return this.establish().detachFd();
            }
            catch (Exception var1_3) {
                this.log_error("tun_builder_establish", var1_3);
                return -1;
            }
        }

        @Override
        public boolean tun_builder_exclude_route(String string2, int n, boolean bl) {
            try {
                Object[] arrobject = new Object[]{string2, n, bl};
                Log.d(OpenVPNService.TAG, (String)String.format("BUILDER: exclude_route %s/%d ipv6=%b (NOT IMPLEMENTED)", arrobject));
                return true;
            }
            catch (Exception var4_5) {
                this.log_error("tun_builder_exclude_route", var4_5);
                return false;
            }
        }

        @Override
        public boolean tun_builder_reroute_gw(boolean ipv4, boolean ipv6, long flags) {
            try {
                String str = OpenVPNService.TAG;
                Object[] objArr = new Object[OpenVPNService.EV_PRIO_HIGH];
                objArr[OpenVPNService.GCI_REQ_ESTABLISH] = Boolean.valueOf(ipv4);
                objArr[OpenVPNService.MSG_EVENT] = Boolean.valueOf(ipv6);
                objArr[OpenVPNService.MSG_LOG] = Long.valueOf(flags);
                Log.d(str, String.format("BUILDER: reroute_gw ipv4=%b ipv6=%b flags=%d", objArr));
                if ((1 & flags) != 0) {
                    return true;
                }
                if (ipv4) {
                    addRoute("0.0.0.0", OpenVPNService.GCI_REQ_ESTABLISH);
                }
                if (!ipv6) {
                    return true;
                }
                addRoute("::", OpenVPNService.GCI_REQ_ESTABLISH);
                return true;
            } catch (Exception e) {
                log_error("tun_builder_add_route", e);
                return false;
            }
        }

        @Override
        public boolean tun_builder_set_mtu(int n) {
            try {
                Object[] arrobject = new Object[]{n};
                Log.d((String)"OpenVPNService", (String)String.format("BUILDER: set_mtu %d", arrobject));
                this.setMtu(n);
                return true;
            }
            catch (Exception var2_3) {
                this.log_error("tun_builder_set_mtu", var2_3);
                return false;
            }
        }

        @Override
        public boolean tun_builder_set_remote_address(String string2, boolean bl) {
            try {
                Object[] arrobject = new Object[]{string2, bl};
                Log.d((String)"OpenVPNService", (String)String.format("BUILDER: set_remote_address %s ipv6=%b", arrobject));
                return true;
            }
            catch (Exception var3_4) {
                this.log_error("tun_builder_set_remote_address", var3_4);
                return false;
            }
        }

        @Override
        public boolean tun_builder_set_session_name(String string2) {
            try {
                Log.d(OpenVPNService.TAG, (String)String.format("BUILDER: set_session_name %s", string2));
                this.setSession(string2);
                return true;
            }
            catch (Exception var2_2) {
                this.log_error("tun_builder_set_session_name", var2_2);
                return false;
            }
        }

        @Override
        public void tun_builder_teardown(boolean bl) {
            try {
                Object[] arrobject = new Object[]{bl};
                Log.d(OpenVPNService.TAG, (String)String.format("BUILDER: teardown disconnect=%b", arrobject));
                return;
            }
            catch (Exception var2_3) {
                this.log_error("tun_builder_teardown", var2_3);
                return;
            }
        }
    }

}

