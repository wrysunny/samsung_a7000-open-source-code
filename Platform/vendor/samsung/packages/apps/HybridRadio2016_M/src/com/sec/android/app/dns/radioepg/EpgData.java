package com.sec.android.app.dns.radioepg;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.RadioDNSUtil;
import com.sec.android.app.dns.radioepg.XsiData.Service;

public class EpgData implements Serializable {
    private static final long serialVersionUID = -2939147088728611714L;
    private static final String TAG = "EpgData";
    private String mCountryCode;
    private Date mDate;
    private String mEpgUrl;
    private String mFreq;
    private String mPid;
    private PiData mPiData;
    private String mPiUrl;
    private Service mXsiService;
    private String mXsiUrl;

    public EpgData(final String freq, final String pid, final String countryCode, final String url) {
        mFreq = freq;
        mEpgUrl = url + ":80";
        mCountryCode = countryCode;
        mPid = pid;
        initXsiData();
        initPiData();
    }

    public String getCountryCode() {
        return mCountryCode;
    }

    public Date getDate() {
        return mDate;
    }

    public String getEpgUrl() {
        return mEpgUrl;
    }

    public String getFrequency() {
        return mFreq;
    }

    public PiData getPiData() {
        return mPiData;
    }

    public String getPiUrl() {
        return mPiUrl;
    }

    public String getProgramId() {
        return mPid;
    }

    public String getStreamUrl() {
        return (mXsiService != null) ? mXsiService.getStreamUrl() : null;
    }

    public Service getXsiService() {
        return mXsiService;
    }

    public String getXsiUrl() {
        return mXsiUrl;
    }

    public void initPiData() {
        mPiData = null;
        mDate = Calendar.getInstance().getTime();
        mPiUrl = new StringBuilder("http://").append(mEpgUrl).append("/radiodns/epg/fm/")
                .append(mCountryCode).append("/").append(mPid).append("/").append(mFreq)
                .append("/").append(RadioDNSUtil.dateToString(mDate)).append("_PI.xml").toString();
        LogDns.d(TAG, "initPiData() - " + LogDns.filter(mPiUrl));
    }

    public void initXsiData() {
        mXsiService = null;
        mXsiUrl = new StringBuilder("http://").append(mEpgUrl).append("/radiodns/epg/XSI.xml")
                .toString();
        LogDns.d(TAG, "initXsiData() - " + LogDns.filter(mXsiUrl));
    }

    public void setDate(final Date date) {
        mDate = date;
    }

    public void setPiData(final PiData data) {
        mPiData = data;
    }

    public void setXsiService(final Service data) {
        mXsiService = data;
    }
}
