package com.sec.android.app.dns;

public class DNSEvent {
    public static final int DNS_EXTERNAL_BASE = 1;
    public static final int DNS_DNS_BASE = 100;
    public static final int DNS_EPG_BASE = 200;
    public static final int DNS_VIS_BASE = 300;
    public static final int DNS_VIS_HTTP_BASE = 400;

    // External DNSSystem Commands
    public static final int DNS_ON = DNS_EXTERNAL_BASE + 1;
    public static final int DNS_OFF = DNS_EXTERNAL_BASE + 2;
    public static final int DNS_PAUSE = DNS_EXTERNAL_BASE + 3;
//    public static final int DNS_START_EPG = DNS_EXTERNAL_BASE + 4;
//    public static final int DNS_STOP_EPG = DNS_EXTERNAL_BASE + 5;
    public static final int DNS_START_VIS = DNS_EXTERNAL_BASE + 6;
    public static final int DNS_STOP_VIS = DNS_EXTERNAL_BASE + 7;
    public static final int DNS_GET_EPG_PI = DNS_EXTERNAL_BASE + 8;
    public static final int DNS_UPDATE_RDS = DNS_EXTERNAL_BASE + 9;
    public static final int DNS_UPDATA_VIS = DNS_EXTERNAL_BASE + 10;

    // Internal DNSSystem Commands (DNS)
    public static final int CURRENT_DATA_UPDATED = DNS_DNS_BASE + 1;
    public static final int ALL_CURRENT_DATA_UPDATED = DNS_DNS_BASE + 2;
    public static final int VIS_CURRENT_DATA_UPDATED = DNS_DNS_BASE + 3;
    public static final int EPG_CURRENT_DATA_UPDATED = DNS_DNS_BASE + 4;
    
    public static final int VIEW_DATA_UPDATED = DNS_DNS_BASE + 5;

    // Internal DNSSystem Commands (EPG)
    public static final int DNS_EPG_SEND_PI = DNS_EPG_BASE + 1;
    public static final int DNS_EPG_SEND_PI_ALL = DNS_EPG_BASE + 2;
    public static final int DNS_EPG_EXPIRED_NOW_PROGRAM_INFO = DNS_EPG_BASE + 3;
    public static final int DNS_EPG_SET_ISSTREAM = DNS_EPG_BASE + 4;

    // Internal DNSSystem Commands (VIS)
    public static final int DNS_VIS_UPDATE_TEXT = DNS_VIS_BASE;
    public static final int DNS_VIS_UPDATE_SHOW = DNS_VIS_BASE + 1;
    public static final int DNS_VIS_UPDATE_LINK = DNS_VIS_BASE + 2;
    public static final int DNS_VIS_CLOSE_UI = DNS_VIS_BASE + 3;

    // Internal DNSSystem Commands (VIS_HTTP)
    public static final int DNS_VIS_HTTP_RECEIVE_DATA_FRAME = DNS_VIS_HTTP_BASE;
    public static final int DNS_VIS_HTTP_RECEIVE_DATA_FRAME_EMPTY = DNS_VIS_HTTP_BASE + 1;

    // Intent Command (DNS)
    public static final String DNS_ACTION_UPDATE_DATA = "com.sec.android.app.dns.action.UPDATE_DATA";

    // Intent Command (VIS)
    public static final String DNS_ACTION_UPDATE_VIS_DATA = "com.sec.android.app.dns.action.UPDATE_VIS_DATA";
    public static final String DNS_ACTION_UPDATE_TEXT = "com.sec.android.app.dns.action.UPDATE_TEXT";
    public static final String DNS_ACTION_UPDATE_SHOW = "com.sec.android.app.dns.action.UPDATE_SHOW";
    public static final String DNS_ACTION_VIS_PROTOCOL = "com.sec.android.app.dns.action.VIS_PROTOCOL";
    public static final int DNS_ACTION_VIS_HTTP = 80;
    public static final int DNS_ACTION_VIS_STOMP = 61613;

    // Intent Command (EPG)
    public static final String DNS_ACTION_MEDIA_INFO_BUFFERING_END = "com.sec.android.app.dns.action.MEDIA_INFO_BUFFERING_END";
    public static final String DNS_ACTION_MEDIA_STOPPED = "com.sec.android.app.dns.action.MEDIA_STOPPED";
    public static final String DNS_ACTION_PROGRAM_INFO = "com.sec.android.app.dns.action.PROGRAM_INFO";
    public static final String DNS_ACTION_PROGRAM_INFO_ALL = "com.sec.android.app.dns.action.PROGRAM_INFO_ALL";
    public static final String DNS_ACTION_UPDATE_ISSTREAM = "com.sec.android.app.dns.action.UPDATE_ISSTREAM";
    public static final String DNS_ACTION_UPDATE_NOW_EPG_DATA = "com.sec.android.app.dns.action.UPDATE_NOW_EPG_DATA";
    public static final String DNS_ACTION_UPDATE_PROGRAM_INFO = "com.sec.android.app.dns.action.UPDATE_PROGRAM_INFO";
    public static final String DNS_ACTION_UPDATE_RSSI = "com.sec.android.app.dns.action.UPDATE_RSSI";
    public static final String DNS_ACTION_UPDATE_STREAM_PREPARED = "com.sec.android.app.dns.action.UPDATE_STREAM_PREPARED";

    protected static final String DNS_ACTION_EPG_STOP_STREAM = "com.sec.android.app.dns.action.stop_internet_stream";
    protected static final String DNS_ACTION_EPG_PAUSE_STREAM = "com.sec.android.app.dns.action.pause_internet_stream";
    protected static final String DNS_ACTION_EPG_RESUME_STREAM = "com.sec.android.app.dns.action.resume_internet_stream";

    public static final String DNS_ACTION_DISPLAY_DNS_UI = "com.sec.android.app.dns.action.DISPLAY_DNS_UI";
    public static final String DNS_ACTION_CLOSE_DNS_UI = "com.sec.android.app.dns.action.CLOSE_DNS_UI";
    public static final String DNS_ACTION_CURRENT_DATA_UPDATED = "com.sec.android.app.dns.action.CURRENT_DATA_UPDATED";

    public static final String DNS_ACTION_VIS_HOST_IS_UPDATED = "com.sec.android.app.dns.action.VIS_HOST_IS_UPDATED";
    public static final String DNS_ACTION_PI_DATA_IS_UPDATED = "com.sec.android.app.dns.action.PI_DATA_IS_UPDATED";
    // Intent extra field name
    public static final String DNS_RESPONSE_RESULT = "Result";
    public static final String DNS_RESPONSE_PROGRAM_FREQ = "ProgramFreq";

    public static final String DNS_EXTRA_FREQUENCY = "DnsFrequency";
    public static final String DNS_EXTRA_RSSI = "DnsRssi";
}
