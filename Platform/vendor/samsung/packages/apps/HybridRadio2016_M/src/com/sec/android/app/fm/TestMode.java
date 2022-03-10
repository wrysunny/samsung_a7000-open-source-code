/**
 *
 */

package com.sec.android.app.fm;

import com.samsung.media.fmradio.FMPlayerException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SamsungAudioManager;

/**
 * @author vanrajvala
 */
public class TestMode {
    private static TestMode _instance = null;
    public static final String TEST_MODE_RADIO_ON_FREQ = "test.mode.radio.on.freq";
    public static final String TEST_MODE_RADIO_ON_FREQ_RES = "test.mode.radio.on.response";
    public static final String TEST_MODE_RADIO_OFF = "test.mode.radio.off";
    public static final String TEST_MODE_RADIO_OFF_RES = "test.mode.radio.off.response";
    public static final String TEST_MODE_SET_FREQ = "test.mode.radio.freq";
    public static final String TEST_MODE_SET_FREQ_RES = "test.mode.radio.freq.response";
    public static final String TEST_MODE_FACTORYRSSI = "test.mode.radio.factoryrssi";
    public static final String TEST_MODE_FACTORYRSSI_RES = "test.mode.radio.factoryrssi.response";
    public static final String TEST_MODE_OUTPUT = "output";
    public static final String TEST_MODE_EARPHONE = "earphone";
    public static final String TEST_MODE_SPEAKER = "speaker";
    public static final String TEST_MODE_FREQUENCY = "frequency";
    public static final String TEST_MODE_SIGNAL_STRENGTH = "signal_strength";

    private boolean mIsTestmodeOff = false;
    private TestMode(Context context) {
        mContext = context;
    }

    private Context mContext = null;

    public static synchronized TestMode getInstance(Context context) {
        if (_instance == null)
            _instance = new TestMode(context);
        return _instance;
    }

    private static final String TAG = "TestMode";

    public void handleIntent(Intent intent) {
        String action = null;
        if ((intent == null) || ((action = intent.getAction()) == null))
            return;
        Log.d(TAG, "action  : " + action);
        try {
            AudioManager audioManager = (AudioManager) mContext
                    .getSystemService(Context.AUDIO_SERVICE);
            RadioPlayer player = RadioPlayer.getInstance();
            if (audioManager == null) {
                Log.e(TAG, "AudioManager is null!!");
                return;
            }
            if (TEST_MODE_RADIO_ON_FREQ.equals(action)) {
                // on and set given freq and output to the given source
                String freq = intent.getStringExtra(TEST_MODE_FREQUENCY);
                String output = intent.getStringExtra(TEST_MODE_OUTPUT);

                // if there is no sound path defined
                if (output == null || output.trim().equals("")) {
                    Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ 1");
                    if (audioManager.isWiredHeadsetOn())
                        player.disableSpeaker();
                    else
                        player.enableSpeaker();
                    Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ 2");
                } else if (TEST_MODE_SPEAKER.equals(output)) {
                    Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ 3");
                    player.enableSpeaker();
                    Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ 4");
                } else if (TEST_MODE_EARPHONE.equals(output)) {
                    Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ 5");
                    player.disableSpeaker();
                    Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ 6");
                }

                audioManager.setStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO), 0, 0);
                // for test mode pass true
                Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ 7");
                player.turnOn(true);
                Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ 8");
                int volume = audioManager.getStreamMaxVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO));
                audioManager.setStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO), volume, 0);
                int val = Integer.parseInt(freq);
                Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ - freq:" + freq + ", val:" + val);
                Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ 9, val : " + val);
                player.tune(val/10);
                Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ 10");

                // send the broadcast
                Intent bintent = new Intent(TEST_MODE_RADIO_ON_FREQ_RES);
                MainActivity._instance.sendBroadcast(bintent);
                MainActivity._instance.setResult(Activity.RESULT_OK, intent);
                Log.d(TAG, "TEST_MODE_RADIO_ON_FREQ 11");

            } else if (TEST_MODE_RADIO_OFF.equals(action)) {
                // off the player
                mIsTestmodeOff = true;
                Log.d(TAG, "TEST_MODE_RADIO_OFF 1");
                player.turnOff();
                Log.d(TAG, "TEST_MODE_RADIO_OFF 2");
                player.disableSpeaker(); // disable speaker when turn off
                // send the broadcast
                //Intent bintent = new Intent(TEST_MODE_RADIO_OFF_RES);
                //MainActivity._instance.sendBroadcast(bintent);
                //MainActivity._instance.setResult(Activity.RESULT_OK, intent);
                Log.d(TAG, "TEST_MODE_RADIO_OFF 3");
            } else if (TEST_MODE_OUTPUT.equals(action)) {
                // toggle output
                Log.d(TAG, "TEST_MODE_OUTPUT 1");
                String output = intent.getStringExtra(TEST_MODE_OUTPUT);
                if (output == null || output.trim().equals("")) {
                    Log.d(TAG, "TEST_MODE_OUTPUT 2");
                    if (audioManager.isWiredHeadsetOn())
                        player.disableSpeaker();
                    else
                        player.enableSpeaker();
                    Log.d(TAG, "TEST_MODE_OUTPUT 3");
                } else if (TEST_MODE_SPEAKER.equals(output)) {
                    Log.d(TAG, "TEST_MODE_OUTPUT 4");
                    player.enableSpeaker();
                    Log.d(TAG, "TEST_MODE_OUTPUT 5");
                } else {
                    Log.d(TAG, "TEST_MODE_OUTPUT 6");
                    player.disableSpeaker();
                    Log.d(TAG, "TEST_MODE_OUTPUT 7");
                }

            } else if (TEST_MODE_SET_FREQ.equals(action)) {
                // tune to given frequncy return rssi
                Log.d(TAG, "TEST_MODE_SET_FREQ 1");
                String freq = intent.getStringExtra(TEST_MODE_FREQUENCY);
                String output = intent.getStringExtra(TEST_MODE_OUTPUT);
                int val = Integer.parseInt(freq);
                if (output == null || output.trim().equals("")) {
                    Log.d(TAG, "TEST_MODE_SET_FREQ 2");
                } else if (TEST_MODE_SPEAKER.equals(output)) {
                    Log.d(TAG, "TEST_MODE_SET_FREQ 3");
                    audioManager.setRadioSpeakerOn(true);
                } else {
                    Log.d(TAG, "TEST_MODE_SET_FREQ 4");
                    audioManager.setRadioSpeakerOn(false);
                }

                audioManager.setStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO), 0, 0);
                // for test mode pass true
                Log.d(TAG, "TEST_MODE_SET_FREQ 5");
                player.turnOn(true);
                Log.d(TAG, "TEST_MODE_SET_FREQ 6");
                int volume = audioManager.getStreamMaxVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO));
                audioManager.setStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO), volume, 0);

                Log.d(TAG, "TEST_MODE_SET_FREQ 7");
                player.tune(val/10);
                Log.d(TAG, "TEST_MODE_SET_FREQ 8");
                String rssi = player.getCurrentRssi() + "";
                Log.d(TAG, "TEST_MODE_SET_FREQ 9");
                // send the broadcast
                Intent bintent = new Intent(TEST_MODE_SET_FREQ_RES);
                bintent.putExtra(TEST_MODE_FREQUENCY, freq);
                bintent.putExtra(TEST_MODE_SIGNAL_STRENGTH, rssi);
                MainActivity._instance.sendBroadcast(bintent);
                Log.d(TAG, "Sending broadcast- freq:" + freq + " rssi:" + rssi);
                intent = new Intent();
                intent.putExtra(TEST_MODE_SIGNAL_STRENGTH, rssi);
                MainActivity._instance.setResult(Activity.RESULT_OK, intent);
                Log.d(TAG, "TEST_MODE_SET_FREQ 10");
            } else if (TEST_MODE_FACTORYRSSI.equals(action)) {
                Log.d(TAG, "TEST_MODE_SET_FACTORYRSSI 1");
                String factoryrssi = intent.getStringExtra(TEST_MODE_SIGNAL_STRENGTH);
                int rssi = 0;
                if (factoryrssi == null || factoryrssi.trim().equals("")) {
                    rssi = MainActivity._instance.GetFactoryRssi();
                    Log.d(TAG, "TEST_MODE_SET_FACTORYRSSI 2:: read rssi=" + rssi);
                    String read_rssi = Integer.toString(rssi, 10);
                    Intent bintent = new Intent(TEST_MODE_FACTORYRSSI_RES);
                    bintent.putExtra(TEST_MODE_SIGNAL_STRENGTH, read_rssi);
                    MainActivity._instance.sendBroadcast(bintent);
                    Log.d(TAG, "Sending broadcast- rssi:" + read_rssi);
                    intent = new Intent();
                    intent.putExtra(TEST_MODE_SIGNAL_STRENGTH, read_rssi);
                    MainActivity._instance.setResult(Activity.RESULT_OK, intent);
                    Log.d(TAG, "TEST_MODE_SET_FACTORYRSSI 3");
                } else {
                    rssi = Integer.parseInt(factoryrssi, 10);
                    if (rssi > 0)
                        rssi = 0 - rssi;
                    MainActivity._instance.SetFactoryRssi(rssi);
                    Log.d(TAG, "TEST_MODE_SET_FACTORYRSSI 4:: write rssi=" + rssi);
                }
            }
        } catch (FMPlayerException e) {
            e.printStackTrace();
        } finally {
            Log.v(TAG, "going for finish");
            MainActivity._instance.finish();
        }
    }

    public void sendOffIntent(){
        Log.d(TAG, "sendIntent!");
        if(mIsTestmodeOff){
                MainActivity._instance.sendBroadcast(new Intent(TEST_MODE_RADIO_OFF_RES));
                mIsTestmodeOff = false;
                Log.d(TAG, "sendBroadcast: FM Radio Off");
        }
    }
}
