package com.sec.android.app.dns;

import android.util.Log;
import com.sec.android.app.fm.util.SystemPropertiesWrapper;
/** Logger Class for FM RadioDNS Tag */
public class LogDns {
    private static final String ALTERNATIVE_STRING = "##";
    private static final String APP = "FM RadioDNS";
    public static final boolean DEBUGGABLE = (SystemPropertiesWrapper.getInstance().getInt("ro.debuggable", 0) == 1);
    private static StringBuffer mBuf = new StringBuffer();

    // Control Log level by this.
    private static final int mDebugLevel = Log.VERBOSE;

    public static void d(String tag, String msg) {
        if (mDebugLevel <= Log.DEBUG)
            Log.d(APP, makeString(tag, msg));
        RadioDNSUtil.saveLog(null, tag, msg);
    }

    public static void e(String tag, Exception e) {
        if (null != e) {
            e.printStackTrace();
            RadioDNSUtil.saveLog(null, tag, e.toString());
        }
    }

    public static void e(String tag, String msg) {
        Log.e(APP, makeString(tag, msg));
        RadioDNSUtil.saveLog(null, tag, msg);
    }

    public static void e(String tag, String msg, Exception e) {
        Log.e(APP, makeString(tag, msg), e);
        RadioDNSUtil.saveLog(null, tag, msg);
    }

    public static String filter(float val) {
        if (!DEBUGGABLE)
            return ALTERNATIVE_STRING;
        else
            return String.valueOf(val);
    }

    public static String filter(int val) {
        if (!DEBUGGABLE)
            return ALTERNATIVE_STRING;
        else
            return String.valueOf(val);
    }

    public static String filter(long val) {
        if (!DEBUGGABLE)
            return ALTERNATIVE_STRING;
        else
            return String.valueOf(val);
    }

    public static String filter(Object val) {
        if (!DEBUGGABLE)
            return ALTERNATIVE_STRING;
        else
            return String.valueOf(val);
    }

    public static String filter(String str) {
        if (!DEBUGGABLE)
            return ALTERNATIVE_STRING;
        else
            return str;
    }

    public static void i(String tag, String msg) {
        if (mDebugLevel <= Log.INFO)
            Log.i(APP, makeString(tag, msg));
        RadioDNSUtil.saveLog(null, tag, msg);
    }

    private static String makeString(String tag, String msg) {
        mBuf.setLength(0);
        return mBuf.append(tag).append("(3678)").append(" : ").append(msg).toString();
    }

    public static void v(String tag, String msg) {
        if (mDebugLevel <= Log.VERBOSE)
            Log.v(APP, makeString(tag, msg));
        RadioDNSUtil.saveLog(null, tag, msg);
    }

    public static void w(String tag, String msg) {
        if (mDebugLevel <= Log.WARN)
            Log.w(APP, makeString(tag, msg));
        RadioDNSUtil.saveLog(null, tag, msg);
    }
}