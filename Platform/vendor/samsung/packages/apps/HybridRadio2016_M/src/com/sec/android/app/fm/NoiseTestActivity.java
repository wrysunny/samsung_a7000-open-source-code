package com.sec.android.app.fm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TableRow.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.content.Context;

import com.samsung.media.fmradio.FMPlayer;
import com.samsung.media.fmradio.FMPlayerException;

public class NoiseTestActivity extends Activity implements OnClickListener, OnCheckedChangeListener {

    public static String TAG = "NoiseTestActivity";

    public static int START_FREQUENCY = 87500;
    public static int END_FREQUENCY = 108000;
    public static int TEST_INTERVAL = 1000;

    private EditText mEditTextStartFreq, mEditTextEndFreq, mEditTextDelay;

    private TextView mEditTextStatus;

    private Button mButtonStart, mButtonStop;

    private RadioGroup mRadioGroupSoundPath;

    private TableLayout mTableLayoutResult;

    private int mStartFreq, mEndFreq, mCurrentFreq, mDelay;

    private boolean mIsTesting;

    private FMPlayer mPlayer;

    private ArrayList<TestResult> mResult;

    protected WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.noise_test);

        mPlayer = (FMPlayer) getSystemService(Context.FM_RADIO_SERVICE);

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(
                Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        mWakeLock.acquire();

        mEditTextStartFreq = (EditText) findViewById(R.id.edit_start_freq);
        mEditTextEndFreq = (EditText) findViewById(R.id.edit_end_freq);
        mEditTextDelay = (EditText) findViewById(R.id.edit_delay);
        mEditTextStatus = (TextView) findViewById(R.id.text_test_status);

        mButtonStart = (Button) findViewById(R.id.btn_test_start);
        mButtonStart.setOnClickListener(this);
        mButtonStop = (Button) findViewById(R.id.btn_test_stop);
        mButtonStop.setOnClickListener(this);
        mButtonStop.setEnabled(false);

        mRadioGroupSoundPath = (RadioGroup) findViewById(R.id.radiogroup_test_sound_path);
        mRadioGroupSoundPath.setOnCheckedChangeListener(this);
        mRadioGroupSoundPath.check(R.id.radiobutton_test_ep);
        try {
            mPlayer.setSpeakerOn(false);
        } catch (FMPlayerException e) {
            e.printStackTrace();
            try {
                mRadioGroupSoundPath.check(R.id.radiobutton_test_speaker);
                mPlayer.setSpeakerOn(true);
            } catch (FMPlayerException e1) {
                e1.printStackTrace();
            }
        }

        mTableLayoutResult = (TableLayout) findViewById(R.id.table_test_result);

        mResult = new ArrayList<TestResult>();

        mEditTextStartFreq.setText(START_FREQUENCY + "");
        mEditTextEndFreq.setText(END_FREQUENCY + "");
        mEditTextDelay.setText(TEST_INTERVAL + "");
        appendRow("frequency", "RSSI", "SNR/CMI");
    }

    @Override
    public void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onStop() {
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        try {
            mPlayer.off();
            if (mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;
            }
        } catch (FMPlayerException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.btn_test_start:
            mIsTesting = true;
            mHandler.sendEmptyMessage(0);
            break;
        case R.id.btn_test_stop:
            mIsTesting = false;
            break;
        default:
            break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
        case R.id.radiobutton_test_ep:
            try {
                mPlayer.setSpeakerOn(false);
            } catch (FMPlayerException e) {
                e.printStackTrace();
                mRadioGroupSoundPath.check(R.id.radiobutton_test_speaker);
            }
            break;
        case R.id.radiobutton_test_speaker:
            try {
                mPlayer.setSpeakerOn(true);
            } catch (FMPlayerException e) {
                e.printStackTrace();
                mRadioGroupSoundPath.check(R.id.radiobutton_test_ep);
            }
            break;
        default:
            break;
        }
    }

    class TestNoiseThread extends Thread {
        public void run() {
            try {
                TestResult result;
                mPlayer.on();
                for (mCurrentFreq = mStartFreq; mIsTesting && mCurrentFreq <= mEndFreq; mCurrentFreq += 100) {
                    result = new TestResult();
                    mHandler.sendEmptyMessage(2);
                    mPlayer.tune(mCurrentFreq);
                    Thread.sleep(mDelay);
                    result.frequency = mCurrentFreq;
                    result.RSSI = mPlayer.getCurrentRSSI();
                    result.SNR = mPlayer.getCurrentSNR();
                    mResult.add(result);
                    Log.d(TAG, result.frequency / 1000f + " \t" + result.RSSI + " \t" + result.SNR);
                }
                mPlayer.off();
            } catch (FMPlayerException e) {
                mPlayer = null;
                mPlayer = (FMPlayer) getSystemService(Context.FM_RADIO_SERVICE);
                Log.d(TAG, "FMPlayerException");
                e.printStackTrace();
            } catch (NoSuchMethodError e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                Log.d(TAG, "InterruptedException");
                e.printStackTrace();
            } finally {
                mIsTesting = false;
                mHandler.sendEmptyMessage(1);
            }
        }
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                mButtonStart.setEnabled(false);
                mButtonStop.setEnabled(true);
                mEditTextStartFreq.setEnabled(false);
                mEditTextEndFreq.setEnabled(false);
                mEditTextDelay.setEnabled(false);
                mStartFreq = Integer.valueOf(mEditTextStartFreq.getText().toString());
                mEndFreq = Integer.valueOf(mEditTextEndFreq.getText().toString());
                mDelay = Integer.valueOf(mEditTextDelay.getText().toString());
                mEditTextStatus.setText("Testing... : Turning on FM");
                mResult.clear();
                mTableLayoutResult.removeAllViewsInLayout();
                appendRow("frequency", "RSSI", "SNR/CMI");
                TestNoiseThread thread = new TestNoiseThread();
                thread.start();
            } else if (msg.what == 1) {
                getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
                mButtonStart.setEnabled(true);
                mButtonStop.setEnabled(false);
                mEditTextStartFreq.setEnabled(true);
                mEditTextEndFreq.setEnabled(true);
                mEditTextDelay.setEnabled(true);

                if (mResult.size() != 0) {
                    File file;
                    String path = android.os.Environment.getExternalStorageDirectory()
                            + "/FMNoiseTest/";
                    String fileName = "FMNoiseTest_" + getTime() + ".txt";
                    file = new File(path);
                    if (!file.exists()) {
                        file.mkdirs();
                    }
                    file = new File(path + fileName);
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(file);
                        fos.write(("Delay : " + mDelay + "ms\n").getBytes());
                        for (int i = 0; i < mResult.size(); i++) {
                            appendRow(mResult.get(i).frequency / 1000f + "", mResult.get(i).RSSI
                                    + "", mResult.get(i).SNR + "");
                            fos.write((String.format("%5.1f", mResult.get(i).frequency / 1000f)
                                    + "\t" + String.format("%5d", mResult.get(i).RSSI) + "\t"
                                    + String.format("%5d", mResult.get(i).SNR) + "\n").getBytes());
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (fos != null) {
                                fos.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mEditTextStatus.setText("Ready");
            } else if (msg.what == 2) {
                mEditTextStatus.setText("Testing... : " + mCurrentFreq / 1000f + "MHz");
            }
        }
    };

    // Update Result TableLaout
    private void appendRow(String frequency, String RSSI, String SNR) {
        TableRow tableRowResult = new TableRow(this);
        tableRowResult.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));

        TextView textviewFrequency = new TextView(this);
        textviewFrequency.setText(frequency);
        textviewFrequency.setTextColor(Color.BLACK);
        textviewFrequency.setLayoutParams(new LayoutParams(999, LayoutParams.WRAP_CONTENT, 1));
        textviewFrequency.setBackgroundColor(Color.parseColor("#FDF5E6"));
        textviewFrequency.setGravity(Gravity.CENTER);

        TextView textviewRSSI = new TextView(this);
        textviewRSSI.setText(RSSI);
        textviewRSSI.setTextColor(Color.BLACK);
        textviewRSSI.setLayoutParams(new LayoutParams(999, LayoutParams.WRAP_CONTENT, 1));
        textviewRSSI.setBackgroundColor(Color.parseColor("#FDF5E6"));
        textviewRSSI.setGravity(Gravity.CENTER);

        TextView textviewSNR = new TextView(this);
        textviewSNR.setText(SNR);
        textviewSNR.setTextColor(Color.BLACK);
        textviewSNR.setLayoutParams(new LayoutParams(999, LayoutParams.WRAP_CONTENT, 1));
        textviewSNR.setBackgroundColor(Color.parseColor("#FDF5E6"));
        textviewSNR.setGravity(Gravity.CENTER);

        TextView textviewDividerHorizontal1 = new TextView(this);
        textviewDividerHorizontal1.setLayoutParams(new LayoutParams(2, LayoutParams.WRAP_CONTENT));

        TextView textviewDividerHorizontal2 = new TextView(this);
        textviewDividerHorizontal2.setLayoutParams(new LayoutParams(2, LayoutParams.WRAP_CONTENT));

        TextView textviewDividerVertical = new TextView(this);
        textviewDividerVertical.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, 2));

        tableRowResult.addView(textviewFrequency);
        tableRowResult.addView(textviewDividerHorizontal1);
        tableRowResult.addView(textviewRSSI);
        tableRowResult.addView(textviewDividerHorizontal2);
        tableRowResult.addView(textviewSNR);

        mTableLayoutResult.addView(tableRowResult, new TableLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mTableLayoutResult.addView(textviewDividerVertical);
    }

    private String getTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }

    class TestResult {
        public long frequency;
        public long RSSI;
        public long SNR;

        public TestResult() {
            frequency = 9999;
            RSSI = 9999;
            SNR = 9999;
        }
    }
}