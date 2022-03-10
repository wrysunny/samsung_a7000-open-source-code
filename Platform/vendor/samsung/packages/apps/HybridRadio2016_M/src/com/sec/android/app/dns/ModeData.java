package com.sec.android.app.dns;

public class ModeData {
    int mDnsMode;
    int mFreq;
    int mPi, mEcc;

    public ModeData(int dnsMode) {
        this.mDnsMode = dnsMode;
    }

    public int getEcc() {
        return mEcc;
    }

    public void setEcc(int ecc) {
        this.mEcc = ecc;
    }

    public int getDnsMode() {
        return mDnsMode;
    }

    public void setDnsMode(int dnsMode) {
        this.mDnsMode = dnsMode;
    }

    public int getFreq() {
        return mFreq;
    }

    public void setFreq(final int freq) {
        this.mFreq = freq;
    }

    public int getPi() {
        return mPi;
    }

    public void setPi(int pi) {
        this.mPi = pi;
    }
}
