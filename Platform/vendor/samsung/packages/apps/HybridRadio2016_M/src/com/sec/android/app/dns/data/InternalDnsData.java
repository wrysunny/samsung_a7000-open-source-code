package com.sec.android.app.dns.data;

import java.io.Serializable;

import com.sec.android.app.dns.LogDns;

public class InternalDnsData implements Serializable, Cloneable {
    public static final int EMPTY_NUMBER = -1;

    private static final long serialVersionUID = -7590228847001370445L;
    private static final String TAG = "InternalDnsData";

    protected String mFrequency = null;
    protected String mPi = null;
    protected boolean mNeedUpdate = false;

    protected CountryData mCountryData = null;
    protected LookupData mLookupData = null;

    public InternalDnsData() {
        initialize();
    }

    public InternalDnsData(String freq, String pi, String countryCode) {
        initialize();

        mFrequency = freq;
        mPi = pi;

        if (countryCode != null) {
            if (countryCode.length() == 2)
                mCountryData.mIsoCountryCode = countryCode;
            else
                mCountryData.mGccCountryCode = countryCode;
        }
    }

    public void initialize() {
        mCountryData = new CountryData();
        mLookupData = new LookupData();
    }

    public Object clone() {
        InternalDnsData c = null;
        LogDns.v(TAG, "clone()");
        try {
            c = (InternalDnsData) super.clone();
            if (null != mFrequency)
                c.mFrequency = new String(mFrequency);
            if (null != mPi)
                c.mPi = new String(mPi);
            c.initialize();
            c.mCountryData.copy(getCountryData());
            c.mLookupData.copy(this);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return c;
    }

    public String getBaseCountryCode() {
        return mCountryData.mBaseCountryCode;
    }

    public String getCname() {
        return mLookupData.mCname;
    }

    public long getCnameTTL() {
        return mLookupData.mCnameTTL;
    }

    public String getCountryCode() {
        if (mCountryData.mIsLookupWithIso)
            return mCountryData.mIsoCountryCode;
        else
            return mCountryData.mGccCountryCode;
    }

    public CountryData getCountryData() {
        return mCountryData;
    }

    public String getEpgHost() {
        return mLookupData.mEpgRecord.host;
    }

    public int getEpgPort() {
        return mLookupData.mEpgRecord.port;
    }

    public long getEpgTTL() {
        return mLookupData.mEpgRecord.ttl;
    }

    public String getFrequency() {
        return mFrequency;
    }

    public String getGccCountryCode() {
        return mCountryData.mGccCountryCode;
    }

    public String getIsoCountryCode() {
        return mCountryData.mIsoCountryCode;
    }

    public String getPi() {
        return mPi;
    }

    public String getVisHttpHost() {
        return mLookupData.mVisHttpRecord.host;
    }

    public int getVisHttpPort() {
        return mLookupData.mVisHttpRecord.port;
    }

    public long getVisHttpTTL() {
        return mLookupData.mVisHttpRecord.ttl;
    }

    public String getVisStompHost() {
        return mLookupData.mVisStompRecord.host;
    }

    public int getVisStompPort() {
        return mLookupData.mVisStompRecord.port;
    }

    public long getVisStompTTL() {
        return mLookupData.mVisStompRecord.ttl;
    }

    public boolean isLookupWithIso() {
        return mCountryData.mIsLookupWithIso;
    }

    public void resetAllData() {
        mFrequency = null;
        mPi = null;
        mNeedUpdate = false;

        initialize();
        // mCountryData.resetCountryData();
        // mLookupData.resetData();
    }

    public void resetCnameData() {
        // mCname = null;
        mLookupData.mCnameTTL = EMPTY_NUMBER;
    }

    public void resetEpgData() {
        mLookupData.mEpgRecord.resetSrvRecord();
    }

    public void resetVisHttpData() {
        mLookupData.mVisHttpRecord.resetSrvRecord();
    }

    public void resetVisStompData() {
        mLookupData.mVisStompRecord.resetSrvRecord();
    }

    public void setBaseCountryCode(String baseCountryCode) {
        mCountryData.mBaseCountryCode = baseCountryCode;
    }

    public void setCname(String cname) {
        mLookupData.mCname = cname;
    }

    public void setCnameTTL(long cnameTTL) {
        mLookupData.mCnameTTL = cnameTTL;
    }

    public void setCountryCode(String countryCode) {
        if (countryCode.length() == 2) {
            mCountryData.mIsoCountryCode = countryCode;
            mCountryData.mIsLookupWithIso = true;
        } else {
            mCountryData.mGccCountryCode = countryCode;
            mCountryData.mIsLookupWithIso = false;
        }
    }

    public void setCountryData(CountryData countryData) {
        mCountryData = countryData;
    }

    public void setFrequency(String frequency) {
        mFrequency = frequency;
    }

    public void setGccCountryCode(String mGccCountryCode) {
        mCountryData.mGccCountryCode = mGccCountryCode;
    }

    public void setIsoCountryCode(String mIsoCountryCode) {
        mCountryData.mIsoCountryCode = mIsoCountryCode;
    }

    public void setLookupWithIso(boolean mIsLookupWithIso) {
        mCountryData.mIsLookupWithIso = mIsLookupWithIso;
    }

    public boolean setPi(String pi) {
        if (((pi != null) && (pi.matches("(?i)^[0-9A-F]{4}$")))) {
            mPi = pi;
            return true;
        }
        LogDns.e(TAG, "PI error in InternalDnsData");
        return false;
    }

    public void setVisStompRecord(SrvRecord visStompRecord) {
        mLookupData.mVisStompRecord = visStompRecord;
    }

    public void setVisHttpRecord(SrvRecord visHttpRecord) {
        mLookupData.mVisHttpRecord = visHttpRecord;
    }

    public void setEpgRecord(SrvRecord epgRecord) {
        mLookupData.mEpgRecord = epgRecord;
    }

    public SrvRecord getEpgRecord() {
        return mLookupData.mEpgRecord;
    }

    public SrvRecord getVisHttpRecord() {
        return mLookupData.mVisHttpRecord;
    }

    public SrvRecord getVisStompRecord() {
        return mLookupData.mVisStompRecord;
    }

    public LookupData getLookupData() {
        return mLookupData;
    }

    public void setLookupData(LookupData lookupData) {
        mLookupData = lookupData;
    }

    public boolean needUpdate() {
        return mNeedUpdate;
    }

    public void setNeedUpdate(boolean needUpdate) {
        LogDns.v(TAG, "setNeedUpdate() - " + needUpdate);
        mNeedUpdate = needUpdate;
    }

    public void copy(InternalDnsData newData) {
        mCountryData.copy(newData.getCountryData());
        mLookupData.copy(newData);
    }

    public boolean isSameHead(InternalDnsData compData, boolean isSimilar) {
        if (null == compData || null == mFrequency || null == mPi)
            return false;
        if (mFrequency.equals(compData.getFrequency()) && mPi.equalsIgnoreCase(compData.getPi()))
            if (mCountryData.isSame(compData.getCountryData())
                    || (isSimilar && mCountryData.isSimilar(compData.getCountryData()))) {
                return true;
            }
        return false;
    }

    @Override
    public String toString() {
        return "InternalDnsData [mFrequency=" + LogDns.filter(mFrequency) + ", mPi=" + mPi
                + ", mNeedUpdate=" + mNeedUpdate + ", mCountryData=" + mCountryData.hashCode()
                + " : " + LogDns.filter(mCountryData) + ", mLookupData=" + mLookupData.hashCode()
                + " : " + mLookupData + "]";
    }
}
