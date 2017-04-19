/*
 * Decompiled with CFR 0_115.
 */
package net.openvpn.openvpn;

public class CPUUsage {
    private double end_time = 0.0;
    private boolean halted = false;
    private double start_time = CPUUsage.cpu_usage();

    private static native double cpu_usage();

    public void stop() {
        if (this.halted) return;
        this.end_time = CPUUsage.cpu_usage();
        this.halted = true;
    }

    public double usage() {
        double d;
        if (this.halted) {
            d = this.end_time;
            return d - this.start_time;
        }
        d = CPUUsage.cpu_usage();
        return d - this.start_time;
    }
}

