/*
 * Decompiled with CFR 0_115.
 */
package net.openvpn.openvpn;

import net.openvpn.openvpn.ClientAPI_Event;
import net.openvpn.openvpn.ClientAPI_ExternalPKICertRequest;
import net.openvpn.openvpn.ClientAPI_ExternalPKISignRequest;
import net.openvpn.openvpn.ClientAPI_LogInfo;
import net.openvpn.openvpn.ClientAPI_OpenVPNClient;
import net.openvpn.openvpn.ClientAPI_Status;

public class OpenVPNClientThread extends ClientAPI_OpenVPNClient implements Runnable {
    private int bytes_in_index = -1;
    private int bytes_out_index = -1;
    private boolean connect_called = false;
    private ClientAPI_Status m_connect_status;
    private EventReceiver parent;
    private Thread thread;
    private TunBuilder tun_builder;

    public OpenVPNClientThread() {
        int n = OpenVPNClientThread.stats_n();
        int n2 = 0;
        while (n2 < n) {
            String string2 = OpenVPNClientThread.stats_name(n2);
            if (string2.equals("BYTES_IN")) {
                this.bytes_in_index = n2;
            }
            if (string2.equals("BYTES_OUT")) {
                this.bytes_out_index = n2;
            }
            ++n2;
        }
    }

    private void call_done(ClientAPI_Status clientAPI_Status) {
        EventReceiver eventReceiver = this.finalize_thread(clientAPI_Status);
        if (eventReceiver == null) return;
        eventReceiver.done(this.m_connect_status);
    }

    private EventReceiver finalize_thread(ClientAPI_Status clientAPI_Status) {
        synchronized (this) {
            EventReceiver eventReceiver = this.parent;
            if (eventReceiver == null) return eventReceiver;
            this.m_connect_status = clientAPI_Status;
            this.parent = null;
            this.tun_builder = null;
            this.thread = null;
            return eventReceiver;
        }
    }

    public long bytes_in() {
        return super.stats_value(this.bytes_in_index);
    }

    public long bytes_out() {
        return super.stats_value(this.bytes_out_index);
    }

    public void connect(EventReceiver eventReceiver) {
        if (this.connect_called) {
            throw new ConnectCalledTwice();
        }
        this.connect_called = true;
        this.parent = eventReceiver;
        this.m_connect_status = null;
        this.thread = new Thread(this, "OpenVPNClientThread");
        this.thread.start();
    }

    @Override
    public void event(ClientAPI_Event clientAPI_Event) {
        EventReceiver eventReceiver = this.parent;
        if (eventReceiver == null) return;
        eventReceiver.event(clientAPI_Event);
    }

    @Override
    public void external_pki_cert_request(ClientAPI_ExternalPKICertRequest clientAPI_ExternalPKICertRequest) {
        EventReceiver eventReceiver = this.parent;
        if (eventReceiver == null) return;
        eventReceiver.external_pki_cert_request(clientAPI_ExternalPKICertRequest);
    }

    @Override
    public void external_pki_sign_request(ClientAPI_ExternalPKISignRequest clientAPI_ExternalPKISignRequest) {
        EventReceiver eventReceiver = this.parent;
        if (eventReceiver == null) return;
        eventReceiver.external_pki_sign_request(clientAPI_ExternalPKISignRequest);
    }

    @Override
    public void log(ClientAPI_LogInfo clientAPI_LogInfo) {
        EventReceiver eventReceiver = this.parent;
        if (eventReceiver == null) return;
        eventReceiver.log(clientAPI_LogInfo);
    }

    @Override
    public boolean pause_on_connection_timeout() {
        EventReceiver eventReceiver = this.parent;
        if (eventReceiver == null) return false;
        return eventReceiver.pause_on_connection_timeout();
    }

    @Override
    public void run() {
        this.call_done(super.connect());
    }

    @Override
    public boolean socket_protect(int n) {
        EventReceiver eventReceiver = this.parent;
        if (eventReceiver == null) return false;
        return eventReceiver.socket_protect(n);
    }

    @Override
    public boolean tun_builder_add_address(String string2, int n, String string3, boolean bl, boolean bl2) {
        TunBuilder tunBuilder = this.tun_builder;
        if (tunBuilder == null) return false;
        return tunBuilder.tun_builder_add_address(string2, n, string3, bl, bl2);
    }

    @Override
    public boolean tun_builder_add_dns_server(String string2, boolean bl) {
        TunBuilder tunBuilder = this.tun_builder;
        if (tunBuilder == null) return false;
        return tunBuilder.tun_builder_add_dns_server(string2, bl);
    }

  	// 这个函数与实际 的tun_builder_add_route 少了一个prefix_length 的参数,故没有override ,那是不是表示是无效的函数 
    //public boolean tun_builder_add_route(String string2, int n, boolean bl) 
		// 这些函数功能 转到了 任何有 import OpenVPNClientThread.TunBuilder 的java类中  ,比如 
		// > OpenVPNService.java:2140:    private class TunBuilder extends VpnService.Builder  implements OpenVPNClientThread.TunBuilder {

		@Override
		public boolean tun_builder_add_route(String string2, int n, int metric, boolean bl) 
		{
        TunBuilder tunBuilder = this.tun_builder;
        if (tunBuilder == null) return false;
        return tunBuilder.tun_builder_add_route(string2, n, bl);
    }

    @Override
    public boolean tun_builder_add_search_domain(String string2) {
        TunBuilder tunBuilder = this.tun_builder;
        if (tunBuilder == null) return false;
        return tunBuilder.tun_builder_add_search_domain(string2);
    }

    @Override
    public int tun_builder_establish() {
        TunBuilder tunBuilder = this.tun_builder;
        if (tunBuilder == null) return -1;
        return tunBuilder.tun_builder_establish();
    }

		@Override 
    public boolean tun_builder_exclude_route(String string2, int n,int metric, boolean bl) {
        TunBuilder tunBuilder = this.tun_builder;
        if (tunBuilder == null) return false;
        return tunBuilder.tun_builder_exclude_route(string2, n, bl);
    }

    @Override
    public boolean tun_builder_new() {
        EventReceiver eventReceiver = this.parent;
        boolean bl = false;
        if (eventReceiver == null) return bl;
        TunBuilder tunBuilder = this.tun_builder = eventReceiver.tun_builder_new();
        bl = false;
        if (tunBuilder == null) return bl;
        return true;
    }

    @Override
    public boolean tun_builder_reroute_gw(boolean bl, boolean bl2, long l) {
        TunBuilder tunBuilder = this.tun_builder;
        if (tunBuilder == null) return false;
        return tunBuilder.tun_builder_reroute_gw(bl, bl2, l);
    }

    @Override
    public boolean tun_builder_set_mtu(int n) {
        TunBuilder tunBuilder = this.tun_builder;
        if (tunBuilder == null) return false;
        return tunBuilder.tun_builder_set_mtu(n);
    }

    @Override
    public boolean tun_builder_set_remote_address(String string2, boolean bl) {
        TunBuilder tunBuilder = this.tun_builder;
        if (tunBuilder == null) return false;
        return tunBuilder.tun_builder_set_remote_address(string2, bl);
    }

    @Override
    public boolean tun_builder_set_session_name(String string2) {
        TunBuilder tunBuilder = this.tun_builder;
        if (tunBuilder == null) return false;
        return tunBuilder.tun_builder_set_session_name(string2);
    }

    @Override
    public void tun_builder_teardown(boolean bl) {
        TunBuilder tunBuilder = this.tun_builder;
        if (tunBuilder == null) return;
        tunBuilder.tun_builder_teardown(bl);
    }

    public void wait_thread_long() {
        boolean bl;
        if (this.thread == null) return;
        do {
            bl = false;
            try {
                this.thread.join();
                continue;
            }
            catch (InterruptedException var2_2) {
                bl = true;
                super.stop();
								//continue; // 我云版本有这个continue
            }
        } while (bl);
    }

    public void wait_thread_short() {
        Thread thread;
        thread = this.thread;
        if (thread == null) return;
        try {
            thread.join(5000);
        }
        catch (InterruptedException var2_3) {}
        if (!thread.isAlive()) return;
        ClientAPI_Status clientAPI_Status = new ClientAPI_Status();
        clientAPI_Status.setError(true);
        clientAPI_Status.setMessage("CORE_THREAD_ABANDONED");
        this.call_done(clientAPI_Status);
    }

    public static class ConnectCalledTwice
    extends RuntimeException {
    }

    public static interface EventReceiver {
        public void done(ClientAPI_Status var1);

        public void event(ClientAPI_Event var1);

        public void external_pki_cert_request(ClientAPI_ExternalPKICertRequest var1);

        public void external_pki_sign_request(ClientAPI_ExternalPKISignRequest var1);

        public void log(ClientAPI_LogInfo var1);

        public boolean pause_on_connection_timeout();

        public boolean socket_protect(int var1);

        public TunBuilder tun_builder_new();
    }

    public static interface TunBuilder {
        public boolean tun_builder_add_address(String var1, int var2, String var3, boolean var4, boolean var5);

        public boolean tun_builder_add_dns_server(String var1, boolean var2);

        public boolean tun_builder_add_route(String var1, int var2, boolean var3);

        public boolean tun_builder_add_search_domain(String var1);

        public int tun_builder_establish();

        public boolean tun_builder_exclude_route(String var1, int var2, boolean var3);

        public boolean tun_builder_reroute_gw(boolean var1, boolean var2, long var3);

        public boolean tun_builder_set_mtu(int var1);

        public boolean tun_builder_set_remote_address(String var1, boolean var2);

        public boolean tun_builder_set_session_name(String var1);

        public void tun_builder_teardown(boolean var1);
    }

}

