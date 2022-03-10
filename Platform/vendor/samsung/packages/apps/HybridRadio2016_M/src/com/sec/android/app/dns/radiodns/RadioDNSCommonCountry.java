package com.sec.android.app.dns.radiodns;

import android.content.Context;

import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.ui.DnsKoreaTestActivity;
import com.sec.android.app.fm.util.SystemPropertiesWrapper;

/**
 * @author Hyejung Kim(hyejung8.kim@samsung.com)
 * 
 */
public class RadioDNSCommonCountry {
    private static final String TAG = "RadioDNSCommonCountry";
    private String mIsoCountryCode = null;

    public String findCountry(Context context) {
        findCountryWithMcc();

        if (mIsoCountryCode == null)
            findCountryWithOthers(context);

        return mIsoCountryCode;
    }

    private void findCountryWithMcc() {
        if (DnsKoreaTestActivity.isKoreaTest() || DnsKoreaTestActivity.KOREA_TEST_WITHOUT_MCC)
            mIsoCountryCode = DnsKoreaTestActivity.getCountryCode();
        else
            mIsoCountryCode = SystemPropertiesWrapper.getInstance().get("gsm.operator.iso-country");

        LogDns.v(TAG, "Mcc country : " + LogDns.filter(mIsoCountryCode));
    }

    private void findCountryWithOthers(Context context) {
        RadioDnsLocationFinder ccLf = RadioDnsLocationFinder.getInstance();
        ccLf.setContext(context);

        if (mIsoCountryCode == null)
            mIsoCountryCode = ccLf.findPosition();

        LogDns.v(TAG, "makeCountryGps : " + LogDns.filter(mIsoCountryCode));
    }
}
