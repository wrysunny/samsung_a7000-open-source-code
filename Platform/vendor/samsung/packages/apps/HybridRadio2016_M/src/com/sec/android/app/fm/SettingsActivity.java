package com.sec.android.app.fm;

import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.fm.ui.SpinnerPreference;
import com.sec.android.app.fm.util.FMUtil;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.secutil.Log;
import android.view.MenuItem;
import android.view.WindowManager;

import java.io.File;
import java.util.ArrayList;

public class SettingsActivity extends Activity {

    private static final String TAG = "SettingsActivity";

    public static final String PREF_FILE = "SettingsPreference";

    public static final String KEY_STORAGE = "storage";
    public static final String KEY_STATION_ID = "stationid";
    public static final String KEY_AF = "af";
    public static final String KEY_AUTO_SWITCH_TO_INTERNET = "autoswitchtointernet";
    public static final String KEY_AUTO_ON_OFF = "autoonoff";

    public static final String STORAGE_MOUNTED = "mounted";

    // For intenna test ----
    public static final String KEY_FACTORY_RSSI = "factoryrssi";
    public static final int FACTORY_RSSI = -70;
    // ---- For intenna test

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");

        setContentView(R.layout.settings);

        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.samsungFlags |= WindowManager.LayoutParams.SAMSUNG_FLAG_ENABLE_STATUSBAR_OPEN_BY_NOTIFICATION;
        getWindow().setAttributes(lp);

        Log.secD(TAG, "onCreate end");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void activateTurnOffAlarm() {
        RadioPlayer player = RadioPlayer.getInstance();
        if (!player.isOn())
            return;
        Context context = RadioApplication.getInstance();
        int valAutoOff = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).getInt(
                KEY_AUTO_ON_OFF, 0);
        if (valAutoOff == 0)
            return;
        long time = getAlarmTime(valAutoOff);
        Log.d(TAG, "Set the alarm for :" + time);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context,
                AlarmReceiver.class), 0);
        AlarmManager mAlarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        mAlarmManager.cancel(pendingIntent);
        mAlarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                + time, pendingIntent);
    }

    private static long getAlarmTime(int valAutoOff) {
        Log.d(TAG, "setOffAlarm val:" + valAutoOff);
        int min = 0;

        if (valAutoOff == 1) {
            min = 15;
        } else if (valAutoOff == 2) {
            min = 30;
        } else if (valAutoOff == 3) {
            min = 60;
        } else if (valAutoOff == 4) {
            min = 120;
        }

        return min * 60 * 1000L;
    }

    public static class SettingsFragment extends PreferenceFragment implements
            OnSharedPreferenceChangeListener {

        private static final String TAG = "SettingsFragment";
        private static final int STORAGE_DEFAULT_VALUE = 0;
        private static final String DEFAULT_STORAGE = Environment.getExternalStorageDirectory()
                .getAbsolutePath();

        private RadioPlayer mPlayer = null;
        private SharedPreferences mPreferences = null;
        private SharedPreferences mScreenPreferences = null;
        private SpinnerPreference mRecordingLocation = null;
        private SwitchPreference mStationId = null;
        private SwitchPreference mAutoSwitchToInternet = null;
        private SwitchPreference mAf = null;
        private SpinnerPreference mAutoOnOff = null;

        private String RecordingLocationValue = "";
        StorageManager mStorageManager;
        StorageVolume[] mStorageVolume;

        private DNSService mDnsBoundService = null;

        public long GSIM_LOGGING_ON = 1000;
        public long GSIM_LOGGING_OFF = 0;

        public long getGSIMLoggingValue(boolean isOn) {
            if (isOn) {
                return GSIM_LOGGING_ON;
            } else {
                return GSIM_LOGGING_OFF;
            }
        }

        private OnPreferenceChangeListener mPreferenceChangeListener = new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object object) {
                // TODO Auto-generated method stub
                SharedPreferences.Editor editor = mPreferences.edit();
                String key = preference.getKey();
                if (key.equals(KEY_STATION_ID)) {
                    boolean value = !((SwitchPreference) preference).isChecked();
                    editor.putBoolean(KEY_STATION_ID, value);
                    editor.commit();
                    mPlayer.applyRds();
                    FMUtil.insertGSIMLog(getContext(), "RDST", getGSIMLoggingValue(value));
                } else if (key.equals(KEY_AF)) {
                    boolean value = !((SwitchPreference) preference).isChecked();
                    editor.putBoolean(KEY_AF, value);
                    editor.commit();
                    mPlayer.applyAf();
                    FMUtil.insertGSIMLog(getContext(), "AFRE", getGSIMLoggingValue(value));
                } else if (key.equals(KEY_AUTO_SWITCH_TO_INTERNET)) {
                    boolean value = !((SwitchPreference) preference).isChecked();
                    editor.putBoolean(KEY_AUTO_SWITCH_TO_INTERNET, value);
                    editor.commit();
                    if (mDnsBoundService != null) {
                        mDnsBoundService.setAutoSwitching(value);
                    } else {
                        LogDns.e(TAG, "mDnsBoundService is null!");
                    }
                    FMUtil.insertGSIMLog(getContext(), "DNSS", getGSIMLoggingValue(value));
                }
                editor.commit();
                return true;
            }
        };

        private ServiceConnection mDNSServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName arg0, IBinder service) {
                LogDns.d(TAG, "onDNSServiceConnected()");
                mDnsBoundService = ((DNSService.LocalBinder) service).getDNSService();
            }

            public void onServiceDisconnected(ComponentName cName) {
                LogDns.d(TAG, "onDNSServiceDisconnected()");
                mDnsBoundService = null;
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Log.v(TAG, "onCreate()");

            mPlayer = RadioPlayer.getInstance();

            addPreferencesFromResource(R.xml.preferences);

            mStorageManager = (StorageManager) getActivity().getSystemService(
                    Context.STORAGE_SERVICE);

            registerBroadcastReceiverSDCard(true);

            int mode = Context.MODE_PRIVATE;
            mPreferences = getActivity().getSharedPreferences(PREF_FILE, mode);
            mRecordingLocation = (SpinnerPreference) findPreference(KEY_STORAGE);
            String[] storageEntries = getStorageVolumeList();
            String[] storageEntryValues = getStorageVolumePaths();
            mRecordingLocation.initSpinnerPreference(storageEntries, storageEntryValues, storageEntryValues[0]);

            if (!FMRadioFeature.FEATURE_DISABLEMENURDS) {
                mStationId = (SwitchPreference) findPreference(KEY_STATION_ID);
                mStationId.setOnPreferenceChangeListener(mPreferenceChangeListener);
            } else {
                getPreferenceScreen().removePreference(findPreference(KEY_STATION_ID));
            }

            if (!FMRadioFeature.FEATURE_DISABLEMENUAF) {
                mAf = (SwitchPreference) findPreference(KEY_AF);
                mAf.setOnPreferenceChangeListener(mPreferenceChangeListener);
            } else {
                getPreferenceScreen().removePreference(findPreference(KEY_AF));
            }

            if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                if (mDnsBoundService == null) {
                    mDnsBoundService = DNSService.bindService(getActivity(), mDNSServiceConnection);
                }
                mAutoSwitchToInternet = (SwitchPreference) findPreference(KEY_AUTO_SWITCH_TO_INTERNET);
                mAutoSwitchToInternet.setOnPreferenceChangeListener(mPreferenceChangeListener);
            } else {
                getPreferenceScreen().removePreference(findPreference(KEY_AUTO_SWITCH_TO_INTERNET));
            }

            mAutoOnOff = (SpinnerPreference) findPreference(KEY_AUTO_ON_OFF);
            String[] entries = getResources().getStringArray(R.array.auto_off_option);
            String[] entryValues = getResources().getStringArray(R.array.auto_off_option);
            mAutoOnOff.initSpinnerPreference(entries, entryValues, entryValues[0]);

            setInitialValues();
            mScreenPreferences = getPreferenceScreen().getSharedPreferences();
            if (mScreenPreferences != null)
                mScreenPreferences.registerOnSharedPreferenceChangeListener(this);

            Log.secD(TAG, "onCreate end");
        }

        @Override
        public void onResume() {
            super.onResume();
            Log.d(TAG, "onResume");
            if (!FMUtil.isKeyguardLocked(getActivity().getApplicationContext()))
                FMNotificationManager.getInstance().removeNotification(true);
            int ivalue = mPreferences.getInt(KEY_AUTO_ON_OFF, 0);
            String[] entries = getResources().getStringArray(R.array.auto_off_option);
            mAutoOnOff.setSummary(entries[ivalue]);
            mAutoOnOff.setSelection(mAutoOnOff.findIndexOfValue(entries[ivalue]));
            Log.secD(TAG, "onResume end");
        }

        @Override
        public void onStop() {
            Log.secD("SettingsFragment", "onStop");
            FMNotificationManager.getInstance().registerNotification(false);
            super.onStop();
        }

        @Override
        public void onDestroy() {
            Log.secD(TAG, "onDestroy");
            super.onDestroy();
            if (mScreenPreferences != null)
                mScreenPreferences.unregisterOnSharedPreferenceChangeListener(this);

            registerBroadcastReceiverSDCard(false);

            if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                if (mDnsBoundService != null) {
                    DNSService.unbindService(getActivity(), mDNSServiceConnection);
                    mDnsBoundService = null;
                }
            }
        }

        private void setInitialValues() {
            mStorageVolume = mStorageManager.getVolumeList();

            initRecordingLocation();

            if (!FMRadioFeature.FEATURE_DISABLEMENURDS) {
                mStationId.setChecked(mPreferences.getBoolean(KEY_STATION_ID, false));
            }
            if (!FMRadioFeature.FEATURE_DISABLEMENUAF) {
                mAf.setChecked(mPreferences.getBoolean(KEY_AF, false));
            }

            if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                mAutoSwitchToInternet.setChecked(mPreferences.getBoolean(
                        KEY_AUTO_SWITCH_TO_INTERNET, false));
            }

            // set auto on/off settings
            int ivalue = mPreferences.getInt(KEY_AUTO_ON_OFF, 0);
            String[] entries = getResources().getStringArray(R.array.auto_off_option);
            mAutoOnOff.setSummary(entries[ivalue]);
            mAutoOnOff.setSelection(mAutoOnOff.findIndexOfValue(entries[ivalue]));
        }

        private void initRecordingLocation() {
            String[] recordingLocation = getStorageVolumeList();

            if (recordingLocation == null) {
                Log.e(TAG, "initRecordingLocation :: recordingLocation is null");
                return;
            }

            mRecordingLocation.setEntries(recordingLocation);
            mRecordingLocation.setEntryValues(getStorageVolumePaths());
            if ((recordingLocation != null) && (recordingLocation.length > 1)) {
                if (MainActivity._instance != null && MainActivity.mIsRecording) {
                    mRecordingLocation.setEnabled(false);
                } else {
                    mRecordingLocation.setEnabled(true);
                }
            } else {
                mRecordingLocation.setEnabled(false);
            }

            String strRecordingLocation = mPreferences.getString(KEY_STORAGE, DEFAULT_STORAGE);
            if (!setRecordingLocation(strRecordingLocation)) {
                SharedPreferences.Editor editor = mPreferences.edit();
                String defaultStr = getStorageVolumePath(STORAGE_DEFAULT_VALUE);
                editor.putString(KEY_STORAGE, defaultStr);
                editor.commit();
            }
        }

        private String[] getStorageVolumeList() {
            ArrayList<String> arrayList = new ArrayList<String>();
            if (mStorageVolume == null) {
                mStorageVolume = mStorageManager.getVolumeList();
            }
            for (StorageVolume item : mStorageVolume) {
                String path = item.getPath();
                if (STORAGE_MOUNTED.equals(Environment.getExternalStorageState(new File(path)))
                    &&(!item.getSubSystem().equals("private"))) {
                    arrayList.add(path);
                }
            }
            String[] retValue;
            if (arrayList.size() == 0) {
                retValue = new String[1];
                retValue[0] = getString(R.string.setting_phone);
            } else {
                retValue = new String[arrayList.size()];
                for (int i = 0; i < retValue.length; i++) {
                    if (i == 0) {
                        retValue[i] = getString(R.string.setting_phone);
                    } else {
                        if (retValue.length == 2) {
                            retValue[i] = getString(R.string.setting_memory_card);
                        } else {
                            retValue[i] = getString(R.string.setting_memory_card) + i;
                        }
                    }
                }
            }
            return retValue;
        }

        private String[] getStorageVolumePaths() {
            ArrayList<String> arrayList = new ArrayList<String>();
            if (mStorageVolume == null) {
                mStorageVolume = mStorageManager.getVolumeList();
            }
            for (StorageVolume item : mStorageVolume) {
                String path = item.getPath();
                if (STORAGE_MOUNTED.equals(Environment.getExternalStorageState(new File(path)))
                    &&(!item.getSubSystem().equals("private"))) {
                    arrayList.add(path);
                }
            }

            String[] retValue = new String[arrayList.size()];
            for (int i = 0; i < retValue.length; i++) {
                retValue[i] = arrayList.get(i);
            }

            return retValue;
        }

        private int getStorageVolumeCount() {
            Log.d(TAG, "getStorageVolumeCount() is called.");
            int count = 0;
            try {
                if (mStorageVolume == null) {
                    mStorageVolume = mStorageManager.getVolumeList();
                }
                for (StorageVolume item : mStorageVolume) {
                    String path = item.getPath();
                    if (STORAGE_MOUNTED.equals(Environment.getExternalStorageState(new File(path)))
                        &&(!item.getSubSystem().equals("private"))) {
                        count++;
                    }
                }
            } catch (StackOverflowError e) {
                Log.w(TAG, "java.lang.StackOverflowError");
                count = 0;
            }

            return count;
        }

        private boolean setRecordingLocation(String strValue) {
            int length = getStorageVolumeCount();
            int value = mRecordingLocation.findIndexOfValue(strValue);
            boolean ret = true;
            if (value == -1 || value >= length) {
                value = STORAGE_DEFAULT_VALUE;
                ret = false;
            }
            CharSequence[] charSequence = getStorageVolumeList();
            if (charSequence != null) {
                mRecordingLocation.setSummary(charSequence[value].toString());
                mRecordingLocation.setSelection(value);
            }

            return ret;
        }

        private String getStorageVolumePath(int index) {
            if (mStorageVolume == null) {
                mStorageVolume = mStorageManager.getVolumeList();
            }
            return mStorageVolume[index].getPath();
        }

        private void registerBroadcastReceiverSDCard(boolean register) {
            if (register) {
                IntentFilter iFilter = new IntentFilter();
                iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
                iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
                iFilter.addDataScheme("file");
                getActivity().registerReceiver(mBroadcastReceiverSDCard, iFilter);
            } else {
                if (mBroadcastReceiverSDCard != null) {
                    getActivity().unregisterReceiver(mBroadcastReceiverSDCard);
                    mBroadcastReceiverSDCard = null;
                }
            }
        }

        private BroadcastReceiver mBroadcastReceiverSDCard = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(Intent.ACTION_MEDIA_MOUNTED)
                        || action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                    setInitialValues();
                }
            }
        };

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            SharedPreferences.Editor editor = mPreferences.edit();
            if (key.equals(KEY_AUTO_ON_OFF)) {
                String value = mAutoOnOff.getValue();
                int index = mAutoOnOff.findIndexOfValue(value);
                Log.d(TAG, "auto off :" + index);
                editor.putInt(KEY_AUTO_ON_OFF, index);
                mAutoOnOff.setSummary(value);
                editor.commit();
                /*if (index == 0 && mPlayer.isOn()) {
                    RadioToast
                            .showToast(
                                    getActivity(),
                                    getString(R.string.toast_disabled,
                                            getString(R.string.setting_autooff)),
                                    Toast.LENGTH_SHORT);
                }*/
                activateTurnOffAlarm();
                FMUtil.insertGSIMLog(getContext(), "ATOF", getGSIMLoggingValue(index!=0));
            } else if (key.equals(KEY_STORAGE)) {
                String strValue = mRecordingLocation.getValue();
                Log.d(TAG, "Recording Location :" + strValue);

                if (RecordingLocationValue.equals(strValue)) {
                    Log.d(TAG, "RecordingLocation is same");
                    return;
                } else {
                    RecordingLocationValue = strValue;
                }

                if (setRecordingLocation(strValue)) {
                    editor.putString(KEY_STORAGE, strValue);
                } else {
                    String defaultStr = getStorageVolumePath(STORAGE_DEFAULT_VALUE);
                    editor.putString(KEY_STORAGE, defaultStr);
                }
                editor.commit();
            }
        }
    }

}
