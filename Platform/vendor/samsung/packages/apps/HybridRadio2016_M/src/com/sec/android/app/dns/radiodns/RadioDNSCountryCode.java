package com.sec.android.app.dns.radiodns;

//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//import com.sec.android.app.dns.DnsInternalDataManager;
import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;
//import com.sec.android.app.dns.RadioDNSUtil;
import com.sec.android.app.dns.data.RadioDNSData;
//import com.sec.android.app.fm.util.NetworkMonitorUtil;
//
//import android.location.Geocoder;
//import android.location.Address;
//import android.location.Location;
//import android.os.Message;

/**
 * @author Hyejung Kim(hyejung8.kim@samsung.com)
 * 
 */
public class RadioDNSCountryCode {
    // public static RadioDNSCountryCode sInstance = null;

    // final static int NOTHING = 0;
    // final static int ECC = 1;
    // final static int NETWORK = 2;
    // final static int GPS = 3;

    private static final String TAG = "RadioDNSCountryCode";
    String mCountryCode = null;
    String mIsoCountryCode = null;

    // int mSource;
    DNSService mDnsService = null;

    int mNibble;
    // Thread mLocThread = null;
    // boolean mNeedRetry = false; // if Internet is not connected when trying
    // to
    // some operation, it's true

    // boolean bDNSUpdateNeeded = false;

    // EUR only ISO
    final static String[][] PI_ISO_table = new String[][] { { "DE", "GR", "MA", "ME", "MD" },
            { "DZ", "CY", "CZ", "IE", "EE" }, { "AD", "SM", "PL", "TR", "KG" },
            { "IL", "CH", "VA", "MK" }, { "IT", "JO", "SK" }, { "BE", "FI", "SY", "", "UA" },
            { "RU", "LU", "TN", "", "ks" }, { "PS", "BG", "", "NL", "PT" },
            { "AL", "DK", "LI", "LV", "SI" }, { "AT", "GI", "IS", "LB", "AM" },
            { "HU", "IQ", "MC", "AZ" }, { "MT", "GB", "LT", "HR", "GE" },
            { "DE", "LY", "RS", "KZ" }, { "", "RO", "ES", "SE" }, { "EG", "FR", "NO", "BY", "BA" } };

    // EUR only
    // final static String[][] ISO_Country_table = new String[][] { { "AL",
    // "Albania" },
    // { "DZ", "Algeria" }, { "AD", "Andorra" }, { "AM", "Armenia" }, { "AT",
    // "Austria" },
    // { "AZ", "Azerbaijan" }, { "PT", /* "Azores", "Madeira", */"Portugal" },
    // { "BE", "Belgium" }, { "BY", "Belarus" }, { "BA", "Bosnia Herzegovina" },
    // { "BG", "Bulgaria" }, { "ES", /* "Canaries", */"Spain" }, { "HR",
    // "Croatia" },
    // { "CY", "Cyprus" }, { "CZ", "Czech Republic" }, { "DK", /* "Faroe",
    // */"Denmark" },
    // { "EG", "Egypt" }, { "EE", "Estonia" }, { "FI", "Finland" }, { "FR",
    // "France" },
    // { "GE", "Georgia" }, { "DE", "Germany" }, { "GI", "Gibraltar" }, { "GR",
    // "Greece" },
    // { "HU", "Hungary" }, { "IS", "Iceland" }, { "IQ", "Iraq" }, { "IE",
    // "Ireland" },
    // { "IL", "Israel" }, { "IT", "Italy" }, { "JO", "Jordan" }, { "ks",
    // "Kosovo" },
    // { "LV", "Latvia" }, { "LB", "Lebanon" }, { "LY", "Libya" }, { "LI",
    // "Liechtenstein" },
    // { "LT", "Lithuania" }, { "LU", "Luxembourg" }, { "MK", "Macedonia" },
    // { "MT", "Malta" }, { "MD", "Moldova" }, { "MC", "Monaco" }, { "ME",
    // "Montenegro" },
    // { "MA", "Morocco" }, { "NL", "Netherlands" }, { "NO", "Norway" },
    // { "PS", "Palestine" }, { "PL", "Poland" }, { "RO", "Romania" },
    // { "RU", "Russian Federation" }, { "SM", "San Marino" }, { "RS", "Serbia"
    // },
    // { "SK", "Slovakia" }, { "SI", "Slovenia" }, { "SE", "Sweden" },
    // { "CH", "Switzerland" }, { "SY", "Syrian Arab Republic" }, { "TN",
    // "Tunisia" },
    // { "TR", "Turkey" }, { "UA", "Ukraine" }, { "GB", "United Kingdom" },
    // { "VA", "Vatican City State" }, { "KZ", "Kazakhstan" }, { "KG",
    // "Kyrghyzstan" } };

    // EUR only
    final static String[][] ISO_GCC_table = new String[][] { { "AL", "9E0" }, { "DZ", "2E0" },
            { "AD", "3E0" }, { "AM", "AE4" }, { "AT", "AE0" }, { "AZ", "BE3" }, { "PT", "8E4" },
            { "BE", "6E0" }, { "BY", "FE3" }, { "BA", "FE4" }, { "BG", "8E1" }, { "ES", "EE2" },
            { "HR", "CE3" }, { "CY", "2E1" }, { "CZ", "2E2" }, { "DK", "9E1" }, { "EG", "FE0" },
            { "EE", "2E4" }, { "FI", "6E1" }, { "FR", "FE1" }, { "GE", "CE4" },
            { "DE", "DE0", "1E0" }, { "GI", "AE1" }, { "GR", "1E1" }, { "HU", "BE0" },
            { "IS", "AE2" }, { "IQ", "BE1" }, { "IE", "2E3" }, { "IL", "4E0" }, { "IT", "5E0" },
            { "JO", "5E1" }, { "ks", "7E4" }, { "LV", "9E3" }, { "LB", "AE3" }, { "LY", "DE1" },
            { "LI", "9E2" }, { "LT", "CE2" }, { "LU", "7E1" }, { "MK", "4E3" }, { "MT", "CE0" },
            { "MD", "1E4" }, { "MC", "BE2" }, { "ME", "1E3" }, { "MA", "1E2" }, { "NL", "8E3" },
            { "NO", "FE2" }, { "PS", "8E0" }, { "PL", "3E2" }, { "RO", "EE1" }, { "RU", "7E0" },
            { "SM", "3E1" }, { "RS", "DE2" }, { "SK", "5E2" }, { "SI", "9E4" }, { "SE", "EE3" },
            { "CH", "4E1" }, { "SY", "6E2" }, { "TN", "7E2" }, { "TR", "3E3" }, { "UA", "6E4" },
            { "GB", "CE1" }, { "VA", "4E2" }, { "KZ", "DE3" }, { "KG", "3E4" } };

    public static String getGccCountryCode(String piCode, String cc) {
        for (int i = 0; i < ISO_GCC_table.length; ++i) {
            if (ISO_GCC_table[i][0].equalsIgnoreCase(cc)) {
                for (int j = 1; j < ISO_GCC_table[i].length; ++j) {
                    String piFirst = piCode.substring(0, 1);
                    String ccFirst = ISO_GCC_table[i][j].substring(0, 1);
                    if (piFirst.equalsIgnoreCase(ccFirst))
                        return ISO_GCC_table[i][j];
                }
            }
        }
        return null;
    }

    public static String getIsoCountryCode(String cc) {
        for (int i = 0; i < ISO_GCC_table.length; ++i) {
            for (int j = 1; j < ISO_GCC_table[i].length; ++j) {
                if (ISO_GCC_table[i][j].equalsIgnoreCase(cc)) {
                    return ISO_GCC_table[i][0];
                }
            }
        }
        return null;
    }

    /**
     * public static RadioDNSCountryCode getInstance(DNSService dnsSystem) { if
     * (sInstance == null) sInstance = new RadioDNSCountryCode(dnsSystem);
     * 
     * return sInstance; }
     */
    public RadioDNSCountryCode(DNSService dnsService) {
        mNibble = 0;
        // mSource = NOTHING;
        mCountryCode = null;
        mIsoCountryCode = null;
        mDnsService = dnsService;
    }

    private void countryCodeToIsoCode() {
        int eccNum = Integer.parseInt(mCountryCode.substring(2));
        mIsoCountryCode = PI_ISO_table[mNibble][eccNum];
    }

    // public String makeCountry_PiEcc(int pi, int ecc) {
    // mNibble = pi >> 12;
    // int country = (mNibble << 8) | ecc;
    //
    // mCountryCode = Integer.toHexString(country);
    // // mSource = ECC;
    // LogDns.v(TAG, "ECC country : " + LogDns.filter(mCountryCode));
    //
    // return mCountryCode;
    // }

    public String makeCountry_PiEcc(String piCode, int ecc) {
        String nibble = piCode.substring(0, 1);
        mCountryCode = nibble.concat(Integer.toHexString(ecc));

        // mSource = ECC;
        LogDns.v(TAG, "ECC country : " + LogDns.filter(mCountryCode));

        return mCountryCode;
    }

    public void makeNibble(int pi) {
        mNibble = pi >> 12;
    }

    public void makeNibble(String piCode) {
        mNibble = Integer.parseInt(piCode.substring(0, 1), 16);
    }

    // private int getCountryName(String ISOCode) {
    // for (int i = 0; i < ISO_Country_table.length; ++i) {
    // if (ISO_Country_table[i][0].equalsIgnoreCase(ISOCode)) {
    // return i;
    // }
    // // getFromLocationName();
    // }
    // return -1;
    // }
    //
    // private static final int MAX_RETRY = 10;
    //
    // private Address getPosition(String countryName) {
    // Geocoder coder = new Geocoder(mDnsService);
    // List<Address> addr = null;
    // boolean exception = false;
    // int count = 0;
    //
    // LogDns.v(TAG, "getPosition : " + LogDns.filter(countryName));
    //
    // do {
    // exception = false;
    // try {
    // // it is not considered for same country
    // addr = coder.getFromLocationName(countryName, 1);
    // } catch (IOException e) {
    // e.printStackTrace();
    // exception = true;
    //
    // if (!NetworkMonitorUtil.isConnected(mDnsService)) {
    // LogDns.v(TAG, "Network is not connected");
    // mNeedRetry = true;
    // break;
    // } else {
    // LogDns.v(TAG, "getFromLocationName() is failed. Try# " + count);
    // }
    // }
    // ++count;
    // } while ((count < MAX_RETRY) && exception);
    //
    // if ((addr == null) || (addr.size() == 0) || (addr.get(0) == null)) {
    // LogDns.v(TAG, "There is no address");
    // return null;
    // }
    // LogDns.v(TAG, LogDns.filter(addr.get(0).toString()));
    //
    // return addr.get(0);
    // }

    // private String getNearestCountry(double latitude, double longitude) {
    // int num = PI_ISO_table[mNibble - 1].length;
    // int pos = 0, minListPos = 0;
    // ArrayList<String> nearCountryList = new ArrayList<String>();
    // ArrayList<String> nearIsoList = new ArrayList<String>();
    // Address tmp = null;
    // float[] results = new float[10];
    // double minDistance = Double.MAX_VALUE;
    //
    // for (int i = 0; i < num; ++i) {
    // pos = getCountryName(PI_ISO_table[mNibble - 1][i]);
    // if (pos == -1)
    // continue;
    //
    // for (int j = 1; j < ISO_Country_table[pos].length; ++j) {
    // nearCountryList.add(ISO_Country_table[pos][j]);
    // nearIsoList.add(ISO_Country_table[pos][0]);
    // }
    // }
    //
    // LogDns.v(TAG, "nearCountryList.size() : " + nearCountryList.size());
    // for (int i = 0; i < nearCountryList.size(); ++i) {
    // tmp = getPosition(nearCountryList.get(i).toString());
    // if (tmp == null)
    // continue;
    //
    // try {
    // Location.distanceBetween(latitude, longitude, tmp.getLatitude(),
    // tmp.getLongitude(), results);
    //
    // LogDns.v(TAG, LogDns.filter(i + " " + latitude + " " + longitude + " " +
    // tmp.getLatitude() + " "
    // + tmp.getLongitude() + " " + results[0] + " " + minDistance));
    //
    // if (minDistance > results[0]) {
    // minDistance = results[0];
    // minListPos = i;
    // }
    // } catch (IllegalArgumentException e) {
    // LogDns.v(TAG, "Nearest Error");
    // e.printStackTrace();
    // return null;
    // }
    // }
    //
    // if (minDistance == Double.MAX_VALUE)
    // return null;
    // return nearIsoList.get(minListPos).toString();
    // }

    private boolean canCountryCheck() {
        int len = 0;
        for (int i = 0; i < 15; ++i) {
            len = PI_ISO_table[i].length;
            for (int j = 0; j < len; ++j) {
                if (PI_ISO_table[i][j].equalsIgnoreCase(mIsoCountryCode))
                    return true;
            }
        }
        return false;
    }

    public boolean isCountryCorrectionNeeded(RadioDNSData dnsData) {
        if (mNibble == 0) {
            LogDns.v(TAG, "isCountryCorrectionNeeded() : There is no nibble!!!!");
            return false;
        }

        mIsoCountryCode = dnsData.getBaseCountryCode();
        if (!canCountryCheck()) {
            LogDns.v(TAG, "It's not in the country list!!");
            return false;
        }

        int num = PI_ISO_table[mNibble - 1].length;

        if ((mCountryCode != null) && (mIsoCountryCode == null))
            countryCodeToIsoCode();

        for (int i = 0; i < num; ++i) {
            if (PI_ISO_table[mNibble - 1][i].equalsIgnoreCase(mIsoCountryCode)) {
                LogDns.v(TAG, "Correct country");
                return false;
            }
        }
        //
        // if (DnsInternalDataManager.getInstance().getCcInternalData(dnsData)
        // == false) {
        // if (!(RadioDNSUtil.Network.isConnected(mDnsService))) {
        // LogDns.v(TAG, "Network is not connected");
        // dnsData.setNeedCcCorrection(true);
        // dnsData.setNeedCcRetryNetwork(true);
        // LogDns.v(TAG, "Need correction!!!!");
        // return true;
        // }
        // countryCorrection(dnsData);
        // } else {
        // return false;
        // }

        LogDns.v(TAG, "Need correction!!!!");
        return true;
    }

    // public boolean canCountryCorrection() {
    // if (mLocThread == null)
    // return true;
    // return false;
    // }

    // public void stopCountryCorrection() {
    // if (mLocThread != null && mLocThread.isAlive())
    // mLocThread.interrupt();
    // }

    // private void countryCorrection(final RadioDNSData dnsData) {
    // if (mNibble == 0) {
    // LogDns.v(TAG, "countryCorrection() : There is no nibble!!!!");
    // return;
    // }
    // // mISOCountryCode = "gb"; // for test
    //
    // final int namePos = getCountryName(mIsoCountryCode);
    //
    // if (namePos == -1) {
    // LogDns.v(TAG, "There is no country name");
    // return;
    // }
    //
    // if (mLocThread != null) {
    // LogDns.v(TAG, "mLocThread already exists~!");
    // return;
    // }
    //
    // mLocThread = new Thread(new Runnable() { // For GeoCoder
    // @Override
    // public void run() {
    // mNeedRetry = false;
    // // the number of (current ISO's country name)
    // // int curNum = ISO_Country_table[namePos].length - 1;
    // //
    // // if (curNum > 1) {
    // // LogDns.v(TAG,"There are more than one country for " +
    // // ISOCountryCode);
    // //
    // // Address myPos = null;
    // // for (int i = 1; i <= curNum;++i) {
    // // myPos =getPosition(ISO_Country_table[namePos][i]);
    // //
    // // need to compare between current GPS position and
    // // country name }
    // // } else{
    // Address myPos = getPosition(ISO_Country_table[namePos][1]);
    // if (myPos != null) {
    // String result = getNearestCountry(myPos.getLatitude(),
    // myPos.getLongitude());
    // if (result != null)
    // mIsoCountryCode = result;
    // }
    // // }
    // // LogDns.v(TAG, "setNeedRetry : "+mNeedRetry);
    //
    // if (!mNeedRetry) {
    // dnsData.setCountryCode(mIsoCountryCode);
    //
    // Message msg = new Message();
    // msg.what = RadioDNSConnection.COUNTRYUPDATED;
    // msg.obj = dnsData;
    //
    // dnsData.setNeedCcCorrection(false);
    //
    // if (mDnsService != null)
    // RadioDNSConnection.getInstance(mDnsService).getEventHandler()
    // .sendMessage(msg);
    // } else {
    // dnsData.setNeedCcCorrection(true);
    // dnsData.setNeedCcRetryNetwork(true);
    //
    // if ((mDnsService != null)
    // && (dnsData.isSame(mDnsService.getCurrentData()))) {
    // mDnsService.getCurrentData().setNeedCcRetryNetwork(mNeedRetry);
    // } else {
    // LogDns.v(
    // TAG,
    // "Country network connection error flag is not updated because the data is not match with current DNS data "
    // + dnsData);
    // }
    // }
    //
    // mLocThread = null;
    // }
    // });
    // mLocThread.start();
    // }
}
