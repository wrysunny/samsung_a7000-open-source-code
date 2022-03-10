package com.sec.android.app.fm;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

public class FMPhoneStateListner extends PhoneStateListener {
    public void onCallStateChanged(int state, String incomingNumber) {
        switch (state) {
        case TelephonyManager.CALL_STATE_RINGING:
            if(MainActivity._instance != null && MainActivity.mIsRecording == true && MainActivity.showRecordingSavePopup)
                RadioToast.showToast(MainActivity._instance.getApplicationContext(),
                         R.string.recording_save_during_call, Toast.LENGTH_LONG);
            MainActivity.showRecordingSavePopup = false;
             break;
        case TelephonyManager.CALL_STATE_OFFHOOK:
             FMNotificationManager  mFmNotiMgr = FMNotificationManager.getInstance();
             if (mFmNotiMgr != null && mFmNotiMgr.isNotified()){
                 mFmNotiMgr.removeNotification(true);
             }
             break;
        case TelephonyManager.CALL_STATE_IDLE:
             MainActivity.showRecordingSavePopup = true;
             break;
        default:
             break;
        }
    }
}
