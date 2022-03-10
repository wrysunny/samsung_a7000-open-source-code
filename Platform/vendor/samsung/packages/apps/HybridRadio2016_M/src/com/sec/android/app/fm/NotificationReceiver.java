package com.sec.android.app.fm;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore.Audio;
import android.provider.Settings;
import android.widget.Toast;

import com.samsung.media.fmradio.FMEventListener;
import com.samsung.media.fmradio.FMPlayer;
import com.samsung.media.fmradio.FMPlayerException;
import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.RadioDNSServiceDataIF;
import com.sec.android.app.fm.data.Channel;
import com.sec.android.app.fm.data.ChannelStore;
import com.sec.android.app.fm.listplayer.FMListPlayerService;
import com.sec.android.app.fm.util.FMUtil;
import com.sec.android.emergencymode.EmergencyConstants;

/**
 * This class gets the broadcast messages from the Notification bar and reply
 * back with broadcast messages for each operation.
 * 
 * @author vanrajvala
 */
public class NotificationReceiver extends BroadcastReceiver {
    public static final String TAG = "NotificationReceiver";
    public static final String LAUNCH_ACTION = "com.sec.android.fm.player";
    public static final String ON_ACTION = "com.sec.android.fm.player.on";
    public static final String OFF_ACTION = "com.sec.android.fm.player.off";
    public static final String NEXT_ACTION = "com.sec.android.fm.player.tune.next";
    public static final String PREV_ACTION = "com.sec.android.fm.player.tune.prev";
    public static final String TUNE_ACTION = "com.sec.android.fm.player.tune";
    public static final String CLOSE_ACTION = "com.sec.android.fm.player.close";
    private static final String APP_NAME = "com.sec.android.app.fm";
    private static final String ACTION_MUSIC_COMMAND = "com.android.music.musicservicecommand";
    private static final String ACTION_SAVE_FMRECORDING_ONLY = "com.samsung.media.save_fmrecording_only";
    public static final String FREQ = "freq";
    private RadioPlayer mPlayer;
    private ChannelStore mChannelStore = ChannelStore.getInstance();
    private AudioManager mAudioManager;
    private int mCurrentFreq;
    private static final int CODE_REMOVE_NOTIFICATION = 1;
    private static final int INITIALIZE_VOLUME_KEY_PRRESS_FLAG = 2;
    private Context mContext;
    private Handler mHandler;
    private boolean mBusy;
    private FMNotificationManager mNotiMgr = FMNotificationManager.getInstance();
    private PowerManager mPowerManager;
    private boolean mIsVolumeKeyDownDuringRecording = false;
    public static final String ACTION_PLAYSTATE_PLAY = "com.sec.android.app.fm.PlaybackState.ACTION_PLAY";
    public static final String ACTION_PLAYSTATE_PLAY_PAUSE = "com.sec.android.app.fm.PlaybackState.ACTION_PLAY_PAUSE";
    public static final String ACTION_PLAYSTATE_PAUSE = "com.sec.android.app.fm.PlaybackState.ACTION_PAUSE";
    public static final String ACTION_PLAYSTATE_STOP = "com.sec.android.app.fm.PlaybackState.ACTION_STOP";
    public static final String ACTION_UNREGISTER_MEDIA_SESSION = "com.sec.android.app.fm.UNREGISTER_MEDIA_SESSION";

    public NotificationReceiver(Context context) {
        mContext = context;
        if (mHandler == null)
            mHandler = new MyHandler();
        mPlayer = RadioPlayer.getInstance();
        mPlayer.registerListener(mListener);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        registerMusicCommandRec();
        registerConfigurationChanged();
        registerCameraCommandRec();
        registerEmergencyStateChangedListener();
        //registerUserSwitch();
        Log.d(TAG, "saving freq in init:" + Log.filter(mCurrentFreq));
        mCurrentFreq = RadioApplication.getInitialFrequency();
    }

    private BroadcastReceiver mMusicCommandRec = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String cmdStr = intent.getStringExtra("command");
            String appName = intent.getStringExtra("from");
            Log.v(TAG, "Notification Rec Got Music command :" + cmdStr + " from:" + appName);
            if (!APP_NAME.equals(appName) && ("stop".equals(cmdStr) || "pause".equals(cmdStr))) {
                if (mHandler != null) {
                    mHandler.removeMessages(CODE_REMOVE_NOTIFICATION);
                    // send after 2 min
                    Message msg = Message.obtain();
                    msg.what = CODE_REMOVE_NOTIFICATION;
                    mHandler.sendMessageDelayed(msg, 120 * 1000);
                }
            }
        }
    };

    private BroadcastReceiver mCameraCommandRec = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mNotiMgr != null && mNotiMgr.isNotified() && mNotiMgr.getVoiceState() == FMNotificationManager.VOICE_STOP) {
                if (mHandler != null) {
                    mHandler.removeMessages(CODE_REMOVE_NOTIFICATION);
                    Message msg = Message.obtain();
                    msg.what = CODE_REMOVE_NOTIFICATION;
                    mHandler.sendMessageDelayed(msg, 0);
                }
            }
        }
    };

    private BroadcastReceiver mConfigurationChanged = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (mHandler != null)
                mHandler.removeMessages(CODE_REMOVE_NOTIFICATION);

            if (Intent.ACTION_CONFIGURATION_CHANGED.equals(action) && mNotiMgr.isNotified()) {
                Log.v(TAG, "Configuration Changing");
                RefreshNotification();
            }
        }
    };

    /*private BroadcastReceiver mUserSwitch = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (mHandler != null)
                mHandler.removeMessages(CODE_REMOVE_NOTIFICATION);
            Log.v(TAG, "Receive UserSwitch Intent");
            if (Intent.ACTION_USER_SWITCHED.equals(action) && mNotiMgr.isNotified()) {
                mNotiMgr.removeNotification(false);
                Log.v(TAG, "Notification Closed ");
            }
            if (Intent.ACTION_USER_SWITCHED.equals(action)) {
                if (MainActivity.mIsRecording && (MainActivity._instance != null)) {
                    MainActivity._instance.stopFMRecording();
                    Log.d(TAG, "Closing recording...");
                }
            }
        }
    };*/
    private final BroadcastReceiver mEmergencyReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            int reason = intent.getIntExtra("reason", 0);
            Log.v(TAG,"mEmergencyReceiver onReceive");
            if ((MainActivity._instance != null) && mPlayer!=null && (reason == EmergencyConstants.MODE_ENABLING)) {
                if(MainActivity.mIsRecording)
                    MainActivity._instance.stopFMRecording();
                mPlayer.turnOff();
                Log.v(TAG, "Closing recording...");
            }
        }
    };
    FMEventListener mListener = new FMEventListener() {
        private int offCode;

        @Override
        public void onOn() {
            Log.v(TAG, "onOn");
            if (!NotificationService.mIsShowNotification) {
                NotificationService.mIsShowNotification = true;
                Intent i = new Intent(NotificationService.ACTION_SHOW_CONTEXTUAL_WIDGET);
                mContext.sendBroadcast(i);
                Log.d(TAG, "ACTION_SHOW_CONTEXTUAL_WIDGET");
            }
            offCode = 0;
            if (mHandler != null)
                mHandler.removeMessages(CODE_REMOVE_NOTIFICATION);
            mNotiMgr.updatePlayButtonState(true);
            if (mContext != null ){
                Intent intent = new Intent(ACTION_PLAYSTATE_PLAY);
                mContext.sendBroadcast(intent);
            }
        };

        @Override
        public void recFinish() {
            Log.d(TAG, "recFinish() : ");
            if (MainActivity.mIsRecording && MainActivity._instance != null) {
                MainActivity._instance.stopFMRecording();
            }
            super.recFinish();
        }

        @Override
        public void onOff(int reasonCode) {
            Log.v(TAG, "onOff start : " + reasonCode);
            if (mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;
            }

            mNotiMgr.updatePlayButtonState(false);
            if (mContext != null ){
                Intent intent = new Intent(ACTION_PLAYSTATE_PAUSE);
                mContext.sendBroadcast(intent);
            }
            if (reasonCode == 11) {
                Log.v(TAG, "paused");
                if (mNotiMgr.isNotified()) {
                    mNotiMgr.showNotification(null);
                    if (mHandler != null) {
                        Message msg = Message.obtain();
                        msg.what = CODE_REMOVE_NOTIFICATION;
                        mHandler.sendMessageDelayed(msg, 120 * 1000);
                    }
                }
                return;
            }

            if (offCode == 1) {
                earphoneDisconnected(true);
                offCode = 0;
            }
            if (reasonCode == FMPlayer.OFF_STOP_COMMAND) {
                if (mHandler != null) {
                    mHandler.removeMessages(CODE_REMOVE_NOTIFICATION);
                }
                mNotiMgr.removeNotification(false);
                return;
            } else if (reasonCode == 10) {
                RadioToast.showToast(mContext, mContext.getString(R.string.app_name) + " - "
                        + mContext.getString(R.string.toast_unavailable_in_tvout_mode),
                        Toast.LENGTH_SHORT);
            }
            Message msg = Message.obtain();
            msg.what = CODE_REMOVE_NOTIFICATION;
            if (mNotiMgr.isNotified())
                mNotiMgr.showNotification(null);
            if (mHandler != null) {
                mHandler.sendMessageDelayed(msg, 120 * 1000);
            }
            Log.v(TAG, "onOff end");
        }

        @Override
        public void earPhoneDisconnected() {
         //   if (!SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_HAS_INTENNA) {
                offCode = 1;
                boolean playState = mNotiMgr.getNotificationState();
                if (!playState) {
                    earphoneDisconnected(playState);
                }
         //   }
        }

        @Override
        public void onTune(long frequency) {
            Log.d(TAG, "Event [onTune]");
            // 2011.11.22 TOD_AHS : No sound when channel moved by media button
            // on sleep mode. [
            if (mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;
            }
            // 2011.11.22 TOD_AHS : No sound when channel moved by media button
            // on sleep mode. ]
            if (frequency != -1) {
/*                Channel channel = mChannelStore.getChannelByFrequency((int) frequency);
                String curChName = channel != null ? channel.mFreqName : "";
                if (MainActivity._instance == null || !MainActivity._instance.isResumed()) {
                    StringBuilder strBuilder = new StringBuilder();
                    if (curChName != null && !curChName.isEmpty()) {
                        strBuilder.append(curChName);
                        strBuilder.append(" - ");
                    } else {
                        //strBuilder.append(mContext.getString(R.string.app_name));
                    }
                    strBuilder.append('\u200e');
                    strBuilder.append(RadioPlayer.convertToMhz((int) frequency)).append(" MHz");
                    mNotiMgr.showNotification(strBuilder.toString());
                }*/
                // to ensure whether fm is on top or not
                mNotiMgr.registerNotification(false);
                mCurrentFreq = (int) frequency;
            }
        };

        // this callback will handle volume lock toast if activity in background
        @Override
        public void volumeLock() {
            Log.d(TAG, "volumeLock mIsVolumeKeyDownDuringRecording:- "+mIsVolumeKeyDownDuringRecording);
            if (mPlayer != null && !mPlayer.isOn()){
                Log.d(TAG, "volumeLock return : ");
                return;
            }
            if(FMUtil.isTopActivity(mContext)) {
                Log.d(TAG, "volumeLock return : fm is top activity");
                return;
            }
            if(!mIsVolumeKeyDownDuringRecording) {
                if (mPowerManager == null) {
                    mPowerManager = (PowerManager) mContext.getSystemService(Activity.POWER_SERVICE);
                }
                boolean isScreenOn = mPowerManager.isInteractive();
                if (isScreenOn
                        && (Settings.System.getInt(mContext.getContentResolver(),"all_sound_off", 0) != 1)
                        && (MainActivity._instance != null && (!MainActivity._instance.isResumed() || MainActivity._instance.iswindowhasfocus))
                        && (mAudioManager.isWiredHeadsetOn())
                        && !FMUtil.isOnCall(mContext)) {
                   RadioToast.showToast(mContext,R.string.recording_volume_control,Toast.LENGTH_SHORT);
                   mIsVolumeKeyDownDuringRecording = true;
                }
            }
            if(mHandler.hasMessages(INITIALIZE_VOLUME_KEY_PRRESS_FLAG)) {
                mHandler.removeMessages(INITIALIZE_VOLUME_KEY_PRRESS_FLAG);
            }
            Message msg = Message.obtain();
            msg.what = INITIALIZE_VOLUME_KEY_PRRESS_FLAG;
            mHandler.sendMessageDelayed(msg, 300);
        };

        @Override
        public void onRDSReceived(long freq, String channelName, String radioText) {
            if (channelName != null && !channelName.trim().equals("")) {
                int frequency = mPlayer.getFrequency();
                ActivityManager am = (ActivityManager) mContext
                        .getSystemService(Context.ACTIVITY_SERVICE);
                if (am != null) {
                    Log.v(TAG, "FMNotificationManager top activity:"
                            + am.getRunningTasks(1).get(0).topActivity.getClassName());
                    if (frequency > 0
                            && (!"com.sec.android.app.fm.MainActivity".equals(am.getRunningTasks(1)
                                    .get(0).topActivity.getClassName()))) {
                        if (MainActivity._instance == null || !MainActivity._instance.isResumed()) {
                            StringBuilder strBuilder = new StringBuilder();
                            if (channelName != null && !channelName.isEmpty()) {
                                strBuilder.append(channelName);
                                strBuilder.append(" - ");
                            } else {
                                //strBuilder.append(mContext.getString(R.string.app_name));
                            }
                            strBuilder.append('\u200e');
                            strBuilder.append(RadioPlayer.convertToMhz(frequency));
                            strBuilder.append(" MHz");
                            mNotiMgr.showNotification(strBuilder.toString());
                        }
                    }
                }
            }
        };
    };

    // 2011.11.22 TOD_AHS : No sound when channel moved by media button on sleep
    // mode. [
    protected WakeLock mWakeLock;

    // 2011.11.22 TOD_AHS : No sound when channel moved by media button on sleep
    // mode. ]

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (mContext == null) {
            mContext = context;
            mPlayer = RadioPlayer.getInstance();
            mPlayer.registerListener(mListener);
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        }
        if (mHandler == null)
            mHandler = new MyHandler();
        String action = intent.getAction();
        Log.d(TAG, "[NotificationReceiver] getting action :" + Log.filter(action));
        if (LAUNCH_ACTION.equals(action)) {
            Intent aintent = new Intent(context, MainActivity.class);
            aintent.setAction(Intent.ACTION_MAIN);
            aintent.addCategory(Intent.CATEGORY_LAUNCHER);
            aintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            aintent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(aintent);
        } else if (ON_ACTION.equals(action)) {
            boolean isOn = false;
            isOn = mPlayer.isOn();
            if (mPlayer.isScanning()) {
                return;
            }
            if (!isOn) {
                if (FMUtil.isVoiceActive(context,FMUtil.NEED_TO_PLAY_FM)) {
                    return;
                }
                try {
                    mPlayer.turnOn();
                } catch (FMPlayerException e) {
                    RadioToast.showToast(mContext, e);
                    return;
                }
                mNotiMgr.updatePlayButtonState(true);
                mCurrentFreq = RadioPlayer.getValidFrequency(mCurrentFreq);
                Log.v(TAG, "tunning to current freq:" + Log.filter(mCurrentFreq));
                mPlayer.tune(mCurrentFreq);
                mHandler.removeMessages(CODE_REMOVE_NOTIFICATION);
                SettingsActivity.activateTurnOffAlarm();
            }
        } else if (OFF_ACTION.equals(action)) {
            if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                DNSService dnsBoundService = mNotiMgr.getDnsService();
                if (dnsBoundService != null) {
                    boolean playingInternetStreaming = RadioDNSServiceDataIF
                            .isEpgPlayingStreamRadio();
                    /*
                     * Because of noise when stream is stop. It's need more time
                     * to stop the media player. Sequence : media player off -
                     * fm radio off - internet stream mode off
                     */
                    if (playingInternetStreaming) {
                        try {
                            Thread.sleep(MainActivity.DELAY_WAITING_STREAM_STOPPED);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (mPlayer.isOn()) {
                mPlayer.cancelSeek();
                if (!mPlayer.isScanning())
                    mCurrentFreq = mPlayer.getFrequency();
                Log.v(TAG, "saving freq:" + Log.filter(mCurrentFreq));
                if (MainActivity._instance != null) {
                    MainActivity._instance.stopFMRecording();
                }
                mNotiMgr.updatePlayButtonState(false);
                mPlayer.turnOff();
                Log.v(TAG, "power off done");
            }
        } else if (NEXT_ACTION.equals(action)) {
            if (!mPlayer.isOn()) {
                RadioToast.showToast(
                        mContext,
                        mContext.getString(R.string.turn_on_radio,
                                mContext.getString(R.string.app_name)), Toast.LENGTH_SHORT);
                return;
            }

            if (mPlayer.isScanning())
                return;
            if (MainActivity._instance != null)
                MainActivity._instance.setVisibeInformationView(MainActivity.SHOW_SEEKING);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!mBusy) {
                        mBusy = true;
                        // 2011.11.22 TOD_AHS : No sound when channel
                        // moved by media button on sleep mode. [
                        PowerManager pm = (PowerManager) context
                                .getSystemService(Context.POWER_SERVICE);
                        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass()
                                .getName());
                        mWakeLock.acquire();
                        // 2011.11.22 TOD_AHS : No sound when channel
                        // moved by media button on sleep mode. ]
                        mPlayer.seekUpAsync();
                        mBusy = false;
                    } else {
                        Log.v(TAG, "busy..");
                    }
                }
            }).start();
        } else if (PREV_ACTION.equals(action)) {
            if (!mPlayer.isOn()) {
                RadioToast.showToast(
                        mContext,
                        mContext.getString(R.string.turn_on_radio,
                                mContext.getString(R.string.app_name)), Toast.LENGTH_SHORT);
                return;
            }
            if (mPlayer.isScanning())
                return;
            if (MainActivity._instance != null)
                MainActivity._instance.setVisibeInformationView(MainActivity.SHOW_SEEKING);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (!mBusy) {
                        mBusy = true;
                        // 2011.11.22 TOD_AHS : No sound when channel
                        // moved by media button on sleep mode. [
                        PowerManager pm = (PowerManager) context
                                .getSystemService(Context.POWER_SERVICE);
                        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass()
                                .getName());
                        mWakeLock.acquire();
                        // 2011.11.22 TOD_AHS : No sound when channel
                        // moved by media button on sleep mode. ]
                        mPlayer.seekDownAsync();
                        mBusy = false;
                    } else {
                        Log.v(TAG, "busy..");
                    }
                }
            }).start();

        } else if (TUNE_ACTION.equals(action)) {
            if (mPlayer.isScanning()) {
                return;
            }
            if (!mPlayer.isOn()) {
                if (FMUtil.isVoiceActive(context,FMUtil.NEED_TO_PLAY_FM)) {
                    return;
                }
                try {
                    mPlayer.turnOn();
                } catch (FMPlayerException e) {
                    RadioToast.showToast(mContext, e);
                    return;
                }
                mNotiMgr.updatePlayButtonState(true);
            }
            mCurrentFreq = intent.getIntExtra(FREQ, RadioPlayer.FREQ_DEFAULT);
            Log.v(TAG, "tunning to current freq:" + Log.filter(mCurrentFreq));
            mCurrentFreq = RadioPlayer.getValidFrequency(mCurrentFreq);
            mPlayer.tune(mCurrentFreq);
            mHandler.removeMessages(CODE_REMOVE_NOTIFICATION);
        } else if (CLOSE_ACTION.equals(action)) {
            if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                DNSService dnsBoundService = mNotiMgr.getDnsService();
                if (dnsBoundService != null) {
                    boolean playingInternetStreaming = RadioDNSServiceDataIF
                            .isEpgPlayingStreamRadio();
                    /*
                     * Because of noise when stream is stop. It's need more time
                     * to stop the media player. Sequence : media player off -
                     * fm radio off - internet stream mode off
                     */
                    if (playingInternetStreaming) {
                        try {
                            Thread.sleep(MainActivity.DELAY_WAITING_STREAM_STOPPED);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (mPlayer.isOn()) {
                mPlayer.cancelSeek();
                if (!mPlayer.isScanning())
                    mCurrentFreq = mPlayer.getFrequency();
                Log.v(TAG, "saving freq:" + Log.filter(mCurrentFreq));
                if (MainActivity._instance != null) {
                    MainActivity._instance.stopFMRecording();
                }
                mNotiMgr.updatePlayButtonState(false);
                mPlayer.turnOff();
                Log.v(TAG, "power off done");
            } else {
                Log.v(TAG, "power off for removing audiofocus");
                mPlayer.turnOff();
            }
            if (mNotiMgr.getVoiceState() != FMNotificationManager.VOICE_STOP){
                mNotiMgr.setVoiceState(FMNotificationManager.VOICE_STOP);
                mContext.sendBroadcast(new Intent(FMListPlayerService.ACTION_STOP));
            }
            mNotiMgr.removeNotification(false);

            Intent intent1 = new Intent(ACTION_UNREGISTER_MEDIA_SESSION);
            mContext.sendBroadcast(intent1);
        }
    }

    private void RefreshNotification() {
        mNotiMgr.initialize(mContext);
        if (mHandler != null)
            mHandler.removeMessages(CODE_REMOVE_NOTIFICATION);
        int frequency = mPlayer.getFrequency();
        Log.d(TAG, " RefreshNotification mFreq = " + Log.filter(frequency));
        if (frequency != -1) {
            Channel channel = mChannelStore.getChannelByFrequency(frequency);
            String curChName = channel != null ? channel.mFreqName : "";
            if (MainActivity._instance == null || !MainActivity._instance.isResumed()) {
                StringBuilder strBuilder = new StringBuilder();
                if (curChName != null && !curChName.isEmpty()) {
                    strBuilder.append(curChName);
                    strBuilder.append(" - ");
                } else {
                    //strBuilder.append(mContext.getString(R.string.app_name));
                }
                strBuilder.append('\u200e');
                strBuilder.append(RadioPlayer.convertToMhz(frequency)).append(" MHz");
                mNotiMgr.updatePlayButtonState(true);
                mNotiMgr.showNotification(strBuilder.toString());
            }
        } else {
            mNotiMgr.updatePlayButtonState(false);
            FMListPlayerService mService = RecordedFileListPlayerActivity.getPlayer();
            if (mNotiMgr.getVoiceState() != FMNotificationManager.VOICE_STOP && mNotiMgr.isVoiceNotified() ) {
                mNotiMgr.setVoiceState(mService.isPlaying() ? FMNotificationManager.VOICE_PLAY : FMNotificationManager.VOICE_PAUSE);
                mNotiMgr.showNotification(FMUtil.getItemString(mContext,mService.getCurrentPlayingId(),Audio.Media.TITLE));
                mService.updateVoiceTime();
            } else if(NotificationService.mIsShowNotification){
                mNotiMgr.showNotification(null);
            }
        }
    }

    protected void registerMusicCommandRec() {
        IntentFilter intentFilter = new IntentFilter(ACTION_MUSIC_COMMAND);
        mContext.registerReceiver(mMusicCommandRec, intentFilter);
        Log.v(TAG, "Notification Rec - music command reciever registered");
    }

    private void unRegisterMusicCommandRec() {
        mContext.unregisterReceiver(mMusicCommandRec);
        Log.v(TAG, "Notification Rec - music command reciever un-registered");
    }

    private void registerCameraCommandRec() {
        IntentFilter intentFilter = new IntentFilter(ACTION_SAVE_FMRECORDING_ONLY);
        mContext.registerReceiver(mCameraCommandRec, intentFilter);
        Log.v(TAG, "Notification Rec - camera open reciever registered");
    }

    private void unRegisterCameraCommandRec() {
        mContext.unregisterReceiver(mCameraCommandRec);
        Log.v(TAG, "Notification Rec - camera open reciever un-registered");
    }

    protected void registerConfigurationChanged() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED);
        mContext.registerReceiver(mConfigurationChanged, intentFilter);
        Log.v(TAG, "Notification Rec - ConfigurationChanged reciever registered");
    }

    private void unregisterConfigurationChanged() {
        mContext.unregisterReceiver(mConfigurationChanged);
        Log.v(TAG, "Notification Rec - ConfigurationChanged reciever un-registered");
    }

    /*protected void registerUserSwitch() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_USER_SWITCHED);
        mContext.registerReceiver(mUserSwitch, intentFilter);
        Log.v(TAG, "Notification Rec - UserSwitch reciever registered");
    }

    private void unregisterUserSwitch() {
        mContext.unregisterReceiver(mUserSwitch);
        Log.v(TAG, "Notification Rec - UserSwitch reciever un-registered");
    }*/

    /**
     * Cleaning up receiver.
     */
    private void registerEmergencyStateChangedListener() {
        IntentFilter intentEmergencyFilter = new IntentFilter();
        intentEmergencyFilter.addAction(EmergencyConstants.EMERGENCY_STATE_CHANGED);
        mContext.registerReceiver(mEmergencyReceiver, intentEmergencyFilter);
        Log.v(TAG,"registering Emergency State Changed Listener");
    }
    private void unregisterEmegencyStateChangedListener() {
        Log.v(TAG,"Unregistering Emergency State Changed Listener");
        mContext.unregisterReceiver(mEmergencyReceiver);
    }
    public void terminate() {
        if (mPlayer != null) {
            mPlayer.unregisterListener(mListener);
            if (mHandler != null) {
                mHandler.removeMessages(CODE_REMOVE_NOTIFICATION);
                mHandler = null;
            }
            unRegisterMusicCommandRec();
            unregisterConfigurationChanged();
            unRegisterCameraCommandRec();
            unregisterEmegencyStateChangedListener();
            //unregisterUserSwitch();
            Log.v(TAG, "removing listener");
            mBusy = false;
        }
        mPlayer = null;
    }

    /**
     * Handler for removing quick panel after 2 mins.
     * 
     * @author vanrajvala
     */
    class MyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case CODE_REMOVE_NOTIFICATION :
                Log.v(TAG, "2 min is over lets remove notification:" + this);
                mNotiMgr.removeNotification(false);
                break;
            case INITIALIZE_VOLUME_KEY_PRRESS_FLAG :
                mIsVolumeKeyDownDuringRecording = false;
                break;
            default :
                break;
        }
        }
    }

    private void earphoneDisconnected(boolean playState) {
        if (mContext != null) {
            if (playState) {
                RadioToast.showToast(mContext, R.string.headset_disconnect, Toast.LENGTH_SHORT);
                if (MainActivity._instance != null) {
                    MainActivity._instance.stopFMRecording();
                }
            }
        }
        if (mHandler != null) {
            mHandler.removeMessages(CODE_REMOVE_NOTIFICATION);
        }
        mNotiMgr.removeNotification(false);
    }
}
