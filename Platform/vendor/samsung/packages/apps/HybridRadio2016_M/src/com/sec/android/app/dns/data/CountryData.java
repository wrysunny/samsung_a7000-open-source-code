package com.sec.android.app.dns.data;

import java.io.Serializable;

import com.sec.android.app.dns.LogDns;

public class CountryData implements Serializable {
    private static final String TAG = "CountryData";
    private static final long serialVersionUID = -2083164313061706794L;
    public String mBaseCountryCode = null;
    public String mGccCountryCode = null;
    public boolean mIsLookupWithIso = true;
    public String mIsoCountryCode = null;

    public CountryData() {
        LogDns.i(TAG, "CountryData()");

        mIsLookupWithIso = true;
        mIsoCountryCode = null;
        mGccCountryCode = null;
        mBaseCountryCode = null;
    }

    public boolean isSame(CountryData data) {
        if (((mIsoCountryCode != null) && (mIsoCountryCode.equalsIgnoreCase(data.mIsoCountryCode)))
                || (mGccCountryCode != null)
                && (mGccCountryCode.equalsIgnoreCase(data.mGccCountryCode)))
            return true;

        return false;
    }

    public boolean isSimilar(CountryData data) {
        if ((mIsoCountryCode != null) && (mIsoCountryCode.equalsIgnoreCase(data.mBaseCountryCode)))
            return true;

        return false;
    }

    public void resetCountryData() {
        mIsLookupWithIso = true;
        mIsoCountryCode = null;
        mGccCountryCode = null;
        mBaseCountryCode = null;
    }

    @Override
    public String toString() {
        return "CountryData [mIsLookupWithIso="
                + mIsLookupWithIso
                + LogDns.filter(", mIsoCountryCode=" + mIsoCountryCode + ", mGccCountryCode="
                        + mGccCountryCode + ", mBaseCountryCode=" + mBaseCountryCode) + "]";
    }

    public void copy(CountryData newData) {
        if (null == newData)
            return;
        mIsLookupWithIso = newData.mIsLookupWithIso;
        if (null != newData.mBaseCountryCode)
            mBaseCountryCode = new String(newData.mBaseCountryCode);
        if (null != newData.mGccCountryCode)
            mGccCountryCode = new String(newData.mGccCountryCode);
        if (null != newData.mIsoCountryCode)
            mIsoCountryCode = new String(newData.mIsoCountryCode);
    }
}
