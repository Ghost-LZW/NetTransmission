package com.soullan.nettransform.Manager;

import android.app.Activity;
import android.util.Log;
import android.util.Pair;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.soullan.nettransform.constant.PermissionConstant;

import static androidx.core.content.PermissionChecker.PERMISSION_DENIED;

public class PermissionManager {
    private Activity activity;
    private static final String TAG = "Permission Manager";

    public PermissionManager(Activity context) {this.activity = context;}

    public boolean askForPermission(Pair<String, Runnable> permission) {
        if (ContextCompat.checkSelfPermission(activity, permission.first) == PERMISSION_DENIED) {
            Integer code = PermissionConstant.getRequestCode(permission.first);
            if (code == null) {
                Log.e(TAG, "askForPermission: none of permission " + permission.first + " code");
                return false;
            }
            ActivityCompat.requestPermissions(activity, new String[]{permission.first}, code);
        } else new Thread(permission.second).start();
        return true;
    }
    public void askForPermission(String permission, Runnable runnable) {
        askForPermission(new Pair<>(permission, runnable));
    }
    public boolean askForPermissions(Pair<String, Runnable>[] permissions) {
        boolean result = true;
        for (Pair<String, Runnable> permission : permissions) result &= askForPermission(permission);
        return result;
    }
    public void askForPermissions(int userCode, String... permissions) {
        ActivityCompat.requestPermissions(activity, permissions, userCode);
    }
}
