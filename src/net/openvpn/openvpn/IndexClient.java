package net.openvpn.openvpn;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.NetworkInterface;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.app.Dialog;
import android.app.AlertDialog;

import android.app.ProgressDialog;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.BroadcastReceiver;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.webkit.WebSettings; 
import android.webkit.WebStorage; 
import android.webkit.JavascriptInterface;
import android.widget.TextView;

import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;  
import android.view.Menu;
import android.view.MenuItem;

import android.widget.Toast;
import android.text.*;

import android.net.Uri;
import android.net.VpnService;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Build;
import android.os.AsyncTask;

import android.preference.PreferenceManager;
import android.util.Log;
import android.text.method.*;


import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import net.openvpn.openvpn.OpenVPNService.ConnectionStats;
import net.openvpn.openvpn.OpenVPNService.EventMsg;
import net.openvpn.openvpn.OpenVPNService.Profile;
import net.openvpn.openvpn.OpenVPNService.ProfileList;
import net.openvpn.openvpn.OpenVPNService.Challenge;

import net.openvpn.openvpn.OpenVPNBase;

import com.thin.downloadmanager.DefaultRetryPolicy;
import com.thin.downloadmanager.DownloadManager;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListenerV1;
import com.thin.downloadmanager.RetryPolicy;
import com.thin.downloadmanager.ThinDownloadManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

public class IndexClient extends OpenVPNClientBase
{
    private static final int REQUEST_IMPORT_PKCS12 = 3;
    private static final int REQUEST_IMPORT_PROFILE = 2;
    private static final int REQUEST_VPN_ACTOR_RIGHTS = 1;
    private static final boolean RETAIN_AUTH = false;
    private static final int S_BIND_CALLED = 1;
    private static final int S_ONSTART_CALLED = 2;
    private static final String TAG = "IndexClient";
    private static final int UIF_PROFILE_SETTING_FROM_SPINNER = 262144; //0x40000
    private static final int UIF_REFLECTED = 131072; //0x20000
    private static final int UIF_RESET = 65536;//0x10000
    private static final boolean UI_OVERLOADED = false;
    private String autostart_profile_name;

    private FinishOnConnect finish_on_connect;
    
    private boolean last_active;
    private int startup_state;
    private Handler stats_timer_handler;
    private Runnable stats_timer_task;
    private boolean stop_service_on_client_exit;
    private Handler ui_reset_timer_handler;
    private Runnable ui_reset_timer_task;
    private PrefUtil prefs;
    private PasswordUtil pwds;
    
    private WebView mWebView;

    private String w_username;
    private String w_password;
    private String w_profile;
    private boolean w_is_pk_pwd_save;

    public int download_id;
    private boolean in_download_profile;
    
    private ThinDownloadManager downloadManager;
    private RetryPolicy retryPolicy;

    public ProgressDialog progressDialog;
    
    MyDownloadDownloadStatusListenerV1  myDownloadStatusListener = new MyDownloadDownloadStatusListenerV1();
    
    private void downloadProfile(String profileUrl) {
	String dst = this.getApplicationContext().getFilesDir()+"/";
	File f = new File(dst);
	f.mkdirs();
	Uri destinationUri = Uri.parse(f+"/"+OpenVPNService.PROFILE_DAT);
	Uri downloadUri = Uri.parse(profileUrl);
	
        final DownloadRequest downloadRequest1 = new DownloadRequest(downloadUri)
	    .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
	    .setRetryPolicy(retryPolicy)
	    .setDownloadContext("Download1")
	    .setStatusListener(myDownloadStatusListener);
	
	download_id = downloadManager.add(downloadRequest1);
	in_download_profile= true;
	exec_js("OPENVPN.set_info(\"获取线路中...\");");
    }

   
    private String selected_profile_name() {
        String ret = null;
        ProfileList proflist = profile_list();
        if (proflist != null && proflist.size() > 0) {
            ret = proflist.size() == S_BIND_CALLED ? ((Profile) proflist.get(0)).get_name() : this.w_profile;
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

    
    private void resolve_epki_alias_then_connect(){
	Log.i(TAG,"In resolve_epki_alias_then_connect");
	this.do_connect("DISABLE_CLIENT_CERT");
    }
    
    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
	NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
	if (networkInfo != null && networkInfo.isConnected()) 
	    return true;
	else
	    return false;   
    }
    
    private void start_connect() {

        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            try {
                Log.d(TAG, "CLI: requesting VPN actor rights");
                startActivityForResult(intent, S_BIND_CALLED); // ->  onActivityResult
                return;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "CLI: requesting VPN actor rights failed", e);
		showToast(resString(R.string.vpn_permission_dialog_missing_title));		
		//                ok_dialog(resString(R.string.vpn_permission_dialog_missing_title), resString(R.string.vpn_permission_dialog_missing_text));
                return;
            }
        }
        Log.d(TAG, "CLI: app is already authorized as VPN actor");
        resolve_epki_alias_then_connect();
    }
    
    private void do_connect(String epki_alias){
        String app_name = "net.openvpn.connect.android";
        String proxy_name = null;
        String server = null;
        String pk_password = null;
        String response = null;
        boolean is_auth_pwd_save = true;
        String profile_name = selected_profile_name();
	
	Log.i(TAG,String.format("do_connect %s",epki_alias));

	if(this.w_username.length() > 0){
	    this.prefs.set_string_by_profile(profile_name,"username",this.w_username);
	}
	
        this.prefs.set_boolean_by_profile(profile_name, "auth_password_save", is_auth_pwd_save);
	if(is_auth_pwd_save){
	    this.pwds.set("auth", profile_name, this.w_password);
	}else{
	    this.pwds.remove("auth",profile_name);
	}
	
        String vpn_proto = this.prefs.get_string("vpn_proto");
        String conn_timeout = this.prefs.get_string("conn_timeout");
        String compression_mode = this.prefs.get_string("compression_mode");
	
	//	submitConnectIntent(profile_name,server,vpn_proto,conn_timeout,this.w_username,this.w_password,

	submitConnectIntent(profile_name, server, vpn_proto, conn_timeout, this.w_username, this.w_password, is_auth_pwd_save, pk_password, response, epki_alias, compression_mode, proxy_name, null, null, true, get_gui_version(app_name)); // from OpenVPNClientBase
	
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
    
    public IndexClient(){
	this.stop_service_on_client_exit = false;
	this.finish_on_connect = FinishOnConnect.DISABLED;
	this.in_download_profile=false;
	this.download_id = 0;
    }
    
 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	//	requestWindowFeature(Window.FEATURE_NO_TITLE);
	//getWindow().requestFeature(Window.FEATURE_PROGRESS);
	//getWindow().requestFeature(Window.FEATURE_PROGRESS);
	
        Intent intent = getIntent();
        String str = TAG;
        Object[] objArr = new Object[S_BIND_CALLED];
        objArr[0] = intent.toString();
        Log.d(str, String.format("CLI: onCreate intent=%s", objArr));
	
        this.prefs = new PrefUtil(PreferenceManager.getDefaultSharedPreferences(this));
        this.pwds = new PasswordUtil(PreferenceManager.getDefaultSharedPreferences(this));

	setContentView(R.layout.index);
        //getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON); 

	downloadManager = new ThinDownloadManager(1);
        retryPolicy = new DefaultRetryPolicy();
	
	//downloadProfile(resString(R.string.profile_link));
	
	//use above to fullscreen, hide the notification bar
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
	
	this.load_ui_elements();
	
	doBindService();
	
	
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
 
        int id = item.getItemId();
        switch (id) {
	case R.id.action_setting:
	    mWebView.loadUrl("file:///android_asset/intro.html");
	    return true;
	case R.id.action_help:{
	    Spanned myMessage = Html.fromHtml(OpenVPNBase.about_str);

	    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
	    alertDialog.setTitle("About");
	    alertDialog.setMessage(myMessage);
	    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
				  new DialogInterface.OnClickListener() {
				      public void onClick(DialogInterface dialog, int which) {
					  dialog.dismiss();
				      }
				  });
	    alertDialog.show();
	    TextView msgTxt = (TextView) alertDialog.findViewById(android.R.id.message);
	    msgTxt.setMovementMethod(LinkMovementMethod.getInstance());

	    return true;
	}
	default:
	    return super.onOptionsItemSelected(item);
	}

    }
    
    protected void onNewIntent(Intent intent) {

        Object[] objArr = new Object[S_BIND_CALLED];
        objArr[0] = intent.toString();
        Log.d(TAG, String.format("CLI: onNewIntent intent=%s", objArr));
        setIntent(intent);
    }
    // for return from notification bar's click
    public PendingIntent get_configure_intent(int requestCode) {
        return PendingIntent.getActivity(this, requestCode, getIntent(),  PendingIntent.FLAG_CANCEL_CURRENT); //268435456);
    }
    
    public void ui_setup(boolean active){
	
	
    }
    
    public boolean is_vpn_active(){

	try {
	    for( NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
		
		// Pass over dormant interfaces
		if(!intf.isUp() || intf.getInterfaceAddresses().size() == 0)
		    continue;
		
		if ("tun0".equals(intf.getName())){
		    // The VPN is up
		    return true;
		}
	    }
	}catch(Exception e){
	    
	}
	return false;
	
    }
    
    public void goIndex(){
	mWebView.loadUrl("file:///android_asset/index.html");
    }

    public void showToast(String toast){
	Toast.makeText(this,toast, Toast.LENGTH_SHORT).show();
    }
    
    public void Connect(String profile_name,String username,String password){
	Log.i(TAG,String.format("Connect %s %s %s", profile_name,username,password));
	this.w_username = username;
	this.w_password = password;
	this.w_profile  = profile_name;

	//every time change the
	this.start_connect();
	
    }
    
    public void clear_progressbar(){
	try{
	    if (progressDialog.isShowing() && in_download_profile == false) {
		progressDialog.dismiss();
		progressDialog = null;
	    }
	    
	}catch(Exception exception){
	    exception.printStackTrace();
	}	
    }
    
    public void Disconnect(){
	submitDisconnectIntent(RETAIN_AUTH); // stop thread,not stopSelf, evenually ,in OpenVPNService.java
    }

    public void announcement_cb(String result){
	exec_js(String.format("OPENVPN.setanno('%s');",result));
    }
    
    public void announcement(){
	Log.i(TAG,"get announcement");
	new HttpAsyncTask(2).execute(String.format("%s?version=%s&name=qq398437535&key=%s",OpenVPNBase.gg_url,OpenVPNBase.version,OpenVPNBase.app_key));
    }
    
    public void daloapi_cb(String result){
	exec_js(String.format("OPENVPN.daloapi('%s');",result));
    }
    
    public void daloapi(String username, String password){
	Log.i(TAG,"get daloapi");
	new HttpAsyncTask(1).execute(String.format("%s?username=%s&password=%s",OpenVPNBase.api_url, username,password));
	
    }
    
    public void exec_js(final String jscode)
    {
	//if higher version come up later
	/*
	if (android.os.Build.VERSION.SDK_INT >= 19)//android.os.Build.VERSION_CODES.KITKAT) {
	{
	    //	    mWebView.evaluateJavascript(jscode, null);
	} else {
	*/
	    mWebView.loadUrl( String.format("javascript:%s",jscode));
	    Log.d(TAG,String.format("exec_js=> javascript:%s",jscode));
       //}
    }

    private void load_ui_elements(){
        //getWindow().requestFeature(Window.FEATURE_NO_TITLE);
	
        mWebView = (WebView)findViewById(R.id.mainwebview);
	if(mWebView == null){
	    Log.i(TAG,"mWebView null");
	    return;
	}
	
	//File file2 = new File("file:///android_asset/index.html");

	WebSettings webSettings = mWebView.getSettings();
	webSettings.setJavaScriptEnabled(true);
	webSettings.setDomStorageEnabled(true);
	webSettings.setLoadWithOverviewMode(true);
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
	    webSettings.setAllowUniversalAccessFromFileURLs(true);
	}
	mWebView.addJavascriptInterface(new WebAppInterface(this), "Android");
        mWebView.setWebViewClient(new WebViewClient() {
		
	    @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
	    @Override
	    public void onLoadResource (WebView view, String url) {
                if (progressDialog == null) {
                    // in standard case YourActivity.this
                    progressDialog = new ProgressDialog(IndexClient.this);
                    progressDialog.setMessage("Loading...");
                    progressDialog.show();
                }
            }
	    @Override
	    public void onPageFinished(WebView view, String url){
		Log.i(TAG,"onPageFinished");
		try{
		    if (progressDialog.isShowing() && in_download_profile == false) {
			progressDialog.dismiss();
			progressDialog = null;
		    }
		    
                }catch(Exception exception){
                    exception.printStackTrace();
                }
		//view.loadUrl("javascript:init();");

		if(download_id != 999999 && IndexClient.this.is_vpn_active() != true){
		    if( OpenVPNBase.download_profile==true){
			downloadProfile(OpenVPNBase.profile_link);
		    }
		 }
		//IndexClient.this.exec_js("init();"); // for function test
 	    }    
        });
	
        mWebView.loadUrl("file:///android_asset/index.html");
	//mWebView.loadUrl("http://t61.guu.party/ovpn/index.html");
	//mWebView.setVisibility(View.INVISIBLE);
	
        this.setContentView(mWebView);
	
    }
    
    private void stop_service(){
	submitDisconnectIntent(true);
    }
    private void stop(){
	doUnbindService();
	if(this.stop_service_on_client_exit){
	    Log.d(TAG,"CLI: shutting down service");
	    stop_service();
	}
    }

    protected void post_bind(){
	Log.d(TAG,"CLI: post bind");
    }
    
    public void event(EventMsg ev) {
        //render_event(ev, RETAIN_AUTH, is_active(), RETAIN_AUTH);
	Log.i(TAG,"in event .......................");
	int progress = ev.progress;
	int res_id = 0;



	switch(ev.res_id){
	case R.string.connecting:
	    {
		res_id = 1;
	    }break;
	case R.string.connected:
	    {
		res_id = 2;
	    }break;
	case R.string.disconnected:
	    {
		res_id = 3;
	    }break;
	case R.string.auth_failed:
	    {
		res_id = 4;
	    }break;
	case R.string.core_thread_active:
	    {
		res_id = 5;
	    }break;
	case R.string.core_thread_inactive:
	    {
		Log.i(TAG," cor-thread-inactive");
		res_id = 6;
	    }break;
	case R.string.wait_proxy:
	    {
		res_id =7;
	    }break;
	default:res_id = 0;
	}

	if(res_id > 0){
	    String res_str = resString(ev.res_id);
	    exec_js(String.format("vpn_event(%d,%d,\"%s\");",res_id,progress,res_str));
	}
    }
    
    /*
    protected void onPause() 
    {
        super.onPause();
	Log.d(TAG,"onPause");
    }
    */
    
    protected void onStart()
    {
	super.onStart();
	boolean active = is_active();
	if(active == true)
	{
	    Log.i(TAG,"onStart, actived");

	}else{
	    Log.i(TAG," onStart, not actived");

	}
	    
    }
    /*
    protected void onResume()
    {
	super.onResume();
	
    }
    protected void onRestart() 
    {
	Log.d(TAG,"onRestart");
        super.onRestart();
    }    
    */
    
    protected void onStop() {
	//this.exec_js("android_on_stop();");
	//mWebView.loadUrl("about:blank");

        super.onStop();
	Log.d(TAG," onStop");
    }

    protected void onDestroy() {
	//this.exec_js("android_on_destory();");
        Log.d(TAG, "CLI: onDestroy called");
        super.onDestroy();
    }

    /*
    protected void onActivityResult(int request, int result, Intent data){
	Object[] objArr = new Object[S_ONSTART_CALLED];
	objArr[0] = Integer.valueOf(request);
	objArr[S_ONSTART_CALLED] = Integer.valueOf(result);
	Log.d(TAG,String.format("CLI: onActivityResult request=%d result=%d",objArr));
	
	String path;
	switch(request){
	case S_BIND_CALLED:
	    if(result == RESULT_OK){
		Log.i(TAG,"S_BIND_CALLED OK ,resolve_epki_alias_then_connect");
		resolve_epki_alias_then_connect();
		return;
	    }else if (result != 0){
	    }else{
		if (this.finish_on_connect == FinishOnConnect.ENABLED) {
		    finish();
		} else if (this.finish_on_connect == FinishOnConnect.ENABLED_ACROSS_ONSTART) {
		    this.finish_on_connect = FinishOnConnect.ENABLED;
		    start_connect();
		}		

		return;
	    }
            default:
                super.onActivityResult(request, result, data);// what if the parent does not have onActivityResult?	    
	}
	
	return;
    }
    */
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

		    return;
                }
            case REQUEST_IMPORT_PKCS12 /*3*/:
                if (result == -1) {
                    path = data.getStringExtra(FileDialog.RESULT_PATH);
                    str = TAG;
                    objArr = new Object[S_BIND_CALLED];
                    objArr[0] = path;
                    Log.d(str, String.format("CLI: IMPORT_PKCS12: %s", objArr));

		    return;
                }
            default:
                super.onActivityResult(request, result, data);// what if the parent does not have onActivityResult?
        }
    }
	
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event)
    {

	
	if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
	    mWebView.goBack();
	    return true;
	}
	
        return super.onKeyDown(keyCode, event);
    }


    //---------------------------------------------------------------------------------------------------
    public class WebAppInterface {
	Context mContext;

	/** Instantiate the interface and set the context */
	WebAppInterface(Context c) {
	    mContext = c;
	}

	/** Show a toast from the web page */
	@JavascriptInterface
	public void showToast(String toast) {
	    Log.i(TAG,"showToast");
	    IndexClient.this.showToast(toast);
	    //	    Toast.makeText(mContext,String.format("%s->%s",IndexClient.this.TAG,toast), Toast.LENGTH_SHORT).show();
	}
	@JavascriptInterface
	public void clear_progressbar(){
	    IndexClient.this.clear_progressbar();
	}

	@JavascriptInterface
	public void getanno(){
	    IndexClient.this.announcement();
	}
	@JavascriptInterface
	public void daloapi(String username, String password){
	    IndexClient.this.daloapi(username,password);
	}
	@JavascriptInterface
	public void Connect(String profile_name,String username,String password){
	    IndexClient.this.Connect(profile_name,username,password);   
	    return;
	}

	@JavascriptInterface
	public void Disconnect(){
	    IndexClient.this.Disconnect();   
	    return;
	}
	
	@JavascriptInterface
	public boolean active(){
	    if(IndexClient.this.is_vpn_active() == true && IndexClient.this.is_active()==true){
		return true;
	    }else{
		return false;
	    }
	      
	    //return IndexClient.this.is_active();
	}
	      
	@JavascriptInterface
	public void goIndex(){
	    IndexClient.this.goIndex();
	    return;
	}
	
	@JavascriptInterface
	public String profile_names(){
	    JSONObject json = new JSONObject();
	    final ProfileList profile_list = IndexClient.this.profile_list();
	    String[] pfn_names ;
	    pfn_names = profile_list.profile_names();
	    Log.i(TAG,Arrays.deepToString(pfn_names));
	    try{
		json.put("profiles",new JSONArray( Arrays.asList( pfn_names))  );
		Log.i(TAG,json.toString());
		return json.toString();
	    }catch (JSONException e) {
		Log.w(TAG,Log.getStackTraceString(e));
	    }
	    return "{'profiles':[]}";	    
	}
	
	@JavascriptInterface
	public void closeMyActivity() {
	    finish();//use js to close activity
	}

    }// end WebAppInterface
    
    class MyDownloadDownloadStatusListenerV1 implements DownloadStatusListenerV1 {

        @Override
        public void onDownloadComplete(DownloadRequest request) {
            final int id = request.getDownloadId();
	    if(id == download_id){
		Log.i(IndexClient.TAG,"Download ok");
		//mWebView.setVisibility(View.VISIBLE);
		refresh_profile_list();
	    }
	    download_id = 999999;
	    in_download_profile = false;
	    exec_js("OPENVPN.set_info(\"获取线路完成\");");
	    exec_js("OPENVPN.reset_lines();");
	}
	@Override
	public void onDownloadFailed(DownloadRequest request, int errorCode, String errorMessage) {
            final int id = request.getDownloadId();

	    if( id == download_id){
		Log.i(IndexClient.TAG,"Download1 id: "+id+" Failed: ErrorCode "+errorCode+", "+errorMessage);
		//mWebView.setVisibility(View.VISIBLE);
	    }

	    in_download_profile=false;
	    exec_js("OPENVPN.set_info(\"获取线路失败,使用本地存档\");");
	}

	@Override
	public void onProgress(DownloadRequest request, long totalBytes, long downloadedBytes, int progress) {
            int id = request.getDownloadId();
	    if(id == download_id){
		
		Log.i(IndexClient.TAG,"Download1 id: "+id+", "+progress+"%"+"  "+getBytesDownloaded(progress,totalBytes));
	    }
	}
	
	private String getBytesDownloaded(int progress, long totalBytes) {
	    //Greater than 1 MB
	    long bytesCompleted = (progress * totalBytes)/100;
	    if (totalBytes >= 1000000) {
		return (""+(String.format("%.1f", (float)bytesCompleted/1000000))+ "/"+ ( String.format("%.1f", (float)totalBytes/1000000)) + "MB");
	    } if (totalBytes >= 1000) {
		return (""+(String.format("%.1f", (float)bytesCompleted/1000))+ "/"+ ( String.format("%.1f", (float)totalBytes/1000)) + "Kb");
		
	    } else {
		return ( ""+bytesCompleted+"/"+totalBytes );
	    }
	}
	
    }

      private class HttpAsyncTask extends AsyncTask<String, Void, String> {
	  private int download_type;
	  public HttpAsyncTask( int type){
	      download_type = type;
	  }
	  
	  public String GET(String url){
	      InputStream inputStream = null;
	      String result = "";
	      try {
		  
		  // create HttpClient
		  HttpClient httpclient = new DefaultHttpClient();
		  
		  // make GET request to the given URL
		  HttpResponse httpResponse = httpclient.execute(new HttpGet(url));
		  
		  // receive response as inputStream
		  inputStream = httpResponse.getEntity().getContent();
		  
		  // convert inputstream to string
		  if(inputStream != null)
		      result = convertInputStreamToString(inputStream);
		  else
		      result = "";
		  
	      } catch (Exception e) {
		  Log.d("InputStream", e.getLocalizedMessage());
	      }
	      
	      return result;
	  }
	  private  String convertInputStreamToString(InputStream inputStream) throws IOException{
	      BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
	      String line = "";
	      String result = "";
	      while((line = bufferedReader.readLine()) != null)
		  result += line;
	      
	      inputStream.close();
	      return result;
	      
	  }	  
	  @Override
	  protected String doInBackground(String... urls) {
	      
	      return GET(urls[0]);
	  }
	  // onPostExecute displays the results of the AsyncTask.
	  @Override
	  protected void onPostExecute(String result) {
	      super.onPostExecute(result);
	      Log.i(IndexClient.TAG,result);
	      switch(download_type){
	      case 1:{
		  IndexClient.this.daloapi_cb(result);
	      }break;
	      case 2:{
		  IndexClient.this.announcement_cb(result);
	      }break;
	      default:break;
	      }
			      
	  }
      }  
}
