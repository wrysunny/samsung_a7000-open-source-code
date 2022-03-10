package com.sec.android.app.fm.util;

import java.util.ArrayList;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import com.sec.android.app.fm.Log;

public class FMPermissionUtil {
    private static final String TAG = "FMPermissionUtil";

    public static final int FM_PERMISSION_REQUEST_RECORD_AUDIO = 1;
    public static final int FM_PERMISSION_REQUEST_RECORDINGS_LIST = 2;
    public static final int FM_PERMISSION_REQUEST_RECORDINGS_LIST_RESUME = 3;
	public static final int FM_PERMISSION_REQUEST_RESET_APP_PREFERENCES = 4;
    public static final int FM_PERMISSION_REQUEST_REFRESH_NOTIFICATION = 5;
    public static final int FM_PERMISSION_REQUEST_BACKUP_RESTORE = 10;

    public static final String[] FM_PERMISSION_RECORDING = {Manifest.permission.RECORD_AUDIO,
                                                            Manifest.permission.READ_EXTERNAL_STORAGE,
                                                            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    public static final String[] FM_PERMISSION_RECORDINGS_LIST = {Manifest.permission.READ_EXTERNAL_STORAGE,
                                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private static String[] convertPermission(int permissionType) {
        String[] permissions={};

        switch(permissionType) {
        case FM_PERMISSION_REQUEST_RECORD_AUDIO:
		case FM_PERMISSION_REQUEST_RESET_APP_PREFERENCES:
            permissions = FM_PERMISSION_RECORDING;
            break;
        case FM_PERMISSION_REQUEST_RECORDINGS_LIST:
        case FM_PERMISSION_REQUEST_RECORDINGS_LIST_RESUME:
        case FM_PERMISSION_REQUEST_BACKUP_RESTORE:
        case FM_PERMISSION_REQUEST_REFRESH_NOTIFICATION:
            permissions = FM_PERMISSION_RECORDINGS_LIST;
            break;
        default:
            break;
        }

        return permissions;
    }

    public static boolean hasPermission(Context context, String permission) {
        Log.d(TAG, "hasPermission : permission = " + permission);

        return (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean hasPermission(Context context, int permissionType) {
        Log.d(TAG, "hasPermission string[] ");

        String[] permissions = convertPermission(permissionType);

        for (String permission : permissions) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }
/*
    public static boolean shouldShowRequestPermission(Activity activity, String permission) {
        Log.d(TAG, "shouldShowRequestPermission : permission = " + permission);

        return activity.shouldShowRequestPermissionRationale(permission);
    }
*/
    public static void requestPermission(Activity activity, int permissionType) {
        Log.d(TAG, "requestPermission");

        String[] permissions = convertPermission(permissionType);
        ArrayList<String> unGrantedPermissions = new ArrayList<>();

        for (String permission : permissions) {
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                unGrantedPermissions.add(permission);
            }
        }

        activity.requestPermissions(unGrantedPermissions.toArray(new String[unGrantedPermissions.size()]), permissionType);
    }

    public static boolean verifyPermissions(int[] grantResults) {
        Log.d(TAG, "verifyPermissions");

        for(int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
