package com.sec.android.app.dns.radiodns;

import java.util.Stack;

import org.radiodns.*;

import com.sec.android.app.dns.*;
import com.sec.android.app.dns.data.DnsCache;
import com.sec.android.app.dns.data.InternalDnsData;
import com.sec.android.app.dns.data.RadioDNSData;
import com.sec.android.app.dns.data.SrvRecord;
import com.sec.android.app.dns.ui.DnsKoreaTestActivity;
import com.sec.android.app.fm.util.NetworkMonitorUtil;
import com.sec.android.app.fm.util.SystemPropertiesWrapper;

import android.os.AsyncTask;
//import android.os.Handler;
import android.os.Message;

/**
 * @author Hyejung Kim(hyejung8.kim@samsung.com)
 * 
 */

public class RadioDNSConnection {
    private static RadioDNSConnection sInstance = null;
    private DNSService mDnsService = null;
    private static final String TAG = "RadioDNSConnection";

    DNSLookUpTask lookupTask;
    Stack<RadioDNSData> currentStack, viewStack;
    public static final int CURRENT_HIGH = 1; // 1 : currentStack is high
    public static final int VIEW_HIGH = 2; // 2 : viewStack is high

    public static final int CURRENT = 1;
    public static final int VIEW = 2;

    private int stackPriority = CURRENT_HIGH;

    // public static final int COUNTRYUPDATED = 0;
    public static final int NORESULT = 0;
    public static final int UPDATED = 1;

    private RadioDNSConnection(DNSService dnsService) {
        mDnsService = dnsService;
        lookupTask = new DNSLookUpTask();

        LogDns.v(TAG, "lookupTask status : " + lookupTask.getStatus() + " Finished : "
                + AsyncTask.Status.FINISHED);
    }

    /**
     * Make SingleTone class
     */
    public synchronized static RadioDNSConnection getInstance(DNSService dnsService) {
        if (sInstance == null)
            sInstance = new RadioDNSConnection(dnsService);

        return sInstance;
    }

    public void initialize() {
        LogDns.v(TAG, "initialize : " + SystemPropertiesWrapper.getInstance().get("gsm.operator.iso-country"));
        currentStack = new Stack<RadioDNSData>();
    }

    public void destroy() {
        LogDns.v(TAG, "destroy");
        stopDnsConnection(false);

        sInstance = null;
        mDnsService = null;
    }

    public int getStackPriority() {
        return stackPriority;
    }

    public void setStackPriority(int priority) {
        stackPriority = priority;
        viewStack = new Stack<RadioDNSData>();
    }

    // stop DNS lookup thread, country thread
    public void stopDnsConnection(boolean currentOnly) {
        // cc.stopCountryCorrection();
        if ((lookupTask != null)
                && (!currentOnly || (currentOnly && (lookupTask.getPriority() == CURRENT)))) {
            if (lookupTask.getStatus() == AsyncTask.Status.RUNNING)
                lookupTask.cancel(true);
        }

        DNSService.getEventHandler().removeMessages(DNSEvent.CURRENT_DATA_UPDATED);
    }

    public AsyncTask.Status getStatus() {
        return lookupTask.getStatus();
    }

    private boolean canUseLookupTask() {
        LogDns.v(TAG, "canUseLookupTask() status : " + lookupTask.getStatus());

        if (lookupTask.getStatus() == AsyncTask.Status.FINISHED) {
            lookupTask = new DNSLookUpTask();
        }

        LogDns.v(TAG, "canUseLookupTask() status : " + lookupTask.getStatus());

        if (lookupTask.getStatus() == AsyncTask.Status.PENDING)
            return true;
        return false;
    }

    private boolean setCountryCode(RadioDNSData dnsData, boolean isRetryCc) {
        if (!isRetryCc && (dnsData.getCountryCode() != null)) {
            LogDns.v(TAG, "CC request is ignored");
            return true;
        }

        String countryCode = mDnsService.getIsoCountryCode();

        if (!DnsKoreaTestActivity.isKoreaTest()) {
            if (dnsData.getPi() != null) {
                RadioDNSCountryCode cc = new RadioDNSCountryCode(mDnsService);

                if ((dnsData.getEcc() != 0) && !isRetryCc) {
                    countryCode = cc.makeCountry_PiEcc(dnsData.getPi(), dnsData.getEcc());
                } else {
                    cc.makeNibble(dnsData.getPi()); // for country
                    // correction
                    if (cc.isCountryCorrectionNeeded(dnsData))
                        return false;
                }
            }
        } else {
            countryCode = DnsKoreaTestActivity.getCountryCode();
        }

        dnsData.setCountryCode(countryCode);
        return true;
    }

    private void sendMsg(int priority, RadioDNSData prevData, RadioDNSData updateData) {
        Message msg = Message.obtain();
        boolean isVisUpdated = false;
        boolean isEpgUpdated = false;

        SrvRecord updateRecord = null;

        if (priority == CURRENT) {
            updateRecord = updateData.getVisStompRecord();

            if ((updateRecord != null) && !(updateRecord.equals(prevData.getVisStompRecord()))) {
                isVisUpdated = true;
            }

            updateRecord = updateData.getVisHttpRecord();
            if ((updateRecord != null) && !(updateRecord.equals(prevData.getVisHttpRecord()))) {
                isVisUpdated = true;
            }
        }

        updateRecord = updateData.getEpgRecord();
        if ((updateRecord != null) && !(updateRecord.equals(prevData.getEpgRecord()))) {
            isEpgUpdated = true;
        }

        LogDns.v(TAG, "sendMsg() dnsData : " + updateData);

        switch (priority) {
        case CURRENT: {
            if (isVisUpdated && isEpgUpdated) {
                msg.what = DNSEvent.CURRENT_DATA_UPDATED;
                msg.arg1 = DNSEvent.ALL_CURRENT_DATA_UPDATED;
            } else if (isVisUpdated) {
                msg.what = DNSEvent.CURRENT_DATA_UPDATED;
                msg.arg1 = DNSEvent.VIS_CURRENT_DATA_UPDATED;
            } else if (isEpgUpdated) {
                msg.what = DNSEvent.CURRENT_DATA_UPDATED;
                msg.arg1 = DNSEvent.EPG_CURRENT_DATA_UPDATED;
            } else {
                LogDns.e(TAG, "Cached data or internal data has error!!");
                return;
            }

            if ((mDnsService != null)
                    && (updateData.canReplaceWith(mDnsService.getCurrentData(), true))) {
                mDnsService.setCurrentData(updateData);
                DNSService.getEventHandler().sendMessage(msg);
            }
        }
            break;

        case VIEW: {
            msg.what = DNSEvent.VIEW_DATA_UPDATED;
            msg.obj = updateData;
            msg.arg1 = isEpgUpdated ? UPDATED : NORESULT;

            DNSService.getEventHandler().sendMessage(msg);
        }
            break;
        }
    }

    public static boolean isTtlExpired(long ttl) {
        if (ttl == InternalDnsData.EMPTY_NUMBER)
            return false;

        long current = (System.currentTimeMillis()) / 1000;
        if (current > ttl) {
            LogDns.d(TAG, "TTL is expired " + current + " " + ttl);
            return true;
        }
        return false;
    }

    private void checkUpdate(InternalDnsData internalData, int priority) {
        if (isTtlExpired(internalData.getCnameTTL()) || isTtlExpired(internalData.getEpgTTL())) {
            internalData.setNeedUpdate(true);
        }

        if ((priority == CURRENT)
                && (isTtlExpired(internalData.getVisStompTTL()) || isTtlExpired(internalData
                        .getVisHttpTTL()))) {
            internalData.setNeedUpdate(true);
        }
    }

    private RadioDNSData checkInternalData(RadioDNSData dnsData, int priority) {
        RadioDNSData internalData = (RadioDNSData) DnsInternalDataManager.getInstance()
                .getCnameInternalData(dnsData);

        if (internalData == null) {
            LogDns.v(TAG, "checkInternalData() is null");
            return null;
        }
        checkUpdate(internalData, priority);

        if (priority == CURRENT)
            internalData.setTimestamp();

        LogDns.v(TAG, "checkInternalData() " + internalData);
        sendMsg(priority, dnsData, (RadioDNSData) internalData.clone());

        return internalData;
    }

    private RadioDNSData checkCache(RadioDNSData dnsData, int priority) {
        DnsCache cache = DnsCacheManager.getInstance().getCache(dnsData.getPi(),
                dnsData.getFrequency());
        InternalDnsData cacheCommonData = null;

        if ((cache == null) || ((cacheCommonData = cache.getDnsData()) == null)) {
            LogDns.v(TAG, "checkCache() is null");
            return null;
        }

        RadioDNSData cacheData = new RadioDNSData(dnsData.getFrequency(), dnsData.getPi(), null,
                dnsData.getTimestamp());

        cacheData.setCountryData(cacheCommonData.getCountryData());
        cacheData.setNeedUpdate(true);
        cacheData.setLookupData(cacheCommonData.getLookupData());

        sendMsg(priority, dnsData, cacheData);

        DnsInternalDataManager.getInstance().updateInternalData(cacheData, true);

        LogDns.v(TAG, "checkCache() : " + cacheData);

        return cacheData;
    }

    public void doDnsLookUp(RadioDNSData dnsRawData, boolean isRetryCc, int priority) {
        if (!NetworkMonitorUtil.isConnected(mDnsService)) {
            LogDns.e(TAG, "Network is not available.");
            dnsRawData.setNeedLookupRetry(true);
            return;
        }
        String baseCountry = mDnsService.getIsoCountryCode();
        if ((baseCountry == null) || (baseCountry.length() == 0)) {
            LogDns.e(TAG, "doDnsLookUp() baseCountry == null");
            return;
        }

        RadioDNSData dnsData = (RadioDNSData) dnsRawData.clone();
        dnsData.setBaseCountryCode(baseCountry);

        if (priority == CURRENT) {
            dnsData.setTimestamp();
        }

        RadioDNSData backupData = checkInternalData(dnsData, priority);
        if (backupData == null)
            backupData = checkCache(dnsData, priority);

        if (backupData != null)
            dnsData = backupData;

        if ((backupData == null) || (priority == CURRENT)) {
            if (setCountryCode(dnsData, isRetryCc)) {
                if ((stackPriority != priority) || !doInternalDnsLookUp(dnsData, priority)) {
                    LogDns.v(TAG, "doDNSLookUp() lookupTask cannot be executed. StackPriority :  "
                            + stackPriority + " priority : " + priority + " dnsData : " + dnsData);
                    if (priority == VIEW)
                        viewStack.push(dnsData);
                    else
                        currentStack.push(dnsData);
                }
            }
            // else {
            // dnsData.setNeedCcCorrection(true);
            //
            // LogDns.v(TAG, "doDNSLookUp() country correction is needed\n" +
            // dnsData);
            // if (priority == VIEW)
            // viewStack.push(dnsData);
            // else
            // currentStack.push(dnsData);
            // }
        }
    }

    // it's not checking stack
    private synchronized boolean doInternalDnsLookUp(RadioDNSData dnsData, int priority) {
        if (!NetworkMonitorUtil.isConnected(mDnsService)) {
            LogDns.e(TAG, "Network is not available.");
            dnsData.setNeedLookupRetry(true);
            return false;
        }

        if ((dnsData == null) || (dnsData.getFrequency() == null)
                || (dnsData.getFrequency().length() == 0)) {
            LogDns.e(TAG, "dnsData or frequency is null. " + dnsData);
            return false;
        }

        if (canUseLookupTask()) {
            lookupTask.setData(dnsData, priority);

            if (priority == VIEW) {
                lookupTask.execute(DNSLookUpTask.CNAMEEPGSRVRECORD);
            } else {
                lookupTask.execute(DNSLookUpTask.FULL);
            }
            return true;
        } else {
            LogDns.v(TAG, "doInternalDnsLookUp() lookupTask cannot be executed" + dnsData);
        }
        return false;
    }

    // private Handler mEventHandler = new Handler() {
    // public void handleMessage(Message msg) {
    // switch (msg.what) {
    // case COUNTRYUPDATED: {
    // RadioDNSData dnsData = (RadioDNSData) msg.obj;
    // DnsInternalDataManager.getInstance().updateInternalData(dnsData, true);
    // LogDns.v(TAG, "COUNTRYUPDATED lookupTask's status : " +
    // lookupTask.getStatus());
    //
    // checkStack();
    // }
    // break;
    // default:
    // break;
    // }
    // }
    // };

    // public Handler getEventHandler() {
    // return mEventHandler;
    // }

    // if stack is not empty, do lookup for next top element
    public void checkStack() {
        if (stackPriority == CURRENT_HIGH) {
            if (!findNextData(currentStack, CURRENT, false))
                findNextData(viewStack, VIEW, true);
        } else {
            if (!findNextData(viewStack, VIEW, false))
                findNextData(currentStack, CURRENT, true);
        }
    }

    private boolean findNextData(Stack<RadioDNSData> stack, int priority,
            boolean checkUpdateNeededData) {
        if (stack == null) {
            LogDns.v(TAG, "findNextData() - stack is null");
            return false;
        }

        if (mDnsService == null) {
            LogDns.e(TAG, "findNextData() - mDnsService is null");
            return false;
        }

        RadioDNSData tmpData = null;
        InternalDnsData internalData = null;

        while (!(stack.isEmpty())) {
            tmpData = stack.peek();
            if (tmpData == null) {
                LogDns.e(TAG, "findNextData() - stack data is null ");
                stack.pop();
                continue;
            }

            if ((priority == CURRENT)
                    && !(tmpData.canReplaceWith(mDnsService.getCurrentData(), false))) {
                LogDns.v(TAG, "findNextData() - old current data : " + tmpData);
                stack.pop();
                continue;
            }

            internalData = DnsInternalDataManager.getInstance().getCnameInternalData(tmpData);

            if (internalData != null) {
                stack.pop();
            } else {
                if (tmpData.isNeedCcCorrection()) {
                    LogDns.e(TAG,
                            "findNextData() - Need to wait until Country Correction is finished");
                } else {
                    doInternalDnsLookUp(tmpData, priority);
                    return true;
                }
                break;
            }
        }

        if (checkUpdateNeededData) {
            internalData = DnsInternalDataManager.getInstance().getNeedUpdateInternalData();

            if (internalData != null)
                doInternalDnsLookUp((RadioDNSData) internalData, VIEW);
        }
        return false;
    }

    private boolean isTopSameWith(Stack<RadioDNSData> stack, RadioDNSData compare) {
        if ((compare == null) || (stack == null)) {
            LogDns.e(TAG, "isTopSameWith() - input data cannot be used " + stack + " " + compare);
            return false;
        }
        RadioDNSData topData = null;

        while (!(stack.isEmpty())) {
            topData = (RadioDNSData) stack.peek();

            if (topData != null) {
                if (topData.isSame(compare)) {
                    return true;
                } else {
                    LogDns.d(TAG, "isTopSameWith() - DNSData is different or has some problem "
                            + topData);
                    break;
                }
            } else {
                LogDns.e(TAG, "isTopSameWith() - stack top data is null");
                stack.pop();
            }
        }

        return false;
    }

    private class DNSLookUpTask extends AsyncTask<Integer, Void, Void> {
        // int freq = 95800;
        // String piCode = "c479";
        // String countryCode = "gb"; // "ce1"
        private static final String TAG = "DNSLookUpTask";

        final static int FULL = 1;
        final static int CNAMEEPGSRVRECORD = 2;

        final static String VIS_STOMP_APP_ID = "radiovis";
        final static String VIS_HTTP_APP_ID = "radiovis-http";
        final static String EPG_APP_ID = "radioepg";

        final static int MAX_RETRY = 5;

        RadioDNSData mDnsData = null;

        boolean isVisUpdated = false;
        boolean isEpgUpdated = false;

        boolean canInputError = false;

        int priority = CURRENT;
        int currentMode = FULL;

        RadioDNS rdns = null;
        Service service = null;

        DNSLookUpTask() {
            super();

            rdns = new RadioDNS("8.8.8.8");
        }

        public void setData(RadioDNSData dnsData, int priority) {
            LogDns.v(TAG, "setData : " + dnsData);
            this.mDnsData = dnsData;
            this.priority = priority;

            isVisUpdated = false;
            isEpgUpdated = false;
        }

        public int getPriority() {
            return priority;
        }

        private long getTtl(long ttl) {
            long current = (System.currentTimeMillis()) / 1000;
            return (current + ttl);
        }

        private void getRadioDNSData() {
            if (getCname()) {
                getVisStompRecord();
                getVisHttpRecord();
                getEpgRecord();
                mDnsData.setNeedUpdate(false);
                mDnsData.increaseRetryCount();
            } else {
                // maybe input error
                LogDns.v(TAG, "getCNAME() has error or cname is null");
                if (!mDnsData.isNeedLookupRetry())
                    canInputError = true;
            }
        }

        // private void getViewData() {
        // if (getCname()) {
        // getEpgRecord();
        // mDnsData.setNeedUpdate(false);
        // } else {
        // // maybe input error
        // LogDns.v(TAG, "getCNAME() has error or cname is null");
        // if (!mDnsData.isNeedLookupRetry())
        // canInputError = true;
        // }
        // }

        private boolean getCname() {
            try {
                int i = MAX_RETRY;
                String cname = null;
                do {
                    LogDns.v(TAG, "getCNAME() try #" + i);
                    cname = service.getAuthoritativeFqdn();
                    --i;
                } while ((i > 0)
                        && ((service.getCname_result() == 2) || (service.getCname_result() == 3)));

                if (cname != null) {
                    mDnsData.setCname(cname);
                    mDnsData.setCnameTTL(getTtl(service.getCname_ttl()));
                }

                if ((service.getCname_result() == 2) || (service.getCname_result() == 3)) {// Lookup.TRY_AGAIN
                                                                                           // or
                                                                                           // Lookup.HOST_NOT_FOUND
                                                                                           // LogDns.v(TAG,
                                                                                           // "setNeedRetry getCNAME() #2: true");
                    mDnsData.setNeedLookupRetry(true);
                    mDnsData.increaseRetryCount();
                    return false;
                } else {
                    // LogDns.v(TAG, "setNeedRetry getCNAME() #3: false");
                    mDnsData.setNeedLookupRetry(false);
                    if (cname == null)
                        return false;
                }
            } catch (LookupException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        private boolean getVisStompRecord() {
            try {
                int i = MAX_RETRY;
                Application application = null;

                do {
                    LogDns.v(TAG, "getVisStompRecord() try #" + i);
                    application = service.getApplication(mDnsData.getCname(), VIS_STOMP_APP_ID);
                    --i;
                } while ((i > 0)
                        && ((service.getSrvRecord_result() == 2) || (service.getSrvRecord_result() == 3)));

                boolean lookupRetry = false;

                // Lookup.TRY_AGAIN or Lookup.HOST_NOT_FOUND
                if ((service.getSrvRecord_result() == 2) || (service.getSrvRecord_result() == 3)) {
                    lookupRetry = true;
                }
                mDnsData.setNeedLookupRetry(lookupRetry || mDnsData.isNeedLookupRetry());

                if (application != null) {
                    Record record = application.getRecords().get(0);
                    int port = record.getPort();
                    String host = record.getTarget().toString();

                    if ((host != null) && (host.endsWith("."))) {
                        host = host.substring(0, host.length() - 1);
                    }

                    SrvRecord visStompRecord = new SrvRecord(host, port,
                            getTtl(service.getSrvRecord_ttl()));

                    if (!(visStompRecord.equals(mDnsData.getVisStompRecord()))) {
                        isVisUpdated = true;
                    }
                    mDnsData.setVisStompRecord(visStompRecord);

                    LogDns.v(TAG, String.format("Host: %s Port: %d Priority: %d Weight: %d",
                            record.getTarget(), record.getPort(), record.getPriority(),
                            record.getWeight()));

                    if (host != null)
                        return true;
                } else {
                    LogDns.v(TAG, "No results for RadioVisStomp");
                }
            } catch (LookupException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }

        private boolean getVisHttpRecord() {
            try {
                int i = MAX_RETRY;
                Application application = null;

                do {
                    LogDns.v(TAG, "getVisHttpRecord() try #" + i);
                    application = service.getApplication(mDnsData.getCname(), VIS_HTTP_APP_ID);
                    --i;
                } while ((i > 0)
                        && ((service.getSrvRecord_result() == 2) || (service.getSrvRecord_result() == 3)));

                boolean lookupRetry = false;
                // Lookup.TRY_AGAIN or Lookup.HOST_NOT_FOUND
                if ((service.getSrvRecord_result() == 2) || (service.getSrvRecord_result() == 3)) {
                    lookupRetry = true;
                }
                mDnsData.setNeedLookupRetry(lookupRetry || mDnsData.isNeedLookupRetry());

                if (application != null) {
                    Record record = application.getRecords().get(0);
                    int port = record.getPort();
                    String host = record.getTarget().toString();

                    if ((host != null) && (host.endsWith("."))) {
                        host = host.substring(0, host.length() - 1);
                    }

                    SrvRecord visHttpRecord = new SrvRecord(host, port,
                            getTtl(service.getSrvRecord_ttl()));

                    if (!(visHttpRecord.equals(mDnsData.getVisHttpRecord()))) {
                        isVisUpdated = true;
                    }
                    mDnsData.setVisHttpRecord(visHttpRecord);

                    LogDns.v(TAG, String.format("Host: %s Port: %d Priority: %d Weight: %d",
                            record.getTarget(), record.getPort(), record.getPriority(),
                            record.getWeight()));

                    if (host != null)
                        return true;
                } else {
                    LogDns.v(TAG, "No results for RadioVisHttp");
                }
            } catch (LookupException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return false;
        }

        private boolean getEpgRecord() {
            try {
                int i = MAX_RETRY;
                Application application = null;

                do {
                    LogDns.v(TAG, this.hashCode() + " getEpgRecord() try #" + i);
                    application = service.getApplication(mDnsData.getCname(), EPG_APP_ID);
                    --i;
                } while ((i > 0)
                        && ((service.getSrvRecord_result() == 2) || (service.getSrvRecord_result() == 3)));
                boolean lookupRetry = false;
                // Lookup.TRY_AGAIN or Lookup.HOST_NOT_FOUND
                if ((service.getSrvRecord_result() == 2) || (service.getSrvRecord_result() == 3)) {
                    lookupRetry = true;
                }
                mDnsData.setNeedLookupRetry(lookupRetry || mDnsData.isNeedLookupRetry());

                if (application != null) {
                    Record record = application.getRecords().get(0);
                    int port = record.getPort();
                    String host = record.getTarget().toString();

                    if ((host != null) && (host.endsWith("."))) {
                        host = host.substring(0, host.length() - 1);
                    }

                    SrvRecord epgRecord = new SrvRecord(host, port,
                            getTtl(service.getSrvRecord_ttl()));

                    if (!(epgRecord.equals(mDnsData.getEpgRecord()))) {
                        isEpgUpdated = true;
                    }
                    mDnsData.setEpgRecord(epgRecord);

                    LogDns.v(TAG, String.format("Host: %s Port: %d Priority: %d Weight: %d",
                            record.getTarget(), record.getPort(), record.getPriority(),
                            record.getWeight()));

                    if (host != null)
                        return true;

                } else {
                    LogDns.v(TAG, "No results for RadioEPG");
                }
            } catch (LookupException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected Void doInBackground(Integer... arg0) {
            currentMode = arg0[0];
            canInputError = false;

            try {
                if ((mDnsData == null) || (mDnsData.getFrequency() == null)
                        || (mDnsData.getFrequency().length() == 0)) {
                    LogDns.e(TAG, "dnsData or frequency is null! need to Check!!" + mDnsData);

                    return null;
                }
                service = rdns.lookupFMService(mDnsData.getCountryCode(), mDnsData.getPi(),
                        Integer.parseInt(mDnsData.getFrequency()));
            } catch (LookupException e) {
                e.printStackTrace();

                return null;
            }

            getRadioDNSData();

            // switch (currentMode) {
            // case FULL:
            // getRadioDNSData();
            // break;
            // case CNAMEEPGSRVRECORD:
            // getViewData();
            // break;
            // }

            return null;
        }

        private boolean retryWithIso() {
            if (canInputError) {
                if (mDnsData.getCountryCode().length() == 3) { // PI+ECC
                                                               // (CE1)
                    LogDns.v(TAG, "Retry with PI only country code");
                    setCountryCode(mDnsData, true);
                    doInternalDnsLookUp(mDnsData, priority);
                    return true;
                }
            }
            return false;
        }

        private void updateInternalData() {
            if ((mDnsData != null)
                    && ((mDnsData.getCnameTTL() != RadioDNSData.EMPTY_NUMBER)
                            || (mDnsData.getVisStompTTL() != RadioDNSData.EMPTY_NUMBER) || (mDnsData
                            .getEpgTTL() != RadioDNSData.EMPTY_NUMBER))) {
                DnsInternalDataManager.getInstance().updateInternalData(mDnsData, true);
            } else {
                LogDns.v(TAG, "cannot save internal data " + mDnsData);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            LogDns.v(TAG, "onPostExecute() dnsData : " + mDnsData);

            Message msg = Message.obtain();

            switch (currentMode) {
            case FULL: {
                if (isVisUpdated && isEpgUpdated) {
                    msg.what = DNSEvent.CURRENT_DATA_UPDATED;
                    msg.arg1 = DNSEvent.ALL_CURRENT_DATA_UPDATED;
                } else if (isVisUpdated) {
                    msg.what = DNSEvent.CURRENT_DATA_UPDATED;
                    msg.arg1 = DNSEvent.VIS_CURRENT_DATA_UPDATED;
                } else if (isEpgUpdated) {
                    msg.what = DNSEvent.CURRENT_DATA_UPDATED;
                    msg.arg1 = DNSEvent.EPG_CURRENT_DATA_UPDATED;
                } else {
                    if (retryWithIso())
                        return;

                    // for update after Internet connection
                    // Retry "true" is not updated if VIS or EPG is not filled
                    // in DNSData
                    if ((mDnsService != null) && (mDnsData.isSame(mDnsService.getCurrentData()))) {
                        mDnsService.setCurrentData(mDnsData);
                    } else {
                        LogDns.v(
                                TAG,
                                "Lookup network connection error flag is not updated because the data is not match with current DNS data "
                                        + mDnsData);
                    }
                    LogDns.v(TAG, "DNS is not supported or Nothing is changed");

                    if (isTopSameWith(currentStack, mDnsData)) {
                        currentStack.pop();
                    }
                    checkStack();
                    return;
                }

                if ((mDnsService != null)
                        && (mDnsData.canReplaceWith(mDnsService.getCurrentData(), true))) {
                    mDnsService.setCurrentData(mDnsData);
                    DNSService.getEventHandler().sendMessage(msg);
                }

                updateInternalData();
                if (isTopSameWith(currentStack, mDnsData)) {
                    currentStack.pop();
                }
                checkStack();
            }
                break;

            case CNAMEEPGSRVRECORD: {
                if (retryWithIso())
                    return;

                msg.what = DNSEvent.VIEW_DATA_UPDATED;
                msg.obj = mDnsData;
                msg.arg1 = isEpgUpdated ? UPDATED : NORESULT;

                DNSService.getEventHandler().sendMessage(msg);

                if (isEpgUpdated)
                    updateInternalData();

                if (isTopSameWith(viewStack, mDnsData)) {
                    viewStack.pop();
                }
                checkStack();
            }
                break;

            default:
                LogDns.v(TAG, "onPostExecute() Error");
                break;
            }

            super.onPostExecute(result);
        }

        @Override
        protected void onCancelled(Void result) {
            super.onCancelled(result);

            if ((mDnsService != null)
                    && (mDnsData.canReplaceWith(mDnsService.getCurrentData(), false))) {
                LogDns.v(TAG, "cancelled by network disconnection and setCurrentData");
                mDnsService.getCurrentData().setNeedLookupRetry(mDnsData.isNeedLookupRetry());
                mDnsService.getCurrentData().setNeedUpdate(mDnsData.needUpdate());
            }

            LogDns.v(TAG, "onCancelled()");
        }
    }
}
