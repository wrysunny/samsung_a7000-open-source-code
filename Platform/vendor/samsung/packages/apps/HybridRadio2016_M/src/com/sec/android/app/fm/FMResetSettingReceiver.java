package com.sec.android.app.fm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;

public class FMResetSettingReceiver extends BroadcastReceiver {
    private static final String TAG = "FMResetSettingReceiver";
    private final String ACTION_SETTINGS_SOFT_RESET = "com.samsung.intent.action.SETTINGS_SOFT_RESET";
    private SharedPreferences mPreferences = null;

    public static final String PREF_FILE = "SettingsPreference";
    public static final String KEY_STORAGE = "storage";
    public static final String KEY_STATION_ID = "stationid";
    public static final String KEY_AF = "af";
    public static final String KEY_AUTO_SWITCH_TO_INTERNET = "autoswitchtointernet";
    public static final String KEY_AUTO_ON_OFF = "autoonoff";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive:action " + intent.getAction() + " " + intent.toString());

        if (ACTION_SETTINGS_SOFT_RESET.equals(intent.getAction())) {
            StorageManager storageManager;
            StorageVolume[] mStorageVolume;

            mPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);

            SharedPreferences.Editor editor;
            editor = mPreferences.edit();

            if (!FMRadioFeature.FEATURE_DISABLEMENURDS){
                editor.putBoolean(KEY_STATION_ID, false);
            }
            
            if (!FMRadioFeature.FEATURE_DISABLEMENUAF) {
                editor.putBoolean(KEY_AF, false);
            }

            if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                editor.putBoolean(KEY_AUTO_SWITCH_TO_INTERNET, false);
            }

            editor.putInt(KEY_AUTO_ON_OFF, 0);
 
            storageManager = (StorageManager) context.getSystemService(
                    Context.STORAGE_SERVICE);

            mStorageVolume = storageManager.getVolumeList();
      
            String defaultStoragePath = mStorageVolume[0].getPath();

             //String defaultStoragePath = "/storage/emulated/0";
            editor.putString(KEY_STORAGE, defaultStoragePath);

            editor.commit();
        }
    }
}
