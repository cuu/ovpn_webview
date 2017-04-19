/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.util.Log
 *  org.json.JSONArray
 *  org.json.JSONException
 *  org.json.JSONObject
 *  org.json.JSONTokener
 */
package net.openvpn.openvpn;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import net.openvpn.openvpn.FileUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ProxyList {
    private static final String TAG = "ProxyList";
    private String backing_file = null;
    private Context context = null;
    private boolean dirty = false;
    private String enabled_name = null;
    private TreeMap<String, Item> list = new TreeMap(String.CASE_INSENSITIVE_ORDER);
    private String none_name = null;

    ProxyList(String string2) {
        if (string2 == null) {
            throw new InternalError();
        }
        this.none_name = string2;
        this.set_enabled(null);
    }

    private void forget_creds(Item item) {
        if (item == null) return;
        item.username = "";
        item.password = "";
        this.dirty = true;
    }

    /*
     * Enabled unnecessary exception pruning
     */
    private JSONObject persist() {
        try {
            JSONObject jSONObject = new JSONObject();
            String string2 = this.get_enabled(false);
            if (string2 != null) {
                jSONObject.put("enabled_name", (Object)string2);
            }
            JSONArray jSONArray = new JSONArray();
            Iterator<Item> iterator = this.list.values().iterator();
            while (iterator.hasNext()) {
                JSONObject jSONObject2 = iterator.next().persist();
                if (jSONObject2 == null) continue;
                jSONArray.put((Object)jSONObject2);
            }
            jSONObject.put("list", (Object)jSONArray);
            return jSONObject;
        }
        catch (JSONException var2_6) {
            Log.e((String)"ProxyList", (String)"ProxyList.persist", (Throwable)var2_6);
            return null;
        }
    }

    private static ProxyList unpersist(JSONObject jSONObject, String string2) {
        try {
            ProxyList proxyList = new ProxyList(string2);
            if (!jSONObject.isNull("enabled_name")) {
                proxyList.enabled_name = jSONObject.getString("enabled_name");
            }
            JSONArray jSONArray = jSONObject.getJSONArray("list");
            int n = jSONArray.length();
            for (int i = 0; i < n; ++i) {
                proxyList.put(Item.unpersist(jSONArray.getJSONObject(i)));
            }
            proxyList.set_enabled(null);
            return proxyList;
        }
        catch (JSONException var3_6) {
            Log.e((String)"ProxyList", (String)"ProxyList.unpersist", (Throwable)var3_6);
            return null;
        }
    }

    public void forget_creds() {
        Iterator<Item> iterator = this.list.values().iterator();
        while (iterator.hasNext()) {
            this.forget_creds(iterator.next());
        }
    }

    public void forget_creds(String string2) {
        this.forget_creds(this.get(string2));
    }

    public Item get(String string2) {
        if (this.is_none(string2)) return null;
        return this.list.get(string2);
    }

    public String get_enabled(boolean bl) {
        if (bl) return this.enabled_name;
        if (this.is_none(this.enabled_name)) return null;
        return this.enabled_name;
    }

    public Item get_enabled_item() {
        return this.get(this.enabled_name);
    }

    public String[] get_name_list(boolean bl) {
        int n = this.list.size();
        Set<String> set = this.list.keySet();
        int n2 = bl ? 1 : 0;
        String[] arrstring = set.toArray(new String[n2 + n]);
        if (!bl) return arrstring;
        arrstring[n] = this.none_name;
        return arrstring;
    }

    public boolean has_saved_creds(String string2) {
        Item item = this.get(string2);
        boolean bl = false;
        if (item == null) return bl;
        int n = item.username.length();
        bl = false;
        if (n <= 0) return bl;
        return true;
    }

    public boolean is_none(String string2) {
        if (string2 == null) return true;
        if (!string2.equals(this.none_name)) return false;
        return true;
    }

    public void load() {
        try {
            if (this.backing_file == null) return;
            ProxyList proxyList = ProxyList.unpersist((JSONObject)new JSONTokener(FileUtil.readFileAppPrivate(this.context, this.backing_file)).nextValue(), this.none_name);
            this.list = proxyList.list;
            this.enabled_name = proxyList.enabled_name;
            this.dirty = false;
            return;
        }
        catch (IOException var3_2) {
            Log.d((String)"ProxyList", (String)"ProxyList.load: no proxy file present");
            return;
        }
        catch (Exception var1_3) {
            Log.e((String)"ProxyList", (String)"ProxyList.load", (Throwable)var1_3);
            return;
        }
    }

    public void put(Item item) {
        if (item == null) return;
        String string2 = item.name();
        if (this.is_none(string2)) return;
        this.list.put(string2, item);
        this.dirty = true;
    }

    public void remove(String string2) {
        if (this.is_none(string2)) return;
        this.list.remove(string2);
        this.set_enabled(null);
        this.dirty = true;
    }

    public void save() {
        try {
            if (!this.dirty) return;
            if (this.backing_file == null) return;
            String string2 = this.persist().toString(4);
            FileUtil.writeFileAppPrivate(this.context, this.backing_file, string2);
            this.dirty = false;
            return;
        }
        catch (Exception var1_2) {
            Log.e((String)"ProxyList", (String)"ProxyList.save", (Throwable)var1_2);
            return;
        }
    }

    public void set_backing_file(Context context, String string2) {
        this.context = context;
        this.backing_file = string2;
    }

    public void set_enabled(String string2) {
        String string3 = this.enabled_name;
        if (string2 == null) {
            string2 = this.enabled_name;
        }
        this.enabled_name = this.is_none(string2) ? this.none_name : (this.get(string2) != null ? string2 : this.none_name);
        if (string3 != null) {
            if (string3.equals(this.enabled_name)) return;
        }
        this.dirty = true;
    }

    public int size() {
        return this.list.size();
    }

    public static class InternalError
    extends RuntimeException {
    }

    public static class Item {
        public boolean allow_cleartext_auth = false;
        public String friendly_name = null;
        public String host = "";
        public String password = "";
        public String port = "";
        public boolean remember_creds = false;
        public String username = "";

        private JSONObject persist() {
            try {
                JSONObject jSONObject = new JSONObject();
                if (this.friendly_name != null) {
                    jSONObject.put("friendly_name", (Object)this.friendly_name);
                }
                jSONObject.put("host", (Object)this.host);
                jSONObject.put("port", (Object)this.port);
                jSONObject.put("remember_creds", this.remember_creds);
                jSONObject.put("allow_cleartext_auth", this.allow_cleartext_auth);
                if (!this.remember_creds) return jSONObject;
                jSONObject.put("username", (Object)this.username);
                jSONObject.put("password", (Object)this.password);
                return jSONObject;
            }
            catch (JSONException var2_2) {
                Log.e((String)"ProxyList", (String)"ProxyList.Item.persist", (Throwable)var2_2);
                return null;
            }
        }

        private static Item unpersist(JSONObject jSONObject) {
            try {
                Item item = new Item();
                item.friendly_name = !jSONObject.isNull("friendly_name") ? jSONObject.getString("friendly_name") : null;
                item.host = jSONObject.getString("host");
                item.port = jSONObject.getString("port");
                item.remember_creds = jSONObject.getBoolean("remember_creds");
                item.allow_cleartext_auth = jSONObject.getBoolean("allow_cleartext_auth");
                if (!jSONObject.isNull("username")) {
                    item.username = jSONObject.getString("username");
                }
                if (jSONObject.isNull("password")) return item;
                item.password = jSONObject.getString("password");
                return item;
            }
            catch (JSONException var2_2) {
                Log.e((String)"ProxyList", (String)"ProxyList.Item.unpersist", (Throwable)var2_2);
                return null;
            }
        }

        public boolean invalidate_creds() {
            boolean bl = this.remember_creds;
            boolean bl2 = false;
            if (!bl) return bl2;
            this.username = "";
            this.password = "";
            this.remember_creds = false;
            return true;
        }

        public boolean is_valid() {
            if (this.host.length() <= 0) return false;
            if (this.port.length() <= 0) return false;
            return true;
        }

        public String name() {
            if (this.friendly_name != null) {
                return this.friendly_name;
            }
            Object[] arrobject = new Object[]{this.host, this.port};
            return String.format("%s:%s", arrobject);
        }
    }

}

