package com.sec.android.app.fm;

import com.sec.android.app.SecProductFeature_FMRADIO;

import android.content.BroadcastReceiver;import android.content.Context;import android.content.Intent;

public class SecretCodeReceiver extends BroadcastReceiver {

    private static final String SECRET_CODE_ACTION = "android.provider.Telephony.SECRET_CODE";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (SECRET_CODE_ACTION.equals(intent.getAction())) {
            System.out.println("FM [SecretCodeReceiver] is getting control:" + intent.getAction());
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            String host = intent.getData().getHost();
            if (host != null && host.equals("0368")) {
                if(SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_QCOM)){
                    i.setClass(context, HwTunningActivity_Qcom.class);
                } else if(SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_RICHWAVE)){
                	i.setClass(context, HwTunningActivity_Richwave.class);
                } else {
                    i.setClass(context, HwTunningActivity.class);
                }
            }
            context.startActivity(i);
        }
    }
}
