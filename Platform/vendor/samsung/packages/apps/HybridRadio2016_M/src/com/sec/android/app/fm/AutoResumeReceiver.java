/**
 *
 */

package com.sec.android.app.fm;

import com.sec.android.app.fm.widget.FMRadioProvider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * @author vanrajvala
 */
public class AutoResumeReceiver extends BroadcastReceiver {
    public static final String ACTION_AUTO_ON = "com.app.fm.auto.on";
    public static final String ACTION_AUTO_OFF = "com.app.fm.auto.off";
    public static final String ACTION_AUTO_PLAYBACK = "com.samsung.app.fmradio.PLAYBACK_VIEWER";
    public static final String TAG = "AutoResumeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        if (intent.getAction() == null) {
            return;
        }

        FMNotificationManager notiMgr = FMNotificationManager.getInstance();
        if (intent.getAction().equals(ACTION_AUTO_ON)) {
            System.out.println("Auto resume on FM");
            Intent i = new Intent(FMRadioProvider.ACTION_RADIO_WIDGET_REFRESH);
            i.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.sendBroadcast(i);
            // if the current service is died then start it
            if (!notiMgr.isNotified()) {
                if (MainActivity._instance == null || !MainActivity._instance.isResumed()) {
                    String freq = intent.getStringExtra("freq");
                    Log.v(TAG, "Getting the frequency from intent:" + Log.filter(freq));
                    StringBuilder strBuilder = new StringBuilder();
                    strBuilder.append(freq);
                    if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
                        int index = freq.indexOf('.');
                        if (index > 0) {
                            String s = freq.substring(index);
                            if (s.length() == 2)
                                strBuilder.append('0');
                        }
                    }
                    strBuilder.append(" MHz");
                    notiMgr.showNotification(strBuilder.toString());
                }
            }
        } else if (intent.getAction().equals(ACTION_AUTO_OFF)) {
            Intent i = new Intent(FMRadioProvider.ACTION_RADIO_WIDGET_REFRESH);
            i.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            context.sendBroadcast(i);
            System.out.println("Auto resume off FM");
            if (notiMgr.isNotified()) {
                notiMgr.removeNotification(false);
            }
        } else if (intent.getAction().equals(ACTION_AUTO_PLAYBACK)) {
            Intent aintent = new Intent(context, MainActivity.class);
            aintent.setAction(Intent.ACTION_MAIN);
            aintent.putExtra("playback", true);
            aintent.addCategory(Intent.CATEGORY_LAUNCHER);
            aintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            aintent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(aintent);
        }
    }
}
