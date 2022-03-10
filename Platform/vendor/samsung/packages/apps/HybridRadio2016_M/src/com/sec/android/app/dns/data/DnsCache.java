package com.sec.android.app.dns.data;

import java.io.Serializable;

import com.sec.android.app.dns.radioepg.XsiData;

public class DnsCache implements Serializable {
    private static final long serialVersionUID = -6828738685385330678L;
    private InternalDnsData mDnsData = null;
    private XsiData mXsiData = null;

    public DnsCache(InternalDnsData dnsData, XsiData xsiData) {
        mDnsData = dnsData;
        mXsiData = xsiData;
    }

    public InternalDnsData getDnsData() {
        return mDnsData;
    }

    public XsiData getXsiData() {
        return mXsiData;
    }
}
