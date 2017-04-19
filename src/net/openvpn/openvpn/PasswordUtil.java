/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.SharedPreferences
 *  android.content.SharedPreferences$Editor
 *  android.util.Base64
 *  android.util.Log
 */
package net.openvpn.openvpn;

import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import java.security.Key;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PasswordUtil {
    private static final String TAG = "PasswordUtil";
    private String cipherName;
    private IvParameterSpec ivParms;
    private SharedPreferences mSettings;
    private String prefPrefix;
    private byte[] salt;
    private SecretKey secret;

    PasswordUtil(SharedPreferences sharedPreferences) {
        this.mSettings = sharedPreferences;
        this.prefPrefix = "pwdv1";
        this.regenerate(false);
    }

    private void check_salt() {
        byte[] arrby = this.get_salt();
        if (arrby == null) return;
        if (this.salt == null) return;
        if (Arrays.equals(arrby, this.salt)) return;
        this.regenerate(false);
    }

    private String de(String string2) {
        this.check_salt();
        if (string2 == null) return null;
        if (this.secret == null) return null;
        if (this.prefPrefix == null) return null;
        try {
            byte[] arrby = Base64.decode((String)string2, (int)0);
            Cipher cipher = Cipher.getInstance(this.cipherName);
            cipher.init(2, (Key)this.secret, this.ivParms);
            return new String(cipher.doFinal(arrby), "UTF-8");
        }
        catch (Exception var2_5) {
            Log.e((String)"PasswordUtil", (String)"de", (Throwable)var2_5);
            this.regenerate(true);
        }
        return null;
    }

    private String en(String string2) {
        this.check_salt();
        if (string2 == null) return null;
        if (this.secret == null) return null;
        if (this.prefPrefix == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(this.cipherName);
            cipher.init(1, (Key)this.secret, this.ivParms);
            return Base64.encodeToString((byte[])cipher.doFinal(string2.getBytes("UTF-8")), (int)2);
        }
        catch (Exception var2_4) {
            Log.e((String)"PasswordUtil", (String)"en", (Throwable)var2_4);
            this.regenerate(true);
        }
        return null;
    }

    private void generate_salt() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] arrby = new byte[16];
        secureRandom.nextBytes(arrby);
        SharedPreferences.Editor editor = this.mSettings.edit();
        editor.putString(this.key_string("settings", "entropy"), Base64.encodeToString((byte[])arrby, (int)2));
        editor.apply();
    }

    private byte[] get_salt() {
        try {
            String string2 = this.key_string("settings", "entropy");
            String string3 = this.mSettings.getString(string2, null);
            byte[] arrby = null;
            if (string3 == null) return arrby;
            byte[] arrby2 = Base64.decode((String)string3, (int)0);
            return arrby2;
        }
        catch (Exception var1_5) {
            return null;
        }
    }

    private String key_prefix(String string2) {
        if (string2 != null) {
            Object[] arrobject = new Object[]{this.prefPrefix, string2};
            return String.format("%s.%s.", arrobject);
        }
        Object[] arrobject = new Object[]{this.prefPrefix};
        return String.format("%s.", arrobject);
    }

    private String key_string(String string2, String string3) {
        Object[] arrobject = new Object[]{this.prefPrefix, string2, string3};
        return String.format("%s.%s.%s", arrobject);
    }

    public String get(String string2, String string3) {
        try {
            String string4 = this.key_string(string2, string3);
            return this.de(this.mSettings.getString(string4, null));
        }
        catch (ClassCastException var3_5) {
            Log.d((String)"PasswordUtil", (String)"get() class cast exception");
            this.regenerate(true);
            return null;
        }
    }

    void regenerate(boolean bl) {
        this.cipherName = "AES/CBC/PKCS5Padding";
        byte[] arrby = new byte[]{-42, -31, -117, 101, 25, 119, 127, 37, 121, -54, 46, 49, -35, -48, -72, 97};
        this.salt = null;
        if (!bl) {
            this.salt = this.get_salt();
        }
        if (bl || this.salt == null) {
            this.remove(null);
            this.generate_salt();
            this.salt = this.get_salt();
        }
        if (this.salt != null) {
            try {
                this.secret = new SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(new PBEKeySpec("It was a bright cold day in April, and the clocks were striking thirteen. Winston Smith, his chin nuzzled into his breast in an effort to escape the vile wind, slipped quickly through the glass doors of Victory Mansions, though not quickly enough to prevent a swirl of gritty dust from entering along with him.".toCharArray(), this.salt, 16, 128)).getEncoded(), this.cipherName);
                this.ivParms = new IvParameterSpec(arrby);
                return;
            }
            catch (Exception var3_3) {
                Log.e((String)"PasswordUtil", (String)"regenerate", (Throwable)var3_3);
            }
        }
        this.secret = null;
        this.prefPrefix = null;
    }

    public void remove(String string2) {
        SharedPreferences.Editor editor = this.mSettings.edit();
        Set set = this.mSettings.getAll().keySet();
        String string3 = this.key_prefix(string2);
        Iterator iterator = set.iterator();
        do {
            if (!iterator.hasNext()) {
                editor.apply();
                return;
            }
            String string4 = (String)iterator.next();
            if (!string4.startsWith(string3)) continue;
            editor.remove(string4);
        } while (true);
    }

    public void remove(String string2, String string3) {
        SharedPreferences.Editor editor = this.mSettings.edit();
        editor.remove(this.key_string(string2, string3));
        editor.apply();
    }

    public void set(String string2, String string3, String string4) {
        SharedPreferences.Editor editor = this.mSettings.edit();
        String string5 = this.key_string(string2, string3);
        String string6 = this.en(string4);
        if (string6 == null) return;
        editor.putString(string5, string6);
        editor.apply();
    }
}

