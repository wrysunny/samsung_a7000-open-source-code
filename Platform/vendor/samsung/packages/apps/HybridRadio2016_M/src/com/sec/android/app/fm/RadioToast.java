package com.sec.android.app.fm;

import com.samsung.media.fmradio.FMPlayerException;

import android.content.Context;
import android.widget.Toast;

public class RadioToast {
    private static int sResId = 0;
    private static String sText = null;
    private static Toast sToast = null;
    private static final String TAG = "RadioToast";

    public static void cancelToast() {
        if (sToast != null) {
            sToast.cancel();
        }
    }

    public static void showToast(Context context, FMPlayerException e) {
        if (context == null) {
            Log.e(TAG, e.toString());
            return;
        }
        boolean knownError = true;
        String text = null;
        switch (e.getCode()) {
        case FMPlayerException.PLAYER_IS_NOT_ON:
            text = context.getString(R.string.toast_fm_not_played,
                    context.getString(R.string.app_name));
            break;
        case FMPlayerException.HEAD_SET_IS_NOT_PLUGGED:
            text = context.getString(R.string.toast_earphone_not_connected);
            break;
        case FMPlayerException.AIRPLANE_MODE:
            text = context.getString(R.string.toast_airplane_mode,
                    context.getString(R.string.app_name));
            break;
        case FMPlayerException.BATTERY_LOW:
            text = context.getString(R.string.toast_battery_low);
            break;
        case FMPlayerException.TV_OUT_PLUGGED:
            text = context.getString(R.string.app_name) + " - "
                    + context.getString(R.string.toast_unavailable_in_tvout_mode);
            break;
        default:
            knownError = false;
            Log.e(TAG, e.toString());
            break;
        }
        if (knownError) {
            showToast(context, text, Toast.LENGTH_SHORT);
        }
    }

    public static void showToast(Context context, int resId, int duration) {
        if (sToast != null && sResId == resId) {
            sToast.cancel();
            sToast = null;
        }
        sResId = resId;
        sText = null;
        sToast = Toast.makeText(context, resId, duration);
        sToast.show();
    }

    public static void showToast(Context context, String text, int duration) {
        if (sToast != null && sText != null && sText.equals(text)) {
            sToast.cancel();
            sToast = null;
        }
        sResId = 0;
        sText = text;
        sToast = Toast.makeText(context, text, duration);
        sToast.show();
    }
}
