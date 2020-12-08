package com.soullan.nettransform.Utils;

public class ArrayUtils {
    public static byte[] toPrimitive(Byte[] res) {
        byte[] result = new byte[res.length];
        for (int i = 0; i < res.length; i++)
            result[i] = res[i];
        return result;
    }
    public static Byte[] unPrimitive(byte[] res) {
        Byte[] result = new Byte[res.length];
        for (int i = 0; i < res.length; i++)
            result[i] = res[i];
        return result;
    }
    public static byte[] addArray(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static String toHexString(byte[] bs) {
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}
