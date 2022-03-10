/**
 *
 */

package com.sec.android.app.fm;

import com.samsung.media.fmradio.FMPlayer;
import com.sec.android.app.dns.ui.DnsKoreaTestActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class HwTunningActivity_Richwave extends Activity implements OnClickListener {

    private int MAX_REG_COUNT = 6;

    private TextView[] mTextLabel = new TextView[MAX_REG_COUNT];

    private EditText[] mEditReg = new EditText[MAX_REG_COUNT];

    String LOG_TAG = "###";

    String registerName[] = { "RSSI(dBuv)", "AF", "AFValid", "CurrentRSSI", "SeekDC", "SeekQA" };

    int mTextViewId[] = { R.id.text_rssi, R.id.text_af, R.id.text_af_valid, R.id.text_current, R.id.text_seekdc , R.id.text_seekqa };

    int mEditTextId[] = { R.id.rssi,R.id.af_threshold, R.id.af_valid_threshold, R.id.current_threshold, R.id.seekdc , R.id.seekqa };

    int i = 0;

    private Button mOkButton, mCancelButton, mSkipValue, mThailandApplyButton,
            mThailandCancelButton;

    com.samsung.media.fmradio.FMPlayer mPlayer;

    private final int MENU_CONTEXT_GOTO_RSSI_TEST = 1;
    private final int MENU_CONTEXT_GOTO_HYBRIDRADIO_TEST = 2;
    private final int MENU_CONTEXT_GOTO_NOISE_TEST = 3;

    private void initialize() {
        mOkButton = (Button) findViewById(R.id.okbtn);
        mOkButton.setOnClickListener(this);
        mOkButton.setText("Save");

        mCancelButton = (Button) findViewById(R.id.cancelbtn);
        mCancelButton.setOnClickListener(this);
        mCancelButton.setText("Cancel");

        mSkipValue = (Button) findViewById(R.id.skiptedtunningvalue);
        mSkipValue.setOnClickListener(this);
        mSkipValue.setText("Apply Value for scan");

        mThailandApplyButton = (Button) findViewById(R.id.thailandapplybtn);
        mThailandApplyButton.setOnClickListener(this);
        mThailandApplyButton.setText("ThaiUI_Apply");

        mThailandCancelButton = (Button) findViewById(R.id.thailandcancelbtn);
        mThailandCancelButton.setOnClickListener(this);
        mThailandCancelButton.setText("ThaiUI_Cancel");

        mPlayer = (FMPlayer) getSystemService(Context.FM_RADIO_SERVICE);

        for (i = 0; i < MAX_REG_COUNT; i++) {
            mTextLabel[i] = (TextView) findViewById(mTextViewId[i]);
            mTextLabel[i].setText(registerName[i]);
            mEditReg[i] = (EditText) findViewById(mEditTextId[i]);
        }

    }

    private void uiclear() {
        for (i = 0; i < MAX_REG_COUNT; i++) {
                mEditReg[i].setText("");
        }
    }

    private void readThreshold() {
        try {
            int ret = mPlayer.getRSSI_th();
            Log.d(LOG_TAG, "Read : RSSI Threshold : " + ret);
            mEditReg[0].setText(Integer.toString(ret, 10));

            // Add Source
            ret = mPlayer.GetAF_th();
            Log.d(LOG_TAG, "Read : AF Threshold : " + ret);
            mEditReg[1].setText(Integer.toString(ret, 10));

            ret = mPlayer.GetAFValid_th();
            Log.d(LOG_TAG, "Read : AFValid Threshold : " + ret);
            mEditReg[2].setText(Integer.toString(ret, 10));

            long retVal = mPlayer.getCurrentRSSI();
            Log.d(LOG_TAG, "Read : CurrentRSSI Threshold : " + (int) retVal);
            mEditReg[3].setText(Integer.toString((int) retVal, 10));

            int seekDC = -1;
           	seekDC = mPlayer.GetSeekDC();
            Log.d(LOG_TAG, "Read : seekDC Threshold : " + seekDC);
            mEditReg[4].setText(Integer.toString((int) seekDC, 10));
            
            int seekQA = -1;
           	seekQA = mPlayer.GetSeekQA();
            Log.d(LOG_TAG, "Read : seekQA Threshold : " + seekQA);
            mEditReg[5].setText(Integer.toString((int) seekQA, 10));
        } catch (Exception e) {
            try {
                e.printStackTrace();
            } catch (Exception ex) {
                ;
            }
        }
    }

    private void SkipTuning_Value() {
        try {
            mPlayer.SkipTuning_Value();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeThreshold() {
        for (i = 0; i < MAX_REG_COUNT; i++) {

            mEditReg[i] = (EditText) findViewById(mEditTextId[i]);

            if (mEditReg[i].getText().length() != 0) {
                String ret = mEditReg[i].getText().toString();
                int Value = Integer.parseInt(ret, 10);

                switch (i) {
                case 0:
                    try {
                        mPlayer.setSeekRSSI(Value);
                        mPlayer.setRSSI_th(Value);

                        Log.d(LOG_TAG, "Write : RSSI Threadhold : " + Value);
                    } catch (Exception e) {
                        try {
                            e.printStackTrace();
                        } catch (Exception ex) {
                            ;
                        }
                    }
                    break;

                case 1:
                    try {
                        mPlayer.SetAF_th(Value);
                        Log.d(LOG_TAG, "Write : SetAF Threadhold : " + Value);
                    } catch (Exception e) {
                        try {
                            e.printStackTrace();
                        } catch (Exception ex) {
                            ;
                        }
                    }
                    break;

                case 2:
                    try {
                        mPlayer.SetAFValid_th(Value);
                        Log.d(LOG_TAG, "Write : SetAFValid Threadhold : " + Value);
                    } catch (Exception e) {
                        try {
                            e.printStackTrace();
                        } catch (Exception ex) {
                            ;
                        }
                    }
                    break;

                case 4:
                    try {
                        mPlayer.SetSeekDC(Value);
                        Log.d(LOG_TAG, "Write : SetSeekDC : " + Value);
                    } catch (Exception e) {
                        try {
                            e.printStackTrace();
                        } catch (Exception ex) {
                            ;
                        }
                    }
                    break;
                case 5:
                    try {
                        mPlayer.SetSeekQA(Value);
                        Log.d(LOG_TAG, "Write : SetSeekQA: " + Value);
                    } catch (Exception e) {
                        try {
                            e.printStackTrace();
                        } catch (Exception ex) {
                            ;
                        }
                    }
                    break;
                default:
                    Log.e(LOG_TAG, "Write : No valid data");
                    break;
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.hwtunning_richwave);
        initialize();
        readThreshold();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case (R.id.okbtn):
            try {
                if (mPlayer.isOn() == false) {
                    Toast.makeText(HwTunningActivity_Richwave.this, "Play FMRadio before setting~!!",
                            Toast.LENGTH_SHORT).show();
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            writeThreshold();
            uiclear();
            readThreshold();

            Toast.makeText(HwTunningActivity_Richwave.this, "Saved Complete~!!", Toast.LENGTH_SHORT).show();
            break;
        case (R.id.cancelbtn):
            uiclear();
            readThreshold();

            Toast.makeText(HwTunningActivity_Richwave.this, "Cancel~!!", Toast.LENGTH_SHORT).show();
            break;
        case (R.id.skiptedtunningvalue):
            SkipTuning_Value();
            Toast.makeText(HwTunningActivity_Richwave.this, "Applied value for scan!!", Toast.LENGTH_SHORT)
                    .show();
            break;
        case (R.id.thailandapplybtn):
            try {
                FMRadioFeature.ForceApply_ThailandFunction();
                if (!mPlayer.isOn()) {
                    Toast.makeText(HwTunningActivity_Richwave.this, "Please turn on Radio",
                            Toast.LENGTH_LONG).show();
                } else {
                    mPlayer.setChannelSpacing(5);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Toast.makeText(HwTunningActivity_Richwave.this, "ForceApply_ThailandFunction",
                    Toast.LENGTH_SHORT).show();
            break;
        case (R.id.thailandcancelbtn):
            FMRadioFeature.Recovery_ThailandFunction();
            try {
                if (!mPlayer.isOn()) {
                    Toast.makeText(HwTunningActivity_Richwave.this, "Please turn on Radio",
                            Toast.LENGTH_LONG).show();
                } else {
                    mPlayer.setChannelSpacing(10);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(HwTunningActivity_Richwave.this, "Recovery_ThailandFunction", Toast.LENGTH_SHORT)
                    .show();
            break;
        default:
            break;

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        menu.add(0, MENU_CONTEXT_GOTO_RSSI_TEST, 0, "RSSI test");
        menu.add(0,MENU_CONTEXT_GOTO_HYBRIDRADIO_TEST,0,"HybridRadio Test");
        menu.add(0,MENU_CONTEXT_GOTO_NOISE_TEST,0,"Noise Test");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == MENU_CONTEXT_GOTO_RSSI_TEST) {
            Intent intent = new Intent(this, RssiTestActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == MENU_CONTEXT_GOTO_HYBRIDRADIO_TEST) {
            Intent intent = new Intent(this, DnsKoreaTestActivity.class);
            startActivity(intent);
            return true;
        }

        if(id == MENU_CONTEXT_GOTO_NOISE_TEST) {
            Intent intent = new Intent(this, NoiseTestActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}