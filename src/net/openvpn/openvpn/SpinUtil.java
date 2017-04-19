/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.widget.ArrayAdapter
 *  android.widget.Spinner
 *  android.widget.SpinnerAdapter
 */
package net.openvpn.openvpn;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import java.util.Arrays;

public class SpinUtil {
    public static int get_spinner_count(Spinner spinner) {
        ArrayAdapter arrayAdapter = (ArrayAdapter)spinner.getAdapter();
        if (arrayAdapter != null) return arrayAdapter.getCount();
        return 0;
    }

    public static String[] get_spinner_list(Spinner spinner) {
        ArrayAdapter arrayAdapter = (ArrayAdapter)spinner.getAdapter();
        if (arrayAdapter == null) {
            return null;
        }
        int n = arrayAdapter.getCount();
        String[] arrstring = new String[n];
        int n2 = 0;
        while (n2 < n) {
            arrstring[n2] = (String)arrayAdapter.getItem(n2);
            ++n2;
        }
        return arrstring;
    }

    public static String get_spinner_list_item(Spinner spinner, int n) {
        ArrayAdapter arrayAdapter = (ArrayAdapter)spinner.getAdapter();
        if (arrayAdapter != null) return (String)arrayAdapter.getItem(n);
        return null;
    }

    public static String get_spinner_selected_item(Spinner spinner) {
        return (String)spinner.getSelectedItem();
    }

    public static void set_spinner_selected_item(Spinner spinner, String string2) {
        if (string2 == null) return;
        String string3 = SpinUtil.get_spinner_selected_item(spinner);
        if (string3 != null) {
            if (string2.equals(string3)) return;
        }
        ArrayAdapter arrayAdapter = (ArrayAdapter)spinner.getAdapter();
        int n = arrayAdapter.getCount();
        int n2 = 0;
        while (n2 < n) {
            if (string2.equals(arrayAdapter.getItem(n2))) {
                spinner.setSelection(n2);
            }
            ++n2;
        }
    }

    public static void show_spinner(Context context, Spinner spinner, String[] arrstring) {
        if (arrstring == null) return;
        Object[] arrobject = SpinUtil.get_spinner_list(spinner);
        if (arrobject != null) {
            if (Arrays.equals(arrstring, arrobject)) return;
        }
				/* 17367048 是一个resource id ,一个xml文件的id,R.layout.xxx 之类的,并且这个xml 中只包含了TextView*/
				/* 原来是 android.R.xxxx 系统默认的一些resource id*/
        ArrayAdapter arrayAdapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, (Object[])arrstring);
        arrayAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item  );
        spinner.setAdapter((SpinnerAdapter)arrayAdapter);
    }
}

