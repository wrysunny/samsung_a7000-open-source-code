package com.sec.android.app.fm;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.SamsungAudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.TelephonyManager;
import android.util.LruCache;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.media.fmradio.FMEventListener;
import com.samsung.media.fmradio.FMPlayerException;
import com.sec.android.app.SecProductFeature_FMRADIO;
import com.sec.android.app.dns.DNSEvent;
import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.ModeData;
import com.sec.android.app.dns.RadioDNSServiceDataIF;
import com.sec.android.app.dns.radioepg.EpgData;
import com.sec.android.app.dns.ui.DnsAlertDialogActivity;
import com.sec.android.app.dns.ui.DnsAlertDialogActivity.DnsDialogFragment;
import com.sec.android.app.dns.ui.DnsTestActivity;
import com.sec.android.app.fm.FMRadioFeature;
import com.sec.android.app.fm.data.Channel;
import com.sec.android.app.fm.data.ChannelStore;
import com.sec.android.app.fm.listplayer.FMListPlayerService;
import com.sec.android.app.fm.ui.AllChannelListFragment;
import com.sec.android.app.fm.ui.FavouriteListFragment;
import com.sec.android.app.fm.ui.FrequencyDisplayBgView;
import com.sec.android.app.fm.ui.FrequencyDisplayBgView.OnFrequencyChangeListener;
import com.sec.android.app.fm.ui.FrequencyDisplayBgView.OnPositionChangeListener;
import com.sec.android.app.fm.ui.RTPTag;
import com.sec.android.app.fm.ui.RTPTagList;
import com.sec.android.app.fm.ui.RTPTagListManager;
import com.sec.android.app.fm.ui.RadioDialogFragment;
import com.sec.android.app.fm.ui.RenameDialog;
import com.sec.android.app.fm.util.FMPermissionUtil;
import com.sec.android.app.fm.util.FMUtil;
import com.sec.android.app.fm.util.NetworkMonitorUtil;
import com.sec.android.app.fm.widget.FMRadioProvider;
import com.sec.android.secmediarecorder.SecMediaRecorder;

/**
 * This Activity is the starting point for the FM app. MainActivity takes care
 * of maintaining FM event listener, drawing main screen, TTS support, managing
 * all channels including favourites.
 * 
 * @author vanrajvala
 */

public class MainActivity extends Activity implements OnTouchListener,OnPositionChangeListener,
    OnFrequencyChangeListener, OnInitListener {

    public static boolean mIsScreenOff = false;
    private TabHost mTabHost;
    private TabWidget mTabWidget;
    public static boolean showRecordingSavePopup = true;
    public static final int DELAY_WAITING_STREAM_STOPPED = 150;

    private static final String ACTION_LOCK_TASK_MODE = "com.samsung.android.action.LOCK_TASK_MODE";
    public static final String ACTION_REGISTER_MEDIA_SESSION = "com.sec.android.app.fm.REGISTER_MEDIA_SESSION";
    public static final String ACTION_UNREGISTER_MEDIA_SESSION = "com.sec.android.app.fm.UNREGISTER_MEDIA_SESSION";
    public static final String ACTION_PLAYSTATE_PLAY = "com.sec.android.app.fm.PlaybackState.ACTION_PLAY";
    public static final String ACTION_PLAYSTATE_PLAY_PAUSE = "com.sec.android.app.fm.PlaybackState.ACTION_PLAY_PAUSE";
    public static final String ACTION_PLAYSTATE_PAUSE = "com.sec.android.app.fm.PlaybackState.ACTION_PAUSE";
    public static final String ACTION_PLAYSTATE_STOP = "com.sec.android.app.fm.PlaybackState.ACTION_STOP";
    public class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
            FavouriteListFragment favouriteListFragment = (FavouriteListFragment) getFragmentManager().findFragmentByTag(FavouriteListFragment.TAG);
            switch (msg.what) {
            case SPEAKER_ENABLE_DISABLE:
                handleEarPhoneClick();
                break;
            case CHECK_INITIAL_ACCESS:
                mIsInitialAccess = RadioApplication.isInitialAccess();
                if (mIsInitialAccess && !FMUtil.isVoiceActive(_instance, FMUtil.NEED_TO_PLAY_FM)) {
                    startAutoScan();
                    if (mAudioManager.isWiredHeadsetOn())
                    {
                        RadioApplication.setInitialAccess(false);
                    }
                }
                break;
            case SET_CHANNEL_INFO:
                resetRDS(mPlayer.getFrequency());
                String radioText = mPlayer.getRadioText();
                if (radioText != null) {
                    setRDSTextView(null, radioText);
                    Log.d(TAG, "RDS text set :" + radioText);
                }
                if (!FMRadioFeature.FEATURE_DISABLERTPLUSINFO && (mRTPText != null)) {
                    setRDSTextView(null, mRTPText);
                    Log.d(TAG, "RTP text set :" + mRTPText);
                }
                break;
            case ONCLICK_PLAY:
                LogDns.v("MAINACTIVITY", "[ONCLICK_PLAY]");
                // toggle on/off
                boolean isUiOn = msg.arg1 == 1 ? true : false;
                if (isUiOn) {
                    try {
                        if (mRadioDNSEnable) {
                            mPlayingInternetStreaming = RadioDNSServiceDataIF
                                    .isEpgPlayingStreamRadio();
                            /*
                             * Because of noise when stream is stop. It's need
                             * more time to stop the media player. Sequence :
                             * media player off - fm radio off - internet stream
                             * mode off
                             */
                            if (mPlayingInternetStreaming) {
                                Thread.sleep(DELAY_WAITING_STREAM_STOPPED);
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        if (mPlayer.isScanning()) {
                            RadioToast.showToast(MainActivity.this, R.string.dialog_cancel_scan,
                                    Toast.LENGTH_SHORT);
                            mPlayer.cancelScan();
                        }
                        mPlayer.cancelSeek();
                        if (mIsRecording)
                            stopFMRecording(false);
                        mPlayer.turnOff();
                    }
                } else {
                    try {
                        mIsOning = true;
                        on();
                        if(!mPlayer.isOn()){
                            mIsOning = false;
                        }
                        if (mPlayer.isOn()) {
                            Log.d(TAG, "initial : " + mIsInitialAccess);
                            if (!mIsInitialAccess)
                                tune(mCurrentFreq);
                        }
                    } catch (FMPlayerException e) {
                        mIsOning = false;
                        e.printStackTrace();
                    }
                }
                break;
            case INITIALIZE_VOLUME_KEY_PRRESS_FLAG :
                mIsVolumeKeyDownDuringRecording = false;
                break;
            case ONCLICK_RECORD_BTN:
                if (!mIsRecording) {
                    if (!FMPermissionUtil.hasPermission(getApplicationContext(), FMPermissionUtil.FM_PERMISSION_REQUEST_RECORD_AUDIO)) {
                        FMPermissionUtil.requestPermission(MainActivity.this, FMPermissionUtil.FM_PERMISSION_REQUEST_RECORD_AUDIO);
                    } else {
                        recordFMRadioAudio();
                    }
                }
                break;
            case RECORD_TIME_UPDATE:
                updateRecordedTime();
                break;
            case FREQ_TUNE:
                int tuneFreq = msg.arg1;
                tune(tuneFreq);
                break;
            case FREQ_ADD:
                int pos = msg.arg1;
                if (!mPlayer.isOn()) {
                    RadioToast.showToast(MainActivity.this, getString(R.string.turn_on_radio, getString(R.string.app_name)), Toast.LENGTH_SHORT);
                    return;
                }
                int formattedCurrentFreq = (int) (Float.parseFloat(RadioPlayer.convertToMhz(mCurrentFreq)) * 100);
                String channelName = mPsText.getText().toString();
                if (!mChannelStore.addFavoriteChannel(formattedCurrentFreq, (channelName != null ? channelName : ""), pos)) {
                    RadioToast.showToast(MainActivity.this, R.string.toast_already_added, Toast.LENGTH_SHORT);
                    return;
                }
                if(favouriteListFragment != null && favouriteListFragment.isVisible())
                    favouriteListFragment.notifyDataSetChanged();
                if(allChannelListFragment != null && allChannelListFragment.isVisible())
                    allChannelListFragment.notifyDataSetChanged();
                refreshAddFavBtn(mCurrentFreq);
                break;
            case FREQ_DELETE:
                Channel channel = (Channel) msg.obj;
                mChannelStore.removeFavoriteChannel(channel.mFreqency);
                widgetRefresh();
                if(favouriteListFragment != null && favouriteListFragment.isVisible()) {
                	favouriteListFragment.rearrangeCheckedItems(channel);
                    favouriteListFragment.notifyDataSetChanged();
                }
                if(allChannelListFragment !=  null && allChannelListFragment.isVisible())
                    allChannelListFragment.notifyDataSetChanged();
                refreshAddFavBtn(mCurrentFreq);
                break;
            case VOLUME_FADE:
                setVolume(mCurrentFadeVolume);
                if (mCurrentFadeVolume == RECORDING_VOLUME) {
                    Intent intent = new Intent(ACTION_VOLUME_LOCK);
                    if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_BRAODCOM)
                            || SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_MRVL)
                            || SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_SPRD)) {
                        intent.putExtra(KEY_RETURNBACK_VOLUME, mReturnFadeVolume);
                        Log.d(TAG, "put the mReturnFadeVolume(" + mReturnFadeVolume + ")");
                    }
                    sendBroadcast(intent);
                    mIsFadeVolume = false;
                } else {
                    if (mCurrentFadeVolume < RECORDING_VOLUME) {
                        mCurrentFadeVolume++;
                    } else {
                        mCurrentFadeVolume--;
                    }
                    mHandler.sendEmptyMessageDelayed(VOLUME_FADE, 100);
                }
                break;
            case RETURN_VOLUME_FADE:
                if (!(mCurrentFadeVolume == mReturnFadeVolume)) {
                    if (mCurrentFadeVolume < mReturnFadeVolume) {
                        mCurrentFadeVolume++;
                    } else {
                        if ((mCurrentFadeVolume > EAR_SHOCK_VOLUME_VALUE)){
                            mCurrentFadeVolume = EAR_SHOCK_VOLUME_VALUE;
                         }
                        mCurrentFadeVolume--;
                    }
                    setVolume(mCurrentFadeVolume);
                    mHandler.sendEmptyMessageDelayed(RETURN_VOLUME_FADE, 100);
                } else {
                    hideRecoder();
                }
                break;
            case TOGGLE_STREAMING_FMRADIO:
                handleSwitchDnsClick();
                break;
            case REM_NOTIFICATION:
                mNotiMgr.removeNotification(false);
                break;
            case SHOW_NOTIFICATION:
                mNotiMgr.registerNotification(false);
                break;
            default:
                break;
            }
        }
    }

    public static MainActivity _instance;
    private static final int CHECK_INITIAL_ACCESS = 8;
    private static final int REM_NOTIFICATION = 3001;
    private static final int SHOW_NOTIFICATION = 3002;
    public static final int FREQ_ADD = 200;
    public static final int FREQ_DELETE = 201;
    public static final int FREQ_TUNE = 202;

    public static final int VOLUME_FADE = 203;
    public static final int RETURN_VOLUME_FADE = 204;

    public static final int FMRADIO_RECORDING = 2;
    public static final int RECORDING_START = 1;
    public static final int RECORDING_END = 0;
    private static final int MENU_CHANGE_SOUND_PATH = 0;
    private static final int MENU_RECORDED_FILES = 5;
    private static final int MENU_SETTINGS = 8;
    private static final int MENU_REMOVE = 10;
    private static final int MENU_EDIT = 11;
    private static final int MENU_SWITCH_INTERNET_STREAMING = 9;

    public static final int MAX_FAVORITES_COUNT_BIGGER_THAN_MDPI = 12;
    public static boolean IS_BIGGER_THAN_MDPI = true;
    public static int MAX_FAVORITES_COUNT = MAX_FAVORITES_COUNT_BIGGER_THAN_MDPI;

    private static final int ONCLICK_PLAY = 11;
    private static final int ONCLICK_RECORD_BTN = 12;

    private static final int INITIALIZE_VOLUME_KEY_PRRESS_FLAG = 212;

    private static final int RECORD_TIME_UPDATE = 13;

    private Resources mResources = null;
    private Channel mSelectedChannel;
    private int mSavedSelectedFreq = -1;

    private static final int SET_CHANNEL_INFO = 10;
    private static final int SPEAKER_ENABLE_DISABLE = 3;
    private static final int TOGGLE_STREAMING_FMRADIO = 4;
    private static final long KBYTES = 1024;
    private static final long LOW_STORAGE_THRESHOLD = KBYTES * KBYTES;
    private static final long LOW_STORAGE_SAFETY_THRESHOLD = LOW_STORAGE_THRESHOLD + (KBYTES * 350);
    private static final long LOW_STORAGE_REMAINING_BYTE = 166000;

    public static final int SHOW_SEEKING = 0;
    public static final int SHOW_TURNING_ON = 1;
    public static final int SHOW_TURN_ON_RADIO = 2;
    public static final int SHOW_RDS = 3;
    public static final int SHOW_RECORDING_TIME = 4;

    private StorageManager mStorageManager;
    public static final String ACTION_TURNING_ON = "action_turning_on";

    // need to change to CSCFeature
    private boolean mRadioDNSEnable = !FMRadioFeature.FEATURE_DISABLEDNS;
    private DNSService mDNSBoundService = null;
    private boolean mEpgStreamAvailable = false;
    private boolean mPlayingInternetStreaming = false;
    public static Toast mEarphoneToast;
    
    private TextToSpeech mTts;

    public DNSService getDNSService() {
        return mDNSBoundService;
    }

    private void handleSwitchDnsClick() {
        LogDns.d("FMApp", "handleSwitchDnsClick()");
        if (mPlayingInternetStreaming) {
            mDNSBoundService.stopStreamRadio();
        } else {
            Intent i = new Intent(this, DnsAlertDialogActivity.class);
            if (NetworkMonitorUtil.needBillWarning(MainActivity.this)) {
                i.putExtra(DnsDialogFragment.KEY_TITLE, R.string.connect_mobile_network);
                i.putExtra(DnsDialogFragment.KEY_ACTION, DnsDialogFragment.ACTION_START_EPG);
            } else {
                i.putExtra(DnsDialogFragment.KEY_TITLE, R.string.switch_to_internet_radio);
                i.putExtra(DnsDialogFragment.KEY_MSG, R.string.dialog_popup_wait);
                mDNSBoundService.startStreamRadio(String.format("%05d", mPlayer.getFrequency()));
            }
            startActivity(i);
        }
        updateButton();
    }

    /* [P130122-6311] */
    private static final String IS_VISIBLE_WINDOW = "AxT9IME.isVisibleWindow";
    private static final String RESPONSE_AXT9INFO = "ResponseAxT9Info";
    public static boolean ISSIPVISIBLE = false;
    private BroadcastReceiver mBroadcastReceiverSip = null;
    private long mReceiveTime = 0;
    private AudioManager mAudioManager;

    // set the flag to MSG_CONFIGURE_SAFE_MEDIA_VOLUME_FORCED in
    // AudioService.java
    public static int mFlagSteamVolume = 0;

    private ChannelStore mChannelStore = null;
    boolean isOnFrequencyProgress = false;

    boolean mPrevDown = false;
    boolean mNextDown = false;
    private FMNotificationManager mNotiMgr = FMNotificationManager.getInstance();

    public View.OnLongClickListener mLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            switch (v.getId()) {
            case R.id.dial_next:
                if (mPlayer.isBusy()) {
                    Log.d(TAG, "RadioPlayer is busy. ignore it");
                    return false;
                }
                Log.d(TAG, "[onClick - Control Next]");
                if (mPlayer.isOn()) {
                    if (mCurrentFreq > RadioPlayer.FREQ_MAX)
                        return false;
                    setVisibeInformationView(SHOW_SEEKING);
                    mPlayer.seekUpAsync();
                } else {
                    RadioToast.showToast(MainActivity.this,
                            getString(R.string.turn_on_radio, getString(R.string.app_name)),
                            Toast.LENGTH_SHORT);
                }
                mNextDown = true;
                break;
            case R.id.dial_prev:
                if (mCurrentFreq < RadioPlayer.FREQ_MIN)
                    return false;
                if (mPlayer.isBusy()) {
                    Log.d(TAG, "RadioPlayer is busy. ignore it");
                    return false;
                }
                Log.v(TAG, "[onClick - Control Prev]");
                if (mPlayer.isOn()) {
                    setVisibeInformationView(SHOW_SEEKING);
                    mPlayer.seekDownAsync();
                } else {
                    RadioToast.showToast(MainActivity.this,
                            getString(R.string.turn_on_radio, getString(R.string.app_name)),
                            Toast.LENGTH_SHORT);
                }
                mPrevDown = true;
                break;
            default:
                break;
            }
            return false;
        }
    };

    private View.OnKeyListener mKeyListener = new View.OnKeyListener() {

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            switch (v.getId()) {
            case R.id.dial_next:
            case R.id.dial_prev:
                if ((keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                        && event.getAction() == KeyEvent.ACTION_UP) {
                    mPrevDown = false;
                    mNextDown = false;
                }
                break;

            case R.id.frq_dsp_bg_view:
                Log.d(TAG, "[SKW] frq_dsp_bg_view");
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (mCurrentFreq < RadioPlayer.FREQ_MIN)
                        break;
                    if (mPlayer.isBusy()) {
                        Log.d(TAG, "RadioPlayer is busy. ignore it");
                        break;
                    }
                    Log.v(TAG, "[onClick - Control Prev through keypad]");
                    if (mPlayer.isOn()) {
                        setVisibeInformationView(SHOW_SEEKING);
                        if (mRecorder != null && mIsRecording) {
                            updateRecordedTime();
                        }
                        if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE){
                            mPlayer.tune(mCurrentFreq - 5);
                        } else {
                            mPlayer.tune(mCurrentFreq - 10);
                        }
                    } else {
                        RadioToast.showToast(MainActivity.this, getString(R.string.turn_on_radio,
                                getString(R.string.app_name)), Toast.LENGTH_SHORT);
                    }
                } else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (mPlayer.isBusy()) {
                        Log.d(TAG, "RadioPlayer is busy. ignore it");
                        break;
                    }
                    Log.d(TAG, "[onClick - Control Next through keypad]");
                    if (mPlayer.isOn()) {
                        if (mCurrentFreq > RadioPlayer.FREQ_MAX)
                            break;
                        setVisibeInformationView(SHOW_SEEKING);
                        if(mRecorder != null && mIsRecording) {
                            updateRecordedTime();
                        }
                        if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE){
                            mPlayer.tune(mCurrentFreq + 5);
                        } else {
                            mPlayer.tune(mCurrentFreq + 10);
                        }
                    } else {
                        RadioToast.showToast(MainActivity.this, getString(R.string.turn_on_radio,
                                getString(R.string.app_name)), Toast.LENGTH_SHORT);
                    }
                    break;
                }
                break;
            default:
                break;
            }
            return false;
        }
    };

    public View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent arg1) {
            switch (v.getId()) {
            case R.id.dial_next:
            case R.id.dial_prev:
                switch (arg1.getAction()) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mPrevDown = false;
                    mNextDown = false;
                    break;
                default:
                    break;
                }
                break;
            default:
                break;
            }
            return false;
        }
    };

    private static final long SKIP_BUTTON_CLICK_EVENT_TIME = 500;
    private long lastButtonClickTime = 0;
    public View.OnClickListener mClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            int id = v.getId();
            long currentTimeMillis = System.currentTimeMillis();
            if (mIsOning 
                    ||((id == R.id.recording_cancel_btn || id == R.id.recording_stop_btn || id == R.id.recording_pause_resume_btn)
                    && (currentTimeMillis - lastButtonClickTime) < SKIP_BUTTON_CLICK_EVENT_TIME)) {
                return;
            }
            lastButtonClickTime = currentTimeMillis;

            switch (id) {
            case R.id.scan_btn:
                Log.d(TAG, "[onClick scan]");
                if (mPlayer.isScanning()) {
                    RadioToast.showToast(MainActivity.this, R.string.toast_already_scan,
                            Toast.LENGTH_SHORT);
                }
                if (mIsRecording) {
                    RadioToast.showToast(MainActivity.this, R.string.toast_unable_scan,
                            Toast.LENGTH_SHORT);
                } else {
                    openDialog(RadioDialogFragment.SCAN_OPTION_DIALOG);
                }
            break;
            case R.id.dial_prev:
                if (mCurrentFreq < RadioPlayer.FREQ_MIN)
                    return;
                if (mPlayer.isBusy()) {
                    Log.d(TAG, "RadioPlayer is busy. ignore it");
                    return;
                }
                Log.v(TAG, "[onClick - Control Prev]");
                if (mPlayer.isOn()) {
                    if (!mIsRecording) {
                        setVisibeInformationView(SHOW_SEEKING);
                    }
                    mPlayer.seekDownAsync();
                } else {
                    RadioToast.showToast(MainActivity.this,
                            getString(R.string.turn_on_radio, getString(R.string.app_name)),
                            Toast.LENGTH_SHORT);
                }
                break;

            case R.id.power_btn:
                Log.d(TAG, "[onClick - Control Play/pause]");
                onClickedPowerButton();
                break;
            // mapped next
            case R.id.dial_next:
                if (mPlayer.isBusy()) {
                    Log.d(TAG, "RadioPlayer is busy. ignore it");
                    return;
                }
                Log.d(TAG, "[onClick - Control Next]");
                if (mPlayer.isOn()) {
                    if (mCurrentFreq > RadioPlayer.FREQ_MAX)
                        return;
                    if (!mIsRecording) {
                        setVisibeInformationView(SHOW_SEEKING);
                    }
                    mPlayer.seekUpAsync();
                } else {
                    RadioToast.showToast(MainActivity.this,
                            getString(R.string.turn_on_radio, getString(R.string.app_name)),
                            Toast.LENGTH_SHORT);
                }
                break;
            case R.id.record_btn:
                if ((android.provider.Settings.System.getInt(getContentResolver(), "all_sound_off", 0) == 1)){
                    RadioToast.showToast(MainActivity.this, getString(R.string.unable_record_turn_off_all_sound), Toast.LENGTH_SHORT);
                }else if (!mHandler.hasMessages(ONCLICK_RECORD_BTN))
                    mHandler.sendEmptyMessage(ONCLICK_RECORD_BTN);
                break;
            case R.id.recording_stop_btn:
                stopFMRecording();
                break;
            case R.id.recording_pause_resume_btn:
                if (mIsRecording) {
                    if (mIsRecordingPause) {
                        resumeFMRecording();
                    } else {
                        pauseFMRecording();
                    }
                }
                break;
            case R.id.recording_cancel_btn:
                openDialog(RadioDialogFragment.RECORD_CANCEL_DIALOG);
                break;
            case R.id.freq_value:
                if ((mAudioManager.isWiredHeadsetOn()) && !FMUtil.isVoiceActive(_instance)) {
                    openDialog(RadioDialogFragment.CHANGE_FREQ_DIALOG);
                }
                if (!mAudioManager.isWiredHeadsetOn()) {
                    RadioToast.showToast(MainActivity.this, R.string.toast_earphone_not_connected,
                            Toast.LENGTH_SHORT);
                }
                break;
            case R.id.vis_btn:
                if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                    if (!(mViewState instanceof InformationViewState)) {
                        if ((mDNSBoundService != null) && (mDNSBoundService.isVisAvailable())) {
                            if (NetworkMonitorUtil.needBillWarning(MainActivity.this)) {
                                Intent i = new Intent(MainActivity.this,
                                        DnsAlertDialogActivity.class);
                                i.putExtra(DnsDialogFragment.KEY_TITLE,
                                        R.string.connect_mobile_network);
                                i.putExtra(DnsDialogFragment.KEY_ACTION,
                                        DnsDialogFragment.ACTION_START_VIS);
                                startActivity(i);
                            } else {
                                mViewState.showPrevious();
                            }
                        } else {
                            RadioToast.showToast(MainActivity.this, R.string.switch_btn_disable,
                                    Toast.LENGTH_SHORT);
                        }
                    } else {
                        mViewState.showPrevious();
                    }
                }
                break;
            case R.id.add_fav_btn:
            	setTTSforAddFavourite(mCurrentFreq);
            	if(isOnFrequencyProgress)
            		return;
                Channel channel = mChannelStore.getChannelByFrequency(mCurrentFreq);
                if (channel != null && channel.mIsFavourite) {
                    if (!mHandler.hasMessages(FREQ_DELETE))
                        mHandler.obtainMessage(FREQ_DELETE, channel).sendToTarget();
                } else {
                    int addPos = mChannelStore.getEmptyPositionOfFavorite();
                    if (addPos >= 0 && addPos < MAX_FAVORITES_COUNT) {
                        if (!mHandler.hasMessages(FREQ_ADD))
                            mHandler.obtainMessage(FREQ_ADD, addPos, 0).sendToTarget();
                    } else {
                        RadioToast.showToast(MainActivity.this,
                                getString(R.string.toast_max_favorite, MAX_FAVORITES_COUNT),
                                Toast.LENGTH_SHORT);
                    }
                }
                break;
            case R.id.rds_panel:
            case R.id.turningtext:
                // open the test activity
                Log.secV("TEST", "click information");
                startActivity(new Intent(MainActivity.this, DnsTestActivity.class));
                if (mDNSBoundService == null) {
                    mDNSBoundService = DNSService.bindService(MainActivity.this,
                            mDNSServiceConnection);
                }
                break;
            default:
                break;
            }
        }
    };

    void onClickedPowerButton() {

        if(mIsOning){
            return;
        }

        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        if (!mIsInitialAccess) {
            if (!mHandler.hasMessages(ONCLICK_PLAY)) {
                Message msg = Message.obtain();
                msg.what = ONCLICK_PLAY;
                msg.arg1 = mIsOn ? 1 : 0;
                Log.e(TAG, "isOn:" + mIsOn);
                mHandler.sendMessageDelayed(msg, 200);
                if (mPlayer != null
                        && !mIsOn
                        && tm.getCallState() != TelephonyManager.CALL_STATE_OFFHOOK
                        && Settings.Global.getInt(getContentResolver(),
                                Settings.Global.AIRPLANE_MODE_ON, 0) == 0) {
                    Intent intent = new Intent(ACTION_TURNING_ON);
                    sendBroadcast(intent);
                }
            }
        } else {
            Log.d(TAG, "[onClickPowerBtn()] ignored key event = " + mIsInitialAccess);
        }
    }

    private int mCurrentFreq = RadioPlayer.FREQ_DEFAULT;
    private PowerManager mPowerManager;
    private boolean mIsVolumeKeyDownDuringRecording = false;

    private FMEventListener mFMListener = new FMEventListener() {
        private int offCode;

        @Override
        public void earPhoneDisconnected() {
            offCode = 1;
            refreshPlayViaIcon();
            mNotiMgr.removeNotification(false);
            closeDialog(RadioDialogFragment.CHANGE_FREQ_DIALOG);
        }

        @Override
        public void earPhoneConnected() {
            if (isResumed()) {
                Message msg = new Message();
                msg.what = CHECK_INITIAL_ACCESS;
                mHandler.sendMessageDelayed(msg, 500);
            }
            refreshPlayViaIcon();
            startService(new Intent(MainActivity.this, NotificationService.class));
            super.earPhoneConnected();
        }

        // this callback will handle volume lock toast if activity in foreground
        @Override
        public void volumeLock() {
            Log.d(TAG, "volumeLock : mIsVolumeKeyDownDuringRecording:- "+mIsVolumeKeyDownDuringRecording);
            if (mPlayer != null && !mPlayer.isOn()) {
                Log.d(TAG, "volumeLock return");
                return;
            }
            if(!FMUtil.isTopActivity(_instance)) {
                Log.d(TAG, "volumeLock return : fm is not top activity");
                return;
            }
            if(!mIsVolumeKeyDownDuringRecording) {
                if (mPowerManager == null) {
                    mPowerManager = (PowerManager)getApplicationContext().getSystemService(Activity.POWER_SERVICE);
                }
                boolean isScreenOn = mPowerManager.isInteractive();
                if (isScreenOn
                        && (Settings.System.getInt(getContentResolver(),"all_sound_off", 0) != 1)
                        && (MainActivity._instance != null && (!MainActivity._instance.isResumed() || MainActivity._instance.iswindowhasfocus))
                        && (mAudioManager.isWiredHeadsetOn())
                        && !FMUtil.isOnCall(getApplicationContext())&& mIsRecording) {
                    RadioToast.showToast(getApplicationContext(),R.string.recording_volume_control,Toast.LENGTH_SHORT);
                    mIsVolumeKeyDownDuringRecording = true;
                }
            }
            if(mHandler.hasMessages(INITIALIZE_VOLUME_KEY_PRRESS_FLAG)) {
                mHandler.removeMessages(INITIALIZE_VOLUME_KEY_PRRESS_FLAG);
            }
            Message msg = Message.obtain();
            msg.what = INITIALIZE_VOLUME_KEY_PRRESS_FLAG;
            mHandler.sendMessageDelayed(msg, 300);
        }


        @Override
        synchronized public void onAFReceived(long freq) {
            LogDns.v(TAG, "onAFReceived : " + LogDns.filter(freq));
            if (freq != -1) {
                int freqInt = (int) (freq / 1000f);
                int freqDec = (int) ((freq / 1000f) * 10 % 10);
                RadioToast.showToast(MainActivity.this,
                        getString(R.string.toast_af_success, freqInt, freqDec), Toast.LENGTH_SHORT);
            } else {
                LogDns.v(TAG, "AF failed");
            }
        }

        @Override
        public void onChannelFound(long frequency) {
            if (mScanFinished) {
                return;
            }
            // update AllChannelListFragment
            AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
            if(allChannelListFragment != null && allChannelListFragment.isVisible()) {
                allChannelListFragment.notifyDataSetChanged();
            }
            // update FavouriteListFragment
            FavouriteListFragment favouriteListFragment = (FavouriteListFragment) getFragmentManager().findFragmentByTag(FavouriteListFragment.TAG);
            if(favouriteListFragment != null && favouriteListFragment.isVisible()) {
                favouriteListFragment.notifyDataSetChanged();
            }
            RadioDialogFragment dialog = (RadioDialogFragment) getFragmentManager()
                    .findFragmentByTag(String.valueOf(RadioDialogFragment.SCAN_PROGRESS_DIALOG));
            if (mIsUpdateUI && dialog != null) {
                int count = mPlayer.getFoundChannelCount();
                if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
                    String freqText = String.format(Locale.US, "%.2f", frequency / 100f);
                    if (count > 1)
                        dialog.setMessage(MainActivity.this.getString(
                                R.string.dialog_channel_found_south_region, freqText, count));
                    else
                        dialog.setMessage(MainActivity.this.getString(
                                R.string.dialog_channel_one_found_south_region, freqText, count));
                } else {
                    int freqInt = (int) (frequency / 100);
                    int freqDec = (int) ((frequency / 10) % 10);
                    String freqText = String.format("%d.%d", freqInt, freqDec);
                    if (count > 1)
                        dialog.setMessage(MainActivity.this.getString(
                                R.string.dialog_channel_found_south_region, freqText, count));
                    else
                        dialog.setMessage(MainActivity.this.getString(
                                R.string.dialog_channel_one_found_south_region, freqText, count));
                }
            }
        }

        @Override
        public void onOff(int reasonCode) {
            LogDns.v(TAG, "Event [onOff] :" + reasonCode);
            if (mRadioDNSEnable) {
                if (mViewState instanceof InformationViewState) {
                    mViewState.showPrevious();
                } else {
                    updateVisView(false);
                }
                resetVisViews();
            }
            updateButton();
            refreshScanIcon();
            refreshAddFavBtn(0);

            mFreqValueLayout.setContentDescription(getString(R.string.desc_frequency));

            // update FavouriteListFragment
            FavouriteListFragment favouriteListFragment = (FavouriteListFragment) getFragmentManager().findFragmentByTag(FavouriteListFragment.TAG);
            if(favouriteListFragment != null && favouriteListFragment.isVisible()) {
                favouriteListFragment.notifyDataSetChanged();
            }

            // update AllChannelListFragment
            AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
            if(allChannelListFragment != null && allChannelListFragment.isVisible()) {
                allChannelListFragment.notifyDataSetChanged();
            }

            mFrqBgView.initializeFrequencyBar();
            setRDSTextView("", "");
            if (reasonCode == 10) {
                RadioToast.showToast(MainActivity.this, getString(R.string.app_name) + " - "
                        + getString(R.string.toast_unavailable_in_tvout_mode), Toast.LENGTH_SHORT);
            }
            if (offCode == 1) {
                if (mEarphoneToast == null)
                    mEarphoneToast = Toast.makeText(MainActivity.this, null, Toast.LENGTH_SHORT);
                mEarphoneToast.setText(R.string.headset_disconnect);
                mEarphoneToast.show();
                offCode = 0;
            }
            mRTPText = null;

            if (mIsRecording) {
                stopFMRecording();
            }
            closeDialog(RadioDialogFragment.RECORD_CANCEL_DIALOG);

            setVisibeInformationView(SHOW_TURN_ON_RADIO);

            invalidateOptionsMenu();

            FMListPlayerService mService = RecordedFileListPlayerActivity.getPlayer();
            if (mService != null && mService.isPlaying()) {
                // to avoid setting wrong mediasession playstate. do nothing
                // play FM -> play recording clip -> sometime fm onoff callback is delayed
            } else {
                Intent intent = new Intent(ACTION_PLAYSTATE_PAUSE);
                sendBroadcast(intent);
            }
            if (offCode == 7)
                finish();
        }

        @Override
        public void onOn() {
            Log.d(TAG, "Event [onOn]");

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mIsOning = false;
                }
            }, 300);

            offCode = 0;
            mPlayer.applyRds();
            mPlayer.applyAf();
            updateButton();
            if (mIsRecording) {
                resumeFMRecording();
            }
            MediaButtonReceiver.mIsFMLastPlay = true;
            setVisibeInformationView(SHOW_RDS);
            Intent intent = new Intent(ACTION_PLAYSTATE_PLAY);
            sendBroadcast(intent);
        }

        @Override
        public void onRDSDisabled() {
            Log.d(TAG, "onRDSDisabled() is called");
            mRTPText = null;
            resetRDS(mPlayer.getFrequency());
        }

        @Override
        public void onRDSEnabled() {
            Log.d(TAG, "onRDSEnabled() is called");
        }

        @Override
        public void onRDSReceived(long freq, String channelName, String radioText) {
            Log.d(TAG, "[onRDSReceived] freq:" + Log.filter(freq) + "ChannelName:" + channelName
                    + "RadioText:" + radioText);

            boolean isUpdatedRDSChName = false;
            boolean isUpdatedRDSText = false;

            if (channelName != null && !channelName.equals("")) {
                isUpdatedRDSChName = true;
            }
            if (radioText != null && !radioText.equals("")) {
                if (radioText.equals(mRtText.getText().toString())) {
                    Log.d(TAG, "RT is same.");
                    mRtText.setSelected(true);
                } else {
                    isUpdatedRDSText = true;
                    mRTPText = null;
                    Log.d(TAG, "RDS String is set on text field");
                }
            }

            if (mRdsPanel.getAnimation() != null && (isUpdatedRDSChName || isUpdatedRDSText)) {
                mRdsPanel.clearAnimation();
                AlphaAnimation displayAnimation = new AlphaAnimation(0.f, 1.f);
                displayAnimation.setDuration(333);
                displayAnimation.setFillEnabled(true);
                displayAnimation.setFillAfter(true);
                displayAnimation.setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        ;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        ;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mRdsPanel.clearAnimation();
                    }
                });
                mRdsPanel.startAnimation(displayAnimation);
            }

            if (isUpdatedRDSChName && isUpdatedRDSText) {
                setRDSTextView(channelName, radioText);
            } else if (isUpdatedRDSChName) {
                setRDSTextView(channelName, null);
            } else if (isUpdatedRDSText) {
                setRDSTextView(null, radioText);
            }
        }

        @Override
        public void recFinish() {
            Log.d(TAG, "[recFinish]");
            if (mIsRecording) {
                stopFMRecording(false);
            }
        }

        @Override
        public void onRTPlusReceived(int contentType1, int startPos1, int additionalLen1,
                int contentType2, int startPos2, int additionalLen2) {
            if (FMRadioFeature.FEATURE_DISABLERTPLUSINFO)
                return;

            RTPTagListManager rtpMgr = RTPTagListManager.getInstance(MainActivity.this);
            RTPTagList curTagList = rtpMgr.getCurTagList();
            String radioText = "";
            RTPTag tag;
            StringBuffer buf = new StringBuffer();

            for (int i = 0; i < curTagList.size(); i++) {
                tag = curTagList.getTag(i);
                buf.append(rtpMgr.getTagName(tag.getTagCode()) + ":" + tag.getInfo() + " / ");
            }
            radioText = buf.toString();
            if (radioText.length() > 4) {
                radioText = radioText.substring(0, radioText.length() - 3);
                setRDSTextView(null, radioText);
                mRTPText = radioText;
            }
            Log.d(TAG, "[onRTPlusReceived] RT+:" + radioText);
        }

        @Override
        public void onScanFinished(long[] frequency) {
            Log.d(TAG, "onScanFinished() - total:" + frequency.length);
            mIsInitialAccess = false;
            if (!mScanFinished) {
                mScanFinished = true;
                scanningOver();
            }
            // update AllChannelListFragment
            AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
            if(allChannelListFragment != null && allChannelListFragment.isVisible()) {
                allChannelListFragment.setChannelListEnable(true);
            }
        }

        @Override
        public void onScanStarted() {
            Log.v(TAG, "Event [onScanStarted]");
            // update AllChannelListFragment
            AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
            if(allChannelListFragment != null && allChannelListFragment.isVisible()) {
                allChannelListFragment.setChannelListEnable(false);
            }

            mScanFinished = false;
            RadioDialogFragment dialog = (RadioDialogFragment) getFragmentManager()
                    .findFragmentByTag(String.valueOf(RadioDialogFragment.SCAN_PROGRESS_DIALOG));
            if (mIsUpdateUI && dialog != null) {
                if (mIsInitialAccess)
                    dialog.setTitle(R.string.dialog_autoscan);
                else
                    dialog.setTitle(R.string.scan);
            }
            mChannelStore.sort();
            if (mIsRecording) {
                stopFMRecording();
            }
        }

        @Override
        public void onScanStopped(long[] frequency) {
            Log.d(TAG, "Event [onScanStopped]");
            // update AllChannelListFragment
            AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
            if(allChannelListFragment != null && allChannelListFragment.isVisible()) {
                allChannelListFragment.setChannelListEnable(true);
            }

            mIsInitialAccess = false;
            if (!mScanFinished) {
                mScanFinished = true;
                scanningOver();
            }
            mChannelStore.store();
        }

        @Override
        public void onTune(final long frequency) {
            Log.d(TAG, "Event [onTune] frequency:" + Log.filter(frequency));

            mIsOning = false;

            if (mFreqValue != null) {
                mFreqValueLayout.setContentDescription(RadioPlayer.convertToMhz((int) frequency)
                        + getString(R.string.mhz));
            }

            mRTPText = null;
            mCurrentFreq = (int) frequency;

            if (!mIsRecording) {
                setVisibeInformationView(SHOW_RDS);
            } else {
                setVisibeInformationView(SHOW_RECORDING_TIME);
            }
            resetRDS(mCurrentFreq);
            if (!FMRadioFeature.FEATURE_DISABLERTPLUSINFO) {
                RTPTagListManager rtpMgr = RTPTagListManager.getInstance(MainActivity.this);
                rtpMgr.clearCurTagList();
                ArrayList<RTPTagList> tagListArray = rtpMgr.getTagListArray();
                if (tagListArray != null && tagListArray.size() == 0) {
                    RadioApplication.setRtPlusEnabled(false);
                }
            }

            refreshScanIcon();
            mFrqBgView.setOnFrequencyChangeListener(_instance);
            mFrqBgView.setFrequency(mCurrentFreq);

            refreshAddFavBtn(mCurrentFreq);

            // update FavouriteListFragment
            FavouriteListFragment favouriteListFragment = (FavouriteListFragment) getFragmentManager().findFragmentByTag(FavouriteListFragment.TAG);
            if(favouriteListFragment != null && favouriteListFragment.isVisible()) {
                favouriteListFragment.notifyDataSetChanged();
            }

            // update AllChannelListFragment
            AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
            if(allChannelListFragment != null && allChannelListFragment.isVisible()) {
                allChannelListFragment.notifyDataSetChanged();
                allChannelListFragment.autoSmoothScrollChannelList();
            }

            if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
                mFreqValue
                        .setText(String.format(Locale.US, "%.2f", (float) mCurrentFreq / 100));
            } else {
                mFreqValue
                        .setText(String.format(Locale.US, "%.1f", (float) mCurrentFreq / 100));
            }

            if (mRadioDNSEnable) {
                if (mViewState instanceof InformationViewState) {
                    mViewState.showPrevious();
                } else {
                    updateVisView(false);
                }
                resetVisViews();
            }

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mNextDown) {
                        if (mPlayer.isBusy()) {
                            Log.d(TAG, "RadioPlayer is busy. ignore it");
                            return;
                        }
                        Log.d(TAG, "[touch down - Control Next]");
                        if (mPlayer.isOn()) {
                            if (mCurrentFreq > RadioPlayer.FREQ_MAX)
                                return;
                            setVisibeInformationView(SHOW_SEEKING);
                            mPlayer.seekUpAsync();
                        } else {
                            RadioToast
                                    .showToast(
                                            MainActivity.this,
                                            getString(R.string.turn_on_radio,
                                                    getString(R.string.app_name)),
                                            Toast.LENGTH_SHORT);
                        }
                    } else if (mPrevDown) {
                        if (mCurrentFreq < RadioPlayer.FREQ_MIN)
                            return;
                        if (mPlayer.isBusy()) {
                            Log.d(TAG, "RadioPlayer is busy. ignore it");
                            return;
                        }
                        Log.d(TAG, "[onClick - Control Prev]");
                        if (mPlayer.isOn()) {
                            setVisibeInformationView(SHOW_SEEKING);
                            mPlayer.seekDownAsync();
                        } else {
                            RadioToast
                                    .showToast(
                                            MainActivity.this,
                                            getString(R.string.turn_on_radio,
                                                    getString(R.string.app_name)),
                                            Toast.LENGTH_SHORT);
                        }
                    }
                }
            }, 200);
            
            if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_BRAODCOM)
                && Settings.System.getInt(getContentResolver(), "all_sound_off", 0) != 1){
                mPlayer.mute(false);
            } else {
                String unmute = "fm_radio_mute=0";
                MainActivity._instance.mAudioManager.setParameters(unmute);
            }
        }

        private void scanningOver() {
            int channelCount = mPlayer.getFoundChannelCount();
            Log.d(TAG, "count is :" + channelCount);
            if (!mIsUpdateUI)
                return;

            // need to notify list in case of no channel found. In rest cases, it is notified in onChannelFound() callback.
            if (channelCount == 0){
                AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
                if(allChannelListFragment != null && allChannelListFragment.isVisible()) {
                    allChannelListFragment.notifyDataSetChanged();
                }
                FavouriteListFragment favouriteListFragment = (FavouriteListFragment) getFragmentManager().findFragmentByTag(FavouriteListFragment.TAG);
                if(favouriteListFragment != null && favouriteListFragment.isVisible()) {
                    favouriteListFragment.notifyDataSetChanged();
                }
            }
            closeDialog(RadioDialogFragment.SCAN_PROGRESS_DIALOG);
            if (!isFinishing()) {
                if (mIsActive) {
                    int count = RadioPlayer.getInstance().getFoundChannelCount();
                    String scanMsg = null;
                    switch (count) {
                    case 0:
                        scanMsg = getString(R.string.toast_no_channel_found);
                        break;
                    case 1:
                        scanMsg = getString(R.string.toast_one_channel_found);
                        break;
                    default:
                        scanMsg = getString(R.string.toast_channel_found, count);
                        break;
                    }
                    closeDialog(RadioDialogFragment.SCAN_PROGRESS_DIALOG);
                    RadioToast.showToast(MainActivity.this, scanMsg, Toast.LENGTH_SHORT);
                }
            }
        }
    };

    private TextView mFreqValue;
    private LinearLayout mFreqValueLayout;
    public MyHandler mHandler;

    private boolean mIsInitialAccess = false;
    private boolean mIsPlaybackMode;

    private boolean mIsUpdateUI;
    private Menu mOptionsMenu = null;
    private ImageButton mNextButton;
    private int mOrientation;
    private RadioPlayer mPlayer;
    public KeyguardManager mKeyguardManager;
    private FrequencyDisplayBgView mFrqBgView;
    private TextView mPsText;
    private TextView mRtText;
    private ImageButton mPrevButton;
    private String RTPlus_perf;
    private String RTPlus_album;
    private FrameLayout mRecordingControl;
    private LinearLayout mVisInfo;
    private LinearLayout mFreqLayout;
    private ImageView mVisBtn;
    private String mRTPText;
    private ImageButton mRecordButton;
    private ImageView mRecordStopButton;
    private ImageView mRecordPauseResumeButton;
    private ImageView mRecordCancelButton;
    private TextView mRecordingDisplayText;
    private ImageView mRecordingStatus;
    private LinearLayout mRecTime;
    private LinearLayout mStationInfo;
    private TextView mRecTimeHHMMSS;
    private static int mSeconds;
    private String mFileName;
    private static String mHiddenFileName;
    public static boolean mIsRecording;
    public static boolean mIsRecordingPause;
    private static SecMediaRecorder mRecorder;
    private boolean mScanFinished;
    private TextView mSeeking;
    private LinearLayout mRdsPanel = null;
    private TextView mScanBtn;
    private View mBottomLine;
    private View topBarActionBarView = null;

    private int RECORDING_VOLUME = 7;
    public static int mCurrentFadeVolume = 0;
    public boolean mIsFadeVolume;
    private View mAddFavBtn;
    public static final String IS_FAV_BTN_CLICK = "isFavBtnClick";
    private boolean mIsOning = false;

    BroadcastReceiver mDnsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int mVisProtocol = intent.getIntExtra(DNSEvent.DNS_ACTION_VIS_PROTOCOL, 0);
            LogDns.d("FMApp", "onReceive() - action, Protocol : " + LogDns.filter(action) + "  "
                    + mVisProtocol);
            if (DNSEvent.DNS_ACTION_UPDATE_SHOW.equals(action)) {
                updateVisImage();
            } else if (DNSEvent.DNS_ACTION_UPDATE_TEXT.equals(action)) {
                updateVisText();
            } else if (DNSEvent.DNS_ACTION_UPDATE_VIS_DATA.equals(action)) {
                if (mVisInfo.getVisibility() != View.VISIBLE) {
                    updateVisButton();
                } else {
                    mDNSBoundService.runDNSSystem(new ModeData(DNSEvent.DNS_UPDATA_VIS));
                }
            } else if (DNSEvent.DNS_ACTION_UPDATE_ISSTREAM.equals(action)
                    || DNSEvent.DNS_ACTION_UPDATE_NOW_EPG_DATA.equals(action)) {
                invalidateOptionsMenu();
            } else if (DNSEvent.DNS_ACTION_UPDATE_DATA.equals(action)) {
                if (!(mViewState instanceof InformationViewState))
                    updateVisButton();
            }
        }
    };

    private boolean isTestMode = false;
    BroadcastReceiver mVolumeRec = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SamsungAudioManager.SAMSUNG_VOLUME_CHANGED_ACTION)) {
                int volume = intent.getIntExtra(SamsungAudioManager.SAMSUNG_EXTRA_VOLUME_STREAM_VALUE, 0);
                if (mIsRecording) {
                    if (volume != RECORDING_VOLUME && !mIsFadeVolume) {
                        setVolume(RECORDING_VOLUME);
                    }
                    return;
                }
            }
        }
    };

    private boolean mIsActive;

    private void startAutoScan() {
        Log.v(TAG, "startAutoScan()");
        if (startScan(true)) {
            if (!isFinishing()) {
                RadioToast.showToast(MainActivity.this, R.string.toast_initial_access,
                        Toast.LENGTH_SHORT);
                openDialog(RadioDialogFragment.SCAN_PROGRESS_DIALOG);
            }
        }
    }

    private void handleEarPhoneClick() {
        int progress = 0;
            if (!mIsRecording && SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_BRAODCOM)){
                mPlayer.mute(true);
                String mute = "fm_radio_mute=1";
                mAudioManager.setParameters(mute);
            }
            else{
                String mute = "fm_radio_mute=1";
                mAudioManager.setParameters(mute);
            }

        if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_SILICON)) {
            if (!mIsRecording)
                mPlayer.setVolume(0);
        }

        if (mIsRecording) {
            mAudioManager.setStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO), mReturnFadeVolume,
                    mFlagSteamVolume);
        }
        mPlayer.applyStereo();
        if (!mAudioManager.isRadioSpeakerOn()) {
            Log.d(TAG, "[speaker click ]speaker on");
            mPlayer.enableSpeaker();
            if (mIsRecording) {
                progress = RECORDING_VOLUME;
                mReturnFadeVolume = mAudioManager.getStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO));
            } else {
                progress = mAudioManager.getStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO));
            }
            setVolume(progress);

            if (progress != 0) {
                    if (!mIsRecording && SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_BRAODCOM)
                        && Settings.System.getInt(getContentResolver(), "all_sound_off", 0) != 1){
                        mPlayer.mute(false);
                        String unmute = "fm_radio_mute=0";
                        mAudioManager.setParameters(unmute);
                    } else {
                        String unmute = "fm_radio_mute=0";
                        mAudioManager.setParameters(unmute);                
                    }
            }
            RadioToast.showToast(MainActivity.this, R.string.sound_path_speaker_mode,
                    Toast.LENGTH_SHORT);
        } else if (mAudioManager.isRadioSpeakerOn()) {
            Log.d(TAG, "[speaker click ]speaker off");
            mPlayer.disableSpeaker();
            if (mIsRecording) {
                progress = RECORDING_VOLUME;
                mReturnFadeVolume = mAudioManager.getStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO));
            } else {
                progress = mAudioManager.getStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO));
                if (progress > EAR_SHOCK_VOLUME_VALUE && mPlayer.isOn()) {
                    progress = EAR_SHOCK_VOLUME_VALUE;
                }
            }
            setVolume(progress);
            if (progress != 0) {
                //if (!mIsRecording){
                    if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_BRAODCOM)
                        && Settings.System.getInt(getContentResolver(), "all_sound_off", 0) != 1){
                        mPlayer.mute(false);
                     } else {
                        String unmute = "fm_radio_mute=0";
                        mAudioManager.setParameters(unmute);
                    }
                //}
            }

            RadioToast.showToast(MainActivity.this, R.string.sound_path_earphone_mode,
                    Toast.LENGTH_SHORT);
        }
    }

    private void hideRecoder() {
        mRecordButton.setVisibility(View.VISIBLE);
        mRecordButton.setClickable(true);
        mRecordingControl.setVisibility(View.GONE);
        if (mPlayer.isOn()) {
            setVisibeInformationView(SHOW_RDS);
        } else {
            setVisibeInformationView(SHOW_TURN_ON_RADIO);
        }
        mSeconds = 0;
        mIsFadeVolume = false;
        if (mHandler.hasMessages(VOLUME_FADE)) {
            mHandler.removeMessages(VOLUME_FADE);
        }
    }

    private LruCache<Integer, View> mViewCache = new LruCache<Integer, View>(2);
    private ImageButton mPowerButton;
    private TextView mRecText;
    private Locale mLocale;
    private final static int EAR_SHOCK_VOLUME_VALUE = AudioManager.getEarProtectLimitIndex()-1;;

    private synchronized View getContentView(int layoutResID) {
        View result = null;
        final int orientation = mOrientation;
        result = mViewCache.get(orientation);
        if (result == null) {
            result = getLayoutInflater().inflate(layoutResID, null);
            mViewCache.put(orientation, result);
        }
        return result;
    }

    private void myOnCreate() {
        Log.v(TAG, "myOnCreate ----------");
        Log.d(TAG, "setContentView ------");
        setContentView(getContentView(R.layout.main));
        setTopPanelOnActionBar();
        Log.d(TAG, "setContentView ------ end");
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mPsText = (TextView) findViewById(R.id.ps_text);
        mRtText = (TextView) findViewById(R.id.rt_text);
        mRtText.setSelected(true);

        mFrqBgView = (FrequencyDisplayBgView) findViewById(R.id.frq_dsp_bg_view);
        mFrqBgView.setContentDescription(getString(R.string.desc_frequency_slider));
        mFrqBgView.initializeFrequencyBar();
        mFrqBgView.setOnTouchListener(this);
        mFrqBgView.setOnKeyListener(mKeyListener);
        mFreqValue = (TextView) findViewById(R.id.freq_value);
        mFreqValue.setText("00.0");
        mFreqValue.setOnClickListener(mClickListener);
        mFreqValueLayout = (LinearLayout) findViewById(R.id.freq_value_layout);

        mRecordingDisplayText = (TextView) findViewById(R.id.rec_text);
        mNextButton = (ImageButton) findViewById(R.id.dial_next);
        mNextButton.setContentDescription(getString(R.string.desc_next));
        mNextButton.setOnClickListener(mClickListener);
        mNextButton.setOnLongClickListener(mLongClickListener);
        mNextButton.setOnTouchListener(mTouchListener);
        mNextButton.setOnKeyListener(mKeyListener);
        mPrevButton = (ImageButton) findViewById(R.id.dial_prev);
        mPrevButton.setContentDescription(getString(R.string.desc_prev));
        mPrevButton.setOnClickListener(mClickListener);
        mPrevButton.setOnLongClickListener(mLongClickListener);
        mPrevButton.setOnTouchListener(mTouchListener);
        mPrevButton.setOnKeyListener(mKeyListener);


        mSeeking = (TextView) findViewById(R.id.seeking);
        mAddFavBtn = (View) findViewById(R.id.add_fav_btn);
        mAddFavBtn.setOnClickListener(mClickListener);

        mFreqLayout = (LinearLayout) findViewById(R.id.frq_bg);
        mVisInfo = (LinearLayout) findViewById(R.id.vis_info);
        mVisImageView = (ImageView) findViewById(R.id.dns_vis_image);
        mVisTextView = (TextView) findViewById(R.id.dns_vis_text);
        mVisBtn = (ImageView) findViewById(R.id.vis_btn);
        if (!FMRadioFeature.FEATURE_DISABLEDNS) {
            mVisBtn.setVisibility(View.VISIBLE);
        } else {
            mVisBtn.setVisibility(View.INVISIBLE);
        }
        mVisBtn.setContentDescription(getString(R.string.desc_button, getString(R.string.information)));
        mVisBtn.setOnClickListener(mClickListener);
        buttonImageTextChanged(mVisBtn, false);

        if (mViewState == null) {
            mViewState = getSharedPreferences(RadioApplication.PREF_FILE, MODE_PRIVATE).getBoolean(
                    IS_FAV_BTN_CLICK, false) ? mStationViewState : mDialViewState;
        }
        mViewState.show();
        mRdsPanel = (LinearLayout) findViewById(R.id.rds_panel);
        mRecordingStatus = (ImageView) findViewById(R.id.rec_icon);
        mRecTime = (LinearLayout) findViewById(R.id.recording_time);
        mRecTimeHHMMSS = (TextView) findViewById(R.id.rec_time);
        mStationInfo = (LinearLayout) findViewById(R.id.turningtext);
        if (DnsTestActivity.isDnsTest()) {
            mRdsPanel.setOnClickListener(mClickListener);
            mStationInfo.setOnClickListener(mClickListener);
        }
        mScanBtn = (TextView) findViewById(R.id.scan_btn);
        mScanBtn.setOnClickListener(mClickListener);
        mScanBtn.setContentDescription(getString(R.string.desc_button, getString(R.string.scan)));
        mBottomLine = (View) findViewById(R.id.bottom_line);

        if (mPlayer.isOn() && !mPlayer.isScanning() && !mPlayer.isSeeking()) {
            int channel = mPlayer.getFrequency();
            if (channel != -1) {
                mCurrentFreq = channel;
                mFrqBgView.setFrequency(mCurrentFreq);
                if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
                    mFreqValue
                            .setText(String.format(Locale.US, "%.2f", (float) mCurrentFreq / 100));
                } else {
                    mFreqValue
                            .setText(String.format(Locale.US, "%.1f", (float) mCurrentFreq / 100));
                }
                Log.d(TAG, "setting initial freq - on:" + Log.filter(mCurrentFreq));
            }
        } else {
            int freq = RadioApplication.getInitialFrequency();
            mCurrentFreq = freq;
            mFrqBgView.setFrequency(mCurrentFreq);
            if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
                mFreqValue.setText(String.format(Locale.US, "%.2f", (float) mCurrentFreq / 100));
            } else {
                mFreqValue.setText(String.format(Locale.US, "%.1f", (float) mCurrentFreq / 100));
            }
            Log.d(TAG, "setting initial freq:" + Log.filter(freq));
        }
        setPauseResumeBtnImage(mIsRecordingPause);
        if (mRadioDNSEnable) {
            if (mDNSBoundService == null) {
                mDNSBoundService = DNSService.bindService(MainActivity.this, mDNSServiceConnection);
            }
        }
        initializeTabContent();
        Log.d(TAG, "myOnCreate ------ -End");
    }

    TabHost.OnTabChangeListener mTabChangeListener = new TabHost.OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            Log.d(TAG, "onTabChanged : "+tabId);
            android.app.FragmentManager fm = getFragmentManager();
            AllChannelListFragment allChannelListFragment = (AllChannelListFragment) fm.findFragmentByTag(AllChannelListFragment.TAG);
            FavouriteListFragment favouriteListFragment = (FavouriteListFragment) fm.findFragmentByTag(FavouriteListFragment.TAG);
            FragmentTransaction ft = fm.beginTransaction();

            if(allChannelListFragment != null)
                ft.detach(allChannelListFragment);

            if(favouriteListFragment != null)
                ft.detach(favouriteListFragment);

            if(tabId.equalsIgnoreCase(AllChannelListFragment.TAG)) {
                if(allChannelListFragment == null){
                    ft.add(R.id.realtabcontent,AllChannelListFragment.newInstance(), AllChannelListFragment.TAG);
                }else{
                    ft.attach(allChannelListFragment);
                }
            }else{
                if (favouriteListFragment == null) {
                    ft.add(R.id.realtabcontent,FavouriteListFragment.newInstance(), FavouriteListFragment.TAG);
                 }else {
                    ft.attach(favouriteListFragment);
                }
            }
            ft.commitAllowingStateLoss();
            refreshScanIcon();
        }
    };

    private void initializeTabContent() {
        Log.d(TAG, "initializeTabContent : ");
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabWidget= (TabWidget)mTabHost.findViewById(android.R.id.tabs);
        if(mTabWidget != null) 
        	mTabWidget.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        if (mTabHost == null)
            return;

        mTabHost.setup();
        mTabHost.clearAllTabs();
        mTabHost.setOnTabChangedListener(mTabChangeListener);

        /** Defining tab builder for Favorite tab */
        TabHost.TabSpec mFavChannels = mTabHost.newTabSpec(FavouriteListFragment.TAG);
        mFavChannels.setIndicator(getTabSelection(mTabHost.getContext(),getResources().getString(R.string.fav_channel)));
        mFavChannels.setContent(new FMTabContent(getBaseContext()));
        mTabHost.addTab(mFavChannels);

        /** Defining tab builder for All Channels tab */
        TabHost.TabSpec mAllChannels = mTabHost.newTabSpec(AllChannelListFragment.TAG);
        mAllChannels.setIndicator(getTabSelection(mTabHost.getContext(),getResources().getString(R.string.allchannels)));
        mAllChannels.setContent(new FMTabContent(getBaseContext()));
        mTabHost.addTab(mAllChannels);

    }

    private View getTabSelection(Context context, String title) {
        View view = LayoutInflater.from(context).inflate(R.layout.tab_layout, null);
        TextView textView = (TextView) view.findViewById(R.id.textView);
        textView.setText(title);
        textView.setContentDescription(getString(R.string.desc_tab, title));
        return view;
    }

    private void myOnResume(boolean isConfigChange) {
        Log.d(TAG, "  myOnResume -------------------");
        updateButton();
        mPlayer.applyRds();
        mRdsPanel.requestFocus();
        mHandler.removeMessages(SET_CHANNEL_INFO);
        mHandler.sendEmptyMessageDelayed(SET_CHANNEL_INFO, 100);
        if (mPlayer.isOn()) {
            refreshAddFavBtn(mCurrentFreq);
            MediaButtonReceiver.mIsFMLastPlay = true;
            if (!mPlayer.isScanning() && !mPlayer.isSeeking())
                resetRDS(mPlayer.getFrequency());
            mFrqBgView.setOnFrequencyChangeListener(_instance);
            mFrqBgView.setFrequency(mPlayer.getFrequency());
        }
        refreshScanIcon();
        if (mKeyguardManager == null) {
            mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        }
        if (!isConfigChange && !mKeyguardManager.isKeyguardLocked() && mNotiMgr.isNotified()) {
            Log.d(TAG, "remove notification");
            mNotiMgr.removeNotification(false);
        }
        if (mIsPlaybackMode) {
            if (!mPlayer.isOn()) {
                if (!mHandler.hasMessages(ONCLICK_PLAY))
                    mHandler.sendEmptyMessage(ONCLICK_PLAY);
            }
        }
        mIsPlaybackMode = false;

        startService(new Intent(this, NotificationService.class));
        startService(new Intent(this, FMListPlayerService.class));

        String desc = getString(R.string.desc_frequency);
        if (mPlayer.isOn() && !mPlayer.isScanning() && !mPlayer.isSeeking())
            desc = RadioPlayer.convertToMhz(mPlayer.getFrequency()) + " " + getString(R.string.mhz);

        mFreqValueLayout.setContentDescription(desc);
        Intent intent = new Intent(ACTION_REGISTER_MEDIA_SESSION);
        sendBroadcast(intent);

        Log.d(TAG, "  myOnResume ------------------- end");
    }

    private void widgetRefresh() {
        widgetRefresh(_instance);
    }

    public static void widgetRefresh(Context context) {
        Intent intent = new Intent(FMRadioProvider.ACTION_RADIO_WIDGET_REFRESH);
        context.sendBroadcast(intent);
    }

    public void on() throws FMPlayerException {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        switch (wm.getDefaultDisplay().getRotation()) {
        case Surface.ROTATION_90:
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            break;
        case Surface.ROTATION_180:
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
            break;
        case Surface.ROTATION_270:
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            break;
        default:
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        if (FMUtil.isVoiceActive(_instance,FMUtil.NEED_TO_PLAY_FM)) {
            setVisibeInformationView(SHOW_TURN_ON_RADIO);
            mIsInitialAccess = false;
            Log.d(TAG, "on() :: isUsedVoice is true. on is canceled.");
            return;
        }
        boolean result = false;
        try {
            result = mPlayer.turnOn();
        } catch (FMPlayerException e) {
            mStationInfo.setVisibility(View.GONE);
            mIsInitialAccess = false;
            RadioToast.showToast(MainActivity.this, e);
            throw e;
        } finally {
            if (!result) {
                Log.d(TAG, "on() :: on is failed.");
                setVisibeInformationView(SHOW_TURN_ON_RADIO);
                mIsInitialAccess = false;
            } else {
                SettingsActivity.activateTurnOffAlarm();
            }
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    @Override
    public void onActionModeFinished(ActionMode mode) {
        super.onActionModeFinished(mode);
        if (!mNeedToRetainActionMode) {
            topBarActionBarView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);
        topBarActionBarView.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ChannelNameToSpeech.TEXT_TO_SPEECH) {
            ChannelNameToSpeech.getInstance().activityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "configuration changed :" + newConfig);
        mOrientation = newConfig.orientation;
        Log.d(TAG, "configuration changed :" + newConfig);
        if (!mLocale.equals(newConfig.locale)) {
            getResources().updateConfiguration(newConfig, getResources().getDisplayMetrics());
            mRecText.setText(R.string.recording);
        }
        mLocale = newConfig.locale;

        View focusedView = this.getCurrentFocus();
        int focusedViewId = 0;
        if (focusedView != null) {
            focusedViewId = focusedView.getId();
        }

        boolean seekingVisible = false;
        if (mStationInfo != null && mStationInfo.getVisibility() == View.VISIBLE) {
            seekingVisible = true;
        }

        saveCurrentTabState(mTabHost.getCurrentTab());
        myOnCreate();
        setCurrentTabState();
        Log.d(TAG, "mActionMode  :" + mActionMode);

        if (seekingVisible && mPlayer.isOn()) {
            setVisibeInformationView(SHOW_SEEKING);
        } else {
            setVisibeInformationView(SHOW_RDS);
        }

        if (focusedViewId != 0) {
            if(findViewById(focusedViewId) != null)
                findViewById(focusedViewId).requestFocus();
        } else {
            mRecordButton.requestFocus();
        }
        myOnResume(true);
        Log.d(TAG, "configuration  mRecorder = " + mRecorder + " mIsRecording = " + mIsRecording);
        if (mRecorder != null && mIsRecording) {
            updateButton();
            showRecoder();
            updateRecordedTime();
        } else {
            hideRecoder();
            updateButton();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("VerificationLog", "onCreate" + mOrientation);
        Log.d(TAG, "oncreate -------------------");

        String action = getIntent().getAction();
        if ((action != null) && (action.startsWith("test.mode"))) {
            mRadioDNSEnable = false;
        }
        if (mRadioDNSEnable) {
            if (mDNSBoundService == null) {
                mDNSBoundService = DNSService.bindService(MainActivity.this, mDNSServiceConnection);
                Log.v("Rahul", "Service is Bind:" + mDNSBoundService);
            }
        }
        _instance = this;
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.samsungFlags |= WindowManager.LayoutParams.SAMSUNG_FLAG_ENABLE_STATUSBAR_OPEN_BY_NOTIFICATION;
        getWindow().setAttributes(lp);

        mPlayer = RadioPlayer.getInstance();
        mResources = getResources();
        mOrientation = mResources.getConfiguration().orientation;
        mLocale = mResources.getConfiguration().locale;
        if (mPlayer == null) {
            Log.d(TAG, "FMRadio Service Is NULL. Exit FMRadio");
            finish();
            return;
        }
        mChannelStore = ChannelStore.getInstance();
        mIsPlaybackMode = getIntent().getBooleanExtra("playback", false);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        
        mTts = new TextToSpeech(this, this);

        Log.d(TAG, "FMRadio getAction is " + Log.filter(action));
        if ((action != null) && (action.startsWith("test.mode"))) {
            System.out.println("going into test mode");
            isTestMode = true;
            TestMode.getInstance(MainActivity.this).handleIntent(getIntent());
            return;
        }

        RECORDING_VOLUME = FMRadioFeature.FEATURE_RECORDINGVOLUME != -1 ? FMRadioFeature.FEATURE_RECORDINGVOLUME
                : (mAudioManager.getStreamMaxVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO)) / 2);

        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate savedinstancestate");
            mSavedSelectedFreq = savedInstanceState.getInt("selected_freq");
            mIsRecordingPause = savedInstanceState.getInt("isRecordingPause") == 1 ? true : false;
            mSeconds = savedInstanceState.getInt("recorded_time", mSeconds);
        }

        mHandler = new MyHandler();

        myOnCreate();

        registerReceiver(mVolumeRec, new IntentFilter(SamsungAudioManager.SAMSUNG_VOLUME_CHANGED_ACTION));
        if (mRadioDNSEnable) {
            registerDNSIntentFilter();
        }
        Log.d(TAG, "setting the listner");
        mPlayer.registerListener(mFMListener);

        if (!mIsRecording) {
            Intent intent = new Intent(ACTION_VOLUME_UNLOCK);
            sendBroadcast(intent);
            Settings.System.putInt(getContentResolver(), "fm_record_enable", 0);
            Log.d(TAG, "init fm_record_enable, 0");
        }

        registerBroadcastReceiverSDCard(true);
        registerBroadcastReceiverLowBattery(true);
        registerBroadcastReceiverTurningOn(true);
        mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        registerRestoreReceiver();
        registerLockTaskListener();

        Log.v(TAG, "oncreate ------------------- end");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.clear();
        menu.add(0, MENU_REMOVE, 0, R.string.remove);
        menu.add(0, MENU_EDIT, 0, R.string.menu_edit);
            if (mAudioManager.isRadioSpeakerOn()) {
                menu.add(0, MENU_CHANGE_SOUND_PATH, 0, R.string.via_earphone);
                if (mAudioManager.isWiredHeadsetOn()) {
                    menu.findItem(MENU_CHANGE_SOUND_PATH).setVisible(true);
                } else {
                    menu.findItem(MENU_CHANGE_SOUND_PATH).setVisible(false);
                }
            } else {
                menu.add(0, MENU_CHANGE_SOUND_PATH, 0, R.string.via_speaker);
                if (mAudioManager.isWiredHeadsetOn()) {
                    menu.findItem(MENU_CHANGE_SOUND_PATH).setVisible(true);
                } else {
                    menu.findItem(MENU_CHANGE_SOUND_PATH).setVisible(false);
                }
            }

        if (mRadioDNSEnable) {
            boolean isOn = false;
            isOn = mPlayer.isOn();
            mPlayingInternetStreaming = RadioDNSServiceDataIF.isEpgPlayingStreamRadio();
            EpgData data = RadioDNSServiceDataIF.getEpgNowEpgData();
            mEpgStreamAvailable = ((data != null) && (data.getStreamUrl() != null)) ? true : false;
            if (!isOn) {
                menu.add(0, MENU_SWITCH_INTERNET_STREAMING, 0, R.string.switch_to_internet_radio);
                menu.findItem(MENU_SWITCH_INTERNET_STREAMING).setVisible(false);
            } else if (!mPlayingInternetStreaming) {
                menu.add(0, MENU_SWITCH_INTERNET_STREAMING, 0, R.string.switch_to_internet_radio);
                menu.findItem(MENU_SWITCH_INTERNET_STREAMING).setVisible(mEpgStreamAvailable);
            } else {
                menu.add(0, MENU_SWITCH_INTERNET_STREAMING, 0, R.string.switch_to_fm_radio);
                menu.findItem(MENU_SWITCH_INTERNET_STREAMING).setVisible(true);
            }
        }
        menu.add(0, MENU_RECORDED_FILES, MENU_RECORDED_FILES, R.string.recordings);
        menu.add(0, MENU_SETTINGS, MENU_SETTINGS, R.string.settings);

        return true;
    
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(0, MENU_REMOVE, 0, R.string.remove);
        menu.add(0, MENU_EDIT, 0, R.string.menu_edit);
        menu.findItem(MENU_REMOVE).setVisible(false);
        menu.findItem(MENU_EDIT).setVisible(false);

        FavouriteListFragment favouriteListFragment = (FavouriteListFragment) getFragmentManager().findFragmentByTag(FavouriteListFragment.TAG);
        AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
        if(allChannelListFragment != null && allChannelListFragment.isVisible() && mChannelStore!= null && mChannelStore.size() > 0) {
            menu.findItem(MENU_EDIT).setVisible(true);
        } else if(favouriteListFragment != null && favouriteListFragment.isVisible() && favouriteListFragment.getFavouritesCount() > 0){
            menu.findItem(MENU_REMOVE).setVisible(true);
        }
            if (mAudioManager.isRadioSpeakerOn()) {
                menu.add(0, MENU_CHANGE_SOUND_PATH, 0, R.string.via_earphone);
                if (mAudioManager.isWiredHeadsetOn()) {
                    menu.findItem(MENU_CHANGE_SOUND_PATH).setVisible(true);
                } else {
                    menu.findItem(MENU_CHANGE_SOUND_PATH).setVisible(false);
                }
            } else {
                menu.add(0, MENU_CHANGE_SOUND_PATH, 0, R.string.via_speaker);
                if (mAudioManager.isWiredHeadsetOn()) {
                    menu.findItem(MENU_CHANGE_SOUND_PATH).setVisible(true);
                } else {
                    menu.findItem(MENU_CHANGE_SOUND_PATH).setVisible(false);
                }
            }

        if (mRadioDNSEnable) {
            boolean isOn = false;
            isOn = mPlayer.isOn();
            mPlayingInternetStreaming = RadioDNSServiceDataIF.isEpgPlayingStreamRadio();
            EpgData data = RadioDNSServiceDataIF.getEpgNowEpgData();
            mEpgStreamAvailable = ((data != null) && (data.getStreamUrl() != null)) ? true : false;
            if (!isOn) {
                menu.add(0, MENU_SWITCH_INTERNET_STREAMING, 0, R.string.switch_to_internet_radio);
                menu.findItem(MENU_SWITCH_INTERNET_STREAMING).setVisible(false);
            } else if (!mPlayingInternetStreaming) {
                menu.add(0, MENU_SWITCH_INTERNET_STREAMING, 0, R.string.switch_to_internet_radio);
                menu.findItem(MENU_SWITCH_INTERNET_STREAMING).setVisible(mEpgStreamAvailable);
            } else {
                menu.add(0, MENU_SWITCH_INTERNET_STREAMING, 0, R.string.switch_to_fm_radio);
                menu.findItem(MENU_SWITCH_INTERNET_STREAMING).setVisible(true);
            }
        }
        menu.add(0, MENU_RECORDED_FILES, MENU_RECORDED_FILES, R.string.recordings);
        menu.add(0, MENU_SETTINGS, MENU_SETTINGS, R.string.settings);

        return true;
    }

    private void registerDNSIntentFilter() {
        registerReceiver(mDnsReceiver, new IntentFilter(DNSEvent.DNS_ACTION_PROGRAM_INFO));
        registerReceiver(mDnsReceiver, new IntentFilter(DNSEvent.DNS_ACTION_UPDATE_SHOW));
        registerReceiver(mDnsReceiver, new IntentFilter(DNSEvent.DNS_ACTION_UPDATE_TEXT));
        registerReceiver(mDnsReceiver, new IntentFilter(DNSEvent.DNS_ACTION_UPDATE_DATA));
        registerReceiver(mDnsReceiver, new IntentFilter(DNSEvent.DNS_ACTION_UPDATE_VIS_DATA));
        registerReceiver(mDnsReceiver, new IntentFilter(DNSEvent.DNS_ACTION_UPDATE_ISSTREAM));
        registerReceiver(mDnsReceiver, new IntentFilter(DNSEvent.DNS_ACTION_UPDATE_NOW_EPG_DATA));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        RadioDialogFragment renameDialog = (RadioDialogFragment) getFragmentManager()
                .findFragmentByTag(String.valueOf(RadioDialogFragment.ITEM_RENAME_DIALOG));
        if (renameDialog != null && renameDialog.isResumed()) {
            if (mSelectedChannel != null || mSavedSelectedFreq != -1) {
                int freq = (mSelectedChannel != null) ? mSelectedChannel.mFreqency
                        : mSavedSelectedFreq;
                outState.putInt("selected_freq", freq);
            }
        }
        outState.putInt("isRecordingPause", mIsRecordingPause ? 1 : 0);
        outState.putInt("recorded_time", mSeconds);
        super.onSaveInstanceState(outState);
    }

    public static final String FREQ_INPUT_PATTERN =
    // 10X.X
    "^1$|^10$|^10[0-8]$|^10[0-8]\\.$|^10[0-7]\\.\\d$|^108\\.0$" +
    // 8X.X
            "|^8$|^8[7-9]$|^8[7-9]\\.$|^87\\.[5-9]$|^8[8-9]\\.\\d$" +
            // 9X.X
            "|^9$|^9[0-9]$|^9[0-9]\\.$|^9[0-9]\\.\\d$";

    public static final String FREQ_PATTERN =
    // 10X.X
    "^10[0-7]\\.\\d$|^108\\.0$" +
    // 8X.X
            "|^87\\.[5-9]$|^8[8-9]\\.\\d$" +
            // 9X.X
            "|^9[0-9]\\.\\d$";

    public static final String FREQ_INPUT_PATTERN_50_SPACE = FREQ_INPUT_PATTERN
            + "|^10[0-7]\\.\\d[05]$" + "|^87\\.[5-9][05]$|^8[8-9]\\.\\d[05]$"
            + "|^9[0-9]\\.\\d[05]$";

    public static final String FREQ_PATTERN_50_SPACE = FREQ_PATTERN + "|^10[0-7]\\.\\d[05]$"
            + "|^87\\.[5-9][05]$|^8[8-9]\\.\\d[05]$" + "|^9[0-9]\\.\\d[05]$";

    DialogInterface.OnClickListener mRenameListener = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEGATIVE)
                return;
            String freqRen = ((RenameDialog) dialog).getText();
            int freq = (mSelectedChannel != null) ? mSelectedChannel.mFreqency : mSavedSelectedFreq;
            if (freq != -1) {
                Channel channel = mChannelStore.getChannelByFrequency(freq);
                if (channel != null && freqRen != null) {
                    channel.mFreqName = freqRen;
                    if (mPlayer.getFrequency() == channel.mFreqency) {
                        setRDSTextView(channel.mFreqName, null);
                    }
                    mChannelStore.store();
                } else {
                    RadioToast.showToast(MainActivity.this, R.string.toast_rename_error,
                            Toast.LENGTH_SHORT);
                }
            } else {
                RadioToast.showToast(MainActivity.this, R.string.toast_rename_error,
                        Toast.LENGTH_SHORT);
            }
        }
    };

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy -----------");
        if (mHandler != null)
            mHandler.removeMessages(CHECK_INITIAL_ACCESS);

        if (mPlayer.isScanning()) {
            mPlayer.cancelScan();
        }

        Log.d(TAG, "we are removing the listner");
        mPlayer.unregisterListener(mFMListener);
        ChannelNameToSpeech.getInstance().destroy();
        stopSpeaking();
        if (mRadioDNSEnable) {
            try {
                unregisterReceiver(mDnsReceiver);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "onDestroy mDnsReceiver is not registered");
            }
        }

        try {
            unregisterReceiver(mVolumeRec);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "onDestroy mVolumeRec is not registered");
        }

        registerBroadcastReceiverSDCard(false);
        registerBroadcastReceiverLowBattery(false);
        registerBroadcastReceiverTurningOn(false);
        unregisterRestoreReceiver();
        unregisterLockTaskListener();
        stopService(new Intent(getBaseContext(), FMListPlayerService.class));
        if (mRadioDNSEnable) {
            if (mDNSBoundService != null) {
                DNSService.unbindService(MainActivity.this, mDNSServiceConnection);
                mDNSBoundService = null;
            }
        }
        //only for Factory Binary
        if (isTestMode){
            Log.v(TAG, "Test Mode: send appfinished intent");
            TestMode.getInstance(MainActivity.this).sendOffIntent();
        }
        Log.v(TAG, "onDestroy ------------------- end");
        super.onDestroy();
    }

    private boolean isVolumeKeyDownDuringRecording = false;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!FMUtil.isVoiceActive(_instance)) {
            if (mIsRecording
                    && Settings.System.getInt(getContentResolver(),"all_sound_off", 0) != 1
                    && (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                    && (keyCode == KeyEvent.KEYCODE_MUTE || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)
                    && !isVolumeKeyDownDuringRecording) {
                RadioToast.showToast(MainActivity.this,
                        R.string.recording_volume_control, Toast.LENGTH_SHORT);
                isVolumeKeyDownDuringRecording = true;
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_UP:
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_MUTE:
        case KeyEvent.KEYCODE_VOLUME_MUTE:
            isVolumeKeyDownDuringRecording = false;
            return true;
        default:
            break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        int id = item.getItemId();

        if (id == MENU_SETTINGS) {
            Log.d(TAG, "MENU_SETTINGS");
            startActivity(new Intent(this, SettingsActivity.class));
        }  else if (id == MENU_CHANGE_SOUND_PATH) {
            if (!mHandler.hasMessages(SPEAKER_ENABLE_DISABLE))
                mHandler.sendEmptyMessage(SPEAKER_ENABLE_DISABLE);
        } else if (id == MENU_RECORDED_FILES) {
            if(!FMPermissionUtil.hasPermission(getApplicationContext(), FMPermissionUtil.FM_PERMISSION_REQUEST_RECORDINGS_LIST)) {
                FMPermissionUtil.requestPermission(this, FMPermissionUtil.FM_PERMISSION_REQUEST_RECORDINGS_LIST);
            } else {
                Intent intent = new Intent(this, RecordedFileListPlayerActivity.class);
                startActivity(intent);
            }
        } else if (id == MENU_EDIT) {
            if(mViewState instanceof InformationViewState) {
                mViewState.showPrevious();
            }
            AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
            if(allChannelListFragment != null && allChannelListFragment.isVisible()) {
                allChannelListFragment.clearChoices();
                allChannelListFragment.startActionMode();
            }
        } else if (id == MENU_REMOVE) {
            if(mViewState instanceof InformationViewState) {
                mViewState.showPrevious();
            }
            FavouriteListFragment favouriteListFragment = (FavouriteListFragment) getFragmentManager().findFragmentByTag(FavouriteListFragment.TAG);
            if(favouriteListFragment != null && favouriteListFragment.isVisible()) {
                favouriteListFragment.clearChoices();
                favouriteListFragment.startActionMode();
            }
        } else if (mRadioDNSEnable && id == MENU_SWITCH_INTERNET_STREAMING) {
            if (mIsRecording) {
                Intent i = new Intent(this, DnsAlertDialogActivity.class);
                i.putExtra(DnsDialogFragment.KEY_TITLE, R.string.switch_to_internet_radio);
                i.putExtra(DnsDialogFragment.KEY_MSG, R.string.record_suspended_continue);
                startActivity(i);
            } else {
                if (!mHandler.hasMessages(TOGGLE_STREAMING_FMRADIO)) {
                    mHandler.sendEmptyMessage(TOGGLE_STREAMING_FMRADIO);
                }
            }
        }

        return true;
    }

    @Override
    protected void onPause() {
        Log.secD("FMApp", "onPause -------------------");
        mIsActive = false;
        registerBroadcastReceiverSip(false); /* [P130122-6311] */

        if (mDNSBoundService != null && mRadioDNSEnable) {
            mDNSBoundService.runDNSSystem(new ModeData(DNSEvent.DNS_STOP_VIS));
        }
        saveCurrentTabState(mTabHost.getCurrentTab());
        Log.secD("FMApp", "onPause ------------------- end");

        super.onPause();
    }

    private void saveCurrentTabState(int currentTab) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("selectedTab", currentTab).commit();
        mNeedToRetainActionMode = mIsActionMode;
    }

    private void setCurrentTabState() {
        mTabHost.setCurrentTab(PreferenceManager.getDefaultSharedPreferences(this).getInt("selectedTab", 0));
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mNeedToRetainActionMode) {
                    FavouriteListFragment favouriteListFragment1 = (FavouriteListFragment) getFragmentManager()
                            .findFragmentByTag(FavouriteListFragment.TAG);
                    if (favouriteListFragment1 != null && favouriteListFragment1.isVisible() && !favouriteListFragment1.isActionMode()) {
                        favouriteListFragment1.startActionMode();
                    }

                    AllChannelListFragment allChannelListFragment1 = (AllChannelListFragment) getFragmentManager()
                            .findFragmentByTag(AllChannelListFragment.TAG);
                    if (allChannelListFragment1 != null && allChannelListFragment1.isVisible() && !allChannelListFragment1.isActionMode()) {
                        allChannelListFragment1.startActionMode();
                    }
                }
                mNeedToRetainActionMode = false;
            }
        }, 10);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
         * The code for Initial Checking has been moved from OnCreate() to here.
         * When Radio was initially started without earphone, there was the
         * problem. If we insert the earphone on
         * SettingsActivity/RecordedFileListPlayerActivity, we
         * could not see the popups related "scan" operation properly.
         */
        Message msg = new Message();
        msg.what = CHECK_INITIAL_ACCESS;
        mHandler.sendMessageDelayed(msg, 500);

        setCurrentTabState();

        myOnResume(false);
        Log.d(TAG, "onResume -------------------");
        if (isTestMode)
            return;
        _instance = this;
        if (mIsRecording) {
            if(!iswindowhasfocus)
                setVolume(RECORDING_VOLUME);
            updateRecordedTime();
            showRecoder();
        } else {
            if (!isFinishing()) {
                hideRecoder();
            }
        }
        mIsActive = true;
        registerBroadcastReceiverSip(true);
        updateButton();

        if (mRadioDNSEnable && (mVisInfo.getVisibility() == View.VISIBLE)) {
            mDNSBoundService.runDNSSystem(new ModeData(DNSEvent.DNS_START_VIS));
        }

        Intent intent = new Intent(ACTION_REGISTER_MEDIA_SESSION);
        sendBroadcast(intent);
        Log.d(TAG, "onResume ------------------- end");
        Log.i("VerificationLog", "Executed");

    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        if (mIsRecording) {
            openDialog(RadioDialogFragment.RECORD_CANCEL_DIALOG);
        } else {
            super.onBackPressed();
        }
    };

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop -------------------");
        mHandler.sendEmptyMessageDelayed(SHOW_NOTIFICATION, 300);
        widgetRefresh();

        // add volume lock code when onStop for defence
        if (mIsRecording
                && RECORDING_VOLUME == mAudioManager.getStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO))) {
            Intent intent = new Intent(ACTION_VOLUME_LOCK);
            sendBroadcast(intent);
        }

        super.onStop();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, " onTouchEvent action = " + Log.filter(event.getAction()));
        if(mViewState instanceof InformationViewState) {
            mViewState.showPrevious();
        }
        return super.onTouchEvent(event);
    }

    private static int mReturnFadeVolume = 0;
    private static final String TAG = "MainActivity";

    void recordFMRadioAudio() {
        Log.d(TAG, "[recordFMRadioAudio - Record Button onClick]");

        boolean isOn = false;
        isOn = mPlayer.isOn();
        if (!isOn) {
            RadioToast.showToast(MainActivity.this,
                    getString(R.string.turn_on_radio, getString(R.string.app_name)),
                    Toast.LENGTH_SHORT);
            return;
        } else if (!getAvailableStorage(true)) {
            return;
        }

        if (mRadioDNSEnable && RadioDNSServiceDataIF.isEpgPlayingStreamRadio()) {
            mIsRecording = false;
            Settings.System.putInt(getContentResolver(), "fm_record_enable", 0);
            Log.d(TAG, "fm_record_enable, 0");
            mHandler.removeMessages(RECORD_TIME_UPDATE);
            mHiddenFileName = RadioMediaStore.makeFilePath(true);
            RadioMediaStore.deleteHiddenFile();
            mDNSBoundService.setDataTarget(mHiddenFileName);
            mDNSBoundService.setOnRecordInfoListener(sInfoListener);
            mIsFadeVolume = true;
            mCurrentFadeVolume = mAudioManager.getStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO));
            mReturnFadeVolume = mCurrentFadeVolume;
            mHandler.sendEmptyMessage(VOLUME_FADE);
            StatusBarManager mStatusBar = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
            try {
                mStatusBar.disable(StatusBarManager.DISABLE_NOTIFICATION_ALERTS);
            } catch (SecurityException e) {
                ;
            }
            mNotiMgr.setAudioSystemMute(true);
            mDNSBoundService.startRecord();
        } else {
            try {
                /* String mute = "fm_radio_mute=1";
                String unmute = "fm_radio_mute=0";
                if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_SUPPORT_CSR) {
                    mAudioManager.setParameters(mute);
                }*/
                mPlayer.setRecordMode(RECORDING_START);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (mRecorder != null) {
                    mRecorder.release();
                    mRecorder = null;
                }
                mIsRecording = false;
                Settings.System.putInt(getContentResolver(), "fm_record_enable", 0);
                Log.d(TAG, "fm_record_enable, 0");
                mHandler.removeMessages(RECORD_TIME_UPDATE);
                mRecorder = new SecMediaRecorder();
                mHiddenFileName = RadioMediaStore.makeFilePath(true);
                RadioMediaStore.deleteHiddenFile();
                mRecorder.setAudioSource(SecMediaRecorder.AudioSource.FM_RX);
                mRecorder.setOutputFormat(SecMediaRecorder.OutputFormat.THREE_GPP);// 1
                mRecorder.setOutputFile(mHiddenFileName);
                mRecorder.setAudioEncodingBitRate(128000);
                if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_BRAODCOM)
                        || SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_SPRD))
                    mRecorder.setAudioChannels(2); // Stereo
                else
                    mRecorder.setAudioChannels(1);
             /*   if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_SUPPORT_CSR) {
                    Log.d(TAG, "[recordFMRadioAudio] SamplingRate : 48000");
                    mRecorder.setAudioSamplingRate(48000);
                } else {*/
                    Log.d(TAG, "[recordFMRadioAudio] SamplingRate : 44100");
                    mRecorder.setAudioSamplingRate(44100);
               // }
                mRecorder.setMaxDuration(13 * 60 * 60 * 1000 + 1000);
                mRecorder.setAudioEncoder(SecMediaRecorder.AudioEncoder.AAC);// 3
                mRecorder.setDurationInterval(100);
                // Duration Update Interval every 0.7k
                mRecorder.setFileSizeInterval(768);
                mRecorder.setAuthor(FMRADIO_RECORDING);
                if (!FMRadioFeature.FEATURE_DISABLERTPLUSINFO) {
                    RTPTagListManager rtpMgr = RTPTagListManager.getInstance(this);
                    RTPTagList curTagList = rtpMgr.getCurTagList();
                    RTPTag tag;
                    RTPlus_perf = null;
                    RTPlus_album = null;
                    if ((tag = curTagList.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_ARTIST)) != null)
                        RTPlus_perf = tag.getInfo();
                    if ((tag = curTagList.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_ALBUM)) != null)
                        RTPlus_album = tag.getInfo();
                    Log.d(TAG, "perf:" + RTPlus_perf + " album:" + RTPlus_album);
                    if (RTPlus_perf != null)
                        mRecorder.setPerformer(RTPlus_perf);
                    if (RTPlus_album != null)
                        mRecorder.setAlbum(RTPlus_album);
                }
                mRecorder.prepare();
                mRecorder.setOnInfoListener(sInfoListener);
                Log.d(TAG, "[recordFMRadioAudio - Record Button starting.... ]");
                mIsFadeVolume = true;
                mCurrentFadeVolume = mAudioManager.getStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO));
                mReturnFadeVolume = mCurrentFadeVolume;
                mHandler.sendEmptyMessage(VOLUME_FADE);
                StatusBarManager mStatusBar = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
                try {
                    mStatusBar.disable(StatusBarManager.DISABLE_NOTIFICATION_ALERTS);
                } catch (SecurityException e) {
                    ;
                }
                mNotiMgr.setAudioSystemMute(true);
                mRecorder.start();
              /*  if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_SUPPORT_CSR) {
                    mAudioManager.setParameters(unmute);
                }*/
            } catch (Exception e) {
                e.printStackTrace();
                if (mRecorder != null) {
                    mRecorder.release();
                    mRecorder = null;
                }
                RadioMediaStore.deleteHiddenFile();
                mIsFadeVolume = false;
                if (mHandler.hasMessages(VOLUME_FADE))
                    mHandler.removeMessages(VOLUME_FADE);
                mNotiMgr.setAudioSystemMute(false);
                return;
            }
        }
        mIsRecording = true;
        refreshScanIcon();
        isWarningckMemFull = false;
        Settings.System.putInt(getContentResolver(), "fm_record_enable", 1);
        Log.d(TAG, "fm_record_enable, 1");
        mSeconds = 0;
        updateRecordedTime();
        showRecoder();
    }

    public void updateRecordedTime() {
        int hours = mSeconds / 3600;
        int minutes = (mSeconds % 3600) / 60;
        int seconds = mSeconds % 60;

        String recordedTime = "";
        if (hours > 0) {
            recordedTime = String.format("%02d", hours) + ":";
        }
        recordedTime = recordedTime + String.format("%02d", minutes)
            + ":" + String.format("%02d", seconds);

        mRecTimeHHMMSS.setText(recordedTime);
        if (!mIsScreenOff)
            FMNotificationManager.getInstance().updateRecordingTime(recordedTime);
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours);
            sb.append(getString(hours > 1 ? R.string.desc_hours : R.string.desc_hour));
        }
        if (minutes > 0) {
            sb.append(minutes);
            sb.append(getString(minutes > 1 ? R.string.desc_minutes : R.string.desc_minute));
        }
        if (seconds > 0) {
            sb.append(seconds);
            sb.append(getString(seconds > 1 ? R.string.desc_seconds : R.string.desc_second));
        }
        mRecTime.setContentDescription(sb.toString());
        getAvailableStorage(false);
        Log.d(TAG, "[updateRecordedTime seconds = " + seconds + " minutes = " + minutes
                + " hours = " + hours + " current time=" + System.currentTimeMillis());
    }

    private boolean checkMemFull = true;

    private boolean isWarningckMemFull = false;

    // Lock/unlock volume actions.
    private static final String ACTION_VOLUME_LOCK = "com.sec.android.fm.volume_lock";
    private static final String ACTION_VOLUME_UNLOCK = "com.sec.android.fm.volume_unlock";
    private static final String KEY_RETURNBACK_VOLUME = "com.sec.android.fm.return_back_volume";

    private boolean getAvailableStorage(boolean isStart) {
        String storageDirectory;
        checkMemFull = true;

        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREF_FILE,
                Context.MODE_PRIVATE);
        storageDirectory = prefs.getString(SettingsActivity.KEY_STORAGE, Environment
                .getExternalStorageDirectory().getAbsolutePath());

        Log.d(TAG, "storageDirectory = " + storageDirectory);

        if (!SettingsActivity.STORAGE_MOUNTED.equals(Environment.getExternalStorageState(new File(storageDirectory)))) {
            storageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
            Log.d(TAG, "The recording path is changed to phone. storageDirectory = "
                    + storageDirectory);
        }

        try {
            StatFs stat = new StatFs(storageDirectory);
            Log.d(TAG, "stat = " + stat);
            long avaliableSize = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            Log.d(TAG,
                    "avaliableSize = " + avaliableSize + " getAvailableBlocks = "
                            + stat.getAvailableBlocksLong() + " getBlockSize = "
                            + stat.getBlockSizeLong() + ", mFileName: " + mFileName);
            Log.d(TAG, "LOW_STORAGE_THRESHOLD = " + LOW_STORAGE_THRESHOLD);
            if (isStart) {
                if (avaliableSize < LOW_STORAGE_SAFETY_THRESHOLD) {
                    checkMemFull = false;
                    RadioToast.showToast(MainActivity.this, R.string.memory_full,
                            Toast.LENGTH_SHORT);
                }
            } else {
                if (!isWarningckMemFull
                        && (avaliableSize < (LOW_STORAGE_THRESHOLD + LOW_STORAGE_REMAINING_BYTE))) {
                    isWarningckMemFull = true;
                    RadioToast.showToast(
                            MainActivity.this,
                            getString(R.string.not_enough_memory_recording_will_stop,
                                    getByte(avaliableSize - LOW_STORAGE_THRESHOLD)),
                            Toast.LENGTH_SHORT);
                }
                if (avaliableSize < LOW_STORAGE_THRESHOLD) {
                    Log.d(TAG, "getAvailableStorage - low");
                    if (mIsRecording) {
                        stopFMRecording();
                    }
                    checkMemFull = false;
                }
            }
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "getAvailableStorage StatFs error : " + storageDirectory);
            e.printStackTrace();
        }

        return checkMemFull;
    }

    public void resetRDS(final int freq) {
        Log.v(TAG, "reset RDS :" + Log.filter(freq));
        if (!mPlayer.isOn()) {
            setRDSTextView("", "");
        } else {
            String channelName = mPlayer.getPsName();
            if (mPlayer.isRdsEnabled() && channelName != null) {
                setRDSTextView(channelName, "");
            } else {
                Channel channel = mChannelStore.getChannelByFrequency(freq);
                if (channel != null)
                    setRDSTextView(channel.mFreqName, "");
                else
                    setRDSTextView("", "");
            }
        }
    }

    private boolean mIsOn = false;

    private void setPlayPauseImage() {
        if (mPlayer.isOn()) {
            mPowerButton.setImageResource(R.drawable.hybrid_radio_player_on);
            mRecordButton.setActivated(true);
            mFreqLayout.setAlpha(1.0f);
            mStationInfo.setAlpha(1.0f);
            mAddFavBtn.setVisibility(View.VISIBLE);
            mRecordButton.setVisibility(View.VISIBLE);
            mFreqValue.setClickable(true);
            mFreqValue.setFocusable(true);
            mPrevButton.setVisibility(View.VISIBLE);
            mNextButton.setVisibility(View.VISIBLE);
            if (!mIsRecording) {
                setVisibeInformationView(SHOW_RDS);
            } else {
                setVisibeInformationView(SHOW_RECORDING_TIME);
            }
            if (mScanFinished) {
                if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
                    mFreqValue
                            .setText(String.format(Locale.US, "%.2f", (float) mCurrentFreq / 100));
                } else {
                    mFreqValue
                            .setText(String.format(Locale.US, "%.1f", (float) mCurrentFreq / 100));
                } 
            }
            mIsOn = true;
        } else {
            mPowerButton.setImageResource(R.drawable.hybrid_radio_player_off);
            mRecordButton.setActivated(false);
            mRecordButton.setVisibility(View.GONE);
            mFreqValue.setClickable(false);
            mFreqValue.setFocusable(false);
            mPrevButton.setVisibility(View.GONE);
            mNextButton.setVisibility(View.GONE);
            if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
                mFreqValue.setText("00.00");
            } else {
                mFreqValue.setText("000.0");
            }
            mFreqLayout.setAlpha(0.4f);
            mStationInfo.setAlpha(0.3f);
            mAddFavBtn.setVisibility(View.INVISIBLE);
            setVisibeInformationView(SHOW_TURN_ON_RADIO);
            mIsOn = false;
            if (IS_BIGGER_THAN_MDPI) {
                AlphaAnimation displayAnimation = new AlphaAnimation(1.f, 0.f);
                displayAnimation.setDuration(333);
                displayAnimation.setStartOffset(167);
                displayAnimation.setFillEnabled(true);
                displayAnimation.setFillAfter(true);
                AlphaAnimation freqAnimation = new AlphaAnimation(1.f, 0.f);
                freqAnimation.setDuration(333);
                freqAnimation.setFillEnabled(true);
                freqAnimation.setFillAfter(true);
                freqAnimation.setAnimationListener(new AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation arg0) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                    }
                });
            }
        }
    }

    private void setVolume(int value) {
        Log.secD(TAG, " setVolume");
        if (!mPlayer.isScanning()) {
            mAudioManager.setStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO), value, mFlagSteamVolume);
        }
    }

    private void showRecoder() {
        mRecordButton.setVisibility(View.GONE);
        mRecordingControl.setVisibility(View.VISIBLE);
        mRecordingControl.bringToFront();
        mRecordCancelButton.requestFocus();
        if (!FMPermissionUtil.hasPermission(getApplicationContext(), FMPermissionUtil.FM_PERMISSION_REQUEST_RESET_APP_PREFERENCES)) {
            FMPermissionUtil.requestPermission(MainActivity.this, FMPermissionUtil.FM_PERMISSION_REQUEST_RESET_APP_PREFERENCES);
        } else {
            mRecordingDisplayText.setText(RadioMediaStore.getRecordingFileTitle());
        }
        Animation blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.rec_time_blink);
        mRecordingDisplayText.startAnimation(blinkAnimation);
        setVisibeInformationView(SHOW_RECORDING_TIME);
        setPauseResumeBtnImage(mIsRecordingPause);
    }

    public boolean startScan(boolean isUpdateUI) {
        mIsUpdateUI = isUpdateUI;
        try {
            if (!mPlayer.isOn()) {
                on();
            }
            mPlayer.scan();
        } catch (FMPlayerException e) {
            return false;
        }
        return true;
    }

    private void pauseFMRecording() {
        Log.d(TAG, "[pauseFMRecording - Record Button mRecorder = " + mRecorder);
        if (mRadioDNSEnable && RadioDNSServiceDataIF.isEpgPlayingStreamRadio()) {
            mDNSBoundService.pauseRecord();
            mIsRecordingPause = true;
        } else {
            if (mRecorder != null) {
                mRecorder.pause();
                mIsRecordingPause = true;
            }
        }
        setPauseResumeBtnImage(mIsRecordingPause);
    }

    private void resumeFMRecording() {
        Log.d(TAG, "[resumeFMRecording - Record Button mRecorder = " + mRecorder);
        if (mRadioDNSEnable && RadioDNSServiceDataIF.isEpgPlayingStreamRadio()) {
            mDNSBoundService.startRecord();
            mIsRecordingPause = false;
        } else {
            if (mRecorder != null) {
                mRecorder.resume();
                mIsRecordingPause = false;
            }
        }

        setPauseResumeBtnImage(mIsRecordingPause);
    }

    private void setPauseResumeBtnImage(boolean isPause) {
        if (isPause) {
            mRecordPauseResumeButton.setImageResource(R.drawable.hybrid_radio_playerbox_rec);
            mRecordPauseResumeButton
                    .setContentDescription(getString(R.string.desc_resume_extended));
            mRecordingDisplayText.setTextColor(getResources().getColor(R.color.rec_text_pause, null));
            mRecordingStatus.setImageResource(R.drawable.fmradio_stations_ic_record_pause);

            Animation blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.rec_time_blink);
            mRecTimeHHMMSS.startAnimation(blinkAnimation);
            mRecordingDisplayText.clearAnimation();
        } else {
            mRecordPauseResumeButton.setImageResource(R.drawable.hybrid_radio_playerbox_pause);
            mRecordPauseResumeButton.setContentDescription(getString(R.string.desc_pause));
            mRecordingDisplayText.setTextColor(getResources().getColor(R.color.rec_text_recording, null));
            mRecordingStatus.setImageResource(R.drawable.fmradio_stations_ic_record_resume);
            Animation blinkAnimation = AnimationUtils.loadAnimation(this, R.anim.rec_time_blink);
            mRecordingDisplayText.startAnimation(blinkAnimation);
            mRecTimeHHMMSS.clearAnimation();
        }
    }

    public void stopFMRecording() {
        if (!FMPermissionUtil.hasPermission(getApplicationContext(), FMPermissionUtil.FM_PERMISSION_REQUEST_RESET_APP_PREFERENCES)) {
            FMPermissionUtil.requestPermission(MainActivity.this, FMPermissionUtil.FM_PERMISSION_REQUEST_RESET_APP_PREFERENCES);
        } else {
            stopFMRecording(true);
        }
    }

    private void stopFMRecording(boolean isReturnVolume) {
        Log.d(TAG, "[stopFMRecording - Record Button mRecorder = " + mRecorder);
        if (mIsRecording == false)
            return;
        releaseFMRecorder(isReturnVolume);
        setPauseResumeBtnImage(false);

        if (!RadioMediaStore.dirRecheck()) {
            RadioToast.showToast(MainActivity.this, R.string.recording_error,
                    Toast.LENGTH_SHORT);
            RadioMediaStore.deleteHiddenFile();
            return;
        }

        if (RadioMediaStore.isValid()) {
            String mRecordingFileName = RadioMediaStore.getRecordingFileTitle();
            if (RadioMediaStore.save(RTPlus_perf, RTPlus_album)) {
                RadioToast.showToast(MainActivity.this, getString(R.string.toast_saved, mRecordingFileName), Toast.LENGTH_LONG);
                if (isResumed()) {
                    startActivity(new Intent(MainActivity.this,
                            RecordedFileListPlayerActivity.class));
                }
            } else {
                RadioToast.showToast(MainActivity.this, R.string.recording_error,
                        Toast.LENGTH_SHORT);
                cancelFMRecording();
            }
        } else {
            RadioMediaStore.deleteHiddenFile();
        }
    }

    public void cancelFMRecording() {
        Log.v(TAG, "cancelFMRecording()");
        releaseFMRecorder();
        mPlayer.setRecordMode(RECORDING_END);
        setPauseResumeBtnImage(false);
        RadioMediaStore.deleteHiddenFile();
    }

    private void releaseFMRecorder() {
        releaseFMRecorder(true);
    }

    private void releaseFMRecorder(boolean isReturnVolume) {
        String keyvalue = "fmradio_recoding=off";
        /*String mute = "fm_radio_mute=1";
        String unmute = "fm_radio_mute=0";*/
        if (mRadioDNSEnable && RadioDNSServiceDataIF.isEpgPlayingStreamRadio()) {
            mAudioManager.setParameters(keyvalue);
            mDNSBoundService.stopRecord();
            mDNSBoundService.releaseRecord();
        } else {
            /*if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_SUPPORT_CSR) {
                mAudioManager.setParameters(mute);
            }*/
            mAudioManager.setParameters(keyvalue);
            try {
                if (mRecorder != null) {
                    mRecorder.stop();
                    mRecorder.reset();
                    mRecorder.release();
                    mRecorder = null;
                }
            } catch (RuntimeException e) {
                Log.v(TAG, "mRecorder : stop failed.");
            }
            mPlayer.setRecordMode(RECORDING_END);
            /*if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_SUPPORT_CSR) {
                mAudioManager.setParameters(unmute);
            }*/
        }

        StatusBarManager mStatusBar;
        mStatusBar = (StatusBarManager) getSystemService(Context.STATUS_BAR_SERVICE);
        // Alert ON
        try {
            mStatusBar.disable(StatusBarManager.DISABLE_NONE);
        } catch (SecurityException e) {
            ;
        }
        // Selection sound ON
        mNotiMgr.setAudioSystemMute(false);

        // Unlock volume after recording.
        Intent intent = new Intent(ACTION_VOLUME_UNLOCK);
        sendBroadcast(intent);
        if (mHandler.hasMessages(VOLUME_FADE)) {
            mHandler.removeMessages(VOLUME_FADE);
        }
        if (isReturnVolume) {
            mHandler.sendEmptyMessageDelayed(RETURN_VOLUME_FADE, 100);
        } else {
            setVolume(mReturnFadeVolume);
            hideRecoder();
        }
        mIsRecording = false;
        refreshScanIcon();
        Settings.System.putInt(getContentResolver(), "fm_record_enable", 0);
        Log.d(TAG, "fm_record_enable, 0");
        mIsRecordingPause = false;
        isWarningckMemFull = false;
        mHandler.removeMessages(RECORD_TIME_UPDATE);
        updateButton();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            if (mHandler.hasMessages(RETURN_VOLUME_FADE)) {
                mHandler.removeMessages(RETURN_VOLUME_FADE);
            }
            e.printStackTrace();
        }
    }

    private void stopSpeaking() {
        if (ChannelNameToSpeech.getInstance().mTts != null
                && ChannelNameToSpeech.getInstance().mTts.isSpeaking()) {
            ChannelNameToSpeech.getInstance().mTts.stop();
            Log.d(TAG, "stop speaking..");
            return;
        }
    }

    void tune(final int frequency) {
        Log.d(TAG, "tune() - freq : " + Log.filter(frequency));
        if (mPlayer.isOn()) {
            int newFreq = RadioPlayer.getValidFrequency(frequency);
            if (mPlayer.isBusy()) {
                Log.d(TAG, "RadioPlayer is busy. ignore it");
                return;
            }
            mPlayer.tuneAsync(newFreq);
        }
    }

    public void refreshAddFavBtn(int freq) {
        boolean isFavorite = false;
        if (mChannelStore == null)
            return;
        int size = mChannelStore.size();
        for (int i = 0; i < size; i++) {
            Channel channel = mChannelStore.getChannel(i);
            if (channel.mFreqency == freq && channel.mIsFavourite == true) {
                isFavorite = true;
                break;
            }
        }
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        
        if (mCurrentFreq == freq && isFavorite) {
            mAddFavBtn.setBackgroundResource(R.drawable.hybrid_radio_on_star);
            mAddFavBtn.setContentDescription(getString(R.string.desc_button, getString(R.string.desc_favorite_button)) +", " + getString(R.string.desc_selected));
        } else if (mCurrentFreq == freq) {
            mAddFavBtn.setBackgroundResource(R.drawable.hybrid_radio_off_star);
            mAddFavBtn.setContentDescription(getString(R.string.desc_button, getString(R.string.desc_favorite_button)) +", " + getString(R.string.desc_not_selected));
        }
    }
    
    public void setTTSforAddFavourite(int freq) {
        boolean isFavorite = false;
        if (mChannelStore == null)
            return;
        int size = mChannelStore.size();
        for (int i = 0; i < size; i++) {
            Channel channel = mChannelStore.getChannel(i);
            if (channel.mFreqency == freq && channel.mIsFavourite == true) {
                isFavorite = true;
                break;
            }
        }
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        String stringTTS="";
        if (mCurrentFreq == freq && isFavorite) {
            if(am.isEnabled()){
            	stringTTS = getString(R.string.desc_frequency_removed_from_favourites,getString(R.string.desc_frequency));
            	getTts().speak(stringTTS, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        } else if (mCurrentFreq == freq) {
        	if(am.isEnabled()){
        		stringTTS = getString(R.string.desc_frequency_added_to_favourites,getString(R.string.desc_frequency));
            	getTts().speak(stringTTS, TextToSpeech.QUEUE_FLUSH, null, null);
        	}
        }
    }

    private static SecMediaRecorder.OnInfoListener sInfoListener = new SecMediaRecorder.OnInfoListener() {
        @Override
        public void onInfo(SecMediaRecorder mr, int what, int extra) {
            switch (what) {
            case SecMediaRecorder.MEDIA_RECORDER_INFO_FILESIZE_PROGRESS:
                break;
            case SecMediaRecorder.MEDIA_RECORDER_INFO_DURATION_PROGRESS:
                if (_instance != null) {
                    int seconds = extra / 1000;
                    if (MainActivity.mSeconds != seconds) {
                        MainActivity.mSeconds = seconds;
                        _instance.updateRecordedTime();
                    }
                }
                break;
            case SecMediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                if (_instance != null) {
                    _instance.stopFMRecording();
                }
                break;
            case SecMediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                break;
            default:
                break;
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiverSDCard = null;

    private void registerBroadcastReceiverSDCard(boolean register) {
        if (register) {
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            iFilter.addAction(Intent.ACTION_MEDIA_EJECT);
            iFilter.addDataScheme("file");

            mBroadcastReceiverSDCard = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Log.v(TAG, "onReceive() - " + Log.filter(action));
                    if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
                        if (mHiddenFileName != null) {
                            int pos = mHiddenFileName.lastIndexOf('/');
                            String filePath = mHiddenFileName.substring(0, pos);
                            pos = filePath.lastIndexOf('/');
                            filePath = filePath.substring(0, pos);
                            if (!SettingsActivity.STORAGE_MOUNTED.equals(Environment.getExternalStorageState(new File(filePath)))) {
                                if (mIsRecording) {
                                    RadioToast.showToast(MainActivity.this, R.string.recording_error, Toast.LENGTH_SHORT);
                                    cancelFMRecording();
                                }
                                SharedPreferences prefs = getSharedPreferences(
                                        SettingsActivity.PREF_FILE, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString(SettingsActivity.KEY_STORAGE, Environment
                                        .getExternalStorageDirectory().getAbsolutePath());
                                editor.commit();
                            }
                        }
                    } else if (action.equals(Intent.ACTION_MEDIA_EJECT)) {
                        if (mHiddenFileName != null) {
                            int pos = mHiddenFileName.lastIndexOf('/');
                            String filePath = mHiddenFileName.substring(0, pos);
                            pos = filePath.lastIndexOf('/');
                            filePath = filePath.substring(0, pos);
                            StorageVolume ejectStorage = null;
                            int ejectedStorageId = -1;
                            int recordingSorageId = -2;
                            Bundle bundle = intent.getExtras();
                            if (bundle != null) {
                                ejectStorage = (StorageVolume) bundle
                                        .get(StorageVolume.EXTRA_STORAGE_VOLUME);
                                ejectedStorageId = ejectStorage.getStorageId();
                            }
                            StorageVolume storageVolume = mStorageManager.getVolume(filePath);
                            if (storageVolume != null) {
                                recordingSorageId = storageVolume.getStorageId();
                            }
                            Log.d(TAG, "ejectedStorageId = " + ejectedStorageId
                                    + "; rocordingSorageId = " + recordingSorageId);
                            if (ejectedStorageId == recordingSorageId) {
                                if (mIsRecording)
                                    cancelFMRecording();
                                SharedPreferences prefs = getSharedPreferences(
                                        SettingsActivity.PREF_FILE, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString(SettingsActivity.KEY_STORAGE, Environment
                                        .getExternalStorageDirectory().getAbsolutePath());
                                editor.commit();
                            }
                        }
                    }
                }
            };
            registerReceiver(mBroadcastReceiverSDCard, iFilter);
        } else {
            if (mBroadcastReceiverSDCard != null) {
                unregisterReceiver(mBroadcastReceiverSDCard);
                mBroadcastReceiverSDCard = null;
            }
        }
    }

    private BroadcastReceiver mBroadcastReceiverBattery;

    private void registerBroadcastReceiverLowBattery(boolean register) {
        if (register) {
            if (mBroadcastReceiverBattery != null)
                return;
            mBroadcastReceiverBattery = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Log.d(TAG, "Battery Intent Received");
                    if (FMUtil.isLowBattery(intent)) {
                        RadioToast.showToast(getApplicationContext(), R.string.low_batt_msg,
                                Toast.LENGTH_SHORT);
                        if(mPlayer !=null && mPlayer.isOn())
                           mPlayer.turnOff();
                        finish();
                    }
                }
            };

            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            this.registerReceiver(mBroadcastReceiverBattery, iFilter);
        } else { // unregister
            if (mBroadcastReceiverBattery != null) {
                this.unregisterReceiver(mBroadcastReceiverBattery);
                mBroadcastReceiverBattery = null;
            }
        }
    }

    private BroadcastReceiver mBroadcastReceiverTurningOn = null;

    private void registerBroadcastReceiverTurningOn(boolean register) {
        if (register) {
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(ACTION_TURNING_ON);

            mBroadcastReceiverTurningOn = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!mAudioManager.isWiredHeadsetOn())
                           {
                        /*RadioToast.showToast(MainActivity.this,
                                R.string.toast_earphone_not_connected, Toast.LENGTH_SHORT);*/
                        return;
                    }
                    setVisibeInformationView(SHOW_TURNING_ON);
                }
            };
            registerReceiver(mBroadcastReceiverTurningOn, iFilter);
        } else {
            if (mBroadcastReceiverTurningOn != null) {
                unregisterReceiver(mBroadcastReceiverTurningOn);
                mBroadcastReceiverTurningOn = null;
            }
        }
    }

    /* [P130122-6311] */
    public void registerBroadcastReceiverSip(boolean register) {
        if (register) {
            if (mBroadcastReceiverSip != null) {
                return;
            }
            mBroadcastReceiverSip = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mReceiveTime = System.currentTimeMillis();
                    ISSIPVISIBLE = intent.getBooleanExtra(IS_VISIBLE_WINDOW, true);
                }
            };

            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(RESPONSE_AXT9INFO);
            registerReceiver(mBroadcastReceiverSip, iFilter);
        } else { // unregister
            if (System.currentTimeMillis() - mReceiveTime < 400) {
                ISSIPVISIBLE = true;
            }
            synchronized (this) {
                if (mBroadcastReceiverSip != null) {
                    try {
                        unregisterReceiver(mBroadcastReceiverSip);
                        mBroadcastReceiverSip = null;
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private BroadcastReceiver mBackupRestoreReceiver = null;

    private void registerRestoreReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BackupAndRestoreService.RESTORE_FINISH);

        mBackupRestoreReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                myOnResume(false);
            }
        };
        registerReceiver(mBackupRestoreReceiver, filter);
    }

    private void unregisterRestoreReceiver() {
        if (mBackupRestoreReceiver != null) {
            unregisterReceiver(mBackupRestoreReceiver);
            mBackupRestoreReceiver = null;
        }
    }

    public int GetFactoryRssi() {
        int factory_rssi = getSharedPreferences(SettingsActivity.PREF_FILE, Context.MODE_PRIVATE)
                .getInt(SettingsActivity.KEY_FACTORY_RSSI, SettingsActivity.FACTORY_RSSI);
        Log.d(TAG, "GetFactoryRssi :: rssi=" + factory_rssi);
        return factory_rssi;
    }

    public void SetFactoryRssi(int rssi) {
        Editor editor = getSharedPreferences(SettingsActivity.PREF_FILE, Context.MODE_PRIVATE)
                .edit();
        editor.putInt(SettingsActivity.KEY_FACTORY_RSSI, rssi);
        editor.commit();
        Log.d(TAG, "SetFactoryRssi :: rssi=" + rssi);
    }

    public long getByte(long bytes) {
        float bitrate = 122.2f;
        long gbytes = bytes;

        gbytes = (long) (gbytes / KBYTES * 8 / bitrate);
        return gbytes;
    }

    private ServiceConnection mDNSServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.d(TAG, "onDNSServiceConnected()");
            mDNSBoundService = ((DNSService.LocalBinder) service).getDNSService();
            Log.d(TAG, "onDNSServiceConnected()" + mDNSBoundService);
        }

        public void onServiceDisconnected(ComponentName cName) {
            Log.d(TAG, "onDNSServiceDisconnected()");
            mDNSBoundService = null;
        }
    };

    private void refreshScanIcon() {
        Log.d(TAG, "refresh Scan Icon" );
        if(mTabHost != null && mTabHost.getCurrentTab() == 0) {
            mScanBtn.setEnabled(false);
            mScanBtn.setAlpha(0.0F);
            mScanBtn.setFocusable(false);
            mScanBtn.setVisibility(View.GONE);
            mBottomLine.setVisibility(View.GONE);
            return;
        }
        if (mScanBtn != null && mAudioManager != null) {
            mScanBtn.setVisibility(View.VISIBLE);
            mBottomLine.setVisibility(View.VISIBLE);
            if (mPlayer.isOn()  && !mIsRecording && !mPlayer.isBusy() && !mPlayer.isScanning()) {
                mScanBtn.setEnabled(true);
                mScanBtn.setAlpha(1.0F);
                mScanBtn.setFocusable(true);
            } else {
                mScanBtn.setEnabled(false);
                mScanBtn.setAlpha(0.4F);
                mScanBtn.setFocusable(false);
            }
        }
    }

    private void refreshPlayViaIcon() {
        Log.d(TAG, "Headset Icon Scan Icon");
        if (mOptionsMenu != null && mAudioManager != null) {
            MenuItem menuItemSoundPath = mOptionsMenu.findItem(MENU_CHANGE_SOUND_PATH);
            if (menuItemSoundPath != null) {
                if (mAudioManager.isWiredHeadsetOn()) {
                    menuItemSoundPath.setVisible(true);
                } else {
                    menuItemSoundPath.setVisible(false);
                }
            } else {
                Log.e(TAG, "menuItemSoundPath is null");
            }
        }
    }

    public void updateButton() {
        boolean isOn = false;
        setPlayPauseImage();
        
        isOn = mPlayer.isOn();
        if (isOn) {
            mFrqBgView.setFocusable(true);
        } else {
            mFrqBgView.setFocusable(false);
        }
        
        if (mRadioDNSEnable)
            updateVisButton();
    }

    private void updateVisButton() {
        Log.d(TAG, "updateVisButton()");
        boolean isOn = false;
        if (mPlayer != null) {
            isOn = mPlayer.isOn();
        }
        Channel channel = mChannelStore.getChannelByFrequency(mCurrentFreq);
        if (isOn && (mDNSBoundService != null) && mDNSBoundService.isVisAvailable()) {
            buttonImageTextChanged(mVisBtn, true);

            if (channel != null) {
                channel.mIsVisAvailable = true;
                mChannelStore.store();
            }
        } else {
            buttonImageTextChanged(mVisBtn, false);

            if (channel != null) {
                channel.mIsVisAvailable = false;
                mChannelStore.store();
            }
        }

    }

    private void buttonImageTextChanged(View button, boolean enable) {
        switch (button.getId()) {
        case R.id.vis_btn:
            if (!enable) {
                if (!FMRadioFeature.FEATURE_DISABLEDNS && mIsOn) {
                    mVisBtn.setVisibility(View.VISIBLE);
                } else {
                    mVisBtn.setVisibility(View.INVISIBLE);
                 }
            } else {
                mVisBtn.setImageResource(R.drawable.image_btn_info);
            }
            break;
        }
    }

    public void openDialog(int type) {
        openDialog(type, null, null);
    }

    private void openDialog(int type, DialogInterface.OnClickListener clickListener,
            Channel selectedChannel) {
        RadioDialogFragment dialog = (RadioDialogFragment) getFragmentManager().findFragmentByTag(
                String.valueOf(type));
        if (dialog != null) {
            LogDns.e(TAG, "OpenDialog - Dialog is already exist???");
            return;
        }
        if (type == RadioDialogFragment.SCAN_PROGRESS_DIALOG) {
            closeDialog(RadioDialogFragment.SCAN_OPTION_DIALOG);
        }
        if (isResumed()) {
            if (type == RadioDialogFragment.ITEM_RENAME_DIALOG) {
                String name = null;
                if (selectedChannel != null) {
                    name = selectedChannel.mFreqName;
                }
                dialog = RadioDialogFragment.newInstance(type,
                        RenameDialog.RENAME_DIALOG_TYPE_STATION, name);
            } else if (type == RadioDialogFragment.CHANGE_FREQ_DIALOG) {
                String frequency = null;
                if (mPlayer != null && mPlayer.isOn()) {
                    frequency = RadioPlayer.convertToMhz(mPlayer.getFrequency());
                }
                dialog = RadioDialogFragment.newInstance(type, frequency);
            } else {
                dialog = RadioDialogFragment.newInstance(type);
            }
            dialog.show(getFragmentManager(), String.valueOf(type));
        }
        if (dialog != null) {
            if (clickListener != null) {
                dialog.setOnClickListener(clickListener);
            }
        }
    }

    private void closeDialog(int type) {
        LogDns.v(TAG, "closeDialog() - start " + type);
        RadioDialogFragment dialog = (RadioDialogFragment) getFragmentManager().findFragmentByTag(
                String.valueOf(type));
        if (dialog != null) {
            try {
                LogDns.v(TAG, "removeDialog() - " + type);
                dialog.dismiss();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.remove(dialog);
                ft.commit();
            } catch (IllegalStateException e) {
                Log.d(TAG, "IllegalStateException in closeDialog");
            }
        }
    }

    public void setRDSTextView(String ps, String rt) {
        if (ps != null) {
            mPsText.setText(ps);
        }
        if (rt != null) {
            mRtText.setText(rt);
            mRtText.setSelected(true);
        }
    }

    public void setVisibeInformationView(int msg) {
        switch (msg) {
        case SHOW_SEEKING:
            if (mStationInfo != null && mSeeking != null && !mIsRecording) {
                mSeeking.setText(R.string.seeking);
                mStationInfo.setVisibility(View.VISIBLE);
            }
            if (mRdsPanel != null) {
                mRdsPanel.setVisibility(View.GONE);
            }
            if (mRecTime != null) {
                mRecTime.setVisibility(View.GONE);
            }
            break;
        case SHOW_TURNING_ON:
            if (mStationInfo != null && mSeeking != null) {
                mStationInfo.setAlpha(1.0f);
                mSeeking.setText(R.string.turning_on);
                mStationInfo.setVisibility(View.VISIBLE);
                mStationInfo.invalidate();
            }
            if (mRdsPanel != null) {
                mRdsPanel.setVisibility(View.GONE);
            }
            if (mRecTime != null) {
                mRecTime.setVisibility(View.GONE);
            }
            break;
        case SHOW_TURN_ON_RADIO:
            if (mStationInfo != null && mSeeking != null) {
                mSeeking.setText(getString(R.string.turn_on_radio, getString(R.string.app_name)));
                mStationInfo.setVisibility(View.VISIBLE);
                mStationInfo.invalidate();
            }
            if (mRdsPanel != null) {
                mRdsPanel.setVisibility(View.GONE);
            }
            if (mRecTime != null) {
                mRecTime.setVisibility(View.GONE);
            }
            break;
        case SHOW_RDS:
            if (mStationInfo != null) {
                mStationInfo.setVisibility(View.GONE);
            }
            if (mRecTime != null) {
                mRecTime.setVisibility(View.GONE);
            }
            if (mRdsPanel != null) {
                mRdsPanel.setVisibility(View.VISIBLE);
            }

            break;
        case SHOW_RECORDING_TIME:
            if (mStationInfo != null) {
                mStationInfo.setVisibility(View.GONE);
            }
            if (mRdsPanel != null) {
                mRdsPanel.setVisibility(View.GONE);
            }
            if (mRecTime != null) {
                mRecTime.setVisibility(View.VISIBLE);
            }
            break;
        default:
            if (mStationInfo != null) {
                mStationInfo.setVisibility(View.GONE);
            }
            if (mRdsPanel != null) {
                mRdsPanel.setVisibility(View.GONE);
            }
            if (mRecTime != null) {
                mRecTime.setVisibility(View.GONE);
            }
            break;
        }
    }

    public void updateVisView(boolean isVisStart) {
        if (mDNSBoundService == null) {
            Log.d(TAG, "Service Null");
            return;
        }
        if (isVisStart) {
            mDNSBoundService.runDNSSystem(new ModeData(DNSEvent.DNS_START_VIS));
        } else {
            Message msg = Message.obtain(DNSService.getEventHandler(), DNSEvent.DNS_VIS_CLOSE_UI);
            DNSService.getEventHandler().sendMessageDelayed(msg, 10);
        }
    }

    private Drawable mVisImage = null;
    private ImageView mVisImageView = null;

    private void updateVisImage() {
        if (null == mDNSBoundService) {
            Log.e(TAG, "updateVisImage() - service is null");
            return;
        }
        Bitmap visBitmap = mDNSBoundService.getVisImage();
        if (visBitmap != null)
            mVisImage = new BitmapDrawable(getResources(), visBitmap);
        mVisImageView.setBackground(mVisImage);
        mVisImageView.invalidate();
    }

    private String mVisText = null;
    private TextView mVisTextView = null;

    private void updateVisText() {
        if (null == mDNSBoundService) {
            Log.e(TAG, "updateVisText() - service is null");
            return;
        }
        String text = mDNSBoundService.getVisText();
        if (null != text)
            mVisText = text;
        mVisTextView.setText(mVisText);
        mVisTextView.invalidate();
    }

    private void resetVisViews() {
        mVisImage = null;
        mVisImageView.setBackground(mVisImage);
        mVisImageView.invalidate();

        mVisText = null;
        mVisTextView.setText(mVisText);
        mVisTextView.invalidate();
    }

    private final BodyViewState mDialViewState = new DialViewState();
    private final BodyViewState mStationViewState = new StationViewState();
    private final BodyViewState mInformationViewState = new InformationViewState();
    private BodyViewState mViewState = null;

    private abstract static class BodyViewState {
        protected static BodyViewState sPrevState = null;

        public abstract void show();

        // in case of switch_dial_fav_btn clicked
        public abstract void showNext();

        // in case of vis_btn clicked
        public abstract void showPrevious();
    }

    private class DialViewState extends BodyViewState {
        private static final String TAG = "DialViewState";

        @Override
        public void show() {
            Log.v(TAG, "show()");
            mVisInfo.setVisibility(View.GONE);
            updateVisButton();
        }

        @Override
        public void showPrevious() {
            // sPrevState should be assigned before show()
            sPrevState = this;

            mViewState = mInformationViewState;
            mViewState.show();
            updateVisView(true);
        }

        @Override
        public void showNext() {
            sPrevState = this;
            mViewState = mStationViewState;
            mViewState.show();
            Editor editor = getSharedPreferences(RadioApplication.PREF_FILE, 0).edit();
            editor.putBoolean(IS_FAV_BTN_CLICK, true);
            editor.commit();
        }
    }

    private class StationViewState extends BodyViewState {
        private static final String TAG = "StationViewState";

        @Override
        public void show() {
            Log.v(TAG, "show()");
            mVisInfo.setVisibility(View.GONE);
            updateVisButton();
        }

        @Override
        public void showPrevious() {
            // sPrevState should be assigned before show()
            sPrevState = this;
            mViewState = mInformationViewState;
            mViewState.show();
            updateVisView(true);
        }

        @Override
        public void showNext() {
            sPrevState = this;
            mViewState = mDialViewState;
            mViewState.show();
            Editor editor = getSharedPreferences(RadioApplication.PREF_FILE, 0).edit();
            editor.putBoolean(IS_FAV_BTN_CLICK, false);
            editor.commit();
        }
    }

    private class InformationViewState extends BodyViewState {
        private static final String TAG = "InformationViewState";

        @Override
        public void show() {
            Log.v(TAG, "show()");
            mVisInfo.setVisibility(View.VISIBLE);
            updateVisButton();
            updateVisImage();
            updateVisText();
        }

        @Override
        public void showPrevious() {
            updateVisView(false);
            if (sPrevState instanceof DialViewState) {
                mViewState = mDialViewState;
            } else {
                mViewState = mStationViewState;
            }

            // sPrevState should be assigned after checking instanceof
            sPrevState = this;

            mViewState.show();
        }

        @Override
        public void showNext() {
            updateVisView(false);
            Editor editor = getSharedPreferences(RadioApplication.PREF_FILE, 0).edit();
            if (sPrevState instanceof DialViewState) {
                mViewState = mStationViewState;
                editor.putBoolean(IS_FAV_BTN_CLICK, true);
            } else {
                mViewState = mDialViewState;
                editor.putBoolean(IS_FAV_BTN_CLICK, false);
            }

            // sPrevState should be assigned after checking instanceof
            sPrevState = this;

            mViewState.show();
            editor.commit();
        }
    }

    public void showInformationView() {
        if (mViewState != null && !(mViewState instanceof InformationViewState)) {
            mViewState.showPrevious();
        } else {
            Log.e(TAG, "[Error] This view is already InformationView.");
        }
    }

    @Override
    public void onPositionChanged(double position) {
        Log.v(TAG,"onPositionChanged : "+position);
        mCurrentFreq = mFrqBgView.frequency;
        if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
            mFreqValue.setText(String.format(Locale.US, "%.2f", (float) mCurrentFreq / 100));
        } else {
            mFreqValue.setText(String.format(Locale.US, "%.1f", (float) mCurrentFreq / 100));
        }
        refreshAddFavBtn(mCurrentFreq);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        Log.v(TAG,"onTouch : ");
        if (view.getId() == R.id.frq_dsp_bg_view) {
            if (mPlayer.isOn()) {
            	isOnFrequencyProgress = true;
            	mAddFavBtn.setEnabled(false);
                mFrqBgView.setOnPositioinChangeListener(this);
                int action = event.getAction();
                int x = -1;
                x = (int) event.getRawX();
                if (!(FMUtil.isRTL(_instance) && Configuration.ORIENTATION_LANDSCAPE == mOrientation))
                    x = x - getResources().getDimensionPixelSize(R.dimen.frerquency_bar_left_space);
                view.setPressed(false);
                mFrqBgView.getFrequencyPosition(x);
                if(action == MotionEvent.ACTION_UP){
                	isOnFrequencyProgress = false;
                	mAddFavBtn.setEnabled(true);
                	setRDSTextView("", "");
                    if(!mFrqBgView.hasFrequencyChangeListener())
                        mFrqBgView.setOnFrequencyChangeListener(_instance);
                    mFrqBgView.frequencyChange();
                 }
            }
        }
        return true;
    }

    @Override
    public void onFrequencyChanged(long frequency) {
        mCurrentFreq = (int) (Float.parseFloat(RadioPlayer.convertToMhz(mCurrentFreq)) * 100);
        tune(mCurrentFreq);
    }

    boolean iswindowhasfocus = false;
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        Log.d(TAG, "onWindowFocusChanged : hasFocus "+hasFocus);
        super.onWindowFocusChanged(hasFocus);
        if(!hasFocus){
            iswindowhasfocus = true;
        }
        else
            iswindowhasfocus = false;
    }

    private BroadcastReceiver mLockTaskReceiver = null;

    private void registerLockTaskListener() {
        IntentFilter intentPinUnpin = new IntentFilter();
        intentPinUnpin.addAction( ACTION_LOCK_TASK_MODE);

        mLockTaskReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean enable = intent.getBooleanExtra("enable", false);
                Log.v(TAG,"mLockTaskReceiver onReceive : "+enable);
                if (!enable) {
                    // device gets locked after 1000 hence adding delay of 1 second    [a.agnihotri]
                    mHandler.sendEmptyMessageDelayed(SHOW_NOTIFICATION, 1000);
                }
            }
        };

        registerReceiver(mLockTaskReceiver, intentPinUnpin);
        Log.v(TAG,"registering pin unpin listener");
    }

    private void unregisterLockTaskListener() {
        Log.v(TAG,"Unregistering pin unpin listener");
        if(mLockTaskReceiver != null) {
            unregisterReceiver(mLockTaskReceiver);
            mLockTaskReceiver = null;
        }
    }

    public class FMTabContent implements TabContentFactory{
        private Context mContext;
     
        public FMTabContent(Context context){
            mContext = context;
        }
     
        @Override
        public View createTabContent(String tag) {
            View v = new View(mContext);
            return v;
        }
    }

    public int getCurrrentFrequency(){
        return mCurrentFreq;
    }

    private ActionMode mActionMode;
    private static boolean mNeedToRetainActionMode;
    private boolean mIsActionMode;

    public void setActionMode(ActionMode value) {
        mActionMode = value;
        mIsActionMode = (value != null);
        if (mIsActionMode && mScanBtn != null) {
            mScanBtn.setEnabled(false);
            mScanBtn.setAlpha(0.4F);
            mScanBtn.setFocusable(false);
        }
        else if (!mIsActionMode && mScanBtn != null){
            mScanBtn.setEnabled(true);
            mScanBtn.setAlpha(1.0F);
            mScanBtn.setFocusable(true);
        }
    }

    public ActionMode getActionMode(){
        return mActionMode;
    }

    public void setTopPanelOnActionBar() {
        ActionBar bar = getActionBar();
        bar.setHomeButtonEnabled(true);
        bar.setDisplayHomeAsUpEnabled(false);
        bar.setDisplayShowHomeEnabled(false);
        bar.setDisplayShowTitleEnabled(false);

        topBarActionBarView = getLayoutInflater().inflate(R.layout.top_bar_panel, null);
        mPowerButton = (ImageButton) topBarActionBarView.findViewById(R.id.power_btn);
        mPowerButton.setOnClickListener(mClickListener);
        mRecordButton = (ImageButton) topBarActionBarView.findViewById(R.id.record_btn);
        mRecordButton.setOnClickListener(mClickListener);

        mRecordStopButton = (ImageView) topBarActionBarView.findViewById(R.id.recording_stop_btn);
        mRecordStopButton.setContentDescription(getString(R.string.desc_stop));
        mRecordStopButton.setOnClickListener(mClickListener);

        mRecordingControl = (FrameLayout) topBarActionBarView.findViewById(R.id.recording_control);
        if (FMUtil.isRTL(_instance)) {
            mRecordingControl.setRotation(180.0F);
        } else {
            mRecordingControl.setRotation(0F);
        }
        mRecordPauseResumeButton = (ImageView) topBarActionBarView.findViewById(R.id.recording_pause_resume_btn);
        mRecordPauseResumeButton.setContentDescription(getString(R.string.desc_pause));
        mRecordPauseResumeButton.setOnClickListener(mClickListener);

        mRecordCancelButton = (ImageView) topBarActionBarView.findViewById(R.id.recording_cancel_btn);
        mRecordCancelButton.setContentDescription(getString(R.string.desc_cancel));
        mRecordCancelButton.setOnClickListener(mClickListener);
        bar.setCustomView(topBarActionBarView);
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        bar.setShowHideAnimationEnabled(false);
        bar.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.v(TAG, "onRequestPermissionsResult requestCode = " + requestCode);

        switch(requestCode) {
        case FMPermissionUtil.FM_PERMISSION_REQUEST_RECORD_AUDIO:
            if (FMPermissionUtil.verifyPermissions(grantResults)) {
                recordFMRadioAudio();
            }
            break;
		
		case FMPermissionUtil.FM_PERMISSION_REQUEST_RESET_APP_PREFERENCES:
            if (!FMPermissionUtil.verifyPermissions(grantResults)) {
                cancelFMRecording();
            }
            else if (!mPlayer.isOn()){
                stopFMRecording();
            }
            break;

        case FMPermissionUtil.FM_PERMISSION_REQUEST_RECORDINGS_LIST:
            if (FMPermissionUtil.verifyPermissions(grantResults)) {
                Intent intent = new Intent(this, RecordedFileListPlayerActivity.class);
                startActivity(intent);
            }
            break;
        default:
            break;
        }
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

    @Override
    public void onInit(int arg0) {
        // TODO Auto-generated method stub
        
    }
    
    public TextToSpeech getTts() {
        if (mTts == null) {
            mTts = new TextToSpeech(this, this);
        }
        switch (mTts.setLanguage(mTts.getLanguage())) {
            case TextToSpeech.LANG_MISSING_DATA: // fall through
            case TextToSpeech.LANG_NOT_SUPPORTED:
                if(mTts != null){
                   mTts.setLanguage(Locale.US);
                }
                break;
            }
        return mTts;
    }

}
