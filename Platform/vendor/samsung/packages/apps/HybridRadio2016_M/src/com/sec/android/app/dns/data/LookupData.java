package com.sec.android.app.dns.data;

import java.io.Serializable;

import com.sec.android.app.dns.LogDns;

public class LookupData implements Serializable {
    private static final long serialVersionUID = 2863613012939132863L;
    private static final String TAG = "LookupData";
    public static final int EMPTY_NUMBER = -1;

    public String mCname = null;
    public long mCnameTTL = (long) EMPTY_NUMBER;

    public SrvRecord mEpgRecord = null;
    public SrvRecord mVisHttpRecord = null;
    public SrvRecord mVisStompRecord = null;

    public LookupData() {
        LogDns.i(TAG, "LookupData()");

        mVisStompRecord = new SrvRecord();
        mVisHttpRecord = new SrvRecord();
        mEpgRecord = new SrvRecord();
    }

    public void resetData() {
        mCname = null;
        mCnameTTL = EMPTY_NUMBER;
        mVisStompRecord.resetSrvRecord();
        mVisHttpRecord.resetSrvRecord();
        mEpgRecord.resetSrvRecord();
    }

    public void copy(InternalDnsData newData) {
        if (null == newData)
            return;
        String s = newData.getCname();
        if (null != s)
            mCname = new String(s);
        mCnameTTL = newData.getCnameTTL();
        mVisHttpRecord.copy(newData.getVisHttpRecord());
        mVisStompRecord.copy(newData.getVisStompRecord());
        mEpgRecord.copy(newData.getEpgRecord());
    }

    public boolean canSave() {
        if (/* (mCname == null) && */(mCnameTTL == EMPTY_NUMBER) && mVisStompRecord.isEmpty()
                && mVisHttpRecord.isEmpty() && mEpgRecord.isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "LookupData " + this.hashCode() + "[mCname=" + LogDns.filter(mCname)
                + ", mCnameTTL=" + mCnameTTL + ", mEpgRecord=" + mEpgRecord + ", mVisHttpRecord="
                + mVisHttpRecord + ", mVisStompRecord=" + mVisStompRecord + "]";
    }
}
