package com.sec.android.app.dns;

import java.util.Date;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.widget.Toast;

import com.sec.android.app.dns.data.RadioDNSData;
import com.sec.android.app.dns.radiodns.RadioDNSCommonCountry;
import com.sec.android.app.dns.radiodns.RadioDNSConnection;
import com.sec.android.app.dns.radioepg.EpgManager;
import com.sec.android.app.dns.radioepg.JlayerLibrary;
import com.sec.android.app.dns.radioepg.PiData;
import com.sec.android.app.dns.radiovis.RadioVISClient;
import com.sec.android.app.dns.radiovis.RadioVISFrame;
import com.sec.android.app.dns.ui.DnsKoreaTestActivity;
import com.sec.android.app.dns.ui.DnsTestActivity;

import com.sec.android.secmediarecorder.SecMediaRecorder;
import com.sec.android.app.fm.MainActivity;
import com.sec.android.app.fm.R;
import com.sec.android.app.fm.RadioPlayer;
import com.sec.android.app.fm.RadioToast;
import com.sec.android.app.fm.SettingsActivity;
import com.sec.android.app.fm.util.NetworkMonitorUtil;
import com.sec.android.app.fm.util.NetworkMonitorUtil.OnNetworkStateChangeListener;

public class DNSService extends Service implements OnNetworkStateChangeListener {
    public class EventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String DNSCommand = null;
            switch (msg.what) {
            case DNSEvent.CURRENT_DATA_UPDATED:
                switch (msg.arg1) {
                case DNSEvent.ALL_CURRENT_DATA_UPDATED:
                    LogDns.d(TAG, "DNSEvent.ALL_CURRENT_DATA_UPDATED " + mCurrentData);
                    LogDns.v("DNSMessageReceiver", "DNSEvent.ALL_CURRENT_DATA_UPDATED");
                    /* updateVIS(); */
                    updateEpg();
                    DNSCommand = DNSEvent.DNS_ACTION_UPDATE_DATA;
                    sendBroadcastCommand(DNSCommand);
                    break;
                case DNSEvent.VIS_CURRENT_DATA_UPDATED:
                    LogDns.d(TAG, "DNSEvent.VIS_CURRENT_DATA_UPDATED " + mCurrentData);
                    /* updateVIS(); */
                    sendBroadcastCommand(DNSEvent.DNS_ACTION_UPDATE_VIS_DATA);
                    break;
                case DNSEvent.EPG_CURRENT_DATA_UPDATED:
                    LogDns.d(TAG, "DNSEvent.EPG_CURRENT_DATA_UPDATED" + mCurrentData);
                    updateEpg();
                    break;
                default:
                    break;
                }
                break;
            case DNSEvent.VIEW_DATA_UPDATED:
                LogDns.d(TAG, "DNSEvent.EPG_VIEW_DATA_UPDATED : " + msg.arg1);
                RadioDNSData data = (RadioDNSData) msg.obj;
                if (data == null) {
                    LogDns.e(TAG, "Data is null!");
                    break;
                }
                String freq = data.getFrequency();
                String pid = data.getPi();
                String cc = data.getCountryCode();
                String url = data.getEpgHost();
                if (data.isVisAvailable()) {
                    Intent intentForView = new Intent(DNSEvent.DNS_ACTION_VIS_HOST_IS_UPDATED);
                    intentForView.putExtra("frequency", freq);
                    sendBroadcast(intentForView);
                }
                if (msg.arg1 == RadioDNSConnection.UPDATED) {
                    mEpgMgr.update(freq, pid, cc, url);
                }
                break;
            case DNSEvent.DNS_EPG_SEND_PI:
                LogDns.d(TAG, "send program info");
                Intent i = new Intent(DNSEvent.DNS_ACTION_PROGRAM_INFO);
                i.putExtra(DNSEvent.DNS_RESPONSE_RESULT, msg.arg1);
                i.putExtra(DNSEvent.DNS_RESPONSE_PROGRAM_FREQ, (String) msg.obj);
                sendBroadcast(i);
                break;
            case DNSEvent.DNS_EPG_SEND_PI_ALL:
                LogDns.d(TAG, "send program info");
                DNSCommand = DNSEvent.DNS_ACTION_PROGRAM_INFO_ALL;
                sendBroadcastCommand(DNSCommand);
                break;
            case DNSEvent.DNS_EPG_EXPIRED_NOW_PROGRAM_INFO:
                LogDns.d(TAG, "Now program is expired.");
                DNSCommand = DNSEvent.DNS_ACTION_UPDATE_PROGRAM_INFO;
                sendBroadcastCommand(DNSCommand);
                break;
            case DNSEvent.DNS_VIS_UPDATE_TEXT:
                LogDns.d(TAG, "DNS_VIS_UPDATE_TEXT");
                DNSCommand = DNSEvent.DNS_ACTION_UPDATE_TEXT;
                sendBroadcastCommand(DNSCommand);
                break;
            case DNSEvent.DNS_VIS_UPDATE_SHOW:
                LogDns.d(TAG, "DNS_VIS_UPDATE_SHOW");
                DNSCommand = DNSEvent.DNS_ACTION_UPDATE_SHOW;
                sendBroadcastCommand(DNSCommand);
                break;
            case DNSEvent.DNS_VIS_UPDATE_LINK:
                LogDns.d(TAG, "DNS_VIS_UPDATE_LINK");
                break;
            case DNSEvent.DNS_EPG_SET_ISSTREAM:
                LogDns.d(TAG, "DNS_EPG_UPDATE_STREAM_MODE false");
                RadioPlayer player = RadioPlayer.getInstance();
                player.setInternetStreamingMode(false);
                break;
            case DNSEvent.DNS_VIS_CLOSE_UI:
                LogDns.d(TAG, "Close DNS UI");
                stopVIS();
                break;
            case DNSEvent.DNS_ON:
                int frequency = (Integer) msg.obj;
                if (mCurFreq == frequency) { // because of timing
                    LogDns.v(TAG, "DNSEvent.DNS_ON ignore same frequency");
                    return;
                }
                LogDns.v(TAG, "DNSEvent.DNS_ON prevFreq : " + LogDns.filter(mCurFreq));
                stopDns();
                mCurFreq = frequency;
                LogDns.v(TAG, "DNSEvent.DNS_ON mCurFreq : " + LogDns.filter(mCurFreq));
                startDNS();
                break;
            case MediaPlayer.MEDIA_ERROR_IO:
                RadioToast.showToast(DNSService.this, R.string.server_response_error,
                        Toast.LENGTH_SHORT);
                break;
            default:
                break;
            }
        }
    }

    private static final String TAG = "DNSService";
    private static EventHandler sEventHandler = null;

    private static DNSService sInstance = null;

    public synchronized static DNSService bindService(Context context, ServiceConnection conn) {
        LogDns.v(TAG, "bindService() - context:" + LogDns.filter(context));
        context.bindService(new Intent(context, DNSService.class), conn, Context.BIND_AUTO_CREATE);
        return sInstance;
    }

    public synchronized static void unbindService(Context context, ServiceConnection conn) {
        LogDns.v(TAG, "unbindService() - context:" + LogDns.filter(context));
        context.unbindService(conn);
    }

    public static DNSService getInstance() {
        return sInstance;
    }

    private boolean mNeedToRestartVIS = false;
    private EpgManager mEpgMgr = null;
    private NetworkMonitorUtil mNetworkMonitor = null;
    private RadioDNSData mCurrentData = null;
    private int mCurFreq = -1;
    private String mIsoCountryCode = null;
    private RadioVISClient mVisClient = null;

    public String getIsoCountryCode() {
        if (mIsoCountryCode == null)
            makeBaseCc();
        return mIsoCountryCode;
    }

    public void setOnRecordInfoListener(SecMediaRecorder.OnInfoListener listener) {
        mEpgMgr.setOnRecordInfoListener(listener);
    }

    public void setStackPriority(int priority) {
        RadioDNSConnection.getInstance(this).setStackPriority(priority);

        if (priority == RadioDNSConnection.VIEW_HIGH) {
            makeBaseCc();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogDns.v(TAG, "onCreate()");
        sInstance = this;
        sEventHandler = new EventHandler();

        mEpgMgr = EpgManager.getInstance(this);
        DnsCacheManager.getInstance().loadCache();

        mNetworkMonitor = NetworkMonitorUtil.getInstance();
        mNetworkMonitor.registerReceiver(true, this);
        mNetworkMonitor.addOnNetworkStateChangeListener(this);

        mCurrentData = new RadioDNSData();
        // System.out.println("onCreate() - mTimeChangeReceiver is registered.");
        // LogDns.d(TAG, "onCreate() - mTimeChangeReceiver is registered.");
        // registerReceiver(mTimeChangeReceiver, new
        // IntentFilter(Intent.ACTION_DATE_CHANGED));

        IntentFilter filter = new IntentFilter();
        filter.addAction(DNSEvent.DNS_ACTION_EPG_STOP_STREAM);
        filter.addAction(DNSEvent.DNS_ACTION_EPG_PAUSE_STREAM);
        filter.addAction(DNSEvent.DNS_ACTION_EPG_RESUME_STREAM);
        registerReceiver(mStreamCommandReceiver, filter);
        JlayerLibrary.load(this);
        LogDns.v(TAG, "onCreate() - success");
    }

    private void makeBaseCc() {
        RadioDNSCommonCountry cc = new RadioDNSCommonCountry();
        mIsoCountryCode = cc.findCountry(this);
        if (mIsoCountryCode != null)
            mIsoCountryCode = new String(mIsoCountryCode);
    }

    public RadioDNSData getCurrentData() {
        return mCurrentData;
    }

    // only after lookup is finished or need to update flags
    public void setCurrentData(RadioDNSData currentData) {
        LogDns.d(TAG, "setCurrentData() - " + currentData.hashCode() + " " + currentData);
        mCurrentData = currentData;
        /*
         * if (currentDnsDataMgr == null) currentDnsDataMgr = new
         * CurrentDNSDataManager(); else if (currentDnsDataMgr.isAlive()) {
         * currentDnsDataMgr.interrupt(); }
         * currentDnsDataMgr.setCurrentData(currentData);
         * 
         * LogDns.d("TEST", "currentDnsDataMgr.getState() - " +
         * currentDnsDataMgr.getState());
         * 
         * if (currentDnsDataMgr.getState().equals(Thread.State.RUNNABLE))
         * currentDnsDataMgr.start();
         */
        sendBroadcast(new Intent(DNSEvent.DNS_ACTION_CURRENT_DATA_UPDATED));
    }

    @Override
    public void onDestroy() {
        LogDns.v(TAG, "DNSService onDestroy start");
        LogDns.d(TAG, "onDestroy() - mTimeChangeReceiver is unregistered.");
        sInstance = null;
        if (mNetworkMonitor != null) {
            mNetworkMonitor.removeOnNetworkStateChangeListener(this);
            mNetworkMonitor.registerReceiver(false, this);
        }
        unregisterReceiver(mStreamCommandReceiver);
        RadioDNSConnection.getInstance(this).destroy();
        DnsInternalDataManager.getInstance().destroy();

        LogDns.v(TAG, "DNSService onDestroy end");
        super.onDestroy();
    }

    public void runDNSSystem(ModeData modeData) {
        switch (modeData.mDnsMode) {
        case DNSEvent.DNS_ON:
            if (mCurFreq == modeData.getFreq()) {
                LogDns.v(TAG, "DNSEvent.DNS_ON ignore same frequency");
                return;
            }
            /** To reduce synchronous run time */
            Message msg = Message.obtain();
            msg.what = DNSEvent.DNS_ON;
            msg.obj = modeData.getFreq();
            /** 450 ms delay for FrequencyDialer animation */
            sEventHandler.sendMessageDelayed(msg, 450);
            break;

        case DNSEvent.DNS_PAUSE:
            if (!isReady()) {
                LogDns.e(TAG, "DNS is not started");
                return;
            }
            pauseDNS();
            break;

        case DNSEvent.DNS_OFF:
            LogDns.v(TAG, "DNSEvent.DNS_OFF");
            if (!isReady()) {
                LogDns.e(TAG, "DNS is not started");
                return;
            }
            stopDns();
            break;

        // case DNSEvent.DNS_START_EPG:
        // startEpg();
        // break;

        // case DNSEvent.DNS_STOP_EPG:
        // stopEpg();
        // break;

        case DNSEvent.DNS_START_VIS:
            startVIS();
            break;

        case DNSEvent.DNS_STOP_VIS:
            stopVIS();
            break;

        case DNSEvent.DNS_UPDATA_VIS:
            updateVIS();
            break;
        // case DNSEvent.DNS_GET_EPG_PI :
        // getEPGPIData();
        // break;

        case DNSEvent.DNS_UPDATE_RDS:
            if (!DnsKoreaTestActivity.isKoreaTest()) {
                if (mCurFreq < 0) {
                    LogDns.v(TAG, "DNSEvent.DNS_UPDATE_RDS mCurFreq < 0");
                    return;
                }
                updateRDS(mCurFreq, modeData.getPi(), modeData.getEcc());
            } else {
                LogDns.d(TAG, "DNSEvent.DNS_UPDATE_RDS : KOREA_TEST_WITHOUT_RDS");
            }
            break;

        default:
            break;
        }
    }

    private boolean isReady() {
        return -1 != mCurFreq;
    }

    private boolean isNeedDNSLookUp(RadioDNSData dnsData) {
        if (dnsData.getPi() == null || Integer.parseInt(dnsData.getPi(), 16) == 0) {
            LogDns.v(TAG, "isNeedDNSLookUp() pi is not correct. PI : " + dnsData.getPi());
            return false;
        }
        /*
         * if (dnsData.isVisAvailable() || dnsData.isEpgAvailable()) {
         * LogDns.v(TAG, "isNeedDNSLookUp() VIS or EPG already exist"); return
         * false; }
         */
        if (dnsData.isNeedLookupRetry() || dnsData.isNeedCcRetry()) {
            LogDns.v(TAG, "isNeedDNSLookUp() NeedRetry is true");
            return false;
        }
        /*
         * if ((dnsData.getPi() != null) &&
         * (RadioDNSConnection.getInstance(this).getStatus() ==
         * AsyncTask.Status.RUNNING)) { return false; }
         */
        return true;
    }

    // sometimes PI is not sent
    private void updateRDS(final int freq, final int pi, final int ecc) {
        String currnetDataPi = mCurrentData.getPi();

        if ((currnetDataPi == null) || (currnetDataPi != null)
                && !(currnetDataPi.equalsIgnoreCase(Integer.toHexString(pi))))
            RadioDNSUtil.saveLog("dnsRDSLog.log", TAG, "updateRDS - Freq : " + LogDns.filter(freq)
                    + " PI : " + Integer.toHexString(pi) + " ECC : " + Integer.toHexString(ecc));

        boolean piChanged = mCurrentData.setPiAfterCheck(Integer.toHexString(pi));
        boolean eccChanged = mCurrentData.setEcc(ecc);

        sendBroadcastCommand(DNSEvent.DNS_ACTION_UPDATE_DATA);

        if (piChanged || (eccChanged && isNeedDNSLookUp(mCurrentData))) {
            mCurrentData.resetRetryCount();
            RadioDNSConnection.getInstance(this).doDnsLookUp(mCurrentData, false,
                    RadioDNSConnection.CURRENT);
        }
    }

    public static EventHandler getEventHandler() {
        return sEventHandler;
    }

    public static final String DNS_RESPONSE_PROGRAM_INFO = "ProgramInfo";
    public static final String DNS_RESPONSE_PROGRAM_FREQ = "ProgramFreq";

    private void sendBroadcastCommand(String actionEvent) {
        int mVisProtocol = 0;
        Intent i = new Intent();
        i.setAction(actionEvent);
        if (mCurrentData.isVisAvailable()) {
            if (mCurrentData.getVisHttpHost() != null)
                mVisProtocol = DNSEvent.DNS_ACTION_VIS_HTTP;
            else
                mVisProtocol = DNSEvent.DNS_ACTION_VIS_STOMP;
            i.putExtra(DNSEvent.DNS_ACTION_VIS_PROTOCOL, mVisProtocol);
        }

        sendBroadcast(i);
    }

    private void startDNS() {
        // if History data exists, add more code.
        // ex) mCurrentData = one of history data
        initDNSConnection();
        if (isReady()) {
            mCurrentData.setFrequency(String.format("%05d", mCurFreq));
            sendBroadcastCommand(DNSEvent.DNS_ACTION_UPDATE_DATA);
        }
    }

    private void pauseDNS() {
        pauseEpg();
        stopVIS();
    }

    private void resumeDNS() {
        resumeEpg();
        startVIS();
    }

    private void stopDns() {
        LogDns.d(TAG, "stopDns()");
        mCurFreq = -1;
        mCurrentData.resetAllData();
        sendBroadcastCommand(DNSEvent.DNS_ACTION_UPDATE_DATA);
        stopDnsConnection(true);
        mEpgMgr.stopEpg();
        stopVIS();
    }

    private void initDNSConnection() {
        RadioDNSConnection.getInstance(this).initialize();
        makeBaseCc();
        if (DnsKoreaTestActivity.isKoreaTest()) {
            LogDns.d(TAG, "initDNSConnection() : KOREA_TEST_WITHOUT_RDS");
            mCurrentData.setPiAfterCheck(DnsKoreaTestActivity.getPi());
            mCurrentData.setFrequency(String.format("%05d", mCurFreq));
            RadioDNSConnection.getInstance(this).doDnsLookUp(mCurrentData, false,
                    RadioDNSConnection.CURRENT);
        }
    }

    private void stopDnsConnection(boolean currentOnly) {
        LogDns.d(TAG, "stopDNSConnection()");
        RadioDNSConnection.getInstance(this).stopDnsConnection(currentOnly);
    }

    public void startStreamRadio(String freq) {
        mEpgMgr.startStreamRadio(freq);
    }

    public void startRecord() {
        mEpgMgr.startRecord();
    }

    public void stopRecord() {
        mEpgMgr.stopRecord();
    }

    public void pauseRecord() {
        mEpgMgr.pauseRecord();
    }

    public void releaseRecord() {
        mEpgMgr.releaseRecord();
    }

    public void setDataTarget(String path) {
        mEpgMgr.setDataTarget(path);
    }

    public void stopStreamRadio() {
        mEpgMgr.stopStreamRadio();
        mEpgMgr.setAutoSwitching(getSharedPreferences(SettingsActivity.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(SettingsActivity.KEY_AUTO_SWITCH_TO_INTERNET,
                false));
    }

    public void cancelJumping() {
        mEpgMgr.stopPollingRssi();
    }

    private void updateEpg() {
        if (mCurrentData != null) {
            mEpgMgr.update(mCurrentData.getFrequency(), mCurrentData.getPi(),
                    mCurrentData.getCountryCode(), mCurrentData.getEpgHost());
            mEpgMgr.startPollingRssi();
        }
    }

    private void pauseEpg() {
        mEpgMgr.pauseEpg();
    }

    private void resumeEpg() {
        mEpgMgr.resumeEpg();
    }

    private void startVIS() {
        if ((MainActivity._instance != null)
                || (DnsTestActivity.getInstance() != null && DnsTestActivity.getInstance()
                        .isResumed())) {
            mNeedToRestartVIS = true;
            if (mCurrentData.getVisHttpHost() != null) {
                LogDns.d(TAG, "Try to VisHttp Connection");
                mVisClient = RadioVISClient.newClient(RadioVISClient.TYPE_HTTP, this);
            } else {
                LogDns.d(TAG, "Try to VisStomp Connection");
                mVisClient = RadioVISClient.newClient(RadioVISClient.TYPE_STOMP, this);
            }
            if (mVisClient != null)
                mVisClient.connect();
            else
                LogDns.e(TAG, "VIS client is null!");
        } else {
            LogDns.v(TAG, "MainActivity is null");
        }
    }

    public void startVIS(int clientType) { // this API for retry
        if ((MainActivity._instance != null)
                || (DnsTestActivity.getInstance() != null && DnsTestActivity.getInstance()
                        .isResumed())) {
            mNeedToRestartVIS = true;
            if (mVisClient != null) {
                mVisClient.disconnect();
                mVisClient = null;
            }
            if ((mCurrentData.getVisHttpHost() != null) && (clientType == RadioVISClient.TYPE_HTTP)) {
                LogDns.d(TAG, "Try to VisHttp Connection");
                mVisClient = RadioVISClient.newClient(RadioVISClient.TYPE_HTTP, this);
            }
            if ((mCurrentData.getVisStompHost() != null)
                    && (clientType == RadioVISClient.TYPE_STOMP)) {
                LogDns.d(TAG, "Try to VisStomp Connection");
                mVisClient = RadioVISClient.newClient(RadioVISClient.TYPE_STOMP, this);
            }
            if (mVisClient != null) {
                mVisClient.connect();
            } else {
                LogDns.e(TAG, "VIS client is null!");
            }
        } else {
            LogDns.v(TAG, "MainActivity is null");
        }
    }

    private void updateVIS() {
        if (mVisClient != null) {
            mVisClient.disconnect();
            mVisClient = null;
        }

        if (mNeedToRestartVIS) {
            startVIS();
        }
    }

    private void stopVIS() {
        if (mVisClient != null) {
            mVisClient.disconnect();
            // mVisClient.destroy();
            mVisClient = null;
            // RadioVISClient.sClients.clear();
        }
        mNeedToRestartVIS = false;
    }

    // private void getEPGPIData(){
    // EPGManager.getInstance(context).setMode(EPGManager.EPG_PI_SEARCH);
    // }

    public boolean isVisAvailable() {
        if (!NetworkMonitorUtil.isConnected(this)) {
            LogDns.e(TAG, "isVisAvailable() - Network is not available.");
            return false;
        }
        return mCurrentData.isVisAvailable();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        public DNSService getDNSService() {
            return DNSService.this;
        }
    }

    public boolean retryDns() {
        RadioDNSConnection dnsConn = RadioDNSConnection.getInstance(this);

        if (mCurrentData.getPi() == null) {
            LogDns.e(TAG, "RDS_PI is not received");
            return false;
        }

        if (mCurrentData.isSameFreq(mCurFreq)) {
            if (mCurrentData.isNeedLookupRetry() || mCurrentData.isNeedCcRetry()) {
                LogDns.v(TAG, "Retry DNSConnection");
                RadioDNSConnection.getInstance(this).doDnsLookUp(mCurrentData, false,
                        RadioDNSConnection.CURRENT);
                return true;
            }
        } else {
            LogDns.e(TAG, "onNetworkConnected" + mCurrentData);
        }

        dnsConn.checkStack();
        return true;
    }

    @Override
    public void onNetworkConnected() {
        LogDns.v(TAG, "onNetworkConnected : curFreq - [" + LogDns.filter(mCurFreq) + "] "
                + "currentData [" + LogDns.filter(mCurrentData.toString()) + "]");
        if (!isReady()) {
            LogDns.e(TAG, "DNS is not started");
            return;
        }
        retryDns();
        if (mCurrentData.isSameFreq(mCurFreq) && mCurrentData.isEpgAvailable()) {
            mEpgMgr.startPollingRssi();
        }
        sendBroadcastCommand(DNSEvent.DNS_ACTION_UPDATE_VIS_DATA);
    }

    @Override
    public void onNetworkDisconnected() {
        LogDns.v(TAG, "onNetworkDisconnected");
        stopDnsConnection(false);
        mEpgMgr.stopEpg();
        stopVIS();
        sendBroadcastCommand(DNSEvent.DNS_ACTION_UPDATE_VIS_DATA);
    }

    /*
     * public class CurrentDNSDataManager extends Thread {
     * 
     * private RadioDNSData dnsData;
     * 
     * public void setCurrentData(RadioDNSData currentData) { dnsData =
     * currentData; }
     * 
     * @Override public void run() { long expiredTime; try { expiredTime =
     * System.currentTimeMillis() - dnsData.getShortestTTL();
     * sleep(expiredTime); if ((dnsData.getExpiredBit()) != 0) ; } catch
     * (InterruptedException e) { e.printStackTrace(); } } }
     */

    // BroadcastReceiver mTimeChangeReceiver = new BroadcastReceiver() {
    // @Override
    // public void onReceive(Context context, Intent intent) {
    // if (intent.getAction().equals(Intent.ACTION_DATE_CHANGED)) {
    // LogDns.d(TAG, "onReceive() - mTimeChangeReceiver");
    // LogDns.d(TAG, "Date has been changed");
    // mEpgMgr.updatePi();
    // }
    // }
    // };

    private BroadcastReceiver mStreamCommandReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogDns.d(TAG, "mStreamCommandReceiver.onReceive() - action:" + LogDns.filter(action));
            if (DNSEvent.DNS_ACTION_EPG_STOP_STREAM.equals(action)) {
                stopDns();
            } else if (DNSEvent.DNS_ACTION_EPG_PAUSE_STREAM.equals(action)) {
                pauseDNS();
            } else if (DNSEvent.DNS_ACTION_EPG_RESUME_STREAM.equals(action)) {
                resumeDNS();
            }
        }
    };

    public PiData requestProgramInfo(String freq, String pid, Date date) {
        if (freq == null || pid == null || date == null) {
            LogDns.e(TAG, "requestProgramInfo() - params are missed. freq:" + LogDns.filter(freq)
                    + " pi:" + LogDns.filter(pid) + " date:" + date);
            return null;
        }
        int tmpFreq = Integer.valueOf(freq);
        if (tmpFreq < 8750 || tmpFreq > 10800 || pid.isEmpty()) {
            LogDns.e(TAG, "requestProgramInfo() - params are wrong. freq:" + LogDns.filter(freq)
                    + " pi:" + LogDns.filter(pid));
            return null;
        }
        if (!mEpgMgr.updatePi(freq, pid, date)) {
            RadioDNSData dnsData = new RadioDNSData();
            dnsData.setFrequency(freq);
            if (dnsData.setPi(pid)) {
                RadioDNSConnection.getInstance(this).doDnsLookUp(dnsData, false,
                        RadioDNSConnection.VIEW);
            }
        }
        LogDns.i("request programInfo", "waiting start(" + LogDns.filter(freq) + ")");
        return mEpgMgr.getPiData(freq);
    }

    public PiData getPiData(String freq) {
        return mEpgMgr.getPiData(freq);
    }

    public void setAutoSwitching(boolean value) {
        mEpgMgr.setAutoSwitching(value);
    }

    public Bitmap getVisImage() {
        if (null == mVisClient) {
            LogDns.e(TAG, "getVisImage() - client is null");
            return null;
        }
        return RadioVISFrame.obtain().getImage();
    }

    public String getVisText() {
        if (null == mVisClient) {
            LogDns.e(TAG, "getVisText() - client is null");
            return null;
        }
        return RadioVISFrame.obtain().getText();
    }

    public String getVisImageUrl() {
        if (null == mVisClient) {
            LogDns.e(TAG, "getVisImageUrl() - client is null");
            return null;
        }
        return RadioVISFrame.obtain().getImageUrl();
    }
}
