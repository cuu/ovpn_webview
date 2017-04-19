/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.app.AlertDialog
 *  android.app.AlertDialog$Builder
 *  android.content.Context
 *  android.content.DialogInterface
 *  android.content.DialogInterface$OnClickListener
 *  android.content.res.Resources
 *  android.os.Handler
 *  android.text.Editable
 *  android.text.method.PasswordTransformationMethod
 *  android.text.method.SingleLineTransformationMethod
 *  android.text.method.TransformationMethod
 *  android.util.Base64
 *  android.util.Log
 *  android.view.LayoutInflater
 *  android.view.View
 *  android.view.ViewGroup
 *  android.widget.EditText
 *  android.widget.TextView
 *  org.apache.http.conn.ssl.BrowserCompatHostnameVerifier
 *  org.xmlpull.v1.XmlPullParser
 *  org.xmlpull.v1.XmlPullParserException
 *  org.xmlpull.v1.XmlPullParserFactory
 */
package net.openvpn.openvpn;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.text.Editable;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import net.openvpn.openvpn.CertWarn;
import net.openvpn.openvpn.FileUtil;
import net.openvpn.openvpn.TrustMan;
import net.openvpn.openvpn.XMLRPC;
import net.openvpn.openvpn.XMLRPC.XMLRPCException;

import org.apache.http.conn.ssl.BrowserCompatHostnameVerifier;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class HttpsClient {
    private static final String TAG = "OpenVPNHttpsClient";

    private static void raise_dialog(Context context, String string2, String string3) {
        new AlertDialog.Builder(context).setTitle((CharSequence)string2).setMessage((CharSequence)string3).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){

            public void onClick(DialogInterface dialogInterface, int n) {
            }
        }).show();
    }

    private static String resstr(Context context, int n) {
        return context.getResources().getString(n);
    }

    public static void run_task(final Context context, final Task task, CancelDetect.I i, final Runnable runnable, final boolean bl, final boolean bl2, long l) {
        final Handler handler = new Handler();
        try {
            final TrustMan trustMan = new TrustMan(context);
            SSLContext sSLContext = SSLContext.getInstance("TLS");
            final CancelDetect cancelDetect = new CancelDetect(i);
            sSLContext.init(null, new X509TrustManager[]{trustMan}, new SecureRandom());
            final AdaptiveHostnameVerifier adaptiveHostnameVerifier = new AdaptiveHostnameVerifier();
            Interact interact = new Interact(){

                @Override
                public void challenge_response_dialog(final AuthContext authContext, final String string2) {
                    Runnable runnable2 = new Runnable(){

                        /*
                         * Enabled unnecessary exception pruning
                         */
                        @Override
                        public void run() {
                            String string22;
                            if (cancelDetect.is_canceled()) {
                                return;
                            }
                            try {
                                authContext.cr_parse(string2);
                                boolean bl = authContext.get_cr().get_echo();
                                boolean bl2 = authContext.get_cr().get_response_required();
                                string22 = authContext.get_cr().get_challenge_text();
                                if (bl2) {
                                    View view = LayoutInflater.from((Context)context).inflate(R.layout.cr_dialog, null);
                                    TextView textView = (TextView)view.findViewById(R.id.dialog_challenge);
                                    final EditText editText = (EditText)view.findViewById(R.id.dialog_response);
                                    textView.setText((CharSequence)string22);
                                    if (bl) {
                                        editText.setTransformationMethod((TransformationMethod)SingleLineTransformationMethod.getInstance());
                                    } else {
                                        editText.setTransformationMethod((TransformationMethod)PasswordTransformationMethod.getInstance());
                                    }
                                    DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener(){

                                        public void onClick(DialogInterface dialogInterface, int n) {
                                            if (cancelDetect.is_canceled()) return;
                                            if (n == -1) {
                                                authContext.get_cr().set_response(editText.getText().toString());
                                                new Thread(task).start();
                                                return;
                                            }
                                            handler.post(runnable);
                                        }
                                    };
                                    new AlertDialog.Builder(context).setTitle((CharSequence)HttpsClient.resstr(context, R.string.cr_title)).setView(view).setPositiveButton(R.string.cr_continue, onClickListener).setNegativeButton(R.string.cr_cancel, onClickListener).show();
                                    return;
                                }
                            }
                            catch (Exception var1_8) {
                                Log.e((String)"OpenVPNHttpsClient", (String)"challenge_response_dialog", (Throwable)var1_8);
                                HttpsClient.raise_dialog(context, HttpsClient.resstr(context, R.string.cr_error), var1_8.toString());
                                handler.post(runnable);
                                return;
                            }
                            DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener(){

                                public void onClick(DialogInterface dialogInterface, int n) {
                                    if (cancelDetect.is_canceled()) return;
                                    if (n == -1) {
                                        new Thread(task).start();
                                        return;
                                    }
                                    handler.post(runnable);
                                }
                            };
                            new AlertDialog.Builder(context).setTitle((CharSequence)HttpsClient.resstr(context, R.string.cr_title)).setMessage((CharSequence)string22).setPositiveButton(R.string.cr_continue, onClickListener).setNegativeButton(R.string.cr_cancel, onClickListener).show();
                        }

                    };
                    handler.post(runnable2);
                }

                @Override
                public void error_dialog(final int n, final int n2, final Object object) {
                    Runnable runnable2 = new Runnable(){

                        @Override
                        public void run() {
                            if (cancelDetect.is_canceled()) {
                                return;
                            }
                            if (object instanceof Exception) {
                                if (TrustMan.isTrustFail((Exception)object)) return;
                            }
                            if (n == 0) return;
                            StringBuilder stringBuilder = new StringBuilder();
                            if (n2 != 0) {
                                stringBuilder.append(HttpsClient.resstr(context, n2));
                            }
                            if (object != null) {
                                if (stringBuilder.length() > 0) {
                                    stringBuilder.append(" : ");
                                }
                                stringBuilder.append(object.toString());
                            }
                            String string2 = HttpsClient.resstr(context, n);
                            String string3 = stringBuilder.toString();
                            if (bl2) {
                                HttpsClient.raise_dialog(context, string2, string3);
                            }
                            handler.post(runnable);
                        }
                    };
                    handler.post(runnable2);
                }

            };
            task.sslContext = sSLContext;
            task.hostnameVerifier = adaptiveHostnameVerifier;
            task.interact = interact;
            task.max_download_size = l;
            trustMan.setCallback(new TrustMan.Callback(){

                @Override
                public void onTrustFail(final TrustMan.TrustContext trustContext) {
                    Runnable runnable2 = new Runnable(){

                        @Override
                        public void run() {
                            if (bl) {
                                new CertWarn(context, trustContext.chain[0], trustContext.excep.toString()){

                                    @Override
                                    protected void done(int n) {
                                        if (n == 1) {
                                            trustMan.trustCert(trustContext);
                                            if (cancelDetect.is_canceled()) return;
                                            new Thread(task).start();
                                            return;
                                        }
                                        if (cancelDetect.is_canceled()) return;
                                        handler.post(runnable);
                                    }
                                };
                                return;
                            }
                            HttpsClient.raise_dialog(context, HttpsClient.resstr(context, R.string.profile_import_error), HttpsClient.resstr(context, R.string.profile_import_invalid_cert));
                            handler.post(runnable);
                        }

                    };
                    handler.post(runnable2);
                }

                @Override
                public void onTrustSucceed(boolean bl2) {
                    adaptiveHostnameVerifier.allowAll(bl2);
                }

            });
            new Thread(task).start();
            return;
        }
        catch (Exception var10_13) {
            Log.e((String)"OpenVPNHttpsClient", (String)"run_task", (Throwable)var10_13);
            HttpsClient.raise_dialog(context, HttpsClient.resstr(context, R.string.https_client_task_error), var10_13.toString());
            handler.post(runnable);
            return;
        }
    }

    public static class AdaptiveHostnameVerifier
    implements HostnameVerifier {
        private HostnameVerifier bchv = new BrowserCompatHostnameVerifier();
        private boolean mode = false;

        public void allowAll(boolean bl) {
            this.mode = bl;
        }

        @Override
        public boolean verify(String string2, SSLSession sSLSession) {
            if (!this.mode) return this.bchv.verify(string2, sSLSession);
            return true;
        }
    }

    public static class AuthContext {
        private CR cr;
        private String hostname = "";
        private String password;
        private boolean pw_is_sess_id = false;
        private String server;
        private String username;

        public AuthContext(String string2, String string3, String string4) {
            this.server = string2;
            this.username = string3;
            this.password = string4;
        }

        public static boolean is_challenge(String string2) {
            return CR.is_challenge(string2);
        }

        public boolean cr_defined() {
            if (this.cr == null) return false;
            return true;
        }

        public void cr_parse(String string2) throws CR.ParseError {
            this.cr = new CR(string2);
        }

        public String getHostname() {
            return this.hostname;
        }

        public CR get_cr() {
            return this.cr;
        }

        public String get_password() {
            if (this.cr == null) return this.password;
            return this.cr.get_password();
        }

        public String get_username() {
            if (this.pw_is_sess_id) {
                return "SESSION_ID";
            }
            if (this.cr == null) return this.username;
            return this.cr.get_username();
        }

        public String profile_filename() {
            Object[] arrobject = new Object[]{this.username, this.server};
            return String.format("%s@%s.ovpn", arrobject);
        }

        public void setHostname(String string2) {
            this.hostname = string2;
        }

        public void set_basic_auth(URLConnection uRLConnection) throws UnsupportedEncodingException {
            String string2 = this.get_username() + ":" + this.get_password();
            uRLConnection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((byte[])string2.getBytes("UTF-8"), (int)2));
        }

        public void set_session_id(String string2) {
            this.pw_is_sess_id = true;
            this.password = string2;
            this.cr = null;
        }

        public static class CR {
            private String challenge_text;
            private boolean echo = false;
            private String response = "";
            private boolean response_required = false;
            private String state_id;
            private String username;

            public CR(String string2) throws ParseError {
                String[] arrstring = string2.split(":", 5);
                if (arrstring.length != 5) {
                    throw new ParseError();
                }
                if (!arrstring[0].equals("CRV1")) {
                    throw new ParseError();
                }
                for (String string3 : arrstring[1].split(",")) {
                    if (string3.equals("E")) {
                        this.echo = true;
                    }
                    if (!string3.equals("R")) continue;
                    this.response_required = true;
                }
                this.state_id = arrstring[2];
                try {
                    this.username = new String(Base64.decode((String)arrstring[3], (int)0), "UTF-8");
                }
                catch (UnsupportedEncodingException var6_7) {
                    throw new ParseError();
                }
                this.challenge_text = arrstring[4];
            }

            public static boolean is_challenge(String string2) {
                if (string2 == null) return false;
                if (!string2.startsWith("CRV1:")) return false;
                return true;
            }

            public String get_challenge_text() {
                return this.challenge_text;
            }

            public boolean get_echo() {
                return this.echo;
            }

            public String get_password() {
                return "CRV1::" + this.state_id + "::" + this.response;
            }

            public String get_response() {
                return this.response;
            }

            public boolean get_response_required() {
                return this.response_required;
            }

            public String get_username() {
                return this.username;
            }

            public void set_response(String string2) {
                this.response = string2;
            }

            public static class ParseError
            extends Exception {
                public ParseError() {
                    super("AuthContext.CR.ParseError");
                }
            }

        }

    }

    public static class CancelDetect {
        private final int gen;
        private final I obj;

        public CancelDetect(I i) {
            this.obj = i;
            this.gen = i.cancel_generation();
        }

        public boolean is_canceled() {
            if (this.gen == this.obj.cancel_generation()) return false;
            return true;
        }

        public static interface I {
            public int cancel_generation();
        }

    }

    public static interface Interact {
        public void challenge_response_dialog(AuthContext var1, String var2);

        public void error_dialog(int var1, int var2, Object var3);
    }

    public static class PresettableHostnameVerifier
    implements HostnameVerifier {
        private HostnameVerifier bchv = new BrowserCompatHostnameVerifier();
        public String hostnameOverride;

        @Override
        public boolean verify(String string2, SSLSession sSLSession) {
            return this.bchv.verify(this.hostnameOverride, sSLSession);
        }
    }

    public static abstract class Task
    implements Runnable {
        protected static final int PROF_AUTOLOGIN = 1;
        protected static final int PROF_USERLOGIN = 2;
        protected HostnameVerifier hostnameVerifier;
        protected Interact interact;
        protected long max_download_size;
        protected SSLContext sslContext;

        protected static String xmlrpc_simple_query(String string2) {
            return String.format("<?xml version=\"1.0\"?>\n<methodCall>\n<methodName>%s</methodName>\n<params></params>\n</methodCall>\n", string2);
        }

        protected void close_session(AuthContext authContext) throws Exception {
            HttpsURLConnection httpsURLConnection = this.get_conn(authContext);
            this.write(httpsURLConnection, Task.xmlrpc_simple_query("CloseSession"));
            this.read(httpsURLConnection);
            httpsURLConnection.disconnect();
        }

        protected HttpsURLConnection get_conn(AuthContext authContext) throws MalformedURLException, IOException, ProtocolException, UnsupportedEncodingException {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection)new URL("https://" + authContext.server + "/RPC2").openConnection();
            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setConnectTimeout(30000);
            httpsURLConnection.setReadTimeout(60000);
            if (!authContext.getHostname().isEmpty()) {
                PresettableHostnameVerifier presettableHostnameVerifier = new PresettableHostnameVerifier();
                presettableHostnameVerifier.hostnameOverride = authContext.getHostname();
                httpsURLConnection.setHostnameVerifier(presettableHostnameVerifier);
            } else {
                httpsURLConnection.setHostnameVerifier(this.hostnameVerifier);
            }
            httpsURLConnection.setSSLSocketFactory(this.sslContext.getSocketFactory());
            authContext.set_basic_auth(httpsURLConnection);
            return httpsURLConnection;
        }

        protected String get_profile(AuthContext authContext, String string2) throws Exception {
            HttpsURLConnection httpsURLConnection = this.get_conn(authContext);
            this.write(httpsURLConnection, Task.xmlrpc_simple_query(string2));
            String string3 = this.read(httpsURLConnection);
            httpsURLConnection.disconnect();
            String string4 = (String)this.parse_xmlrpc(string3);
            if (string4 == null) throw new XMLRPC.XMLRPCException("malformed XML response to " + string2);
            return string4;
        }

        protected void get_session_id(AuthContext authContext) throws Exception {
            HttpsURLConnection httpsURLConnection = this.get_conn(authContext);
            this.write(httpsURLConnection, Task.xmlrpc_simple_query("GetSession"));
            String string2 = this.read(httpsURLConnection);
            httpsURLConnection.disconnect();
            Map map = (Map)this.parse_xmlrpc(string2);
            if (map == null) throw new XMLRPC.XMLRPCException("malformed XML response to GetSession");
            Integer n = (Integer)map.get("status");
            if (n == null) throw new XMLRPC.XMLRPCException("malformed XML response to GetSession");
            if (n == 0) {
                String string3 = (String)map.get("session_id");
                if (string3 == null) throw new XMLRPC.XMLRPCException("malformed XML response to GetSession");
                authContext.set_session_id(string3);
                return;
            }
            if (n != 1) throw new XMLRPC.XMLRPCException("malformed XML response to GetSession");
            String string4 = (String)map.get("client_reason");
            if (string4 == null) throw new ErrorDialogException(R.string.profile_import_error, R.string.auth_failed, null);
            if (!AuthContext.is_challenge(string4)) throw new ErrorDialogException(R.string.profile_import_error, R.string.auth_failed, string4);
            this.interact.challenge_response_dialog(authContext, string4);
            throw new SilentException();
        }

        protected Object parse_xmlrpc(String string2) throws XmlPullParserException, XMLRPC.XMLRPCException, IOException {
            XmlPullParser xmlPullParser = XmlPullParserFactory.newInstance().newPullParser();
            xmlPullParser.setInput((Reader)new StringReader(string2));
            return XMLRPC.parse_response(xmlPullParser);
        }

        protected int profile_types_available(AuthContext ac) throws Exception {
            int ret = 0;
            HttpsURLConnection conn = get_conn(ac);
            write(conn, xmlrpc_simple_query("EnumConfigTypes"));
            String xml_text = read(conn);
            conn.disconnect();
            Map<String, Object> map = (Map) parse_xmlrpc(xml_text);
            if (map != null) {
                Boolean autologin = (Boolean) map.get("autologin");
                if (autologin != null && autologin.booleanValue()) {
                    ret = 0 | PROF_AUTOLOGIN;
                }
                Boolean userlogin = (Boolean) map.get("userlogin");
                if (userlogin == null || !userlogin.booleanValue()) {
                    return ret;
                }
                return ret | PROF_USERLOGIN;
            }
            throw new XMLRPCException("malformed XML response to EnumConfigTypes");
        }


        protected String read(HttpsURLConnection httpsURLConnection) throws IOException {
            return FileUtil.readStream(new BufferedInputStream(httpsURLConnection.getInputStream()), this.max_download_size, "<XML-RPC input>");
        }

        protected void write(HttpsURLConnection httpsURLConnection, String string2) throws IOException {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(httpsURLConnection.getOutputStream());
            outputStreamWriter.write(string2);
            outputStreamWriter.flush();
        }

        protected static class ErrorDialogException
        extends Exception {
            private int msg_resid;
            private Object obj;
            private int title_resid;

            public ErrorDialogException(int n, int n2, Object object) {
                super("ErrorDialogException");
                this.title_resid = n;
                this.msg_resid = n2;
                this.obj = object;
            }

            void dispatch(Interact interact) {
                interact.error_dialog(this.title_resid, this.msg_resid, this.obj);
            }
        }

        protected static class SilentException
        extends ErrorDialogException {
            public SilentException() {
                super(0, 0, null);
            }
        }

    }

}

