/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.SharedPreferences
 *  android.content.SharedPreferences$Editor
 *  android.util.Log
 */
package net.openvpn.openvpn;

import android.content.SharedPreferences;
import android.util.Log;
import java.util.Set;

public class PrefUtil {
    private static final String TAG = "PrefUtil";
    private SharedPreferences mSettings;

    PrefUtil(SharedPreferences sharedPreferences) {
        this.mSettings = sharedPreferences;
    }

    private String key_by_profile(String string2, String string3) {
        return String.format("%s.%s", string3, string2);
    }

    public boolean contains_key(String string2) {
        return this.mSettings.contains(string2);
    }

    public void delete_key(String string2) {
        SharedPreferences.Editor editor = this.mSettings.edit();
        Log.d((String)"PrefUtil", (String)String.format("delete_key: key='%s'", string2));
        editor.remove(string2);
        editor.apply();
    }

    public void delete_key_by_profile(String string2, String string3) {
        SharedPreferences.Editor editor = this.mSettings.edit();
        String string4 = this.key_by_profile(string2, string3);
        Log.d((String)"PrefUtil", (String)String.format("delete_key_by_profile: key='%s'", string4));
        editor.remove(string4);
        editor.apply();
    }

    public boolean get_boolean(String string2, boolean bl) {
        try {
            boolean bl2 = this.mSettings.getBoolean(string2, bl);
            Object[] arrobject = new Object[]{string2, bl2};
            Log.d((String)"PrefUtil", (String)String.format("get_boolean: %s=%b", arrobject));
            return bl2;
        }
        catch (ClassCastException var3_5) {
            Log.d((String)"PrefUtil", (String)String.format("get_boolean %s class cast exception", string2));
            return bl;
        }
    }

    public boolean get_boolean_by_profile(String string2, String string3, boolean bl) {
        try {
            String string4 = this.key_by_profile(string2, string3);
            boolean bl2 = this.mSettings.getBoolean(string4, bl);
            Object[] arrobject = new Object[]{string4, bl2};
            Log.d((String)"PrefUtil", (String)String.format("get_boolean_by_profile: key='%s' value=%b", arrobject));
            return bl2;
        }
        catch (ClassCastException var4_7) {
            Log.d((String)"PrefUtil", (String)"get_boolean_by_profile class cast exception");
            return bl;
        }
    }

    public String get_string(String string2) {
        try {
            String string3 = this.mSettings.getString(string2, null);
            Log.d((String)"PrefUtil", (String)String.format("get_string: %s='%s'", string2, string3));
            return string3;
        }
        catch (ClassCastException var2_3) {
            Log.d((String)"PrefUtil", (String)String.format("get_string %s class cast exception", string2));
            return null;
        }
    }

    public String get_string_by_profile(String string2, String string3) {
        try {
            String string4 = this.key_by_profile(string2, string3);
            String string5 = this.mSettings.getString(string4, null);
            Log.d((String)"PrefUtil", (String)String.format("get_string_by_profile: key='%s' value='%s'", string4, string5));
            return string5;
        }
        catch (ClassCastException var3_5) {
            Log.d((String)"PrefUtil", (String)"get_string_by_profile class cast exception");
            return null;
        }
    }

    public Set<String> get_string_set(String string2) {
        try {
            Set set = this.mSettings.getStringSet(string2, null);
            Log.d((String)"PrefUtil", (String)String.format("get_string_set: %s='%s'", string2, set));
            return set;
        }
        catch (ClassCastException var2_3) {
            Log.d((String)"PrefUtil", (String)String.format("get_string_set %s class cast exception", string2));
            return null;
        }
    }

    public void set_boolean(String string2, boolean bl) {
        SharedPreferences.Editor editor = this.mSettings.edit();
        editor.putBoolean(string2, bl);
        Object[] arrobject = new Object[]{string2, bl};
        Log.d((String)"PrefUtil", (String)String.format("set_boolean: %s=%b", arrobject));
        editor.apply();
    }

    public void set_boolean_by_profile(String string2, String string3, boolean bl) {
        SharedPreferences.Editor editor = this.mSettings.edit();
        String string4 = this.key_by_profile(string2, string3);
        Object[] arrobject = new Object[]{string4, bl};
        Log.d((String)"PrefUtil", (String)String.format("set_boolean_by_profile: key='%s' value=%b", arrobject));
        editor.putBoolean(string4, bl);
        editor.apply();
    }

    public void set_string(String string2, String string3) {
        SharedPreferences.Editor editor = this.mSettings.edit();
        editor.putString(string2, string3);
        Log.d((String)"PrefUtil", (String)String.format("set_string: %s='%s'", string2, string3));
        editor.apply();
    }

    public void set_string_by_profile(String string2, String string3, String string4) {
        SharedPreferences.Editor editor = this.mSettings.edit();
        String string5 = this.key_by_profile(string2, string3);
        Log.d((String)"PrefUtil", (String)String.format("set_string_by_profile: key='%s' value='%s'", string5, string4));
        editor.putString(string5, string4);
        editor.apply();
    }

    public void set_string_set(String string2, Set<String> set) {
        SharedPreferences.Editor editor = this.mSettings.edit();
        editor.putStringSet(string2, set);
        Log.d((String)"PrefUtil", (String)String.format("set_string: %s='%s'", string2, set));
        editor.apply();
    }
}

