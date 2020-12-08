package com.soullan.nettransform.Utils;

import androidx.core.util.Pair;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteUtils {
    public static int toUnsigned(byte val) {
        return val & 0xFF;
    }
    public static Byte getLastByte(int t) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(t).array()[3];
    }
    public static Pair<Byte, Byte> getLastTwoBytes(int t) {
        byte[] res = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(t).array();
        return new Pair<>(res[2], res[3]);
    }
}
