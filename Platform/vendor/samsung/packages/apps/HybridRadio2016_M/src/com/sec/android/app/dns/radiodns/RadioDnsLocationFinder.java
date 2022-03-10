package com.sec.android.app.dns.radiodns;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;

import com.sec.android.app.dns.LogDns;
import com.sec.android.app.fm.util.NetworkMonitorUtil;

public class RadioDnsLocationFinder {
    private static final int MIN_TIME = 0;// 1000; // 5mins
    private static final float MIN_DISTANCE = 0;// 100f; // 10km
    private static final int MAX_RETRY = 10;

    private static final String TAG = "RadioDnsLocationFinder";
    private static RadioDnsLocationFinder sInstance = new RadioDnsLocationFinder();
    private double mLongitude = -1, mLatitude = -1;
    private String mIsoCountry = null;
    private LocationManager mLocMan = null;
    private Thread mLocThread = null;
    private Context mContext;
    private LocationFinder mLfGps = null;
    private LocationFinder mLfNet = null;

    public static RadioDnsLocationFinder getInstance() {
        return sInstance;
    }

    public void setContext(Context context) {
        mLocMan = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mContext = context;
    }

    public String findPosition() {
        if ((mIsoCountry == null) && (mLfGps == null) && (mLfNet == null) && (mLocThread == null)
                && NetworkMonitorUtil.isConnected(mContext)) {
            Location lastLocation = mLocMan.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation == null) {
                lastLocation = mLocMan.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastLocation != null) {
                LogDns.v(TAG, LogDns.filter(lastLocation.toString()));

                mLatitude = lastLocation.getLatitude();
                mLongitude = lastLocation.getLongitude();

                getLocation();
            } else {
                addLocListener();
            }
        } else {
            LogDns.v(TAG, "Location listener cannot be added");
        }

        return mIsoCountry;
    }

    private void removeLocListener() {
        mLocMan.removeUpdates(mLfNet);
        mLocMan.removeUpdates(mLfGps);
        mLfNet = null;
        mLfGps = null;
    }

    private void addLocListener() {
        String provider = LocationManager.GPS_PROVIDER;

        if (mLocMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            LogDns.v(TAG, "addLocationListener NETWORK_PROVIDER is enabled");
            provider = LocationManager.NETWORK_PROVIDER;

            mLfNet = new LocationFinder(provider);
            mLocMan.requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, mLfNet,
                    Looper.getMainLooper());
        }
        if (mLocMan.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LogDns.v(TAG, "addLocationListener GPS_PROVIDER is enabled");
            provider = LocationManager.GPS_PROVIDER;

            mLfGps = new LocationFinder(provider);
            mLocMan.requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, mLfGps,
                    Looper.getMainLooper());
        } else {
            Settings.Secure.setLocationProviderEnabled(mContext.getContentResolver(),
                    LocationManager.GPS_PROVIDER, true);
            LogDns.e(TAG, "addLocationListener provider is null");
            provider = LocationManager.GPS_PROVIDER;

            mLfGps = new LocationFinder(provider);
            mLocMan.requestLocationUpdates(provider, MIN_TIME, MIN_DISTANCE, mLfGps,
                    Looper.getMainLooper());
        }
    }

    private class LocationFinder implements LocationListener {
        private String mProvider = null;

        public LocationFinder(String provider) {
            mProvider = provider;
        }

        @Override
        public void onLocationChanged(Location arg0) {
            mLatitude = arg0.getLatitude();
            mLongitude = arg0.getLongitude();

            LogDns.v(TAG,
                    "onLocationChanged(" + mProvider + ") : Latitude - " + LogDns.filter(mLatitude)
                            + " Longitude - " + LogDns.filter(mLongitude));

            RadioDnsLocationFinder.this.removeLocListener();
            getLocation();
        }

        @Override
        public void onProviderDisabled(String arg0) {
            LogDns.v(TAG, "onProviderDisabled : " + arg0);
        }

        @Override
        public void onProviderEnabled(String arg0) {
            LogDns.v(TAG, "onProviderEnabled : " + arg0);
        }

        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
            LogDns.v(TAG, "onStatusChanged - provider : " + arg0 + " status : " + arg1);
        }
    }

    private void getLocation() {
        LogDns.v(TAG, "getLocation()");
        mLocThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Geocoder coder = new Geocoder(mContext);
                List<Address> addr = null;
                int count = 0;
                boolean exception = false;

                do {
                    exception = false;
                    try {
                        addr = coder.getFromLocation(mLatitude, mLongitude, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                        exception = true;
                    }
                    ++count;
                } while ((count < MAX_RETRY) && exception);

                if ((addr != null) && (addr.get(0) != null)) {
                    mIsoCountry = addr.get(0).getCountryCode();
                    LogDns.v(TAG,
                            "mIsoCountry from LocationManager : " + LogDns.filter(mIsoCountry));
                } else {
                    LogDns.v(TAG, "addr is null");
                }

                mLocThread = null;
            }
        });

        mLocThread.start();
    }
}
