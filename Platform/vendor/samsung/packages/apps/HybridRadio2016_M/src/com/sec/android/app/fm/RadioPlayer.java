package com.sec.android.app.fm;

import java.util.ArrayList;
import java.util.Locale;

import android.content.Context;
import android.media.AudioManager;
import android.media.SamsungAudioManager;
import android.provider.Settings;

import com.samsung.media.fmradio.FMEventListener;
import com.samsung.media.fmradio.FMPlayer;
import com.samsung.media.fmradio.FMPlayerException;
import com.sec.android.app.dns.DNSEvent;
import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.ModeData;
import com.sec.android.app.fm.data.Channel;
import com.sec.android.app.fm.data.ChannelStore;
import com.sec.android.app.fm.ui.RTPTagList;
import com.sec.android.app.fm.ui.RTPTagListManager;
import com.sec.android.app.fm.util.FMUtil;

public class RadioPlayer {
    private static final String _TAG = "RadioPlayer";
    public static final int FREQ_DEFAULT = 8750;
    private static final int FREQ_DEFAULT_PHILIPHINES = 8830;
    public static final int FREQ_MAX = 10800;
    public static final int FREQ_MIN = 8750;
    private static final String MONO_AUDIO_KEY_CHECKBOX_DB = "mono_audio_db";
    private static RadioPlayer sInstance = new RadioPlayer();
    private long mLastOnTime = -1;

    public static String convertToMhz(int freq) {
        if (freq == 0) {
            if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
                return "000.00";
            } else {
                return "000.0";
            }
        } else {
            if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
                return String.format(Locale.US, "%.2f", freq / 100f);
            } else {
                return String.format(Locale.US, "%.1f", freq / 100f);
            }
        }
    }

    public static int getDefaultFrequency() {
        return "88.3".equals(FMRadioFeature.FEATURE_DEFAULTCHANNEL) ? FREQ_DEFAULT_PHILIPHINES
                : FREQ_DEFAULT;
    }

    public static RadioPlayer getInstance() {
        return sInstance;
    }

    public static int getValidFrequency(final int freq) {
        return ((freq < FREQ_MIN) || (freq > FREQ_MAX)) ? RadioApplication.getInitialFrequency()
                : freq;
    }

    private AudioManager mAudioManager = null;
    private ArrayList<FMEventListener> mCallbackListeners = new ArrayList<FMEventListener>(16);
    private ChannelStore mChannelStore = null;
    private Context mContext = null;
    private Channel mFoundChannel = null;
    private int mFoundChannelCount = 0;
    private int mFrequency = -1;
    private FMPlayer mPlayer = null;

    private FMEventListener mPlayerListener = new FMEventListener() {

        @Override
        public void earPhoneConnected() {
            for (FMEventListener listener : mCallbackListeners) {
                listener.earPhoneConnected();
            }
        }

        @Override
        public void earPhoneDisconnected() {
            mFrequency = -1;
            for (FMEventListener listener : mCallbackListeners) {
                listener.earPhoneDisconnected();
            }
            if (mWorkerThread != null) {
                mWorkerThread.terminate();
                mWorkerThread = null;
            }
        }

        @Override
        public void onAFReceived(final long freq) {
            mFrequency = (int) (freq / 10);
            for (FMEventListener listener : mCallbackListeners) {
                listener.onAFReceived(freq);
            }
        }

        @Override
        public void onAFStarted() {
            LogDns.v(_TAG, "onAFStarted");
        }

        @Override
        public void onChannelFound(final long freq) {
            ++mFoundChannelCount;
            Log.d(_TAG, "onChannelFound() - freq : " + Log.filter(freq) + " count : "
                    + mFoundChannelCount);
            int frequency = (int) (freq / 10);
            if ((mFoundChannel = mChannelStore.getChannelByFrequency(frequency)) == null) {
                if (isDnsEnabled()) {
                    mFoundChannel = new Channel(frequency, "", 0);
                } else {
                    mFoundChannel = new Channel(frequency, "");
                }
                mChannelStore.addChannel(mFoundChannel);
                mChannelStore.store();
            }
            for (FMEventListener listener : mCallbackListeners) {
                listener.onChannelFound(frequency);
            }
        }

        @Override
        public void onOff(final int reasonCode) {
            mPsName = null;
            mRadioText = null;
            mFrequency = -1;
            if (!FMRadioFeature.FEATURE_DISABLERTPLUSINFO) {
                RTPTagListManager rtpMgr = RTPTagListManager.getInstance(mContext);
                rtpMgr.clearCurTagList();
                ArrayList<RTPTagList> tagListArray = rtpMgr.getTagListArray();
                if (tagListArray != null && tagListArray.size() == 0) {
                    RadioApplication.setRtPlusEnabled(false);
                }
            }
            for (FMEventListener listener : mCallbackListeners) {
                listener.onOff(reasonCode);
            }
            if (mWorkerThread != null) {
                mWorkerThread.terminate();
                mWorkerThread = null;
            }

            long playingTime = System.currentTimeMillis()- mLastOnTime;
            mLastOnTime = -1;
            FMUtil.insertGSIMLog(mContext, "RUNT", playingTime);
        }

        @Override
        public void onOn() {
            if (mWorkerThread == null) {
                mWorkerThread = new WorkerThread();
                mWorkerThread.start();
            }
            for (FMEventListener listener : mCallbackListeners) {
                listener.onOn();
            }

            mLastOnTime = System.currentTimeMillis();
        }

        @Override
        public void onPIECCReceived(final int pi, final int ecc) {
            DNSService serivce = DNSService.getInstance();
            if (FMRadioFeature.FEATURE_DISABLEDNS || (serivce == null)) {
                return;
            }
            LogDns.d(_TAG, "[onPIECCReceived] PI:" + Integer.toHexString(pi) + " ECC:" + ecc);

            boolean updated = false;
            if (isScanning()) {
                if ((mFoundChannel != null) && (mFoundChannel.mPi != pi)) {
                    mFoundChannel.mPi = pi;
                    updated = true;
                }
            } else {
                Channel channel = mChannelStore.getChannelByFrequency(getFrequency());
                if ((channel != null) && (channel.mPi != pi)) {
                    channel.mPi = pi;
                    updated = true;
                }
            }
            if (updated)
                mChannelStore.store();

            ModeData dnsUpdatePI = new ModeData(DNSEvent.DNS_UPDATE_RDS);
            dnsUpdatePI.setPi(pi);
            dnsUpdatePI.setEcc(ecc);
            serivce.runDNSSystem(dnsUpdatePI);

            for (FMEventListener listener : mCallbackListeners) {
                listener.onPIECCReceived(pi, ecc);
            }
        }

        @Override
        public void onRDSDisabled() {
            mPsName = null;
            mRadioText = null;
            if (!FMRadioFeature.FEATURE_DISABLERTPLUSINFO) {
                RTPTagListManager rtpMgr = RTPTagListManager.getInstance(mContext);
                rtpMgr.clearCurTagList();
                ArrayList<RTPTagList> tagListArray = rtpMgr.getTagListArray();
                if (tagListArray != null && tagListArray.size() == 0) {
                    RadioApplication.setRtPlusEnabled(false);
                }
            }
            for (FMEventListener listener : mCallbackListeners) {
                listener.onRDSDisabled();
            }
        }

        @Override
        public void onRDSEnabled() {
            for (FMEventListener listener : mCallbackListeners) {
                listener.onRDSEnabled();
            }
        }

        @Override
        public void onRDSReceived(final long freq, final String channelName, final String radioText) {
            Log.v(_TAG, "onRDSReceived() - channel name:" + channelName + " radio text:"
                    + radioText);
            Log.v(_TAG, "mPsName:" + mPsName + " mRadioText:" + mRadioText);
            String cName = null;
            boolean channelNameUpdated = false, radioTextUpdated = false;
            if (channelName != null && !channelName.isEmpty()) {
                cName = channelName.trim();
                if (!cName.equals(mPsName)) {
                    channelNameUpdated = true;
                    mPsName = cName;
                }
            }
            String rText = null;
            if (radioText != null && !radioText.isEmpty()) {
                rText = radioText.trim();
                if (!rText.equals(mRadioText)) {
                    radioTextUpdated = true;
                    mRadioText = rText;
                }
            }
            if (!FMRadioFeature.FEATURE_DISABLERTPLUSINFO) {
                RTPTagListManager rtpMgr = RTPTagListManager.getInstance(mContext);
                if (rtpMgr.setRadioText(rText)) {
                    if (rtpMgr.getTagListArray() != null && rtpMgr.getTagListArray().size() == 0) {
                        RadioApplication.setRtPlusEnabled(false);
                    }
                }
            }
            if (channelNameUpdated || radioTextUpdated) {
                for (FMEventListener listener : mCallbackListeners) {
                    listener.onRDSReceived(freq, cName, rText);
                }
            }
        }

        @Override
        public void onRTPlusReceived(final int contentType1, final int startPos1,
                final int additionalLen1, final int contentType2, final int startPos2,
                final int additionalLen2) {
            if (FMRadioFeature.FEATURE_DISABLERTPLUSINFO)
                return;
            Log.d(_TAG, "[onRTPlusReceived] contentType1:" + contentType1 + "  startPos1:"
                    + startPos1 + "  additionalLen1:" + additionalLen1);
            Log.d(_TAG, "[onRTPlusReceived] contentType2:" + contentType2 + "  startPos2:"
                    + startPos2 + "  additionalLen2:" + additionalLen2);
            RTPTagListManager rtpMgr = RTPTagListManager.getInstance(mContext);
            boolean neetToUpdate1 = rtpMgr.addCurTagList(contentType1, startPos1, additionalLen1);
            boolean neetToUpdate2 = rtpMgr.addCurTagList(contentType2, startPos2, additionalLen2);
            if (neetToUpdate1 || neetToUpdate2) {
                boolean isRtpEnabled = RadioApplication.isRtPlusEnabled();
                if (!isRtpEnabled) {
                    RadioApplication.setRtPlusEnabled(true);
                }
                for (FMEventListener listener : mCallbackListeners) {
                    listener.onRTPlusReceived(contentType1, startPos1, additionalLen1,
                            contentType2, startPos2, additionalLen2);
                }
            }
        }

        @Override
        public void onScanFinished(final long[] frequency) {
            if (isOn()) {
                if (mFrequency == -1 && mFoundChannelCount > 0) {
                    if (mChannelStore.size() > 0) {
                        mFrequency = mChannelStore.getChannel(0).mFreqency;
                    }
                }
                tuneAsync(getValidFrequency(mFrequency));
            }
            for (FMEventListener listener : mCallbackListeners) {
                listener.onScanFinished(frequency);
            }
        }

        @Override
        public void onScanStarted() {
            mFoundChannelCount = 0;
            for (FMEventListener listener : mCallbackListeners) {
                listener.onScanStarted();
            }
        }

        @Override
        public void onScanStopped(final long[] frequency) {
            if (isOn()) {
                if (mFrequency == -1 && mFoundChannelCount > 0) {
                    if (mChannelStore.size() > 0) {
                        mFrequency = mChannelStore.getChannel(0).mFreqency;
                    }
                }
                tuneAsync(getValidFrequency(mFrequency));
            }
            for (FMEventListener listener : mCallbackListeners) {
                listener.onScanStopped(frequency);
            }
        }

        @Override
        public void onTune(final long frequency) {
            Log.v(_TAG, "onTune() - freq:" + Log.filter(frequency));
            mPsName = null;
            mRadioText = null;
            if (frequency == -1)
                return;
            mFrequency = (int) (frequency / 10);
            RadioApplication.setInitialFrequency(mFrequency);
            if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                DNSService dnsService = DNSService.getInstance();
                if (null != dnsService) {
                    ModeData data = new ModeData(DNSEvent.DNS_ON);
                    data.setFreq((int) mFrequency);
                    dnsService.runDNSSystem(data);
                }
            }
            for (FMEventListener listener : mCallbackListeners) {
                listener.onTune(mFrequency);
            }
        }

        @Override
        public void recFinish() {
            for (FMEventListener listener : mCallbackListeners) {
                listener.recFinish();
            }
        }

        @Override
        public void volumeLock() {
            for (FMEventListener listener : mCallbackListeners) {
                listener.volumeLock();
            }
            /*if ((MainActivity._instance != null) && (!MainActivity._instance.isResumed())) {
                RadioToast.showToast(mContext, R.string.recording_volume_control,
                        Toast.LENGTH_SHORT);
            }*/
        }
    };

    private String mPsName = null;
    private String mRadioText = null;
    private WorkerThread mWorkerThread = null;

    public void applyAf() {
        try {
            if (!mPlayer.isOn()) {
                return;
            }
            boolean enable = mContext.getSharedPreferences(SettingsActivity.PREF_FILE,
                    Context.MODE_PRIVATE).getBoolean(SettingsActivity.KEY_AF, false);
            Log.d(_TAG, "applyAf() - " + enable);
            if (enable) {
                mPlayer.enableAF();
            } else {
                mPlayer.disableAF();
            }
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void applyRds() {
        try {
            if (!mPlayer.isOn()) {
                return;
            }
            boolean enable = mContext.getSharedPreferences(SettingsActivity.PREF_FILE,
                    Context.MODE_PRIVATE).getBoolean(SettingsActivity.KEY_STATION_ID, false);
            Log.d(_TAG, "applyRds() - " + enable);
            if (enable) {
                mPlayer.enableRDS();
            } else {
                mPlayer.disableRDS();
            }
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void applyStereo() {
        try {
            if (!mAudioManager.isRadioSpeakerOn()) {
                int value = Settings.System.getInt(mContext.getContentResolver(),
                        MONO_AUDIO_KEY_CHECKBOX_DB, 0);
                if (value == 0) {
                    mPlayer.setStereo();
                    Log.v(_TAG, "setStereo() is called");
                } else {
                    mPlayer.setMono();
                    Log.v(_TAG, "setMono() is called");
                }
            } else {
                mPlayer.setMono();
                Log.v(_TAG, "setMono() is called");
            }
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void cancelScan() {
        try {
            mPlayer.cancelScan();
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void cancelSeek() {
        try {
            mPlayer.cancelSeek();
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void disableSpeaker() {
        try {
            mPlayer.setSpeakerOn(false);
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void disableStereo() {
        try {
            mPlayer.setMono();
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    private void enableDns() {
        try {
            if (mPlayer.isOn()) {
                mPlayer.enableDNS();
            }
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void enableSpeaker() {
        try {
            mPlayer.setSpeakerOn(true);
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    private void error(FMPlayerException e) {
        Log.e(_TAG, e.toString());
        return;
    }

    public long getCurrentRssi() {
        try {
            return mPlayer.getCurrentRSSI();
        } catch (FMPlayerException e) {
            error(e);
        }
        return -1;
    }

    public int getFoundChannelCount() {
        return mFoundChannelCount;
    }

    public int getFrequency() {
        try {
            if (mPlayer.isOn()) {
                if (mPlayer.isScanning() || mPlayer.isSeeking()) {
                    return RadioApplication.getInitialFrequency();
                } else if ((mFrequency == -1)) {
                    mFrequency = (int) (mPlayer.getCurrentChannel() / 10);
                }
            } else {
                mFrequency = -1;
            }
        } catch (FMPlayerException e) {
            error(e);
        }
        return mFrequency;
    }

    public String getPsName() {
        return mPsName;
    }

    public String getRadioText() {
        return mRadioText;
    }

    public void initialize(Context context) {
        if ((context == null) || (mContext != null))
            return;
        Log.v(_TAG, "initialize()");
        mContext = context.getApplicationContext();
        if (mContext == null)
            return;
        mPlayer = (FMPlayer) mContext.getSystemService(Context.FM_RADIO_SERVICE);
        try {
            mPlayer.setListener(mPlayerListener);
        } catch (FMPlayerException e) {
            error(e);
        }
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mChannelStore = ChannelStore.getInstance();
    }

    public boolean isBusy() {
        return mWorkerThread != null ? mWorkerThread.isBusy() : false;
    }

    public boolean isDnsEnabled() {
        try {
            return mPlayer.isDNSEnable();
        } catch (FMPlayerException e) {
            error(e);
        }
        return false;
    }

    public boolean isOn() {
        try {
            return mPlayer.isOn();
        } catch (FMPlayerException e) {
            error(e);
        }
        return false;
    }

    public boolean isRdsEnabled() {
        try {
            return mPlayer.isRDSEnable();
        } catch (FMPlayerException e) {
            error(e);
        }
        return false;
    }

    public boolean isScanning() {
        try {
            return mPlayer.isScanning();
        } catch (FMPlayerException e) {
            error(e);
        }
        return false;
    }

    public boolean isSeeking() {
        try {
            return mPlayer.isSeeking();
        } catch (FMPlayerException e) {
            error(e);
        }
        return false;
    }

    private boolean isWorkerThreadReady() {
        if (mWorkerThread == null) {
            if (isOn()) {
                mWorkerThread = new WorkerThread();
                mWorkerThread.start();
            } else {
                Log.e(_TAG, "isWorkerThreadReady() - WorkerThread is null.");
                return false;
            }
        } else if (mWorkerThread.isBusy()) {
            Log.e(_TAG, "isWorkerThreadReady() - WorkerThread is busy.");
            return false;
        }
        return true;
    }

    public void registerListener(FMEventListener listener) {
        Log.v(_TAG, "registerListener() - listener:" + Log.filter(listener));
        if (listener != null && !mCallbackListeners.contains(listener)) {
            mCallbackListeners.add(listener);
        }
    }

    public void scan() {
        try {
            mPlayer.scan();
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public long seekDown() {
        if (!FMRadioFeature.FEATURE_DISABLEDNS) {
            DNSService dnsService = DNSService.getInstance();
            if (null != dnsService)
                dnsService.runDNSSystem(new ModeData(DNSEvent.DNS_OFF));
        }
        try {
            return mPlayer.seekDown();
        } catch (FMPlayerException e) {
            error(e);
        }
        return -1;
    }

    public void seekDownAsync() {
        Log.v(_TAG, "seekDownAsync() ");
        if (!isWorkerThreadReady()) {
            return;
        }
        mWorkerThread.doOperation(WorkerThread.OPERATION_SEEKDOWN, -1);
    }

    public long seekUp() {
        if (!FMRadioFeature.FEATURE_DISABLEDNS) {
            DNSService dnsService = DNSService.getInstance();
            if (null != dnsService)
                dnsService.runDNSSystem(new ModeData(DNSEvent.DNS_OFF));
        }
        try {
            return mPlayer.seekUp();
        } catch (FMPlayerException e) {
            error(e);
        }
        return -1;
    }

    public void seekUpAsync() {
        Log.v(_TAG, "seekUpAsync() ");
        if (!isWorkerThreadReady()) {
            return;
        }
        mWorkerThread.doOperation(WorkerThread.OPERATION_SEEKUP, -1);
    }

    public void setInternetStreamingMode(boolean mode) {
        try {
            mPlayer.setInternetStreamingMode(mode);
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void setRecordMode(int mode) {
        try {
            mPlayer.setRecordMode(mode);
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void setVolume(long volume) {
        try {
            mPlayer.setVolume(volume);
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void tune(final int freq) {
        if (!FMRadioFeature.FEATURE_DISABLEDNS) {
            DNSService dnsService = DNSService.getInstance();
            if (null != dnsService)
                dnsService.runDNSSystem(new ModeData(DNSEvent.DNS_OFF));
        }
        try {
            mPlayer.tune(freq * 10);
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void mute(boolean value) {
        try {
            mPlayer.mute(value);
        } catch (FMPlayerException e) {
            error(e);
        }
    }

    public void tuneAsync(final int freq) {
        Log.v(_TAG, "tuneAsync() - freq : " + Log.filter(freq));
        if (!isWorkerThreadReady()) {
            return;
        }
        mWorkerThread.doOperation(WorkerThread.OPERATION_TUNE, freq);
    }

    public void tuneAsyncEx(final int freq) throws FMPlayerException {
        try {
            if (!isOn()) {
                turnOn();
            }
            tuneAsync(freq);
        } catch (FMPlayerException e) {
            error(e);
            throw e;
        }
    }

    public boolean turnOff() {
        mFrequency = -1;
        if (!FMRadioFeature.FEATURE_DISABLEDNS) {
            DNSService dnsService = DNSService.getInstance();
            if (null != dnsService)
                dnsService.runDNSSystem(new ModeData(DNSEvent.DNS_OFF));
        }
        try {
            return mPlayer.off();
        } catch (FMPlayerException e) {
            error(e);
        }
        if (mWorkerThread != null) {
            mWorkerThread.terminate();
            mWorkerThread = null;
        }
        return false;
    }

    public boolean turnOn() throws FMPlayerException {
        Log.v(_TAG, "turnOn()");
        if (mWorkerThread != null)
            mWorkerThread.terminate();
        mWorkerThread = new WorkerThread();
        mWorkerThread.start();
        try {
            if (mPlayer.on()) {
                applyStereo();
                applyRds();
                applyAf();
                if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                    enableDns();
                }
                int volume = mAudioManager.getStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO));
                Log.v(_TAG, "The volume from audiomanager:" + volume);
                mAudioManager.setStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO), volume, 0);
                return true;
            }
        } catch (FMPlayerException e) {
            error(e);
            throw e;
        }
        return false;
    }

    public boolean turnOn(boolean testMode) throws FMPlayerException {
        Log.v(_TAG, "turnOn()");
        if (mWorkerThread != null)
            mWorkerThread.terminate();
        mWorkerThread = new WorkerThread();
        mWorkerThread.start();
        try {
            return mPlayer.on(testMode);
        } catch (FMPlayerException e) {
            error(e);
            throw e;
        }
    }

    public void unregisterListener(FMEventListener listener) {
        Log.v(_TAG, "unregisterListener() - listener:" + Log.filter(listener));
        mCallbackListeners.remove(listener);
    }
}
