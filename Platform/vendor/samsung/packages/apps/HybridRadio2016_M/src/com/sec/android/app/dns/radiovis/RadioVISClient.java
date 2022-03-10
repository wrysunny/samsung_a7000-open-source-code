package com.sec.android.app.dns.radiovis;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.SparseArray;

import com.sec.android.app.dns.DNSEvent;
import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.radiovis.http.RadioVISHttpClient;
import com.sec.android.app.dns.radiovis.stomp.RadioVISStompClient;

public abstract class RadioVISClient {
    private class ImageDownloader extends AsyncTask<String, Void, Bitmap> {
        private int mType;
        private String mUrl;

        public ImageDownloader(int clientType) {
            mType = clientType;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            LogDns.v(TAG, "ImageDownloader - start");
            Bitmap image = null;
            BufferedInputStream input = null;
            if (urls[0] != null && !urls[0].isEmpty()) {
                try {
                    mUrl = urls[0];
                    URL url = new URL(urls[0]);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setInstanceFollowRedirects(true);
                    conn.connect();
                    int size = conn.getContentLength();
                    input = new BufferedInputStream(conn.getInputStream(), size);
                    image = BitmapFactory.decodeStream(input);
                } catch (MalformedURLException e) {
                    // new URL(urls[0]);
                    LogDns.e(TAG, e);
                } catch (IOException e) {
                    // url.openConnection()
                    // conn.connect()
                    // conn.getInputStream()
                    LogDns.e(TAG, e);
                } finally {
                    LogDns.v(TAG, "ImageDownloader - finish");
                    try {
                        if (input != null) {
                            input.close();
                        }
                    } catch (IOException e) {
                        LogDns.e(TAG, e);
                    }
                }
            }
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if ((null == mUrl) || mUrl.isEmpty() || (null == result))
                return;

            LogDns.d(TAG, "onPostExecute() - url:" + mUrl + " image:" + result);
            sImageCache.put(mUrl, result);

            String url = null;
            RadioVISFrame frame = RadioVISFrame.obtain();
            if (null == (url = frame.getImageUrl()))
                return;

            if (mUrl.equals(url)) {
                frame.setImage(result);
                Message msg = Message.obtain();
                msg.what = DNSEvent.DNS_VIS_UPDATE_SHOW;
                if (mType == TYPE_HTTP)
                    RadioVISHttpClient.getEventHandler().sendMessage(msg);
                else
                    DNSService.getEventHandler().sendMessage(msg);
            }
        }
    }

    protected void downloadImage(int clientType, String url) {
        AsyncTask<String, Void, Bitmap> imageDownloader = new ImageDownloader(clientType);
        imageDownloader.execute(url);
    }

    protected static final int IMAGE_CACHE_SIZE = 10;
    private static SparseArray<RadioVISClient> sClients = new SparseArray<RadioVISClient>(2);
    private static LruCache<String, Bitmap> sImageCache = new LruCache<String, Bitmap>(
            IMAGE_CACHE_SIZE);

    protected static Bitmap getImageFromCache(String key) {
        return sImageCache.get(key);
    }

    private static final String TAG = "RadioVisClient";
    protected static int trailCount;
    public static final int TYPE_HTTP = 1;
    public static final int TYPE_STOMP = 2;

    public static final RadioVISClient newClient(int type, DNSService service) {
        trailCount = 0;
        RadioVISClient client = sClients.get(type);
        if (client == null) {
            switch (type) {
            case TYPE_HTTP:
                client = new RadioVISHttpClient(service);
                break;
            case TYPE_STOMP:
                client = new RadioVISStompClient(service);
                break;
            default:
                return null;
            }
            sClients.append(type, client);
        }
        return client;
    }

    public abstract void connect();

    public abstract void disconnect();
}
