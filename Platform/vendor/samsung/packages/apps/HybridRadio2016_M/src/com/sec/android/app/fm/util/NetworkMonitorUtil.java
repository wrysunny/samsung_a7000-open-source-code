package com.sec.android.app.fm.util;

import java.util.ArrayList;

import com.sec.android.app.fm.RadioApplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class NetworkMonitorUtil {
    protected static final String TAG = "NetworkMonitorUtil";
    private static NetworkMonitorUtil sNetworkMonitorUtil = new NetworkMonitorUtil();

    private ConnectivityManager mConnectivityManager = null;
    private Context mContext = null;

    private int mCurrentNetworkType = ConnectivityManager.TYPE_NONE; // TYPE_MOBILE,TYPE_WIFI,TYPE_WIMAX

    private static ArrayList<OnNetworkStateChangeListener> sListener = new ArrayList<OnNetworkStateChangeListener>();

    public void addOnNetworkStateChangeListener(OnNetworkStateChangeListener listener) {
        sListener.add(listener);
    }

    public void removeOnNetworkStateChangeListener(OnNetworkStateChangeListener listener) {
        sListener.remove(listener);
    }

    public interface OnNetworkStateChangeListener {
        public void onNetworkConnected();

        public void onNetworkDisconnected();
    }

    BroadcastReceiver mNetworkStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive - action:" + action);
            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo activeNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
                NetworkInfo currentNetworkInfo = null;

                if (mCurrentNetworkType != -1) {
                    Network[] networks = mConnectivityManager.getAllNetworks();
                    NetworkInfo networkInfo;
                    Network network;

                        for (int i = 0; i < networks.length; i++){
                            network = networks[i];
                            networkInfo = mConnectivityManager.getNetworkInfo(network);

                            if ( networkInfo.getType() == mCurrentNetworkType ) {
                                currentNetworkInfo = networkInfo;
                            }
                        }
                }

                if (activeNetworkInfo != null) {
                    Log.v(TAG, "ActiveNetwork State Changed - "
                            + activeNetworkInfo.getState().name());

                    switch (activeNetworkInfo.getState()) {
                    case CONNECTED:
                        mCurrentNetworkType = activeNetworkInfo.getType();
                        if (sListener != null) {
                            for (OnNetworkStateChangeListener listener : sListener)
                                listener.onNetworkConnected();
                        }
                        break;
                    default:
                        break;
                    }
                } else if (currentNetworkInfo != null) {
                    Log.v(TAG, "Current Network is " + currentNetworkInfo.getTypeName());
                    Log.v(TAG, "CurrentNetwork State Changed - "
                            + currentNetworkInfo.getState().name());

                    switch (currentNetworkInfo.getState()) {
                    case DISCONNECTING:
                        mCurrentNetworkType = ConnectivityManager.TYPE_NONE;
                        break;
                    case DISCONNECTED:
                        mCurrentNetworkType = ConnectivityManager.TYPE_NONE;
                        break;
                    case CONNECTED:
                        if (sListener != null) {
                            for (OnNetworkStateChangeListener listener : sListener)
                                listener.onNetworkConnected();
                        }
                        break;
                    default:
                        break;
                    }
                } else
                    Log.e(TAG, "Active network and current network are Null");
            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                boolean noConnectivity = intent.getBooleanExtra(
                        ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (noConnectivity) {
                    // mDnsSystem.runDNSSystem(new
                    // ModeData(DNSEvent.DNS_PAUSE));
                    if (sListener != null) {
                        for (OnNetworkStateChangeListener listener : sListener)
                            listener.onNetworkDisconnected();
                    }
                }
            }
        }

    };

    public static NetworkMonitorUtil getInstance() {
        return sNetworkMonitorUtil;
    }

    public synchronized void registerReceiver(boolean register, Context context) {
        mContext = context;
        mConnectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        Log.v(TAG, "registerReceiver : " + mContext.hashCode() + " " + register + " "
                + mNetworkStateReceiver.hashCode());

        if (register) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            mContext.registerReceiver(mNetworkStateReceiver, intentFilter);
        } else {
            mContext.unregisterReceiver(mNetworkStateReceiver);
        }

    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                return networkInfo.isConnected();
            }
        }

        return false;
    }

    public static boolean needBillWarning(Context context) {
    	boolean wifiConnected = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        Network[] networks = cm.getAllNetworks();
        NetworkInfo networkInfo;
        Network network;

        for (int i = 0; i < networks.length; i++){
            network = networks[i];
            networkInfo = cm.getNetworkInfo(network);

            if ( networkInfo.getType() == ConnectivityManager.TYPE_WIFI
                && networkInfo.getState() == NetworkInfo.State.CONNECTED ) {
                wifiConnected = true;
            }
        }

        boolean warning = RadioApplication.isWifiChecked();
        Log.v(TAG, "Don't show? " + warning);
        return !wifiConnected && !warning;
    }
}
