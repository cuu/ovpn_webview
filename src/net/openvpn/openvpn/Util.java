/*
 * Decompiled with CFR 0_115.
 */
package net.openvpn.openvpn;

public class Util {
    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] arrby) {
        char[] arrc = new char[2 * arrby.length];
        int n = 0;
        while (n < arrby.length) {
            int n2 = 255 & arrby[n];
            arrc[n * 2] = hexArray[n2 >>> 4];
            arrc[1 + n * 2] = hexArray[n2 & 15];
            ++n;
        }
        return new String(arrc);
    }
}

