package com.sec.android.app.fm;

import com.sec.android.app.dns.DNSService;
import com.sec.android.app.fm.data.ChannelStore;
import com.sec.android.app.fm.util.FMUtil;
import com.sec.android.app.fm.widget.FMRadioProvider;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.content.res.Configuration;

import java.util.Locale;

public class RadioApplication extends Application {
    private static final String _TAG = "RadioApplication";
    public static final String PREF_FILE = "localpreference";
    private static final String PREF_INITIAL_FREQ = "initialfreq";
    private static final String PREF_IS_DNS_WIFI_CHECKED = "wifiwarningdonotshow";
    private static final String PREF_IS_INITIAL_ACCESS = "initialAccess";
    private static final String PREF_IS_RTPLUS_ENABLED = "rtplus";

    private static RadioApplication sInstance = null;
    private Locale mLocale;

    public static int getInitialFrequency() {
        int defaultFreq = RadioPlayer.getDefaultFrequency();
        int freq = -1;
        SharedPreferences pref = sInstance.getSharedPreferences(RadioApplication.PREF_FILE,
                Context.MODE_PRIVATE);
        try {
            freq = pref.getInt(PREF_INITIAL_FREQ, defaultFreq);
        } catch (ClassCastException e) {
            freq = (int) (pref.getFloat(PREF_INITIAL_FREQ,
                    "88.3".equals(FMRadioFeature.FEATURE_DEFAULTCHANNEL) ? 88.3f : 87.5f) * 100);
        }
        return (freq <= 0) ? defaultFreq : freq;
    }

    public static RadioApplication getInstance() {
        return sInstance;
    }

    public static boolean isInitialAccess() {
        return sInstance.getSharedPreferences(RadioApplication.PREF_FILE, Context.MODE_PRIVATE)
                .getBoolean(RadioApplication.PREF_IS_INITIAL_ACCESS, true);
    }

    public static boolean isRtPlusEnabled() {
        return sInstance.getSharedPreferences(RadioApplication.PREF_FILE, Context.MODE_PRIVATE)
                .getBoolean(RadioApplication.PREF_IS_RTPLUS_ENABLED, false);
    }

    public static boolean isWifiChecked() {
        return sInstance.getSharedPreferences(RadioApplication.PREF_FILE, Context.MODE_PRIVATE)
                .getBoolean(RadioApplication.PREF_IS_DNS_WIFI_CHECKED, false);
    }

    public static void setInitialAccess(boolean value) {
        sInstance.getSharedPreferences(RadioApplication.PREF_FILE, Context.MODE_PRIVATE).edit()
                .putBoolean(RadioApplication.PREF_IS_INITIAL_ACCESS, value).commit();
    }

    public static void setInitialFrequency(final int newFreq) {
        if (newFreq <=0 )
         return;
        
        sInstance.getSharedPreferences(RadioApplication.PREF_FILE, Context.MODE_PRIVATE).edit()
                .putInt(RadioApplication.PREF_INITIAL_FREQ, newFreq).commit();
        Log.d(_TAG, "setInitialFrequency() - put:" + Log.filter(newFreq));
    }

    public static void setRtPlusEnabled(boolean value) {
        sInstance.getSharedPreferences(RadioApplication.PREF_FILE, Context.MODE_PRIVATE).edit()
                .putBoolean(RadioApplication.PREF_IS_RTPLUS_ENABLED, value).commit();
    }

    public static void setWifiChecked(boolean value) {
        sInstance.getSharedPreferences(RadioApplication.PREF_FILE, Context.MODE_PRIVATE).edit()
                .putBoolean(RadioApplication.PREF_IS_DNS_WIFI_CHECKED, value).commit();
    }

    private ServiceConnection mDnsServiceConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName cName, IBinder service) {
            Log.v(_TAG, "onServiceConnected()");
        }

        public void onServiceDisconnected(ComponentName cName) {
            Log.v(_TAG, "onServiceDisconnected()");
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(_TAG, "onCreate() - start");

        sInstance = this;
        if (!FMRadioFeature.FEATURE_DISABLEDNS) {
            DNSService.bindService(this, mDnsServiceConn);
        }
        ChannelStore channelStore = ChannelStore.getInstance();
        channelStore.initialize(this);
        channelStore.load();
        RadioPlayer.getInstance().initialize(this);
        FMNotificationManager.getInstance().initialize(this);
        mLocale = getResources().getConfiguration().locale;
        FMRadioProvider.loadSpanYMap(this);

        // IntetFilter with Action
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_USER_PRESENT);// Keyguard is GONE


        // register BroadcastReceiver and IntentFilter
        registerReceiver(mReceiver, intentFilter);
        Log.v(_TAG, "onCreate() - finish");
    }

    @Override
    public void onTerminate() {
        // register BroadcastReceiver and IntentFilter
        unregisterReceiver(mReceiver);
        super.onTerminate();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.v(_TAG, "onConfigurationChanged");

        if (!mLocale.equals(newConfig.locale)) {
            FMNotificationManager.getInstance().initialize(this);
        }
        mLocale = newConfig.locale;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                // Screen is off
                Log.d(_TAG, "ACTION_SCREEN_OFF");
                MainActivity.mIsScreenOff = true;
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                // Screen is on
                Log.d(_TAG, "ACTION_SCREEN_ON");
                MainActivity.mIsScreenOff = false;
                FMNotificationManager.getInstance().registerNotification(false);
            } else if (action.equals(Intent.ACTION_USER_PRESENT)) {
                // The user has unlocked the screen. Enabled!
                Log.d(_TAG, "ACTION_USER_PRESENT");
                if (FMUtil.isTopActivity(getApplicationContext()))
                    FMNotificationManager.getInstance().removeNotification(true);
            }
        }
    };
}
