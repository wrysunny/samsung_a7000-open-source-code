/**
 *
 */

package com.sec.android.app.fm;

import com.samsung.media.fmradio.FMPlayer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.media.AudioManager;
import android.media.SamsungAudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class IntennaTestActivity extends Activity implements OnClickListener {

    String LOG_TAG = "### IntennaTestActivity ###";

    public FMPlayer mPlayer;

    public AudioManager mAudioManager;

    private int MAX_REG_COUNT = 3;

    private TextView[] mTextLabel = new TextView[MAX_REG_COUNT];

    private EditText[] mEditReg = new EditText[MAX_REG_COUNT];

    private TextView mTextResult;

    String registerName[] = { "CurrentFreq(Mhz)", "FactoryRSSI(dBm)", "CurrentRSSI(dBm)" };

    int mTextViewId[] = { R.id.text_cur_freq, R.id.text_factory_rssi, R.id.text_cur_rssi };

    int mEditTextId[] = { R.id.cur_freq, R.id.factory_rssi, R.id.cur_rssi };

    private Button mOkButton, mCancelButton;

    private long test_frequency = 103000;

    private int factory_rssi;

    private int current_rssi = 0;

    private Handler handler = new Handler();

    public int GetFactoryRssi() {
        int factory_rssi = getSharedPreferences(SettingsActivity.PREF_FILE, MODE_PRIVATE).getInt(
                SettingsActivity.KEY_FACTORY_RSSI, SettingsActivity.FACTORY_RSSI);

        Log.d(LOG_TAG, "GetFactoryRssi :: rssi=" + factory_rssi);
        return factory_rssi;
    }

    public void SetFactoryRssi(int rssi) {
        Editor editor = getSharedPreferences(SettingsActivity.PREF_FILE,
                Context.MODE_PRIVATE).edit();

        editor.putInt(SettingsActivity.KEY_FACTORY_RSSI, rssi);
        editor.commit();
        Log.d(LOG_TAG, "SetFactoryRssi :: rssi=" + rssi);
    }

    private void initialize() {
        // init apply button
        mOkButton = (Button) findViewById(R.id.apply_done);
        mOkButton.setOnClickListener(this);
        mOkButton.setText("Apply");

        // init cancel button
        mCancelButton = (Button) findViewById(R.id.cancel_done);
        mCancelButton.setOnClickListener(this);
        mCancelButton.setText("Cancel");

        // init textview, edittext
        for (int i = 0; i < MAX_REG_COUNT; i++) {
            mTextLabel[i] = (TextView) findViewById(mTextViewId[i]);
            mTextLabel[i].setText(registerName[i]);
            mEditReg[i] = (EditText) findViewById(mEditTextId[i]);
        }

        // init textview for test result
        mTextResult = (TextView) findViewById(R.id.text_intenna_test);
        mTextResult.setText("Ready");
        mTextResult.setBackgroundColor(0xffffffff); // white

        mPlayer = (FMPlayer) getSystemService(Context.FM_RADIO_SERVICE);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        factory_rssi = GetFactoryRssi();
    }

    private void setFreq() {
        if (mPlayer == null)
            return;

        Log.d(LOG_TAG, "setFreq ");

        try {
            if (mPlayer.isOn() == false) {

                mPlayer.on();

                Log.d(LOG_TAG, "setFreq :: set volume 7 ");
                mAudioManager.setStreamVolume(SamsungAudioManager.STREAM_FM_RADIO, 7, 0);
                mPlayer.setSpeakerOn(true);
            }

            mPlayer.tune(test_frequency);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uiclear() {
        for (int i = 0; i < MAX_REG_COUNT; i++) {
            mEditReg[i].setText("");
        }
    }

    private void readThreshold(int index) {
        if (mPlayer == null)
            return;

        switch (index) {
        case 0:
            try {
                if (mPlayer.isOn()) {
                    long freq = mPlayer.getCurrentChannel();
                    Log.d(LOG_TAG, "Read : Frequency : " + freq);
                    mEditReg[index].setText(Integer.toString((int) freq, 10));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            break;
        case 1:
            Log.d(LOG_TAG, "Read : FactoryRSSI : " + factory_rssi);
            mEditReg[index].setText(Integer.toString((int) factory_rssi, 10));

            break;
        case 2:
            try {
                if (mPlayer.isOn()) {
                    current_rssi = (int) mPlayer.getCurrentRSSI();
                    Log.d(LOG_TAG, "Read : CurrentRSSI : " + current_rssi);
                    mEditReg[index].setText(Integer.toString(current_rssi, 10));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            break;
        default:
            break;
        }

    }

    private void writeThreshold(int index) {
        if (index >= MAX_REG_COUNT)
            return;

        mEditReg[index] = (EditText) findViewById(mEditTextId[index]);

        if (mEditReg[index].getText().length() == 0)
            return;

        switch (index) {
        case 0:
            break;
        case 1:
            String ret = mEditReg[index].getText().toString();
            int Value = Integer.parseInt(ret, 10);

            Log.d(LOG_TAG, "writeThreshold: FactoryRSSI=" + Value);
            factory_rssi = Value;

            SetFactoryRssi(factory_rssi);
            break;
        case 2:
            break;
        default:
            break;
        }
    }

    private void UpdateIntennaTest() {
        if (current_rssi >= factory_rssi) {
            Log.d(LOG_TAG, "checkIntennaRssi :: PASS!!!");
            mTextResult.setText("PASS");
            mTextResult.setBackgroundColor(0xff0000ff); // blue

        } else {
            Log.d(LOG_TAG, "checkIntennaRssi :: FAIL!!!");
            mTextResult.setText("FAIL");
            mTextResult.setBackgroundColor(0xffff0000); // red
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case (R.id.apply_done):
            writeThreshold(1);
            uiclear();
            readThreshold(0);
            readThreshold(1);

            Toast.makeText(this, "Apply Complete~!!", Toast.LENGTH_SHORT).show();
            break;
        case (R.id.cancel_done):
            uiclear();
            readThreshold(0);
            readThreshold(1);

            Toast.makeText(this, "Cancel~!!", Toast.LENGTH_SHORT).show();
            break;
        default:
            break;

        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.intenna_test);

        initialize();
        setFreq();

        uiclear();
        readThreshold(0);
        readThreshold(1);

        handler.removeCallbacks(doUpdate);
        handler.postDelayed(doUpdate, 8000); // 8 sec
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");

        handler.removeCallbacks(doUpdate);

        if (mPlayer == null)
            return;

        try {
            if (mPlayer.isOn() == true) {

                mPlayer.off();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Runnable doUpdate = new Runnable() {
        @Override
        public void run() {
            readThreshold(2);
            UpdateIntennaTest();
            handler.postDelayed(doUpdate, 1000); // 1 sec
        }
    };

}
