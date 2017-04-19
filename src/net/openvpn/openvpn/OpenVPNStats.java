/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  android.content.Context
 *  android.os.Bundle
 *  android.os.Handler
 *  android.util.Log
 *  android.view.View
 *  android.view.ViewGroup
 *  android.view.ViewGroup$LayoutParams
 *  android.widget.AbsListView
 *  android.widget.AbsListView$LayoutParams
 *  android.widget.BaseAdapter
 *  android.widget.GridView
 *  android.widget.ListAdapter
 *  android.widget.TextView
 */
package net.openvpn.openvpn;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import net.openvpn.openvpn.ClientAPI_LLVector;
import net.openvpn.openvpn.OpenVPNClientBase;
import net.openvpn.openvpn.OpenVPNService;

public class OpenVPNStats
extends OpenVPNClientBase {
    private static final String TAG = "OpenVPNClientStats";
    private StatsAdapter adapter;
    private GridView gridview;
    private Handler stats_timer_handler = new Handler();
    private Runnable stats_timer_task;

    public OpenVPNStats() {
        this.stats_timer_task = new Runnable(){

            @Override
            public void run() {
                OpenVPNStats.this.show_stats();
                OpenVPNStats.this.schedule_stats();
            }
        };
    }

    private void cancel_stats() {
        this.stats_timer_handler.removeCallbacks(this.stats_timer_task);
    }

    private void schedule_stats() {
        this.cancel_stats();
        this.stats_timer_handler.postDelayed(this.stats_timer_task, 1000);
    }

    private void show_stats() {
        ClientAPI_LLVector clientAPI_LLVector = this.get_stat_values_full();
        if (clientAPI_LLVector == null) return;
        this.adapter.update_stats(clientAPI_LLVector);
    }

    private void stop() {
        this.cancel_stats();
        this.doUnbindService();
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(R.layout.stats);
        this.adapter = new StatsAdapter();
        this.gridview = (GridView)this.findViewById(R.id.stats_grid);
        this.gridview.setAdapter((ListAdapter)this.adapter);
        this.doBindService();
    }

    protected void onDestroy() {
        Log.d((String)"OpenVPNClientStats", (String)"STATS: onDestroy");
        this.stop();
        super.onDestroy();
    }

    protected void onStart() {
        Log.d((String)"OpenVPNClientStats", (String)"STATS: onStart");
        this.show_stats();
        this.schedule_stats();
        super.onStart();
    }

    protected void onStop() {
        Log.d((String)"OpenVPNClientStats", (String)"STATS: onStop");
        this.cancel_stats();
        super.onStop();
    }

    @Override
    protected void post_bind() {
        Log.d((String)"OpenVPNClientStats", (String)"STATS: post_bind");
        this.show_stats();
    }

    private static class Stat {
        public int name_idx;
        public long value;

        private Stat() {
        }
    }

    private class StatsAdapter
    extends BaseAdapter {
        private String[] stat_names;
        private ArrayList<Stat> stats;

        StatsAdapter() {
            this.stat_names = OpenVPNService.stat_names();
        }

        private String text_at_pos(int n) {
            int n2 = n / 2;
            int n3 = n % 2;
            Stat stat = this.stats.get(n2);
            if (n3 == 0) {
                return this.stat_names[stat.name_idx];
            }
            Object[] arrobject = new Object[]{stat.value};
            return String.format("%d", arrobject);
        }

        public int getCount() {
            if (this.stats == null) return 0;
            return 2 * this.stats.size();
        }

        public Object getItem(int n) {
            return null;
        }

        public long getItemId(int n) {
            return 0;
        }

        public View getView(int n, View view, ViewGroup viewGroup) {
            TextView textView;
            if (view == null) {
                textView = new TextView((Context)OpenVPNStats.this);
                textView.setLayoutParams((ViewGroup.LayoutParams)new AbsListView.LayoutParams(-2, -2));
            } else {
                textView = (TextView)view;
            }
            textView.setText((CharSequence)this.text_at_pos(n));
            return textView;
        }

        public void update_stats(ClientAPI_LLVector clientAPI_LLVector) {
            ArrayList<Stat> arrayList = new ArrayList<Stat>();
            int n = 0;
            do {
                if (n >= this.stat_names.length) {
                    this.stats = arrayList;
                    this.notifyDataSetChanged();
                    return;
                }
                long l = clientAPI_LLVector.get(n);
                if (l > 0) {
                    Stat stat = new Stat();
                    stat.name_idx = n;
                    stat.value = l;
                    arrayList.add(stat);
                }
                ++n;
            } while (true);
        }
    }

}

