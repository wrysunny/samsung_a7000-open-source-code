package com.sec.android.app.dns.radiovis.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import android.os.AsyncTask;
import android.os.Message;
import com.sec.android.app.dns.DNSEvent;
import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.data.RadioDNSData;
import com.sec.android.app.dns.radiovis.RadioVISProtocol;

public class RadioVISHttpProtocol implements RadioVISProtocol {
    private static final String TAG = RadioVISHttpProtocol.class.getSimpleName();

    private RadioDNSData mDNSData;
    private String mLastId = null;
    private String mTopicType = "";
    private static int mCounter = 0;
    private URL mSRVTopicUrl = null;
    private DNSService mDnsSystem = null;
    private boolean mFirstRequest = false;
    private boolean mConnectionState = false;
    public static final String SHOW = "SHOW";
    public static final String TEXT = "TEXT";
    public static final String NONE = "NONE";
    public static final String HTTP_BODY = "body";
    private static RadioVISHttpProtocol sInstance;
    private static MakeConnection sConnection = null;
    private HttpURLConnection mHttpURLConnection = null;
    public static final String HTTP_HEADERS = "headers";
    public static final String HTTP_LINK = "RadioVIS-Link";
    public static final String VIS_HTTP_REQEST_TEXT = "text";
    public static final String VIS_HTTP_REQEST_IMAGE = "image";
    public static final String HTTP_MESSAGE_ID = "RadioVIS-Message-ID";
    public static final String HTTP_DESTINATION = "RadioVIS-Destination";
    public static final String HTTP_TRIGGER_TIME = "RadioVIS-Trigger-Time";
    public static final String HTTP_CONNECTION_ERROR = "Error in Connection";

    private RadioVISHttpProtocol(RadioVISHttpClient radioVISHttpClient) {
        mDnsSystem = radioVISHttpClient.getDnsSystem();
        this.mDNSData = mDnsSystem.getCurrentData();
    }

    public static RadioVISHttpProtocol getInstance(RadioVISHttpClient radioVISHttpClient) {
        if (sInstance == null) {
            sInstance = new RadioVISHttpProtocol(radioVISHttpClient);
        }
        return sInstance;
    }

    @Override
    public void connectNetwork() {
        LogDns.v(TAG, "Inside the Connect Network");
        mConnectionState = false;
        mFirstRequest = true;
        sConnection = new MakeConnection();
        sConnection.execute(makeRequestURL());
    }

    public void connectNetwork(String topicType, String lastId) {
        System.setProperty("http.keepAlive", "true");
        LogDns.v(TAG, "VISHTTP Connect Network : " + topicType);
        if (mConnectionState) {
            LogDns.v(TAG, "Return from IS_CONNECTION_CLOSE");
            return;
        }
        mFirstRequest = false;
        this.mTopicType = topicType;
        this.mLastId = lastId;
        sConnection = new MakeConnection();
        sConnection.execute(makeRequestURL());
    }

    @Override
    public void disconnectNetwork() {
        LogDns.v(TAG, "Shuting Down the Http Connection");
        if (sConnection != null)
            sConnection.cancel(true);
        sConnection = null;
        mConnectionState = true;
    }

    public String makeTopic() {
        RadioDNSData data = mDnsSystem.getCurrentData();
        String finalReturnURI = "";
        StringBuilder topicBuilder = new StringBuilder("/topic/fm");
        topicBuilder.append("/" + data.getCountryCode() + "/" + data.getPi() + "/"
                + data.getFrequency());
        topicBuilder.append("/");
        String broadCastUri = topicBuilder.toString();
        try {
            if (mFirstRequest) {
                finalReturnURI = "topic=" + URLEncoder.encode(broadCastUri + "image", "UTF-8")
                        + "&topic=" + URLEncoder.encode(broadCastUri + "text","UTF-8");
            } else {
                finalReturnURI = "topic=" + URLEncoder.encode(broadCastUri + mTopicType, "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            LogDns.e(TAG, "makeTopic failed");
            e.printStackTrace();
        }
        LogDns.v(TAG, finalReturnURI);
        return finalReturnURI;
    }

    public String makeRequestURL() {
        mDNSData = mDnsSystem.getCurrentData();
        StringBuilder url = new StringBuilder();
        url.append("http://" + mDNSData.getVisHttpHost() + ":" + mDNSData.getVisHttpPort());
        LogDns.v(TAG, mDNSData.getVisHttpHost() + ":" + mDNSData.getVisHttpPort());
        String topic = makeTopic();
        if (topic != null) {
            if ((mLastId != null) && !mFirstRequest) {
                url.append("/radiodns/vis/vis.json?" + makeTopic() + "&last_id=" + mLastId);
            } else {
                url.append("/radiodns/vis/vis.json?" + makeTopic());
            }
            return url.toString();
        } else {
            LogDns.e("TAG", "topic is null");
            return null;
        }
    }

    private class MakeConnection extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            // params comes from the execute() call: params[0] is the url.
            try {
                if (isCancelled() || urls[0] == null)
                    return HTTP_CONNECTION_ERROR;
                else
                    return getresponse(urls[0]);
            } catch (IOException e) {
                return HTTP_CONNECTION_ERROR;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            LogDns.v(TAG, result);
            Message msg = new Message();
            if ((result == "") || (result.compareToIgnoreCase(HTTP_CONNECTION_ERROR) == 0)) {
                msg.what = DNSEvent.DNS_VIS_HTTP_RECEIVE_DATA_FRAME_EMPTY;
                // msg.obj = TOPIC_ID;
            } else {
                msg.what = DNSEvent.DNS_VIS_HTTP_RECEIVE_DATA_FRAME;
                msg.obj = result;
            }
            // IS_CONNECTION_CLOSE = true;
            RadioVISHttpClient.getEventHandler().sendMessage(msg);
        }
    }

    private String getresponse(String myurl) throws IOException {
        String responseResult = "";

        try {
            LogDns.v(TAG, "URL:" + myurl);
            mSRVTopicUrl = new URL(myurl);
            if (mHttpURLConnection == null) {
                mHttpURLConnection = (HttpURLConnection) mSRVTopicUrl.openConnection();
                LogDns.v(TAG, "Open connection");
            }
            mHttpURLConnection.setReadTimeout(15000);
            mHttpURLConnection.setConnectTimeout(15000);
            mHttpURLConnection.setRequestMethod("GET");
            LogDns.v(TAG, "Connectiom started");
            mHttpURLConnection.connect();
            LogDns.v(TAG, "Connectiom stoped");
            int response = mHttpURLConnection.getResponseCode();
            LogDns.d(TAG, "The response is: " + response);
            switch (response) {
            case HttpURLConnection.HTTP_OK: // response 200
                LogDns.v(TAG, "HttpOK");
                responseResult = getStringDATA(new BufferedReader(new InputStreamReader(
                        mHttpURLConnection.getInputStream())));
                break;
            case HttpURLConnection.HTTP_NOT_FOUND:
            case HttpURLConnection.HTTP_UNAVAILABLE:
            case HttpURLConnection.HTTP_BAD_REQUEST:
            case HttpURLConnection.HTTP_BAD_GATEWAY:
            case HttpURLConnection.HTTP_NOT_ACCEPTABLE:// cases,request should
                                                       // be closed.
                LogDns.v(TAG, "Page Not found" + response);
                responseResult = HTTP_CONNECTION_ERROR;
                break;
            case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
            case HttpURLConnection.HTTP_RESET: // resend the request
                LogDns.v(TAG, "Resend the Request" + response);
                if (mCounter < 3) {
                    mHttpURLConnection.disconnect();
                    responseResult = getresponse(myurl);
                    mCounter++;
                } else {
                    responseResult = HTTP_CONNECTION_ERROR;
                    mCounter = 0;
                }
                break;
            case HttpURLConnection.HTTP_MOVED_PERM:
            case HttpURLConnection.HTTP_MOVED_TEMP:
                LogDns.v(TAG, "Resend the Request" + response);
                String newUrl = mHttpURLConnection.getHeaderField("Location");
                if (newUrl != null) {
                    mHttpURLConnection.disconnect();
                    responseResult = getresponse(newUrl);
                }
                break;
            default:
                break;
            }
        } finally {
            if (mHttpURLConnection != null) {
                mHttpURLConnection.disconnect();
                mHttpURLConnection = null;
            }

        }
        return responseResult;
    }

    public String getStringDATA(BufferedReader reader) {
        String line;
        StringBuilder builder = new StringBuilder();
        try {
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
