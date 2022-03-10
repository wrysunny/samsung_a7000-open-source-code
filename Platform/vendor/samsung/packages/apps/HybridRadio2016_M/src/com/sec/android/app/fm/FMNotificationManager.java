package com.sec.android.app.fm;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.media.AudioManager;
import android.provider.MediaStore.Audio;
import android.util.secutil.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.sec.android.app.dns.DNSService;
import com.sec.android.app.fm.data.Channel;
import com.sec.android.app.fm.data.ChannelStore;
import com.sec.android.app.fm.listplayer.FMListPlayerService;
import com.sec.android.app.fm.util.FMUtil;

/**
 * Notification Manager to takes of updating FM app quickpanel.
 * 
 * @author vanrajvala
 */
public class FMNotificationManager {
    private static FMNotificationManager sinstance = new FMNotificationManager();
    private static String TAG = "FMNotificationManager";

    private int mVoiceState = VOICE_STOP;
    public static final int VOICE_STOP = 0;
    public static final int VOICE_PLAY = 1;
    public static final int VOICE_PAUSE = 2;

    public static FMNotificationManager getInstance() {
        return sinstance;
    }

    private FMNotificationManager(){}

    private Context mContext = null;
    private DNSService mDnsService = null;
    private boolean mIsNotified = true;
    private Notification mNotification = null;
    private Notification mPublicNotification = null;
    private RadioPlayer mPlayer = null;
    private boolean mUpdatePlayState = true;

    public DNSService getDnsService() {
        return mDnsService;
    }

    public Notification getNotification() {
        return mNotification;
    }

    public boolean getNotificationState() {
        return mUpdatePlayState;
    }

    public void initialize(Context context) {
        Log.v(TAG, "initialize()");
        mContext = context.getApplicationContext();
        mPlayer = RadioPlayer.getInstance();
        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(),
                R.layout.notificationbar);
        remoteViews.setContentDescription(R.id.quickpanel_radio_recording_launch,
                mContext.getString(R.string.app_name));
        remoteViews.setContentDescription(R.id.quickpanel_radio_launch,
                mContext.getString(R.string.app_name));
        remoteViews.setOnClickPendingIntent(R.id.quickpanel_radio_rew, PendingIntent.getBroadcast(
                mContext, 1, new Intent(NotificationReceiver.PREV_ACTION), 0));
        remoteViews.setContentDescription(R.id.quickpanel_radio_rew,
                mContext.getString(R.string.desc_prev));
        remoteViews.setOnClickPendingIntent(R.id.quickpanel_radio_ff, PendingIntent.getBroadcast(
                mContext, 1, new Intent(NotificationReceiver.NEXT_ACTION), 0));
        remoteViews.setContentDescription(R.id.quickpanel_radio_ff,
                mContext.getString(R.string.desc_next));
        remoteViews.setOnClickPendingIntent(R.id.quickpanel_player_close, PendingIntent
                .getBroadcast(mContext, 1, new Intent(NotificationReceiver.CLOSE_ACTION), 0));
        remoteViews.setContentDescription(R.id.quickpanel_player_close,
                mContext.getString(R.string.desc_close));

        Intent launchIntent = new Intent(context, MainActivity.class);
        launchIntent.setAction(Intent.ACTION_MAIN);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        mNotification = new Notification.Builder(mContext).setSmallIcon(R.drawable.stat_notify_fmradio)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(mContext, 0, launchIntent, 0))
                .setTicker(mContext.getString(R.string.app_name)).setContent(remoteViews).build();
        //mNotification.twQuickPanelEvent = Notification.TWQUICKPANEL_NOTIFICATION_RADIO;

        // Set a public notification for hidden content stauts.
        if (mPublicNotification == null) {
            final Notification.Builder builder = new Notification.Builder(mContext);
            mPublicNotification = builder.build();
        }

        RemoteViews publicRemoteView = new RemoteViews(mContext.getPackageName(),
                R.layout.notification_fmradio_hidden_common);
        BitmapDrawable icon = (BitmapDrawable) mContext.getApplicationInfo().loadIcon(mContext.getPackageManager());
        publicRemoteView.setImageViewBitmap(R.id.hidden_quick_panel_icon, icon.getBitmap());
        mPublicNotification.contentView = publicRemoteView;
        mPublicNotification.bigContentView = publicRemoteView;

        mNotification.publicVersion = mPublicNotification;
    }

    public boolean isNotified() {
        return mIsNotified;
    }

    public boolean isVoiceNotified() {
        return mIsNotified & mVoiceState != VOICE_STOP;
    }

    /**
     * Removing quick panel from notification bar.
     */
    public void removeNotification(boolean forceRemove) {
        Log.v(TAG, "removeNotification()");
        if (isVoiceNotified() && !forceRemove) {
            Log.v(TAG, "removeNotification() return");
            return;
        }
        ((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
        Intent intent = new Intent(NotificationService.ACTION_STOP_SERVICE);
        mContext.sendBroadcast(intent);
        mIsNotified = false;
        NotificationService.mIsShowNotification = false;
        Intent i = new Intent(NotificationService.ACTION_HIDE_CONTEXTUAL_WIDGET);
        mContext.sendBroadcast(i);
    }

    public void setDnsService(DNSService service) {
        mDnsService = service;
    }

    public void registerNotification(boolean forceShow) {
        if (!forceShow && (FMUtil.isTopActivity(mContext) && !FMUtil.isLockScreen(mContext))) {
            return;
        }
        Log.v(TAG, "showNotification()");
        FMListPlayerService mService = RecordedFileListPlayerActivity.getPlayer();
        if (mService != null && (mService.isPlaying() || mService.isPaused()) && !mPlayer.isOn()) {
            setVoiceState(mService.isPlaying() ? FMNotificationManager.VOICE_PLAY : FMNotificationManager.VOICE_PAUSE);
            showNotification(FMUtil.getItemString(mContext, mService.getCurrentPlayingId(), Audio.Media.TITLE));
            mService.updateVoiceTime();
        } else if (mPlayer.isOn()){
            int freq = mPlayer.getFrequency();
            mUpdatePlayState = true;
            if (freq > 0) {
                StringBuilder strBuilder = new StringBuilder();
                String channelName = mPlayer.getPsName();
                if (channelName != null && !channelName.isEmpty()) {
                    strBuilder.append(channelName);
                    strBuilder.append(" - ");
                    strBuilder.append('\u200e');
                }
                ChannelStore channelStore = ChannelStore.getInstance();
                Channel channel = channelStore.getChannelByFrequency(freq);
                channelName = channel != null ? channel.mFreqName : "";
                if (channelName != null && !channelName.isEmpty()) {
                    strBuilder.append(channelName);
                    strBuilder.append(" - ");
                } else {
                    //strBuilder.append(mContext.getString(R.string.app_name));
                }
                    
                strBuilder.append('\u200e');
                strBuilder.append(RadioPlayer.convertToMhz(freq)).append(" MHz");
                showNotification(strBuilder.toString());
            } else {
                showNotification(null);
            }
        } else {
            Log.v(TAG, "Radio notification didn't register in quick panel.");
        }
    }

    /**
     * To show quick panel.
     *
     * @param textToShow
     *            Text to show with player controls.
     */
    public void showNotification(String textToShow) {
        KeyguardManager mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        boolean isScreenLocked = mKeyguardManager.inKeyguardRestrictedInputMode();
        if (mNotification == null || (FMUtil.isTopActivity(mContext) &&  !isScreenLocked)) {
            Log.e(TAG, "showNotification() - notification is null!!");
            return;
        }
        mNotification.contentView.setContentDescription(R.id.quickpanel_radio_play_pause,
                mContext.getString(R.string.desc_quick_pannel_power));

        updateVoiceUI();

        Log.d(TAG, "showNotification() playState = " + mUpdatePlayState);
        if (mNotification.when < 0) {
            mNotification.when = System.currentTimeMillis();
        }
        String appName = mContext.getString(R.string.app_name);

        if (MainActivity.mIsRecording) {
            mNotification.contentView.setTextViewText(R.id.quickpanel_radio_description, appName+" - "+textToShow);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_description, View.VISIBLE);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_description_below, View.GONE);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_rec_time, View.VISIBLE);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_voice_time, View.GONE);
        } else if (mVoiceState != VOICE_STOP){
            mNotification.contentView.setTextViewText(R.id.quickpanel_radio_description, textToShow);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_description, View.VISIBLE);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_description_below, View.GONE);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_rec_time, View.GONE);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_voice_time, View.VISIBLE);
        } else {
            mNotification.contentView.setTextViewText(R.id.quickpanel_radio_description, appName);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_description, View.VISIBLE);
            mNotification.contentView.setTextViewText(R.id.quickpanel_radio_description_below, textToShow);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_rec_time, View.GONE);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_voice_time, View.GONE);
            if (mPlayer.isOn()) {
                mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_description_below, View.VISIBLE);
            }
            else {
                mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_description_below, View.GONE);
            }
        }

        if (mPlayer.isOn()) {
            mNotification.contentView.setImageViewResource(R.id.quickpanel_radio_play_pause,
                    R.drawable.quickpanel_btn_pause);
            mNotification.contentView.setOnClickPendingIntent(R.id.quickpanel_radio_play_pause,
                    PendingIntent.getBroadcast(mContext, 1, new Intent(
                            NotificationReceiver.OFF_ACTION), 0));

            mNotification.contentView.setInt(R.id.quickpanel_radio_play_pause, "setImageAlpha", 255);
            mNotification.contentView.setInt(R.id.quickpanel_radio_rew, "setImageAlpha", 255);
            mNotification.contentView.setInt(R.id.quickpanel_radio_ff, "setImageAlpha", 255);
        } else if (mVoiceState == VOICE_STOP){
            mNotification.contentView.setImageViewResource(R.id.quickpanel_radio_play_pause,
                    R.drawable.quickpanel_btn_pause);
            mNotification.contentView.setOnClickPendingIntent(R.id.quickpanel_radio_play_pause,
                    PendingIntent.getBroadcast(mContext, 1, new Intent(
                            NotificationReceiver.ON_ACTION), 0));

            mNotification.contentView.setInt(R.id.quickpanel_radio_play_pause, "setImageAlpha", 66);
            mNotification.contentView.setInt(R.id.quickpanel_radio_rew, "setImageAlpha", 66);
            mNotification.contentView.setInt(R.id.quickpanel_radio_ff, "setImageAlpha", 66);
        }

        ((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1,
                mNotification);
        Intent intent = new Intent(mContext, NotificationService.class);
        intent.putExtra(NotificationService.NOTIFICATION_ACTION,
                NotificationService.SHOW_NOTIFICATION);
        mContext.startService(intent);
        mIsNotified = true;
        // need to update recording time if recording is paused
        updateRecordingPauseUI();
        Log.v(TAG, "notification done..");
    }

    public void updatePlayButtonState(boolean playState) {
        mUpdatePlayState = playState;
    }
    
    public void setVoiceState(int voiceState) {
        mVoiceState = voiceState;
    }

    public int getVoiceState() {
        return mVoiceState;
    }

    public void updateVoiceUI(){
        if (mPlayer.isOn())
            mVoiceState = VOICE_STOP;
        mNotification.contentView.setInt(R.id.quickpanel_radio_play_pause, "setImageAlpha", 255);
        mNotification.contentView.setInt(R.id.quickpanel_radio_rew, "setImageAlpha", 255);
        mNotification.contentView.setInt(R.id.quickpanel_radio_ff, "setImageAlpha", 255);

        switch(mVoiceState){
        case VOICE_PLAY :
            mNotification.contentView.setImageViewResource(R.id.quickpanel_radio_rew, R.drawable.quickpanel_btn_voice_rew);
            mNotification.contentView.setOnClickPendingIntent(R.id.quickpanel_radio_rew, PendingIntent.getBroadcast(
                 mContext, 1, new Intent(FMListPlayerService.ACTION_PREVIOUS), 0));
            mNotification.contentView.setContentDescription(R.id.quickpanel_radio_rew,
                  mContext.getString(R.string.tts_previous_button));

            mNotification.contentView.setImageViewResource(R.id.quickpanel_radio_ff, R.drawable.quickpanel_btn_voice_ff);
            mNotification.contentView.setOnClickPendingIntent(R.id.quickpanel_radio_ff, PendingIntent.getBroadcast(
                mContext, 1, new Intent(FMListPlayerService.ACTION_NEXT), 0));
            mNotification.contentView.setContentDescription(R.id.quickpanel_radio_ff,
                mContext.getString(R.string.tts_Next_button));

            mNotification.contentView.setImageViewResource(R.id.quickpanel_radio_play_pause,
                    R.drawable.quickpanel_btn_voice_pause);
            mNotification.contentView.setOnClickPendingIntent(R.id.quickpanel_radio_play_pause,
                    PendingIntent.getBroadcast(mContext, 1, new Intent(
                            FMListPlayerService.ACTION_PAUSE), 0));
            mNotification.contentView.setContentDescription(R.id.quickpanel_radio_play_pause,
                    mContext.getString(R.string.desc_pause));

            mNotification.setSmallIcon(Icon.createWithResource(mContext,R.drawable.stat_play));
            break;
         case VOICE_PAUSE :
             mNotification.contentView.setImageViewResource(R.id.quickpanel_radio_rew, R.drawable.quickpanel_btn_voice_rew);
             mNotification.contentView.setOnClickPendingIntent(R.id.quickpanel_radio_rew, PendingIntent.getBroadcast(
                  mContext, 1, new Intent(FMListPlayerService.ACTION_PREVIOUS), 0));
             mNotification.contentView.setContentDescription(R.id.quickpanel_radio_rew,
                   mContext.getString(R.string.tts_previous_button));

             mNotification.contentView.setImageViewResource(R.id.quickpanel_radio_ff, R.drawable.quickpanel_btn_voice_ff);
             mNotification.contentView.setOnClickPendingIntent(R.id.quickpanel_radio_ff, PendingIntent.getBroadcast(
                 mContext, 1, new Intent(FMListPlayerService.ACTION_NEXT), 0));
             mNotification.contentView.setContentDescription(R.id.quickpanel_radio_ff,
                 mContext.getString(R.string.tts_Next_button));

            mNotification.contentView.setImageViewResource(R.id.quickpanel_radio_play_pause,
                    R.drawable.quickpanel_btn_voice_play);
            mNotification.contentView.setOnClickPendingIntent(R.id.quickpanel_radio_play_pause,
                    PendingIntent.getBroadcast(mContext, 1, new Intent(
                            FMListPlayerService.ACTION_PLAY), 0));
            mNotification.contentView.setContentDescription(R.id.quickpanel_radio_play_pause,
                    mContext.getString(R.string.desc_resume));

            mNotification.setSmallIcon(Icon.createWithResource(mContext,R.drawable.stat_pause));
            break;
         case VOICE_STOP :
            mNotification.contentView.setImageViewResource(R.id.quickpanel_radio_rew, R.drawable.quickpanel_btn_rew);
            mNotification.contentView.setOnClickPendingIntent(R.id.quickpanel_radio_rew, PendingIntent.getBroadcast(
                mContext, 1, new Intent(NotificationReceiver.PREV_ACTION), 0));
            mNotification.contentView.setContentDescription(R.id.quickpanel_radio_rew,
                mContext.getString(R.string.desc_prev));

            mNotification.contentView.setImageViewResource(R.id.quickpanel_radio_ff, R.drawable.quickpanel_btn_ff);
            mNotification.contentView.setOnClickPendingIntent(R.id.quickpanel_radio_ff, PendingIntent.getBroadcast(
                mContext, 1, new Intent(NotificationReceiver.NEXT_ACTION), 0));
            mNotification.contentView.setContentDescription(R.id.quickpanel_radio_ff,
                mContext.getString(R.string.desc_next));

            mNotification.contentView.setTextViewText(R.id.quickpanel_voice_time, "");

            mNotification.setSmallIcon(Icon.createWithResource(mContext,R.drawable.stat_notify_fmradio));
         default :
           break;
       }
    }

    public void updateRecordingPauseUI (){
        if (MainActivity.mIsRecording && MainActivity.mIsRecordingPause && MainActivity._instance != null) {
            MainActivity._instance.updateRecordedTime();
            mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_recording_launch, View.VISIBLE);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_launch, View.INVISIBLE);
            mNotification.contentView.setImageViewResource(R.id.quickpanel_recording_image, R.drawable.quick_panel_icon_rec_dim);
        }
        else if (MainActivity.mIsRecording && !MainActivity.mIsRecordingPause) {
            mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_recording_launch, View.VISIBLE);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_launch, View.INVISIBLE);
            mNotification.contentView.setImageViewResource(R.id.quickpanel_recording_image, R.drawable.quick_panel_icon_rec);
        } else {
            mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_recording_launch, View.INVISIBLE);
            mNotification.contentView.setViewVisibility(R.id.quickpanel_radio_launch, View.VISIBLE);
        }
    }

    public void updateVoiceTime (String time){
        if (isVoiceNotified()){
            mNotification.contentView.setTextViewText(R.id.quickpanel_voice_time, time);
            ((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1,
                    mNotification);
        }
    }

    public void updateRecordingTime(String recordedTime) {
        if (mIsNotified ){
            mNotification.contentView.setTextViewText(R.id.quickpanel_recording_time, recordedTime);
            ((NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE)).notify(1,
                    mNotification);
            Log.d(TAG, "updateRecordingTime() :: notify() is called");
        }
    }

    public void setAudioSystemMute(boolean isMute) {
        if(mContext != null) {
            if (isMute){
                ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_MUTE, 0);
                ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0);
            }else{
                ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_SYSTEM, AudioManager.ADJUST_UNMUTE, 0);
                ((AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE)).adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0);
            }
        }
    }
}
