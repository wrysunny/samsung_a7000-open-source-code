package com.sec.android.app.fm;

import java.util.List;

import com.sec.android.app.fm.listplayer.FMListPlayerService;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.view.KeyEvent;

// 2011.09.16 TOD_AHS [P110915-1741] Media button.
public class MediaButtonReceiver extends BroadcastReceiver {

    private static final String TAG = MediaButtonReceiver.class.getSimpleName();

    private static final int ACTION_SHORT_PRESS = 1;

    private static final int ACTION_DOUBLE_CLICK = 2;

    private static int double_click = 0;

    private static final int DOUBLE_CLICK_DELAY = 300;

    // 2011.09.21 TOD_AHS [P110915-1741] Media button long/short press. [
    private static boolean mPressed = false;

    // 2011.09.21 TOD_AHS [P110915-1741] Media button long/short press. ]

    // 2012.03.12 TOD [P120308-0210] Media button long press does not work while
    // screen off. [
    private static Context mContext;
    public static MediaSession mMediaSession;
    public static PlaybackState.Builder mMediaStateBuilder;
    public static boolean mIsFMLastPlay = true;
    public static final String ACTION_REGISTER_MEDIA_SESSION = "com.sec.android.app.fm.REGISTER_MEDIA_SESSION";
    public static final String ACTION_UNREGISTER_MEDIA_SESSION = "com.sec.android.app.fm.UNREGISTER_MEDIA_SESSION";
    public static final String ACTION_PLAYSTATE_PLAY = "com.sec.android.app.fm.PlaybackState.ACTION_PLAY";
    public static final String ACTION_PLAYSTATE_PLAY_PAUSE = "com.sec.android.app.fm.PlaybackState.ACTION_PLAY_PAUSE";
    public static final String ACTION_PLAYSTATE_PAUSE = "com.sec.android.app.fm.PlaybackState.ACTION_PAUSE";
    public static final String ACTION_PLAYSTATE_STOP = "com.sec.android.app.fm.PlaybackState.ACTION_STOP";

    private static final String CLASSNAME = MediaButtonReceiver.class.getSimpleName();

    MediaController mCurrentSession;
    static ComponentName componentName;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ashok", "Action: "+intent.getAction());
        mContext = context;
        componentName = new ComponentName(mContext, MediaButtonReceiver.class.getName());
        switch (intent.getAction()) {
        case ACTION_REGISTER_MEDIA_SESSION:
            registerFmMediaButtonEventReceiver(true);
            break;
        case ACTION_UNREGISTER_MEDIA_SESSION:
            registerFmMediaButtonEventReceiver(false);
            break;
        case ACTION_PLAYSTATE_PLAY:
            setFmPlayState(PlaybackState.STATE_PLAYING);
            break;
        case ACTION_PLAYSTATE_PLAY_PAUSE:
            setFmPlayState(PlaybackState.STATE_PAUSED);
            break;
        case ACTION_PLAYSTATE_PAUSE:
            setFmPlayState(PlaybackState.STATE_PAUSED);
            break;
        case ACTION_PLAYSTATE_STOP:
            setFmPlayState(PlaybackState.STATE_STOPPED);
            break;
        default:
            break;
        }
    }
    public void registerFmMediaButtonEventReceiver(boolean register) {
        Log.d(TAG, "registerFmMediaButtonEventReceiver");
        if (register) {
            if (mMediaSession != null && mMediaSession.isActive()){
                Log.d(TAG, "FM mediasession is already active");
            }else{
                prepareFmMediaSession();
            }

        } else {
            if (mMediaStateBuilder != null && mMediaSession != null){
                Log.d(TAG, "unregister FM MediaButtonEventReceiver");
                mMediaStateBuilder.setState(PlaybackState.STATE_STOPPED, 0, 1.0f);
                mMediaSession.setPlaybackState(mMediaStateBuilder.build());
                mMediaSession.release();
                mMediaSession = null;
            }
        }
    }
    private void prepareFmMediaSession() {
        Log.d(TAG, "prepareFmMediaSession ");
        mMediaSession = new MediaSession(mContext, CLASSNAME);
        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS); 
        mMediaSession.setActive(true);

        Intent mediaButton = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButton.setComponent(componentName);

        mMediaSession.setCallback(new MediaSessionCallback());

        mMediaStateBuilder = new PlaybackState.Builder();
        mMediaStateBuilder.setState(PlaybackState.STATE_STOPPED, 0, 0);
        mMediaSession.setPlaybackState(mMediaStateBuilder.build());
    }

    PowerManager mPowerManager ;
    static RadioPlayer mPlayer = RadioPlayer.getInstance();

    private class MediaSessionCallback extends MediaSession.Callback {
        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            Log.d(TAG, "onMediaButtonEvent() mediaButtonIntent : "
                    + mediaButtonIntent);
            if (mIsFMLastPlay && isCameraTopActivity()){
                return false;
            }
            if (mMediaSession != null
                    && Intent.ACTION_MEDIA_BUTTON.equals(mediaButtonIntent.getAction())) {
                KeyEvent event = (KeyEvent) mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event == null) {
                    return false;
                }
                Log.d(TAG, "mIsFMLastPlay: "+mIsFMLastPlay);
                if (!mIsFMLastPlay){// Play Recorded File
                    KeyEvent ke = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

                    if (ke != null) {
                        int keycode = ke.getKeyCode();
                        switch (ke.getAction()) {
                        case KeyEvent.ACTION_DOWN:
                            Log.d(TAG, "ACTION_DOWN ");
                            onMediaKeyDown(ke);
                            return true;
                        case KeyEvent.ACTION_UP:
                            Log.d(TAG, "ACTION_UP ");
                            switch (ke.getKeyCode()) {
                            case KeyEvent.KEYCODE_HEADSETHOOK:
                            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                            case KeyEvent.KEYCODE_MEDIA_STOP:
                            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                            case KeyEvent.KEYCODE_MEDIA_REWIND:
                                sendMediaButtonReceived(mContext, keycode, ke.getRepeatCount(), false);
                                break;
                            default :
                                break;
                            }
                        default:
                            break;
                        }
                    }

                } else{//Play FM Radio
                    int keycode = event.getKeyCode();
                    int action = event.getAction();

                    boolean isLongPress = event.isLongPress();

                    if (isLongPress) {
                        mPressed = true;
                    }
                    Intent serviceIntent = new Intent(mContext, NotificationService.class);
                    mContext.startService(serviceIntent);

                    if (mPowerManager == null){
                        mPowerManager = (PowerManager) mContext.getSystemService(Activity.POWER_SERVICE);
                    }
                    boolean isScreenOn = mPowerManager.isInteractive();
                    
                    switch (keycode) {
                    case KeyEvent.KEYCODE_HEADSETHOOK:
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:

                        if (action == KeyEvent.ACTION_DOWN) {
                            if (double_click == 2) {
                                mButtonHandler.sendEmptyMessage(ACTION_DOUBLE_CLICK);
                            } else {
                                double_click++;
                            }

                            if (!isScreenOn && mPlayer != null && !mPlayer.isOn()){
                                Log.i(TAG, "used sendEmptyMessage ");
                                mButtonHandler.sendEmptyMessage(ACTION_SHORT_PRESS);
                            }else if(double_click == 1){
                                Log.i(TAG, "used sendEmptyMessageDelayed ");
                                mButtonHandler.sendEmptyMessageDelayed(ACTION_SHORT_PRESS, DOUBLE_CLICK_DELAY);
                            }

                        } else if (action == KeyEvent.ACTION_UP && !mPressed) {
                            if (double_click == 1) {
                                double_click++;
                            }
                        } else {
                            if (mButtonHandler.hasMessages(0)) {
                                Log.i(TAG, "Short -> Message removed");
                                mButtonHandler.removeMessages(0);
                            } else {
                                Log.i(TAG, "Long -> Do nothing");
                                return false;
                            }
                        }

                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        if (action == KeyEvent.ACTION_UP) {
                            mContext.sendBroadcast(new Intent(NotificationReceiver.NEXT_ACTION));
                        }
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        if (action == KeyEvent.ACTION_UP) {
                            mContext.sendBroadcast(new Intent(NotificationReceiver.PREV_ACTION));
                        }
                        break;
                    default:
                        break;
                    }

                    if (action == KeyEvent.ACTION_UP) {
                        mPressed = false;
                    }
                }
            }
            return super.onMediaButtonEvent(mediaButtonIntent);
        }
        private void onMediaKeyDown(KeyEvent ke) {
            int keycode = ke.getKeyCode();
            switch (ke.getKeyCode()) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE: {
                sendMediaButtonReceived(mContext, KeyEvent.KEYCODE_HEADSETHOOK,
                        ke.getRepeatCount(), true);
            }
            break;

            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                Log.d(TAG, "KEYCODE_MEDIA_PLAY ");
                sendMediaButtonReceived(mContext, keycode, ke.getRepeatCount(), true);
                break;
            default :
                break;
            }
        }
    }
    private static Handler mButtonHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(TAG, "handleMessage");
            if (mContext == null) {
                Log.i(TAG, "Cant handle Message cause mContext");
                return;
            }
            switch (msg.what) {
            case ACTION_SHORT_PRESS:
                double_click = 0;
                if (mPlayer!= null && mPlayer.isOn()) {
                    mContext.sendBroadcast(new Intent(NotificationReceiver.OFF_ACTION));
                } else {
                    mContext.sendBroadcast(new Intent(NotificationReceiver.ON_ACTION));
                }
                break;
            case ACTION_DOUBLE_CLICK:
                if (mButtonHandler.hasMessages(ACTION_SHORT_PRESS))
                    removeMessages(ACTION_SHORT_PRESS);
                double_click = 0;
                mContext.sendBroadcast(new Intent(NotificationReceiver.NEXT_ACTION));
                break;

            default:
                double_click = 0;
                break;
            }
        }
    };
    public void setFmPlayState(int state){
        Log.d(TAG, " mMediaStateBuilder: "+mMediaStateBuilder+" mMediaSession"+mMediaSession);
        if  (mMediaStateBuilder != null && mMediaSession != null){
            if (PlaybackState.STATE_PLAYING == state){
                Log.d(TAG, "PlaybackState is playing:");
                mMediaStateBuilder.setState(PlaybackState.STATE_PLAYING, 0, 1.0f);
                mMediaSession.setPlaybackState(mMediaStateBuilder.build());
            }else if (PlaybackState.STATE_STOPPED == state){
                Log.d(TAG, "PlaybackState is stopped:");
                mMediaStateBuilder.setState(PlaybackState.STATE_STOPPED, 0, 1.0f);
                mMediaSession.setPlaybackState(mMediaStateBuilder.build());
            }else if (PlaybackState.STATE_PAUSED == state){
                Log.d(TAG, "PlaybackState is paused:");
                mMediaStateBuilder.setState(PlaybackState.STATE_PAUSED, 0, 1.0f);
                mMediaSession.setPlaybackState(mMediaStateBuilder.build());
            }else{
                Log.d(TAG, "PlaybackState :"+state);
            }
        }else{
            Log.d(TAG, "Media Session is not active :");
        }
    }
    private static void sendMediaButtonReceived(Context context, int button, int repeatCount, boolean isKeyDown) {
        Log.d(TAG, "sendMediaButtonReceived ");
        Intent i = new Intent(FMListPlayerService.MEDIABUTTON_RECEIVED);

        i.putExtra(FMListPlayerService.MEDIABUTTON, button);
        i.putExtra(FMListPlayerService.MEDIABUTTON_REPEAT_COUNT, repeatCount);
        i.putExtra(FMListPlayerService.MEDIABUTTON_ISKEYDOWN, isKeyDown);
        context.sendBroadcast(i);
    }

    public boolean isCameraTopActivity() {
        if(mContext != null) {
            ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            List<RunningTaskInfo> Info = am.getRunningTasks(1);
            ComponentName topActivity = Info.get(0).topActivity;
            String topActivityPackageName = topActivity.getPackageName();
            if (topActivityPackageName.equals("com.sec.android.app.camera")) {
                Log.d(TAG, "Caamer is Top Activity. - avoid playing FM");
                return true;
            }
        }
        return false;
    }
}
