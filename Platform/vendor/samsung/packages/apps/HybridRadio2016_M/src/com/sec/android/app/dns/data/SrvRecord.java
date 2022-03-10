package com.sec.android.app.dns.data;

import java.io.Serializable;

import com.sec.android.app.dns.LogDns;

public class SrvRecord implements Serializable {
    private static final String TAG = "SrvRecord";
    public static final int EMPTY_NUMBER = -1;
    private static final long serialVersionUID = -4691425033520913616L;

    String host = null;
    int port = EMPTY_NUMBER;
    long ttl = (long) EMPTY_NUMBER;

    public SrvRecord() {
        host = null;
        port = EMPTY_NUMBER;
        ttl = (long) EMPTY_NUMBER;
    }

    public SrvRecord(String host, int port, long ttl) {
        this.host = host;
        this.port = port;
        this.ttl = ttl;
    }

    public boolean isEmpty() {
        if ((host == null) && (port == EMPTY_NUMBER) && (ttl == EMPTY_NUMBER)) {
            return true;
        }
        return false;
    }

    public void resetSrvRecord() {
        host = null;
        port = EMPTY_NUMBER;
        ttl = (long) EMPTY_NUMBER;
    }

    @Override
    public String toString() {
        return "SrvRecord " + this.hashCode() + "[host=" + LogDns.filter(host) + ", port=" + port
                + ", ttl=" + ttl + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            LogDns.v(TAG, "o is null");
            return false;
        }

        if (!(o instanceof SrvRecord)) {
            LogDns.v(TAG, "o is not instance of SrvRecord");
            return false;
        }

        SrvRecord comp = (SrvRecord) o;

        if ((((host == null) && (comp.host == null)) || ((host != null) && (host
                .equalsIgnoreCase(comp.host)))) && (port == comp.port)) {
            LogDns.v(TAG, "SrvRecord are equal");
            return true;
        }
        LogDns.v(TAG, "SrvRecord are not equal " + this + " " + comp);

        return false;
    }

    public void copy(SrvRecord newData) {
        if (null == newData)
            return;
        if (null != newData.host)
            host = new String(newData.host);
        port = newData.port;
        ttl = newData.ttl;
    }
}