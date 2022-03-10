package com.sec.android.app.dns.radioepg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.sec.android.app.dns.DnsCacheManager;
import com.sec.android.app.dns.DnsInternalDataManager;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.RadioDNSUtil;
import com.sec.android.app.dns.data.DnsCache;
import com.sec.android.app.dns.data.InternalDnsData;
import com.sec.android.app.dns.radioepg.XsiDataHandler.XsiSAXTerminalException;

public class EpgManagerHelper {
    public interface OnPreparedListener {
        void onPiPrepared(final EpgData data);

        void onXsiPrepared(final EpgData data);
    }

    private class PiParser extends EpgParser {
        private static final String TAG = "PiParser";

        @Override
        protected EpgData getData(final String freq) {
            return freq != null ? mHashEpgData.get(freq) : null;
        }

        @Override
        protected void notify(EpgData data) {
            if (sListener != null) {
                sListener.onPiPrepared(data);
            }
        }

        @Override
        protected void parse(EpgData data) {
            if (data == null) {
                LogDns.e(TAG, "parse() - data is null!!");
                return;
            }
            InputStream iStream = null;
            LogDns.d(TAG, "parse() - piUrl:" + LogDns.filter(data.getPiUrl()));
            try {
                SAXParserFactory saxPF = SAXParserFactory.newInstance();
                SAXParser saxP = saxPF.newSAXParser();
                XMLReader xmlR = saxP.getXMLReader();
                URL url = new URL(data.getPiUrl());
                iStream = url.openStream();

                // *** To download file for test ***********
                if (LogDns.DEBUGGABLE) {
                    saveServerFile(url, data, TAG);
                }

                PiDataHandler piDataHandler = new PiDataHandler();
                xmlR.setContentHandler(piDataHandler);
                xmlR.parse(new InputSource(iStream));
                data.setPiData(piDataHandler.getData());
            } catch (ParserConfigurationException e) {
                LogDns.e(TAG, e);
            } catch (SAXException e) {
                LogDns.e(TAG, e);
            } catch (MalformedURLException e) {
                LogDns.e(TAG, e);
            } catch (IOException e) {
                LogDns.e(TAG, e);
            } finally {
                notify(data);
                if (iStream != null) {
                    try {
                        iStream.close();
                    } catch (IOException e) {
                        LogDns.e(TAG, e);
                    }
                }
                LogDns.d(TAG, "parse() - finished");
            }
        }

        @Override
        protected boolean ready(final EpgData data) {
            if (data == null) {
                LogDns.e(TAG, "parse() - data is null!!");
                return false;
            }
            if (data.getPiData() != null) {
                LogDns.d(TAG, "isPrepared() - piData is already prepared.");
                notify(data);
                return true;
            }
            return false;
        }
    }

    private class XsiParser extends EpgParser {
        private static final String TAG = "XsiParser";

        @Override
        protected EpgData getData(final String freq) {
            return freq != null ? mHashEpgData.get(freq) : null;
        }

        @Override
        protected void notify(EpgData data) {
            if (sListener != null) {
                sListener.onXsiPrepared(data);
            }
        }

        @Override
        protected void parse(EpgData data) {
            if (data == null) {
                LogDns.e(TAG, "parse() - data is null!!");
                return;
            }
            LogDns.d(TAG, "parse() - xsiUrl:" + LogDns.filter(data.getXsiUrl()));
            InputStream iStream = null;
            DnsCache cache = null;
            String pid = data.getProgramId();
            String freq = data.getFrequency();
            DnsCacheManager cacheMgr = DnsCacheManager.getInstance();

            cache = cacheMgr.getCache(pid, freq);
            if (cache != null) {
                LogDns.d(TAG, "Xsi data is already parsed.");
                data.setXsiService(cache.getXsiData().getService(pid, freq));
                notify(data);
            }
            try {
                SAXParserFactory saxPF = SAXParserFactory.newInstance();
                SAXParser saxP = saxPF.newSAXParser();
                XMLReader xmlR = saxP.getXMLReader();
                URL url = new URL(data.getXsiUrl());
                iStream = url.openStream();

                // *** To download file for test ***********
                if (LogDns.DEBUGGABLE) {
                    saveServerFile(url, data, TAG);
                }

                XsiDataHandler xsiDataHandler = new XsiDataHandler();
                xmlR.setContentHandler(xsiDataHandler);
                if (cache != null) {
                    xsiDataHandler.setUpdateTime(cache.getXsiData().getCreationTime());
                }
                xmlR.parse(new InputSource(iStream));

                // LogDns.i(TAG, "New xsi data!!!");

                XsiData xsiData = xsiDataHandler.getData();
                if (xsiData != null) {
                    data.setXsiService(xsiData.getService(pid, freq));
                    InternalDnsData dnsData = DnsInternalDataManager.getInstance()
                            .getCnameInternalData(
                                    new InternalDnsData(freq, pid, data.getCountryCode()));

                    LogDns.i(TAG, "New xsi data!!!" + dnsData);

                    if (dnsData != null) {
                        cacheMgr.addCache(new DnsCache(dnsData, xsiData));
                    }
                }
            } catch (ParserConfigurationException e) {
                LogDns.e(TAG, e);
            } catch (XsiSAXTerminalException e) {
                /** Xsi document is not updated. It's a normal case */
            } catch (SAXException e) {
                LogDns.e(TAG, e);
            } catch (MalformedURLException e) {
                LogDns.e(TAG, e);
            } catch (IOException e) {
                LogDns.e(TAG, e);
            } finally {
                if (cache == null) {
                    notify(data);
                }
                if (iStream != null) {
                    try {
                        iStream.close();
                    } catch (IOException e) {
                        LogDns.e(TAG, e);
                    }
                }
                LogDns.d(TAG, "parse() - finished");
            }
        }

        @Override
        protected boolean ready(final EpgData data) {
            if (data == null) {
                LogDns.e(TAG, "parse() - data is null!!");
                return false;
            }
            if (data.getXsiService() != null) {
                LogDns.d(TAG, "prepare() - xsiData is already prepared.");
                notify(data);
                return true;
            }
            return false;
        }
    }

    public static final boolean EPG_TEST = false;
    private static String FILE_PATH_LOG_XML = "storage/sdcard0/log/";
    private static int mFileIndex = 0;
    private static OnPreparedListener sListener = null;
    private static final String TAG = "EPGManageHelper";
    private HashMap<String, EpgData> mHashEpgData = null;
    private PiParser mPiParser = null;
    private XsiParser mXsiParser = null;

    public EpgManagerHelper() {
        mHashEpgData = new HashMap<String, EpgData>();
        mPiParser = new PiParser();
        mXsiParser = new XsiParser();
        if (EPG_TEST) {
            mHashEpgData.put("09240", new EpgData("09240", "d301", "de", "epg4swr.irt.de"));
            mHashEpgData.put("09310", new EpgData("09310", "38cf", "us", "epg.ebulabs.org"));
            mHashEpgData.put("09320", new EpgData("09320", "6354", "be", "epg.ebulabs.org"));
            mHashEpgData.put("09580", new EpgData("09580", "c479", "gb", "epg.musicradio.com"));
            mHashEpgData.put("09630", new EpgData("09630", "c36b", "gb", "epg.musicradio.com"));
            mHashEpgData.put("09650", new EpgData("09650", "8203", "nl", "epg.ebulabs.org"));
            mHashEpgData.put("09730", new EpgData("09730", "c478", "gb", "epg.musicradio.com"));
            mHashEpgData.put("10160", new EpgData("10160", "6354", "gb", "epg.musicradio.com"));
            mHashEpgData.put("10320", new EpgData("10320", "c57a", "gb", "epg.musicradio.com"));
            mHashEpgData.put("10490", new EpgData("10490", "c474", "gb", "epg.musicradio.com"));
            mHashEpgData.put("10610", new EpgData("10610", "c372", "gb", "epg.musicradio.com"));
            mHashEpgData.put("10620", new EpgData("10620", "c460", "gb", "epg.musicradio.com"));
        }
    }

    public EpgData getEpgData(String freq) {
        return freq != null ? mHashEpgData.get(freq) : null;
    }

    public boolean isStreamAvailable(String freq) {
        boolean ret = false;
        if (freq != null) {
            EpgData data = mHashEpgData.get(freq);
            ret = ((data != null) && (data.getXsiService() != null) && (data.getStreamUrl() != null)) ? true
                    : false;
        }
        LogDns.d(TAG, "isStreamAvailable() - valid:" + ret);
        return ret;
    }

    public void run(final String freq) {
        if (freq != null && mHashEpgData.containsKey(freq)) {
            mFileIndex++;
            mXsiParser.run(freq);
            mPiParser.run(freq);
        }
    }

    private void saveServerFile(final URL url, final EpgData data, final String className) {
        byte[] buffer = new byte[1024 * 8];
        String[] a = null;
        if (url == null || data == null || className == null) {
            LogDns.e(TAG, "saveServerFile() - url:" + url + " data:" + data + " className:"
                    + className);
            return;
        }
        if (className.equals(PiParser.TAG)) {
            a = data.getPiUrl().split("/");
        } else if (className.equals(XsiParser.TAG)) {
            a = data.getXsiUrl().split("/");
        } else {
            LogDns.e(TAG, "saveServerFile() - className is wrong!! " + className);
            return;
        }
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(url.openStream());
            bos = new BufferedOutputStream(new FileOutputStream(FILE_PATH_LOG_XML + mFileIndex
                    + "_" + data.getCountryCode() + "_" + data.getProgramId() + "_"
                    + data.getFrequency() + "_" + a[a.length - 1]));
            for (int c = 0; (c = bis.read(buffer, 0, 1024 * 8)) != -1;) {
                bos.write(buffer, 0, c);
            }
        } catch (IOException e) {
            LogDns.e(TAG, e);
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } catch (IOException e) {
                LogDns.e(TAG, e);
            } finally {
                try {
                    if (bos != null) {
                        bos.flush();
                    }
                } catch (IOException e) {
                    LogDns.e(TAG, e);
                } finally {
                    try {
                        if (bos != null) {
                            bos.close();
                        }
                    } catch (IOException e) {
                        LogDns.e(TAG, e);
                    }
                }
            }
        }
    }

    public void setOnPreparedListener(final OnPreparedListener _listener) {
        sListener = _listener;
    }

    public void update(final String freq, final String pid, final String countryCode,
            final String url) {
        LogDns.d(TAG, "update() - freq:" + LogDns.filter(freq) + " pid:" + pid + " cc:"
                + countryCode + " url:" + url);
        if ((freq == null) || (pid == null) || (countryCode == null) || (url == null))
            return;
        EpgData data = mHashEpgData.get(freq);
        if (data == null || !data.getProgramId().equalsIgnoreCase(pid)) {
            data = new EpgData(freq, pid, countryCode, url);
            mHashEpgData.put(freq, data);
        }
        run(freq);
    }

    public boolean updatePi(final String freq) {
        boolean ret = false;
        mFileIndex++;
        EpgData data = getEpgData(freq);
        if (data != null) {
            ret = true;
            if (!RadioDNSUtil.sameDate(data.getDate(), Calendar.getInstance().getTime())) {
                data.initPiData();
            }
            mPiParser.run(freq);
        }
        return ret;
    }

    public boolean updatePi(final String freq, final String pid, final Date date) {
        boolean ret = false;
        mFileIndex++;
        EpgData data = getEpgData(freq);
        if (data != null && data.getProgramId().equals(pid)) {
            ret = true;
            if (!RadioDNSUtil.sameDate(data.getDate(), date)) {
                data.initPiData();
                data.setDate(date);
            }
            mPiParser.run(freq);
        }
        return ret;
    }
}
