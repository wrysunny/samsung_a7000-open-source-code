package com.sec.android.app.dns;

//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;

import java.util.ArrayList;

import com.sec.android.app.dns.data.InternalDnsData;
//import com.sec.android.app.dns.data.RadioDNSData;
import com.sec.android.app.dns.radiodns.RadioDNSCountryCode;

public class DnsInternalDataManager {
    private static final String TAG = "DNSInternalData";
    private ArrayList<InternalDnsData> mData = null;

    private static DnsInternalDataManager sInstance = null;

    public static synchronized DnsInternalDataManager getInstance() {
        if (sInstance == null) {
            sInstance = new DnsInternalDataManager();
        }
        return sInstance;
    }

    public void destroy() {
        sInstance = null;
    }

    private DnsInternalDataManager() {
        mData = new ArrayList<InternalDnsData>();
        // loadData();
    }

    public int getSize() {
        return mData.size();
    }

    // //////////////////////////////////
    // File structure
    // Freq|PI|CC|CNAME|CNAME TTL|VIS Protocol|VIS Host|VIS Port|VIS TTL|EPG
    // Host|EPG TTL
    // private static final String FILE_NAME =
    // "./data/data/com.sec.android.app.fm/files/internalData.dat";
    /*
     * private void loadData() { Log.secV(TAG, "loadData()");
     * 
     * BufferedReader reader = null; try { File file = new File(FILE_NAME); if
     * (!file.exists()) { file.createNewFile(); return; }
     * 
     * reader = new BufferedReader(new FileReader(file)); String dataStr = null;
     * RadioDNSData tmpData = null;
     * 
     * while ((dataStr = reader.readLine()) != null) { tmpData = new
     * RadioDNSData(); tmpData.setDnsData(dataStr);
     * 
     * mData.add(tmpData); } reader.close(); reader = null;
     * 
     * } catch (IOException e) { Log.secE(TAG, "loadData fail!");
     * e.printStackTrace(); } finally { if (reader != null) { try {
     * reader.close(); } catch (IOException e) { e.printStackTrace(); } } }
     * 
     * }
     * 
     * private void storeData() { Log.secV(TAG, "storeData()");
     * 
     * BufferedWriter writer = null; try { File file = new File(FILE_NAME); if
     * (!file.exists()) { Log.secE(TAG, "store data - No FILE");
     * file.createNewFile(); }
     * 
     * writer = new BufferedWriter(new FileWriter(file));
     * 
     * for (int i = 0; i < mData.size(); ++i) {
     * writer.append(mData.get(i).getString() + "\n"); } } catch (IOException e)
     * { Log.secE(TAG, "storeData fail!"); e.printStackTrace(); } finally { try
     * { if (writer != null) { writer.close(); writer = null; } } catch
     * (IOException e1) { e1.printStackTrace(); } }
     * 
     * }
     */

    // For Country Code
    // public boolean getCcInternalData(RadioDNSData dnsData) {
    // String compareFreq = dnsData.getFrequency();
    // String comparePiNibble = dnsData.getPi().substring(0, 1);
    // String compareBaseCountry = dnsData.getBaseCountryCode();
    //
    // if (compareFreq == null || comparePiNibble == null || compareBaseCountry
    // == null) {
    // LogDns.e(TAG, "getCcInternalData - compareFreq : " +
    // LogDns.filter(compareFreq)
    // + ", comparePiNibble : " + LogDns.filter(comparePiNibble)
    // + ", compareBaseCountry : " + LogDns.filter(compareBaseCountry));
    // return false;
    // }
    //
    // for (int i = 0; i < mData.size(); i++) {
    // InternalDnsData tmpData = mData.get(i);
    // LogDns.v(TAG, "getCcInternalData : #" + i + " " + tmpData);
    //
    // if (tmpData == null) {
    // LogDns.e(TAG, "getCcInternalData tmpData==null " + i);
    // continue;
    // }
    //
    // if (tmpData.getPi() != null) {
    // String tmpPiNibble = tmpData.getPi().substring(0, 1);
    //
    // if (compareFreq.equals(tmpData.getFrequency())
    // && comparePiNibble.equalsIgnoreCase(tmpPiNibble)
    // && compareBaseCountry.equalsIgnoreCase(tmpData.getBaseCountryCode())) {
    // dnsData.setCountryData(tmpData.getCountryData());
    // return true;
    // }
    // } else {
    // LogDns.e(TAG, "getCcInternalData tmpData.getPi()==null " + i);
    // }
    // }
    //
    // return false;
    // }

    // For CNAME
    public InternalDnsData getCnameInternalData(InternalDnsData compData) {
        if (compData == null)
            return null;

        for (int i = 0; i < mData.size(); i++) {
            InternalDnsData tmpData = mData.get(i);
            LogDns.v(TAG, "getCnameInternalData : #" + i + " " + tmpData);

            if (tmpData == null) {
                LogDns.e(TAG, "getCnameInternalData tmpData==null " + i);
                continue;
            }

            if (tmpData.isSameHead(compData, true)) {
                /*
                 * if (isTtlExpired(tmpData.getCnameTTL())) {
                 * tmpData.resetCnameData(); // if (!tmpData.canSave()) { //
                 * mData.remove(i); // --i; // } } else {
                 */
                return tmpData;
                // }
            }
        }

        LogDns.v(TAG, "getCnameInternalData size : " + mData.size());

        return null;
    }

    // For VIS
    public InternalDnsData getVisStompInternalData(String cname) {
        if (cname == null)
            return null;

        for (int i = 0; i < mData.size(); i++) {
            InternalDnsData tmpData = mData.get(i);
            if (tmpData == null) {
                LogDns.e(TAG, "getVisStompInternalData tmpData==null " + i);
                continue;
            }

            if (cname.equalsIgnoreCase(tmpData.getCname())) {
                /*
                 * if (isTtlExpired(tmpData.getVisStompTTL())) {
                 * tmpData.resetVisStompData(); // if (!tmpData.canSave()) { //
                 * mData.remove(i); // --i; // } } else {
                 */
                return tmpData;
                // }
            }
        }

        return null;
    }

    public InternalDnsData getVisHttpInternalData(String cname) {
        if (cname == null)
            return null;

        for (int i = 0; i < mData.size(); i++) {
            InternalDnsData tmpData = mData.get(i);
            if (tmpData == null) {
                LogDns.e(TAG, "getVisHttpInternalData tmpData==null " + i);
                continue;
            }

            if (cname.equalsIgnoreCase(tmpData.getCname())) {
                /*
                 * if (isTtlExpired(tmpData.getVisHttpTTL())) {
                 * tmpData.resetVisHttpData();
                 * 
                 * // if (!tmpData.canSave()) { // mData.remove(i); // --i; // }
                 * } else {
                 */
                return tmpData;
                // }
            }
        }

        return null;
    }

    // For EPG
    public InternalDnsData getEpgInternalData(String cname) {
        if (cname == null)
            return null;

        for (int i = 0; i < mData.size(); i++) {
            InternalDnsData tmpData = mData.get(i);
            if (tmpData == null) {
                LogDns.e(TAG, "getEPGinInternalData tmpData==null " + i);
                continue;
            }

            if (cname.equalsIgnoreCase(tmpData.getCname())) {
                /*
                 * if (isTtlExpired(tmpData.getEpgTTL())) {
                 * tmpData.resetEpgData(); // if (!tmpData.canSave()) { //
                 * mData.remove(i); // --i; // } } else {
                 */
                return tmpData;
                // }
            }
        }

        return null;
    }

    public InternalDnsData getNeedUpdateInternalData() {
        for (int i = 0; i < mData.size(); i++) {
            InternalDnsData tmpData = mData.get(i);
            if (tmpData == null) {
                LogDns.e(TAG, "getEPGinInternalData tmpData==null " + i);
                continue;
            }

            if (tmpData.needUpdate())
                return tmpData;
        }

        return null;
    }

    public synchronized void updateInternalData(InternalDnsData newData, boolean canClone) {
        LogDns.v(TAG, "updateInternalData : " + newData);

        for (int i = 0; i < mData.size(); i++) {
            InternalDnsData tmpData = mData.get(i);
            if (tmpData == null) {
                LogDns.e(TAG, "updateInternalData tmpData==null " + i);
                continue;
            }

            if (tmpData.isSameHead(newData, false)) {
                tmpData.setCountryData(newData.getCountryData());
                tmpData.setLookupData(newData.getLookupData());
                return;
            }
        }
        insertInternalData(newData, canClone);
    }

    private synchronized void insertInternalData(InternalDnsData newData, boolean canClone) {
        InternalDnsData tmpData = null;
        if (canClone)
            tmpData = (InternalDnsData) newData.clone();
        else
            tmpData = newData;

        if (tmpData.isLookupWithIso() && (tmpData.getGccCountryCode() == null)) {
            tmpData.setGccCountryCode(RadioDNSCountryCode.getGccCountryCode(tmpData.getPi(),
                    tmpData.getCountryCode()));
        } else if (!(tmpData.isLookupWithIso()) && (tmpData.getIsoCountryCode() == null)) {
            tmpData.setIsoCountryCode(RadioDNSCountryCode.getIsoCountryCode(tmpData
                    .getCountryCode()));
        }
        LogDns.v(TAG, "insertInternalData ok Data : " + tmpData);
        mData.add(tmpData);
    }
}
