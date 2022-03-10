package com.sec.android.app.dns.data;

import com.sec.android.app.dns.LogDns;

/**
 * @author hs77.jung
 */
public class RadioDNSData extends InternalDnsData {
    private static final long serialVersionUID = -3303761228138908426L;
    private static final String TAG = "RadioDNSData";
    public static final int MAX_LOOKUP_RETRY = 5;

    // public static final char RETRY_CNAME = 1;
    // public static final char RETRY_VIS = 2;
    // public static final char RETRY_EPG = 4;

    private boolean mNeedLookupRetry, mNeedCcRetryNetwork;
    private boolean mNeedCcCorrection;
    private int mEcc;
    private long mTimestamp = EMPTY_NUMBER;
    private int mRetryCount;

    public RadioDNSData() {
        super();
        LogDns.v(TAG, "RadioDNSData()");
        resetData();
    }

    public RadioDNSData(String freq, String pi, String countryCode, long timestamp) {
        super(freq, pi, countryCode);
        resetData();
        mTimestamp = timestamp;
    }

    private void resetData() {
        mNeedLookupRetry = false;
        mNeedCcRetryNetwork = false;
        mNeedCcCorrection = false;
        mTimestamp = EMPTY_NUMBER;
        mEcc = 0;
        mRetryCount = 0;
    }

    public void resetAllData() {
        super.resetAllData();

        resetData();
    }

    private boolean isRegionalChannel(String pi) {
        // only 2nd part of pi is changed
        if (mPi.substring(0, 1).equalsIgnoreCase(pi.substring(0, 1))
                && mPi.substring(2).equalsIgnoreCase(pi.substring(2))) {
            return true;
        }

        // XSI.xml could be used to verify
        return false;
    }

    public boolean setPiAfterCheck(String pi) {
        if (((pi != null) && (pi.matches("(?i)^[0-9A-F]{4}$")))) {
            if ((mPi == null) || (mPi != null) && !(mPi.equalsIgnoreCase(pi))) {
                if (!(isVisAvailable() || isEpgAvailable())
                        || ((mPi != null) && isRegionalChannel(pi))) {
                    this.mPi = pi;
                    return true;
                }
            }
        }
        return false;
    }

    public void resetRetryCount() {
        LogDns.v(TAG, "resetRetryCount()");
        mRetryCount = 0;
    }

    public void increaseRetryCount() {
        ++mRetryCount;
    }

    public boolean isNeedLookupRetry() {
        LogDns.v(TAG, "isNeedLookupRetry() - " + mNeedLookupRetry + " ,RetryCount : " + mRetryCount);
        if (mNeedLookupRetry && (mRetryCount < MAX_LOOKUP_RETRY))
            return true;
        return false;
    }

    public boolean isNeedCcRetry() {
        return mNeedCcRetryNetwork;
    }

    public void setNeedLookupRetry(boolean needLookupRetry) {
        LogDns.v(TAG, "setNeedLookupRetry : " + needLookupRetry);
        mNeedLookupRetry = needLookupRetry;
    }

    public void setNeedCcRetryNetwork(boolean needCcRetryNetwork) {
        // LogDns.v("RadioDNSData", "setneedRetry");
        mNeedCcRetryNetwork = needCcRetryNetwork;
    }

    public int getEcc() {
        return mEcc;
    }

    public boolean setEcc(int ecc) {
        if ((ecc != 0) && ((mEcc == 0) || !(isVisAvailable() || isEpgAvailable()))) {
            this.mEcc = ecc;
            return true;
        }
        return false;
    }

    public boolean isVisAvailable() {
        if ((mLookupData.mVisHttpRecord.host != null) || (mLookupData.mVisStompRecord.host != null))
            return true;
        return false;
    }

    public boolean isEpgAvailable() {
        if (mLookupData.mEpgRecord.host != null)
            return true;
        return false;
    }

    public boolean isNeedCcCorrection() {
        return mNeedCcCorrection;
    }

    public void setNeedCcCorrection(boolean isNeedCcCorrection) {
        mNeedCcCorrection = isNeedCcCorrection;
    }

    public boolean isSame(RadioDNSData data) {
        if (data == null)
            return false;

        if (mFrequency.equals(data.getFrequency()) && mPi.equalsIgnoreCase(data.getPi()))
            return true;
        return false;
    }

    public boolean isSameFreq(final int freq) {
        String curFreq = String.format("%05d", freq);
        if (curFreq.equals(mFrequency))
            return true;
        return false;
    }

    public void setTimestamp() {
        mTimestamp = (System.currentTimeMillis()) / 1000;
        LogDns.v(TAG, "setTimestamp() : " + mTimestamp);
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public boolean canReplaceWith(RadioDNSData data, boolean isResult) {
        if (data == null) {
            LogDns.e(TAG, "canReplaceWith() - data is null");
            return false;
        }

        if (isResult && !(isVisAvailable() || isEpgAvailable())) {
            LogDns.e(TAG, "VIS or EPG is not available");
            return false;
        }

        if (mTimestamp == EMPTY_NUMBER) {
            LogDns.e(TAG, "canReplaceWith() - timestamp is EMPTY_NUMBER!!!");
            return false;
        }

        try {
            if (mFrequency.equals(data.getFrequency()) && (mTimestamp >= data.getTimestamp())
            /* && !(data.isVisAvailable() && data.isEpgAvailable()) */) {
                return true;
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        LogDns.e(
                TAG,
                "canReplaceWith() - frequency or timestamp is not ok Freq :"
                        + LogDns.filter(mFrequency) + " timestamp :" + mTimestamp + " data :"
                        + data);
        return false;
    }

    @Override
    public String toString() {
        return "RadioDNSData [mNeedLookupRetry=" + mNeedLookupRetry + ", mNeedCcRetryNetwork="
                + mNeedCcRetryNetwork + ", mNeedCcCorrection=" + mNeedCcCorrection + ", mEcc="
                + mEcc + ", mTimestamp=" + mTimestamp + ", mRetryCount=" + mRetryCount
                + ", mFrequency=" + LogDns.filter(mFrequency) + ", mPi=" + mPi + ", mNeedUpdate="
                + mNeedUpdate + ", mCountryData=" + mCountryData + ", mLookupData=" + mLookupData
                + "]";
    }

    /*
     * public String getString() { StringBuffer str = new
     * StringBuffer(mFrequency); str.append("|"); str.append(mPi);
     * str.append("|"); str.append(mIsLookupWithIso); str.append("|");
     * str.append(mIsoCountryCode); str.append("|");
     * str.append(mGccCountryCode); str.append("|"); str.append(mCname);
     * str.append("|"); str.append(mCnameTTL); str.append("|");
     * str.append(mVisStompHost); str.append("|"); str.append(mVisStompPort);
     * str.append("|"); str.append(mVisStompTTL); str.append("|");
     * str.append(mEpgHost); str.append("|"); str.append(mEpgPort);
     * str.append("|"); str.append(mEpgTTL); return str.toString(); }
     * 
     * public void setDnsData(String allData) { String[] tokens =
     * allData.split("\\|", 13); mFrequency = tokens[0]; mPi = tokens[1];
     * mIsLookupWithIso = Boolean.parseBoolean(tokens[2]); mIsoCountryCode =
     * tokens[3]; mGccCountryCode = tokens[4]; mCname = tokens[5]; mCnameTTL =
     * Long.parseLong(tokens[6]); mVisStompHost = tokens[7]; mVisStompPort =
     * Integer.parseInt(tokens[8]); mVisStompTTL = Long.parseLong(tokens[9]);
     * mEpgHost = tokens[10]; mEpgPort = Integer.parseInt(tokens[11]); mEpgTTL =
     * Long.parseLong(tokens[12]); }
     * 
     * public long getShortestTTL() { long shortestTTL = mCnameTTL; mExpiredBit
     * = 0; if (!(shortestTTL < mEpgTTL)) { shortestTTL = mEpgTTL; mExpiredBit =
     * (char) (mExpiredBit | RETRY_EPG); } if (!(shortestTTL < mVisStompTTL)) {
     * shortestTTL = mVisStompTTL; mExpiredBit = (char) (mExpiredBit |
     * RETRY_VIS); } if (shortestTTL == mCnameTTL) mExpiredBit = (char)
     * (mExpiredBit | RETRY_CNAME); return shortestTTL; }
     */

}
