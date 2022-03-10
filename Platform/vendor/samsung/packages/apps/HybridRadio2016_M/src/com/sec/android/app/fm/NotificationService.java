/**
 *
 */

package com.sec.android.app.fm;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.secutil.Log;

import com.samsung.media.fmradio.FMEventListener;
import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;

/**
 * Notification service will be started when FM player goes into backgroud.
 * 
 * @author vanrajvala
 */
public class NotificationService extends Service {
    public static final String TAG = "NotificationService";
    public static final String ACTION_STOP_SERVICE = "com.sec.android.fm.notification.service.stop";
    public static final String NOTIFICATION_ACTION = "com.sec.android.fm.notification.service.notification";
    public static final String SHOW_NOTIFICATION = "show.notification";

    public static final String ACTION_SHOW_CONTEXTUAL_WIDGET = "com.sec.android.app.fm.intent.action.SHOW_CONTEXTUAL_WIDGET";
    public static final String ACTION_HIDE_CONTEXTUAL_WIDGET = "com.sec.android.app.fm.intent.action.HIDE_CONTEXTUAL_WIDGET";
    private NotificationReceiver mReceiver;
    public static boolean mIsShowNotification = false;
    private AudioManager mAudioManager;
    private RadioPlayer mPlayer;
    private DNSService mDnsService = null;
    private ServiceConnection mDnsServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogDns.v(TAG, "onServiceConnected()");
            mDnsService = ((DNSService.LocalBinder) service).getDNSService();
            mNotiMgr.setDnsService(mDnsService);
        }

        public void onServiceDisconnected(ComponentName name) {
            LogDns.v(TAG, "onServiceDisconnected()");
            mDnsService = null;
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int n;
        if (intent == null) {
        	Log.d(TAG, "onStartCommand - intent is null");
        	n = START_STICKY;
        } else {
            n = super.onStartCommand(intent, flags, startId);
        }

        if (intent != null
                && SHOW_NOTIFICATION.equals(intent
                        .getStringExtra(NotificationService.NOTIFICATION_ACTION))) {
            if (mAudioManager == null) {
                mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            }
            mPlayer = RadioPlayer.getInstance();
            mPlayer.registerListener(mListener);

            /*if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_HAS_INTENNA) {
                System.out.println("Always onStartCommand startForeground in case of INTENNA");
                try {
                    Notification status = mNotiMgr.getNotification();
                    if (status.contentView != null) {
                    	startForegroundthrowException(1, status);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } */
                if (mAudioManager.isWiredHeadsetOn()) {
                    System.out.println("onStartCommand startForeground");
                    try {
                        Notification status = mNotiMgr.getNotification();
                        if (status.contentView != null) {
                        	startForegroundthrowException(1, status);
                            
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("[onStartCommand] WiredHeasetOn is false");
                    mNotiMgr.removeNotification(false);
                }
            if (!mIsShowNotification && mPlayer.isOn()) {
                mIsShowNotification = true;
                Intent i = new Intent(ACTION_SHOW_CONTEXTUAL_WIDGET);
                sendBroadcast(i);
                Log.d("NotificationService", "ACTION_SHOW_CONTEXTUAL_WIDGET");
            }
        }
        Log.v(TAG, "FM NotificationService onStartCommand registered rec");
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(NotificationReceiver.ON_ACTION)) {
                Log.d(TAG, "NotificationReceiver.ON_ACTION");
                sendBroadcast(new Intent(NotificationReceiver.ON_ACTION));
            } else if (intent.getAction().equals(NotificationReceiver.OFF_ACTION)) {
                Log.d(TAG, "NotificationReceiver.OFF_ACTION");
                sendBroadcast(new Intent(NotificationReceiver.OFF_ACTION));
            } else if (intent.getAction().equals(NotificationReceiver.PREV_ACTION)) {
                Log.d(TAG, "NotificationReceiver.PREV_ACTION");
                sendBroadcast(new Intent(NotificationReceiver.PREV_ACTION));
            } else if (intent.getAction().equals(NotificationReceiver.NEXT_ACTION)) {
                Log.d(TAG, "NotificationReceiver.NEXT_ACTION");
                sendBroadcast(new Intent(NotificationReceiver.NEXT_ACTION));
            } else if (intent.getAction().equals(NotificationReceiver.CLOSE_ACTION)) {
                Log.d(TAG, "NotificationReceiver.CLOSE_ACTION");
                sendBroadcast(new Intent(NotificationReceiver.CLOSE_ACTION));
            } else if (intent.getAction().equals(NotificationReceiver.TUNE_ACTION)) {
                Log.d(TAG, "NotificationReceiver.TUNE_ACTION");
                Intent tuneIntent = new Intent(NotificationReceiver.TUNE_ACTION);
                tuneIntent.putExtra(NotificationReceiver.FREQ,
                        intent.getIntExtra(NotificationReceiver.FREQ, RadioPlayer.getDefaultFrequency()));
                sendBroadcast(tuneIntent);
            }
        }

        if (!FMRadioFeature.FEATURE_DISABLEDNS) {
            if (mDnsService == null) {
                mDnsService = DNSService.bindService(NotificationService.this,
                        mDnsServiceConnection);
                mNotiMgr.setDnsService(mDnsService);
            }
        }
        return n;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerBroadcastReceiver();
    }

    FMEventListener mListener = new FMEventListener() {
        public void onOn() {
            if (!mIsShowNotification) {
                mIsShowNotification = true;
                Intent i = new Intent(ACTION_SHOW_CONTEXTUAL_WIDGET);
                sendBroadcast(i);
                Log.d("NotificationService", "ACTION_SHOW_CONTEXTUAL_WIDGET");
            }
        };
    };

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");
        unregisterBroadcastReceiver();
        if (mPlayer != null) {
            mPlayer.unregisterListener(mListener);
            if (!mKeepPlaying) {
                mPlayer.turnOff();
            }
        }
        if (!FMRadioFeature.FEATURE_DISABLEDNS) {
            DNSService.unbindService(this, mDnsServiceConnection);
            mNotiMgr.setDnsService(null);
        }
        super.onDestroy();
    }

    private void registerBroadcastReceiver() {
        if (mReceiver == null) {
            mReceiver = new NotificationReceiver(this);
            registerReceiver(mReceiver, createFilter());
        }
        registerReceiver(mStopServiceReceiver, new IntentFilter(ACTION_STOP_SERVICE));
    }

    private void unregisterBroadcastReceiver() {
        // mReceiver should not be null.
        if (mReceiver != null) {
            mReceiver.terminate();
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        unregisterReceiver(mStopServiceReceiver);
    }

    private boolean mKeepPlaying = false;
    private FMNotificationManager mNotiMgr = FMNotificationManager.getInstance();
    private BroadcastReceiver mStopServiceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mKeepPlaying = true;
            stopSelf();
        }
    };

    private IntentFilter createFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationReceiver.LAUNCH_ACTION);
        filter.addAction(NotificationReceiver.ON_ACTION);
        filter.addAction(NotificationReceiver.OFF_ACTION);
        filter.addAction(NotificationReceiver.NEXT_ACTION);
        filter.addAction(NotificationReceiver.PREV_ACTION);
        filter.addAction(NotificationReceiver.TUNE_ACTION);
        filter.addAction(NotificationReceiver.CLOSE_ACTION);

        return filter;
    }
    
    private void startForegroundthrowException(int i, Notification noti) throws RemoteException {
    	startForeground(i, noti);
    }
}
