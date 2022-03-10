package com.sec.android.app.fm;

import com.samsung.media.fmradio.FMEventListener;
import com.samsung.media.fmradio.FMPlayer;
import com.samsung.media.fmradio.FMPlayerException;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SamsungAudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 2011.10.18 TOD_AHS : RSSI test.
 */
public class RssiTestActivity extends Activity {
    private TextView mTextCurrent;
    private EditText mEditRssi;
    private EditText mEditStartFreq;
    private EditText mEditEndFreq;
    private Button mBtnStart;
    private Button mBtnStop;
    private CheckBox mCheckLcd;
    private int mRssi;
    private long mStartFreq;
    private long mEndFreq;
    private FMPlayer mPlayer;
    private int mCount = 0;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            removeMessages(0);
            try {
                mTextCurrent.setText("Current freq : " + mPlayer.getCurrentChannel() + "   rssi : "
                        + mPlayer.getCurrentRSSI());

                if (mRssi > mPlayer.getCurrentRSSI())
                    mCount++;

                if (mCount > 5) {
                    mCount = 0;
                    mPlayer.seekUp();
                } else {
                    sendMessageDelayed(obtainMessage(0), 500);
                }
            } catch (FMPlayerException e) {
                e.printStackTrace();
            }
        }
    };

    private FMEventListener mListener = new FMEventListener() {

        @Override
        public void onOff(int reasonCode) {
            mTextCurrent.setText("Current freq : --   rssi : --");

            mEditRssi.setEnabled(true);
            mEditStartFreq.setEnabled(true);
            mEditEndFreq.setEnabled(true);

            mHandler.removeMessages(0);
            mCount = 0;
        }

        @Override
        public void onOn() {
            mEditRssi.setEnabled(false);
            mEditStartFreq.setEnabled(false);
            mEditEndFreq.setEnabled(false);

            try {
                if (mStartFreq < RadioPlayer.FREQ_MIN || mStartFreq > RadioPlayer.FREQ_MAX) {
                    mStartFreq = RadioPlayer.FREQ_DEFAULT * 10;
                }
                mPlayer.tune(mStartFreq);
            } catch (FMPlayerException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onTune(long frequency) {
            if (mEndFreq < frequency) {
                try {
                    mPlayer.off();
                } catch (FMPlayerException e) {
                    e.printStackTrace();
                }
                return;
            }

            mHandler.sendEmptyMessage(0);
        }
    };

    protected AudioManager mAudioManager;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rssi_test);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mPlayer = (FMPlayer) getSystemService(Context.FM_RADIO_SERVICE);
        try {
            mPlayer.setListener(mListener);
        } catch (FMPlayerException e) {
            e.printStackTrace();
        }

        mTextCurrent = (TextView) findViewById(R.id.text_current);
        mTextCurrent.setText("Current freq : --   rssi : --");

        mEditRssi = (EditText) findViewById(R.id.edit_rssi);
        mEditStartFreq = (EditText) findViewById(R.id.edit_start);
        mEditEndFreq = (EditText) findViewById(R.id.edit_end);

        mBtnStart = (Button) findViewById(R.id.btn_start);
        mBtnStop = (Button) findViewById(R.id.btn_end);

        mCheckLcd = (CheckBox) findViewById(R.id.check_lcd);
        setLcd(mCheckLcd.isChecked());

        mBtnStart.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    mRssi = Integer.parseInt(mEditRssi.getText().toString());
                    mStartFreq = Long.parseLong(mEditStartFreq.getText().toString());
                    mEndFreq = Long.parseLong(mEditEndFreq.getText().toString());
                } catch (NumberFormatException e) {
                    RadioToast.showToast(RssiTestActivity.this, "Wrong number format",
                            Toast.LENGTH_SHORT);
                    e.printStackTrace();
                    return;
                }
                try {
                    mPlayer.on();
                    int volume = mAudioManager.getStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO));
                    mAudioManager.setStreamVolume(SamsungAudioManager.stream(SamsungAudioManager.STREAM_FM_RADIO), volume, 0);
                } catch (FMPlayerException e) {
                    e.printStackTrace();
                }
            }
        });

        mBtnStop.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    mPlayer.off();
                } catch (FMPlayerException e) {
                    e.printStackTrace();
                }
            }
        });

        mCheckLcd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setLcd(isChecked);
            }
        });
    }

    private void setLcd(boolean keepOn) {
        if (keepOn)
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onDestroy() {
        setLcd(false);
        try {
            mPlayer.off();
            mPlayer.removeListener(mListener);
        } catch (FMPlayerException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
