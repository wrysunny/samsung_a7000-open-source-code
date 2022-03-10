package com.sec.android.app.fm;

import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.RadioDNSServiceDataIF;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

/**
 * This class is getting used when FM app sets the auto on-off option. After
 * receiving the time message from the AlarmManager this class takes care of
 * switching off FM player if its running.
 * 
 * @author vanrajvala
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "[onReceive - AlarmRec] ..");
        RadioPlayer player = RadioPlayer.getInstance();
        if (player.isOn()) {
            SharedPreferences preferences = context.getSharedPreferences(
                    SettingsActivity.PREF_FILE, Context.MODE_PRIVATE);
            int value = preferences.getInt(SettingsActivity.KEY_AUTO_ON_OFF, 0);
            String str = "";
            if (value == 0)
                return;
            else if (value == 1)
                str += context.getString(R.string.alrm_15_min_past, 15);
            else if (value == 2)
                str += context.getString(R.string.alrm_30_min_past, 30);
            else if (value == 3)
                str += context.getString(R.string.alrm_1_hr_past,
                        context.getString(R.string.app_name));
            else if (value == 4)
                str += context.getString(R.string.alrm_2_hrs_past,
                        context.getString(R.string.app_name), 2);

            FMNotificationManager notiMgr = FMNotificationManager.getInstance();
            notiMgr.removeNotification(false);

            if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                DNSService service = DNSService.getInstance();
                if (service != null) {
                    boolean playingInternetStreaming = RadioDNSServiceDataIF
                            .isEpgPlayingStreamRadio();
                    /*
                     * Because of noise when stream is stop. It's need more time
                     * to stop the media player. Sequence : media player off -
                     * fm radio off - internet stream mode off
                     */
                    if (playingInternetStreaming) {
                        try {
                            Thread.sleep(MainActivity.DELAY_WAITING_STREAM_STOPPED);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            boolean res = player.turnOff();
            if (res) {
                RadioToast.showToast(context, str, Toast.LENGTH_LONG);
                // make it OFF now
                Editor editor = preferences.edit();
                editor.putInt(SettingsActivity.KEY_AUTO_ON_OFF, 0);
                editor.commit();
            }
        }
    }
}
