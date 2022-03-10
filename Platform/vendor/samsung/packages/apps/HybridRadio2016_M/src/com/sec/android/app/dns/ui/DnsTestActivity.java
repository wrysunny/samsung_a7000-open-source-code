package com.sec.android.app.dns.ui;

import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import com.samsung.media.fmradio.FMPlayerException;
import com.sec.android.app.dns.DNSEvent;
import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.ModeData;
import com.sec.android.app.dns.RadioDNSServiceDataIF;
import com.sec.android.app.dns.data.RadioDNSData;
import com.sec.android.app.dns.radioepg.EpgData;
import com.sec.android.app.dns.radioepg.PiData;
import com.sec.android.app.fm.R;
import com.sec.android.app.fm.RadioPlayer;
import com.sec.android.app.fm.RadioToast;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.telephony.TelephonyManager;
import android.util.secutil.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DnsTestActivity extends Activity {

    class displayContent extends AsyncTask<String, Void, Bitmap> {
        /* RadioDNSDisplayService display; */

        @Override
        protected Bitmap doInBackground(String... params) {
            Log.secV("displayContent", "doInBackground");
            Bitmap imgBitmap = null;
            try {
                URL url = new URL(params[0]);
                URLConnection conn = url.openConnection();
                conn.connect();

                int size = conn.getContentLength();
                BufferedInputStream input = new BufferedInputStream(conn.getInputStream(), size);
                imgBitmap = BitmapFactory.decodeStream(input);
                input.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.e("displayContent", "bitmap created");
            return imgBitmap;

        }

        @Override
        protected void onPostExecute(Bitmap result) {
            Log.secV("displayContent", "displayContent - onPostExcute");
            super.onPostExecute(result);
            if (result != null) {
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(result, 360, 270, true);
                setImageContent(resizedBitmap);
                setLinkContent(visContentLink);
            }
        }
    }

    private static DnsTestActivity instance = null;
    private static boolean sIsDnsTest = false;

    public static DnsTestActivity getInstance() {
        if (instance == null)
            instance = new DnsTestActivity();
        return instance;
    }

    public static boolean isDnsTest() {
        return sIsDnsTest;
    }

    public static void setDnsTest(boolean value) {
        sIsDnsTest = value;
    }

    private Button mAF1, mAF2;
    private Bitmap[] mBitmap = new Bitmap[2];
    private TextView mCountryCode = null;
    private TextView mCurrentFrequency = null;
    private ServiceConnection mDNSServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            Log.secD("TEST", "onDNSServiceConnected()");
            mDnsSystem = ((DNSService.LocalBinder) service).getDNSService();
            updateData();
        }

        public void onServiceDisconnected(ComponentName cName) {
            Log.secD("TEST", "onDNSServiceDisconnected()");
            mDnsSystem = null;
        }
    };
    private DNSService mDnsSystem = null;
    // private LinearLayout mDNSTitlebarBg = null;
    private TextView mEcc = null;
    private Button mEPGApplyRSSI;
    private TextView mEpgAvailable = null;
    private TextView mEPGBufferingTime;
    private EditText mEPGDelayNormal;
    private EditText mEPGDelayToFMRadio;
    private EditText mEPGDelayToStream;
    private TextView mEPGHost = null;
    private TextView mEPGIsStream = null;
    private TextView mEpgNowPi;
    private TextView mEPGNowRSSI;
    private TextView mEPGPIUrl;
    private EditText mEPGPollingCount;
    private EditText mEPGRSSIToFM;
    private EditText mEPGRSSIToStream;
    private TextView mEPGStreamUrl;
    // private LinearLayout mEPGTitlebarBg = null;
    private TextView mEPGXSIUrl;

    private String[] mLink = { "No link", "No link" };

    BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e("DNSMessageReceiver", "Get the content");
            String action = intent.getAction();
            if (action.equals(DNSEvent.DNS_ACTION_UPDATE_DATA)) {
                updateData();
            } else if (action.equals(DNSEvent.DNS_ACTION_UPDATE_SHOW)) {
                if(null == mDnsSystem)
                    return;
                visContentBody = mDnsSystem.getVisImageUrl();
                visContentLink = mDnsSystem.getVisText();
                new displayContent().execute(visContentBody);
                Log.e("VISMessageReceiver", "onReceive() - " + DNSEvent.DNS_ACTION_UPDATE_SHOW
                        + " SHOW:" + visContentBody + "Link : " + visContentLink);
            } else if (action.equals(DNSEvent.DNS_ACTION_UPDATE_TEXT)) {
                if(null == mDnsSystem)
                    return;
                visContentBody = mDnsSystem.getVisText();
                setTextContent(visContentBody);
                Log.e("VISMessageReceiver", "onReceive() - " + DNSEvent.DNS_ACTION_UPDATE_TEXT
                        + " TEXT:" + visContentBody);
            } else if (action.equals(DNSEvent.DNS_ACTION_UPDATE_STREAM_PREPARED)) {
                mEPGBufferingTime.setText(String.valueOf(RadioDNSServiceDataIF
                        .getEpgBufferingTime()));
                if (RadioDNSServiceDataIF.isEpgPlayingStreamRadio())
                    mEPGIsStream.setText("Internet Radio");
                /*
                 * else mEPGIsStream.setText("FM Radio");
                 */
            } else if (action.equals(DNSEvent.DNS_ACTION_UPDATE_ISSTREAM)) {
                if (!RadioDNSServiceDataIF.isEpgPlayingStreamRadio())
                    /*
                     * mEPGIsStream.setText("Internet Radio"); else
                     */
                    mEPGIsStream.setText("FM Radio");
            } else if (action.equals(DNSEvent.DNS_ACTION_UPDATE_NOW_EPG_DATA)) {
                EpgData epgData = RadioDNSServiceDataIF.getEpgNowEpgData();
                if (epgData != null) {
                    mEPGHost.setText(epgData.getEpgUrl());
                    mEPGXSIUrl.setText(epgData.getXsiUrl());
                    mEPGPIUrl.setText(epgData.getPiUrl());
                    mEPGStreamUrl.setText(epgData.getStreamUrl());
                }
            } else if (action.equals(DNSEvent.DNS_ACTION_UPDATE_RSSI)) {
                int rssi = intent.getIntExtra(DNSEvent.DNS_EXTRA_RSSI, 0);
                mEPGNowRSSI.setText(String.valueOf(rssi));
            } else if (action.equals(DNSEvent.DNS_ACTION_UPDATE_PROGRAM_INFO)
                    || action.equals(Intent.ACTION_TIME_CHANGED)
                    || action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                EpgData epgData = RadioDNSServiceDataIF.getEpgNowEpgData();
                if (epgData != null) {
                    PiData piData = epgData.getPiData();
                    if (piData != null) {
                        mEpgNowPi.setText(piData.getProgramName(Calendar.getInstance().getTime()));
                    }
                }
            }
        }
    };

    private boolean mNeedResumeVis = false;
    private TextView mPiRDS = null;
    private TextView mVisAvailable = null;
    private TextView mVISHost = null;
    private ImageView mVISImage1 = null;
    private ImageView mVISImage2 = null;
    private TextView mVISLink1 = null;
    private TextView mVISLink2 = null;
    private TextView mVISPort = null;
    private TextView mVISText = null;
    // private LinearLayout mVISTitlebarBg = null;
    private String visContentBody = null;
    private String visContentLink = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_dns);
        setDNSContentVIew();

        IntentFilter filter = new IntentFilter();
        filter.addAction(DNSEvent.DNS_ACTION_UPDATE_DATA);
        filter.addAction(DNSEvent.DNS_ACTION_UPDATE_SHOW);
        filter.addAction(DNSEvent.DNS_ACTION_UPDATE_TEXT);
        filter.addAction(DNSEvent.DNS_ACTION_UPDATE_STREAM_PREPARED);
        filter.addAction(DNSEvent.DNS_ACTION_UPDATE_ISSTREAM);
        filter.addAction(DNSEvent.DNS_ACTION_UPDATE_NOW_EPG_DATA);
        filter.addAction(DNSEvent.DNS_ACTION_UPDATE_RSSI);
        filter.addAction(DNSEvent.DNS_ACTION_UPDATE_PROGRAM_INFO);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mMessageReceiver, filter);
        mDnsSystem = DNSService.bindService(DnsTestActivity.this, mDNSServiceConnection);
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mMessageReceiver);
        if (mDnsSystem != null) {
            DNSService.unbindService(DnsTestActivity.this, mDNSServiceConnection);
            mDnsSystem = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * if (mDnsSystem == null){ Log.secE("TEST","system is null!!!!!");
         * checkDNSSystem.run(); }
         */

        String countrycode = "";

        TelephonyManager phone = (TelephonyManager) this
                .getSystemService(Context.TELEPHONY_SERVICE);
        countrycode = phone.getNetworkCountryIso();
        LogDns.e("TEST", "jhs_test " + countrycode);
        Toast.makeText(this, countrycode, Toast.LENGTH_SHORT).show();
        if (mNeedResumeVis) {
            mDnsSystem.runDNSSystem(new ModeData(DNSEvent.DNS_START_VIS));
            mNeedResumeVis = false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDnsSystem.runDNSSystem(new ModeData(DNSEvent.DNS_STOP_VIS));
        mNeedResumeVis = true;
    }

    private void setDNSContentVIew() {
        // mDNSTitlebarBg = (LinearLayout) findViewById(R.id.dns_titlebarbg);
        mCurrentFrequency = (TextView) findViewById(R.id.currentfrequency);
        mPiRDS = (TextView) findViewById(R.id.pi_rds);
        mCountryCode = (TextView) findViewById(R.id.countrycode);
        mEcc = (TextView) findViewById(R.id.ecc);
        mVisAvailable = (TextView) findViewById(R.id.visAvailable);
        mEpgAvailable = (TextView) findViewById(R.id.epgAvailable);

        // mVISTitlebarBg = (LinearLayout) findViewById(R.id.vis_titlebarbg);
        mVISHost = (TextView) findViewById(R.id.vis_host);
        mVISPort = (TextView) findViewById(R.id.vis_port);
        mVISImage1 = (ImageView) findViewById(R.id.vis_image1);
        mVISLink1 = (TextView) findViewById(R.id.vis_link1);
        mVISImage2 = (ImageView) findViewById(R.id.vis_image2);
        mVISLink2 = (TextView) findViewById(R.id.vis_link2);
        mVISText = (TextView) findViewById(R.id.vis_text);

        // mEPGTitlebarBg = (LinearLayout) findViewById(R.id.epg_titlebarbg);
        mEPGIsStream = (TextView) findViewById(R.id.epg_is_stream);
        mEPGBufferingTime = (TextView) findViewById(R.id.epg_buffering_time);
        mEPGHost = (TextView) findViewById(R.id.epg_host);
        mEPGXSIUrl = (TextView) findViewById(R.id.epg_xsiUrl);
        mEPGPIUrl = (TextView) findViewById(R.id.epg_piUrl);
        mEPGStreamUrl = (TextView) findViewById(R.id.epg_streamUrl);
        mEPGNowRSSI = (TextView) findViewById(R.id.epg_nowrssi);
        mEPGRSSIToFM = (EditText) findViewById(R.id.epg_rssitofm);
        mEPGRSSIToStream = (EditText) findViewById(R.id.epg_rssitostream);
        mEPGPollingCount = (EditText) findViewById(R.id.epg_polling_count);
        mEPGDelayNormal = (EditText) findViewById(R.id.epg_normal_delay);
        mEPGDelayToStream = (EditText) findViewById(R.id.epg_stream_delay);
        mEPGDelayToFMRadio = (EditText) findViewById(R.id.epg_fmradio_delay);

        mEPGApplyRSSI = (Button) findViewById(R.id.epg_save);
        mEPGApplyRSSI.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                RadioDNSServiceDataIF.setEpgRssiToRadio(Integer.parseInt(mEPGRSSIToFM.getText()
                        .toString()));
                RadioDNSServiceDataIF.setEpgRssiToStream(Integer.parseInt(mEPGRSSIToStream
                        .getText().toString()));
                RadioDNSServiceDataIF.setEpgPollingCount(Integer.parseInt(mEPGPollingCount
                        .getText().toString()));
                RadioDNSServiceDataIF.setEpgPollingDelayNormal(Integer.parseInt(mEPGDelayNormal
                        .getText().toString()));
                RadioDNSServiceDataIF.setEpgPollingDelayToStream(Integer.parseInt(mEPGDelayToStream
                        .getText().toString()));
                RadioDNSServiceDataIF.setEpgPollingDelayToRadio(Integer.parseInt(mEPGDelayToFMRadio
                        .getText().toString()));
            }
        });

        mAF1 = (Button) findViewById(R.id.Classic1);
        mAF1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    RadioPlayer.getInstance().tuneAsyncEx(10060);
                } catch (FMPlayerException e) {
                    RadioToast.showToast(DnsTestActivity.this, e);
                }
            }
        });

        mAF2 = (Button) findViewById(R.id.Classic2);
        mAF2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                try {
                    RadioPlayer.getInstance().tuneAsyncEx(10090);
                } catch (FMPlayerException e) {
                    RadioToast.showToast(DnsTestActivity.this, e);
                }
            }
        });

        mEpgNowPi = (TextView) findViewById(R.id.epg_now_pi);
    }

    public void setImageContent(Bitmap image) {
        Log.secD("FMApp", "setImageContent");
        mBitmap[1] = mBitmap[0];
        mBitmap[0] = image;
        mVISImage1.setImageBitmap(mBitmap[0]);
        mVISImage1.invalidate();
        if (mBitmap[1] != null) {
            mVISImage2.setImageBitmap(mBitmap[1]);
            mVISImage2.invalidate();
        }
    }

    public void setLinkContent(String visLink) {
        if (visLink == null)
            visLink = "No Link";
        mLink[1] = mLink[0];
        mLink[0] = visLink;
        mVISLink1.setText(mLink[0]);
        mVISLink1.invalidate();
        mVISLink2.setText(mLink[1]);
        mVISLink2.invalidate();
    }

    public void setTextContent(String text) {
        Log.secD("FMApp", "setTextContent");
        StringBuffer visText = new StringBuffer(mVISText.getText());
        visText.append('\n');
        mVISText.setText(visText.append(text));
    }

    // public void setVISClient(RadioVISStompClient visClient) {
    // this.client = visClient;
    // }

    private void updateData() {
        RadioDNSData dnsData = mDnsSystem.getCurrentData();
        mCurrentFrequency.setText(dnsData.getFrequency());
        mPiRDS.setText(dnsData.getPi());
        mCountryCode.setText(dnsData.getCountryCode());
        mVisAvailable.setText(String.valueOf(dnsData.isVisAvailable()));
        mEpgAvailable.setText(String.valueOf(dnsData.isEpgAvailable()));

        mEcc.setText(Integer.toHexString(dnsData.getEcc()));
        mVISHost.setText(dnsData.getVisStompHost());
        mVISPort.setText(String.valueOf(dnsData.getVisStompPort()));

        Log.e("DNSMessageReceiver", "setDNSData() - dnsData:" + dnsData);

        if (RadioDNSServiceDataIF.isEpgPlayingStreamRadio())
            mEPGIsStream.setText("Internet Radio");
        else
            mEPGIsStream.setText("FM Radio");
        mEPGBufferingTime.setText(String.valueOf(RadioDNSServiceDataIF.getEpgBufferingTime()));
        EpgData epgData = RadioDNSServiceDataIF.getEpgNowEpgData();
        if (epgData != null) {
            mEPGHost.setText(epgData.getEpgUrl());
            mEPGXSIUrl.setText(epgData.getXsiUrl());
            mEPGPIUrl.setText(epgData.getPiUrl());
            mEPGStreamUrl.setText(epgData.getStreamUrl());
            PiData piData = epgData.getPiData();
            if (piData != null) {
                mEpgNowPi.setText(piData.getProgramName(Calendar.getInstance().getTime()));
            }
        }
        mEPGRSSIToFM.setText(String.valueOf(RadioDNSServiceDataIF.getEpgRssiToRadio()));
        mEPGRSSIToStream.setText(String.valueOf(RadioDNSServiceDataIF.getEpgRssiToStream()));
        mEPGPollingCount.setText(String.valueOf(RadioDNSServiceDataIF.getEpgPollingCount()));
        mEPGDelayNormal.setText(String.valueOf(RadioDNSServiceDataIF.getEpgPollingDelayNormal()));
        mEPGDelayToStream
                .setText(String.valueOf(RadioDNSServiceDataIF.getEpgPollingDelayToStream()));
        mEPGDelayToFMRadio
                .setText(String.valueOf(RadioDNSServiceDataIF.getEpgPollingDelayToRadio()));
    }
}
