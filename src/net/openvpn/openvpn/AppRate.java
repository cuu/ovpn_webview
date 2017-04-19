package net.openvpn.openvpn;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.util.Log;

public class AppRate implements OnCancelListener, OnClickListener {
    public static final String PREF_DATE_FIRST_LAUNCH = "date_firstlaunch";
    public static final String PREF_DONT_SHOW_AGAIN = "dont_show_again";
    public static final String PREF_LAUNCH_COUNT = "launch_count";
    public static final String SHARED_PREFS_NAME = "apprate_prefs";
    private static final String TAG = "AppRate";
    private Builder dialogBuilder;
    private Activity hostActivity;
    private long minDaysUntilPrompt;
    private long minLaunchesUntilPrompt;
    private SharedPreferences preferences;

    public AppRate(Activity hostActivity) {
        this.dialogBuilder = null;
        this.minLaunchesUntilPrompt = 10;
        this.minDaysUntilPrompt = 7;
        this.hostActivity = hostActivity;
        this.preferences = hostActivity.getSharedPreferences(SHARED_PREFS_NAME, 0);
    }

    public AppRate setMinLaunchesUntilPrompt(long minLaunchesUntilPrompt) {
        this.minLaunchesUntilPrompt = minLaunchesUntilPrompt;
        return this;
    }

    public AppRate setMinDaysUntilPrompt(long minDaysUntilPrompt) {
        this.minDaysUntilPrompt = minDaysUntilPrompt;
        return this;
    }

    public AppRate setCustomDialog(Builder customBuilder) {
        this.dialogBuilder = customBuilder;
        return this;
    }

    public static void reset(Context context) {
        context.getSharedPreferences(SHARED_PREFS_NAME, 0).edit().clear().commit();
        Log.d(TAG, "reset");
    }

    public void init() {
        Log.d(TAG, "init");
        if (!this.preferences.getBoolean(PREF_DONT_SHOW_AGAIN, false)) {
            Editor editor = this.preferences.edit();
            long launch_count = this.preferences.getLong(PREF_LAUNCH_COUNT, 0) + 1;
            editor.putLong(PREF_LAUNCH_COUNT, launch_count);
            Long date_firstLaunch = Long.valueOf(this.preferences.getLong(PREF_DATE_FIRST_LAUNCH, 0));
            if (date_firstLaunch.longValue() == 0) {
                date_firstLaunch = Long.valueOf(System.currentTimeMillis());
                editor.putLong(PREF_DATE_FIRST_LAUNCH, date_firstLaunch.longValue());
            }
            if (launch_count >= this.minLaunchesUntilPrompt && System.currentTimeMillis() >= date_firstLaunch.longValue() + (this.minDaysUntilPrompt * 86400000)) {
                if (this.dialogBuilder != null) {
                    showDialog(this.dialogBuilder);
                } else {
                    showDefaultDialog();
                }
            }
            editor.commit();
        }
    }

    private void showDefaultDialog() {
        Log.d(TAG, "create default dialog");
        String appName = getApplicationName(this.hostActivity.getApplicationContext());
        String title = String.format(resString(R.string.apprate_title), new Object[]{appName});
        String message = String.format(resString(R.string.apprate_message), new Object[]{appName});
        String rate = resString(R.string.apprate_rate);
        new Builder(this.hostActivity).setTitle(title).setMessage(message).setPositiveButton(rate, this).setNegativeButton(resString(R.string.apprate_dismiss), this).setNeutralButton(resString(R.string.apprate_remind_later), this).setOnCancelListener(this).create().show();
    }

    private void showDialog(Builder builder) {
        Log.d(TAG, "create custom dialog");
        AlertDialog dialog = builder.create();
        dialog.show();
        String remindLater = (String) dialog.getButton(-3).getText();
        String dismiss = (String) dialog.getButton(-2).getText();
        dialog.setButton(-1, (String) dialog.getButton(-1).getText(), this);
        dialog.setButton(-3, remindLater, this);
        dialog.setButton(-2, dismiss, this);
        dialog.setOnCancelListener(this);
    }

    public void onCancel(DialogInterface dialog) {
        Editor editor = this.preferences.edit();
        editor.putLong(PREF_DATE_FIRST_LAUNCH, System.currentTimeMillis());
        editor.putLong(PREF_LAUNCH_COUNT, 0);
        editor.commit();
    }

    public void onClick(DialogInterface dialog, int which) {
        Editor editor = this.preferences.edit();
        switch (which) {
            case -3:
                editor.putLong(PREF_DATE_FIRST_LAUNCH, System.currentTimeMillis());
                editor.putLong(PREF_LAUNCH_COUNT, 0);
                break;
            case -2:
                editor.putBoolean(PREF_DONT_SHOW_AGAIN, true);
                break;
            case -1:
                this.hostActivity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + this.hostActivity.getPackageName())));
                editor.putBoolean(PREF_DONT_SHOW_AGAIN, true);
                break;
        }
        editor.commit();
        dialog.dismiss();
    }

    private static final String getApplicationName(Context context) {
        ApplicationInfo applicationInfo;
        PackageManager packageManager = context.getPackageManager();
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            applicationInfo = null;
        }
        return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "(unknown)");
    }

    private String resString(int res_id) {
        return this.hostActivity.getResources().getString(res_id);
    }
}
