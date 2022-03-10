package com.sec.android.app.dns.radiovis.http;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import com.sec.android.app.dns.DNSEvent;
import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.ModeData;
import com.sec.android.app.dns.radiovis.RadioVISClient;
import com.sec.android.app.dns.radiovis.RadioVISFrame;

public class RadioVISHttpClient extends RadioVISClient {

    protected static final String TAG = RadioVISHttpClient.class.getSimpleName();
    private int mJsonLength = 0;
    private String mLastIdText = null;
    private String mLastIdImage = null;
    private String mContentType = null;
    private DNSService mDnsSystem = null;
    private boolean mTrigerTimeNow = true;
    private boolean mIsTextSuccess = false;
    private RadioVISHttpProtocol mVISHttpProtocol;
    private static EventHandler sEventHandler = null;

    public DNSService getDnsSystem() {
        return mDnsSystem;
    }

    public RadioVISHttpClient(DNSService dnsSystem) {
        this.mDnsSystem = dnsSystem;
        mVISHttpProtocol = RadioVISHttpProtocol.getInstance(this);
        sEventHandler = new EventHandler();
    }

    public static EventHandler getEventHandler() {
        return sEventHandler;
    }

    @Override
    public void connect() {
        LogDns.v(TAG, "Inside the Connect");
        mVISHttpProtocol.connectNetwork();
    }

    @Override
    public void disconnect() {
        mVISHttpProtocol.disconnectNetwork();
    }

    public class EventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            String results = null;
            mJsonLength = 0;
            switch (msg.what) {
            case DNSEvent.DNS_VIS_HTTP_RECEIVE_DATA_FRAME:
                results = (String) msg.obj;
                mIsTextSuccess = false;
                String contentType = null;
                JSONObject jsonObject = null;
                try {
                    if (results.contains("[")) {
                        JSONArray mJsonArray = new JSONArray(results);
                        int mJsonArrayLength = mJsonArray.length();
                        LogDns.v(TAG, "JSON Array Length:" + mJsonArrayLength);
                        mJsonLength = mJsonArrayLength - 1;
                        for (int i = mJsonArrayLength - 1; i >= 0; i--) {
                            LogDns.v(TAG, "mJsonArray:length " + mJsonLength);
                            try {
                                jsonObject = mJsonArray.getJSONObject(i);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
 
                            if(jsonObject != null){
                                contentType = RadioVISHttpParser.getRadioVisFrameBodyType(jsonObject);
                                if ("TEXT".equals(contentType)) {
                                    mIsTextSuccess = true;
                                    showVISData(jsonObject);
                                } else if ("SHOW".equals(contentType)) {
                                    showVISData(jsonObject);
                                }
                            }
                            mJsonLength--;
                        }

                    } else {
                        jsonObject = new JSONObject(results);
                        LogDns.v(TAG, "mJsonObject" + jsonObject.toString());
                        showVISData(jsonObject);
                    }
                } catch (JSONException e) {
                    LogDns.v(TAG, "This is not valid JSON data");
                    e.printStackTrace();
                }
                break;
            case DNSEvent.DNS_VIS_HTTP_RECEIVE_DATA_FRAME_EMPTY:
                LogDns.v(TAG, "There is no Data received from Server");
                trailCount++;
                if (trailCount < 3) {
                    if (trailCount == 1) {
                        disconnect();
                        mDnsSystem.startVIS(TYPE_STOMP);
                    }
                } else {
                    mDnsSystem.runDNSSystem(new ModeData(DNSEvent.DNS_STOP_VIS));
                }
                break;
            case DNSEvent.DNS_VIS_UPDATE_SHOW:
                if (mTrigerTimeNow) {
                    LogDns.v(TAG, "trigger time now from RadioVISHttpClient");
                    Message msgUpdate = Message.obtain();
                    msgUpdate.what = DNSEvent.DNS_VIS_UPDATE_SHOW;
                    DNSService.getEventHandler().sendMessage(msgUpdate);
                }
                break;
            default:
                break;
            }
        }
    }

    public void showVISData(final JSONObject jsonDataShow) {
        String contentBody = null;
        String contentLink = null;
        RadioVISFrame frame = RadioVISFrame.obtain();
        mContentType = RadioVISHttpParser.getRadioVisFrameBodyType(jsonDataShow);
        if (RadioVISHttpProtocol.NONE.equals(mContentType)) {
            String destination = RadioVISHttpParser.getRadioVisFrameDestination(jsonDataShow);
            if (destination.contains(RadioVISHttpProtocol.VIS_HTTP_REQEST_TEXT)) {
                mVISHttpProtocol.connectNetwork(RadioVISHttpProtocol.VIS_HTTP_REQEST_TEXT, null);
            } else if (destination.contains(RadioVISHttpProtocol.VIS_HTTP_REQEST_IMAGE))
                mVISHttpProtocol.connectNetwork(RadioVISHttpProtocol.VIS_HTTP_REQEST_IMAGE, null);
        } else if (RadioVISHttpProtocol.TEXT.equals(mContentType)) {
            frame.setType(mContentType);
            contentBody = RadioVISHttpParser.getRadioVisFrameBody(jsonDataShow);
            mLastIdText = RadioVISHttpParser.getRadioVisFrameMessageId(jsonDataShow);
            if (!mIsTextSuccess) {
                Message msgToapp = Message.obtain();
                frame.setText(contentBody);
                msgToapp.what = DNSEvent.DNS_VIS_UPDATE_TEXT;
                DNSService.getEventHandler().sendMessage(msgToapp);
            }
            if (mJsonLength <= 0) {
                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        mVISHttpProtocol.connectNetwork(RadioVISHttpProtocol.VIS_HTTP_REQEST_IMAGE,
                                mLastIdImage);
                    }
                }, 1000);
            } else {
                LogDns.v(TAG, "Text Not Making request,it is json array");
            }
        } else if (RadioVISHttpProtocol.SHOW.equals(mContentType)) {
            frame.setType(mContentType);
            contentBody = RadioVISHttpParser.getRadioVisFrameBody(jsonDataShow);
            frame.setImageUrl(contentBody);
            Bitmap image = getImageFromCache(contentBody);
            if (image == null) {
                downloadImage(TYPE_HTTP, contentBody);
            } else {
                LogDns.v(TAG, "Image is Taken from cache");
            }

            String triggerTime = RadioVISHttpParser.getRadioVisFrameTriggerTime(jsonDataShow);
            if(triggerTime != null) {
                if (triggerTime.equalsIgnoreCase("now")) {
                    mTrigerTimeNow = true;
                } else {
                    mTrigerTimeNow = false;
                }
            } else {
                mTrigerTimeNow = false;
            }

            contentLink = RadioVISHttpParser.getRadioVisFrameLink(jsonDataShow);
            LogDns.v(TAG, "Content_link : " + contentLink);
            frame.setLink(contentLink);
            mLastIdImage = RadioVISHttpParser.getRadioVisFrameMessageId(jsonDataShow);
            if (mJsonLength <= 0) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        LogDns.v(TAG, "Delayed here");
                        mVISHttpProtocol.connectNetwork(RadioVISHttpProtocol.VIS_HTTP_REQEST_TEXT,
                                mLastIdText);
                    }
                }, 1000);

            } else {
                LogDns.v(TAG, "Image:Not Making request,it is json array");
            }
        }
    }
}
