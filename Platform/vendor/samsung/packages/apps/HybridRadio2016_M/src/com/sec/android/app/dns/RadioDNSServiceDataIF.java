package com.sec.android.app.dns;


import com.sec.android.app.dns.radioepg.EpgData;
import com.sec.android.app.dns.radioepg.EpgManager;
import com.sec.android.app.dns.radioepg.EpgPlayer.OnBufferingUpdateListener;

public class RadioDNSServiceDataIF {
    public static long getEpgBufferingTime() {
        EpgManager epgMgr = EpgManager.getInstance(null);
        return (epgMgr != null) ? epgMgr.getBufferingTime() : 0;
    }

    public static EpgData getEpgNowEpgData() {
        EpgManager epgMgr = EpgManager.getInstance(null);
        return epgMgr != null ? epgMgr.getNowEpgData() : null;
    }

    public static int getEpgPollingCount() {
        EpgManager epgMgr = EpgManager.getInstance(null);
        return (epgMgr != null) ? epgMgr.getPollingCount() : 0;
    }

    public static int getEpgPollingDelayNormal() {
        EpgManager epgMgr = EpgManager.getInstance(null);
        return (epgMgr != null) ? epgMgr.getPollingDelayNormal() : 0;
    }

    public static int getEpgPollingDelayToRadio() {
        EpgManager epgMgr = EpgManager.getInstance(null);
        return (epgMgr != null) ? epgMgr.getPollingDelayToRadio() : 0;
    }

    public static int getEpgPollingDelayToStream() {
        EpgManager epgMgr = EpgManager.getInstance(null);
        return (epgMgr != null) ? epgMgr.getPollingDelayToStream() : 0;
    }

    public static int getEpgRssiToRadio() {
        EpgManager epgMgr = EpgManager.getInstance(null);
        return (epgMgr != null) ? epgMgr.getRssiToRadio() : 0;
    }

    public static int getEpgRssiToStream() {
        EpgManager epgMgr = EpgManager.getInstance(null);
        return (epgMgr != null) ? epgMgr.getRssiToStream() : 0;
    }

    public static boolean isEpgPlayingStreamRadio() {
        EpgManager epgMgr = EpgManager.getInstance(null);
        return (epgMgr != null) ? epgMgr.isStreamPlaying() : false;
    }

    public static boolean isEpgStreamAvailable(String freq) {
        EpgManager epgMgr = EpgManager.getInstance(null);
        return (epgMgr != null) ? epgMgr.isStreamAvailable(freq) : false;
    }

    public static void removeOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        EpgManager epgMgr = EpgManager.getInstance(null);
        if (epgMgr != null) {
            epgMgr.removeOnBufferingUpdateListener(listener);
        }
    }

    public static void setEpgPollingCount(int count) {
        EpgManager epgMgr = EpgManager.getInstance(null);
        if (epgMgr != null) {
            epgMgr.setPollingCount(count);
        }
    }

    public static void setEpgPollingDelayNormal(int delay) {
        EpgManager epgMgr = EpgManager.getInstance(null);
        if (epgMgr != null) {
            epgMgr.setPollingDelayNormal(delay);
        }
    }

    public static void setEpgPollingDelayToRadio(int delay) {
        EpgManager epgMgr = EpgManager.getInstance(null);
        if (epgMgr != null) {
            epgMgr.setPollingDelayToRadio(delay);
        }
    }

    public static void setEpgPollingDelayToStream(int delay) {
        EpgManager epgMgr = EpgManager.getInstance(null);
        if (epgMgr != null) {
            epgMgr.setPollingDelayToStream(delay);
        }
    }

    public static void setEpgRssiToRadio(int rssi) {
        EpgManager epgMgr = EpgManager.getInstance(null);
        if (epgMgr != null) {
            epgMgr.setRssiToRadio(rssi);
        }
    }

    public static void setEpgRssiToStream(int rssi) {
        EpgManager epgMgr = EpgManager.getInstance(null);
        if (epgMgr != null) {
            epgMgr.setRssiToStream(rssi);
        }
    }

    public static void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        EpgManager epgMgr = EpgManager.getInstance(null);
        if (epgMgr != null) {
            epgMgr.setOnBufferingUpdateListener(listener);
        }
    }

    public static boolean waitAndGetEpgStartingStream() {
        EpgManager epgMgr = EpgManager.getInstance(null);
        boolean value = false;
        if (epgMgr != null) {
            value = epgMgr.waitAndGetStartingStreamRadio();
        }
        return value;
    }

    public RadioDNSServiceDataIF() {
    }
}
