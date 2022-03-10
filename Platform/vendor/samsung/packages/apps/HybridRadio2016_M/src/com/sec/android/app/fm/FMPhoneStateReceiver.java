package com.sec.android.app.fm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class FMPhoneStateReceiver extends BroadcastReceiver {
    private static final String TAG = "FMPhoneStateReceiver";

    @Override
    public void onReceive(Context context, Intent arg1) {
        // TODO Auto-generated method stub
        FMPhoneStateListner mPhonelistner = new FMPhoneStateListner();
        TelephonyManager mTelephonyManager1 = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager1.listen(mPhonelistner, PhoneStateListener.LISTEN_CALL_STATE);

		/*
        if (SecProductFeature_RIL.SEC_PRODUCT_FEATURE_RIL_CALL_DUALMODE_CDMAGSM
                || SecProductFeature_COMMON.SEC_PRODUCT_FEATURE_COMMON_USE_MULTISIM) {
            TelephonyManager mTelephonyManager2 = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE_2);
            if (mTelephonyManager2 != null) {
                mTelephonyManager2.listen(mPhonelistner, PhoneStateListener.LISTEN_CALL_STATE);
            } else {
                Log.e(TAG, "mTelephonyManager2 is NULL!!");
            }
        }
        */
    }
}
