package com.sec.android.app.dns.radiovis.stomp;

import android.graphics.Bitmap;
import android.os.Message;

import com.sec.android.app.dns.DNSEvent;
import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.radiovis.RadioVISClient;
import com.sec.android.app.dns.radiovis.RadioVISFrame;

public class RadioVISStompClient extends RadioVISClient {
    public interface OnCallbackListener {
        public void onCallbackReceived(String message);
    }

    public static final String CONNECTED_ACTION = "com.sec.android.app.dns.radiovis.connected";
    public static final String MESSAGE_ACTION = "com.sec.android.app.dns.radiovis.message";
    private static final String TAG = RadioVISStompClient.class.getSimpleName();

    private DNSService mDnsSystem = null;

    private final OnCallbackListener mListener = new OnCallbackListener() {

        /**
         * @param message
         *            the exception message
         */
        @Override
        public void onCallbackReceived(String message) {
            LogDns.v(TAG, "onCallbackReceived() - " + message);
            RadioVISFrame frame = RadioVISStompParser.parse(message);
            if (null == frame)
                return;
            switch (frame.getCommand()) {
            case RadioVISStompProtocol._CONNECTED:
                LogDns.v(TAG, "_CONNECTED Received");
                mVISStompProtocol.subscribe();
                break;
            case RadioVISStompProtocol._MESSAGE:
                LogDns.v(TAG, "_MESSAGE Received");
                String ContentType = frame.getType();
                if (ContentType != null) {
                    if (ContentType.equals(RadioVISStompProtocol.TEXT)) {
                        Message msg = Message.obtain();
                        msg.what = DNSEvent.DNS_VIS_UPDATE_TEXT;
                        DNSService.getEventHandler().sendMessage(msg);
                    } else if (ContentType.equals(RadioVISStompProtocol.SHOW)) {
                        Bitmap image = getImageFromCache(frame.getImageUrl());
                        if (image == null) {
                            downloadImage(TYPE_STOMP, frame.getImageUrl());
                        } else {
                            LogDns.v(TAG, "Image is Taken from cache");
                            frame.setImage(image);
                            Message msg = Message.obtain();
                            msg.what = DNSEvent.DNS_VIS_UPDATE_SHOW;
                            DNSService.getEventHandler().sendMessage(msg);
                        }
                    }
                }
                break;
            case RadioVISStompProtocol._ERROR:
                LogDns.v(TAG, "Error Occure!!!!!!!");
                trailCount++;
                if (trailCount == 2) {
                    disconnect();
                    mDnsSystem.startVIS(TYPE_HTTP);
                }
                break;
            default:
                break;
            }
        }
    };

    private RadioVISStompProtocol mVISStompProtocol;

    public RadioVISStompClient(DNSService dnsSystem) {
        mDnsSystem = dnsSystem;
        mVISStompProtocol = RadioVISStompProtocol.getInstance();
        mVISStompProtocol.setListener(mListener);
    }

    @Override
    public void connect() {
        mVISStompProtocol.connectNetwork();
        RadioVISFrame.obtain().clear();
    }

    @Override
    public void disconnect() {
        mVISStompProtocol.disconnectNetwork();
        RadioVISFrame.obtain().clear();
    }

    public DNSService getDnsSystem() {
        return mDnsSystem;
    }
}
