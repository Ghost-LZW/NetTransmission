package com.soullan.nettransform.constant;

import android.Manifest;

import java.util.HashMap;
import java.util.Map;

public class PermissionConstant {
    public static final int RequestInterNetPermission = 1;
    public static final int RequestWriteExternalStorage = 2;
    public static final int RequestReadExternalStorage = 3;
    public static final int RequestGetCodePermission = 4;

    public static final Map<String, Integer> requestCode = new HashMap<>();
    static {
        requestCode.put(Manifest.permission.INTERNET, RequestInterNetPermission);
        requestCode.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, RequestWriteExternalStorage);
        requestCode.put(Manifest.permission.READ_EXTERNAL_STORAGE, RequestReadExternalStorage);
    }

    public static Integer getRequestCode(String permission) {
        return requestCode.get(permission);
    }
}
