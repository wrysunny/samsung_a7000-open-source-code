package com.sec.android.app.dns;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.sec.android.app.fm.util.SystemPropertiesWrapper;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

/**
 * @author Hyejung Kim(hyejung8.kim@samsung.com)
 * 
 */

public class RadioDNSUtil {
    public static class Network {
        public static boolean isConnected(Context context) {
            boolean isWifiConn = false;
            boolean isMobileConn = false;

                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                android.net.Network[] networks = cm.getAllNetworks();
                NetworkInfo networkInfo;
                android.net.Network network;

                for (int i = 0; i < networks.length; i++){
                    network = networks[i];
                    networkInfo = cm.getNetworkInfo(network);

                    if ( networkInfo.getType() == ConnectivityManager.TYPE_WIFI 
                        && networkInfo.getState() == NetworkInfo.State.CONNECTED ) {
                        isWifiConn = true;
                    } else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE
                        && networkInfo.getState() == NetworkInfo.State.CONNECTED ) {
                        isMobileConn = true;
                    }
                }

                return isWifiConn || isMobileConn;
        }
    }

    public static final String DEFAULT_LOG_FILE_NAME = "dnsLog.log";
    private static final boolean isTestMode = (SystemPropertiesWrapper.getInstance().getInt("ro.debuggable", 0) == 1);
    public static final String LOG_FILE_PATH = "./storage/sdcard0/log/";
    private static final String TAG = "RadioDNSUtil";

    public static int dateToInt(Date date) {
        return Integer.parseInt(new SimpleDateFormat("yyyyMMdd").format(date));
    }

    public static String dateToString(Date date) {
        return new SimpleDateFormat("yyyyMMdd").format(date);
    }

    public static boolean sameDate(Date lDate, Date rDate) {
        String l = dateToString(lDate);
        String r = dateToString(rDate);
        return l.equals(r);
    }

    public static synchronized void saveLog(String filename, String tag, String msg) {
        if (!isTestMode)
            return;
        String today = RadioDNSUtil.dateToString(Calendar.getInstance().getTime());
        if (filename == null)
            filename = today + "_" + DEFAULT_LOG_FILE_NAME;
        else
            filename = today + "_" + filename;
        StringBuffer strBuffer = new StringBuffer();
        SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String time = mSimpleDateFormat.format(System.currentTimeMillis());
        strBuffer.append(time);
        strBuffer.append("\t ");
        strBuffer.append(tag);
        strBuffer.append("\t ");
        strBuffer.append(msg);
        BufferedWriter writer = null;
        try {
            File path = new File(LOG_FILE_PATH);
            if (!path.exists()) {
                path.mkdirs();
            }
            File file = new File(LOG_FILE_PATH + filename);
            if (!file.exists()) {
                Log.e(TAG, "log file - No FILE");
                file.createNewFile();
            }
            writer = new BufferedWriter(new FileWriter(file, true));
            writer.append(strBuffer.toString());
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "write log to file -  fail!");
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static Date stringToDate(String date) throws ParseException {
        String convertDate = null;
        int dateLength = date.length();
        if (dateLength == 25) {
            convertDate = date.substring(0, dateLength - 3) + "00";
        } else if (dateLength == 19) {
            convertDate = date + "+0000";
            Log.e(TAG, "Date Format in which the length is 19: " + date);
        } else {
            convertDate = date;
            Log.e(TAG, "Unknown Date Format: " + date);
        }
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(convertDate);
    }

    public static int timeToInt(Date date) {
        return Integer.parseInt(new SimpleDateFormat("HHmmss").format(date));
    }
}
