/**
 *
 */

package com.sec.android.app.fm;

import com.samsung.media.fmradio.FMPlayer;
import com.sec.android.app.SecProductFeature_FMRADIO;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HwTunningActivity extends Activity implements OnClickListener {

    private int MAX_REG_COUNT = 10;

    private TextView[] mTextLabel = new TextView[MAX_REG_COUNT];

    private EditText[] mEditReg = new EditText[MAX_REG_COUNT];

    String LOG_TAG = "###";

    String registerName[] = { "RSSI", "SNR", "CNT", "RSSI_2", "SNR_2", "CNT_2", "AF", "AFValid",
            "CurrentRSSI", "CurrentSNR(Silab only)"};

    int mTextViewId[] = { R.id.text_rssi, R.id.text_snr, R.id.text_cnt, R.id.text_rssi_2,
            R.id.text_snr_2, R.id.text_cnt_2, R.id.text_af, R.id.text_af_valid, R.id.text_current, R.id.text_currentSNR};

    int mEditTextId[] = { R.id.rssi, R.id.snr, R.id.cnt, R.id.rssi_2, R.id.snr_2, R.id.cnt_2,
            R.id.af_threshold, R.id.af_valid_threshold, R.id.current_threshold, R.id.current_snr};

    int i = 0;

    private Button mOkButton, mCancelButton, mSkipValue, mThailandApplyButton,
            mThailandCancelButton;

    // private HwCodecControl mHwControl;
    com.samsung.media.fmradio.FMPlayer mPlayer;

    private final int MENU_CONTEXT_GOTO_RSSI_TEST = 1;

    //private final int MENU_CONTEXT_GOTO_INTENNA_TEST = 2;

    private final int MENU_CONTEXT_GOTO_SOFTMUTE_TEST = 3;
    private final int MENU_CONTEXT_GOTO_SOFTMUTE_TEST_MRVL = 4;
    //private final int MENU_CONTEXT_GOTO_SOFTMUTE_TEST_STE = 5;

    private final int MENU_CONTEXT_GOTO_VOLUMESETTING_TEST = 6;

    private final int MENU_CONTEXT_GOTO_HYBRIDRADIO_TEST = 7;
    private final int MENU_CONTEXT_GOTO_NOISE_TEST = 8;

    private void initialize() {
        // Button button = (Button)findViewById(R.id.okbtn);
        // button.setText("Apply");

        // mTextViewHead = (TextView)findViewById(R.id.TextView01);
        // mTextViewHead.setText(ActivityMenuName);
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

        if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_BRAODCOM)) {
            registerName[2] = "COS";
            registerName[5] = "COS_2";
        } else if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_SPRD)) {
            registerName[2] = "COS";
            registerName[5] = "COS_2";
        } else if(SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_MRVL)){
            registerName[2] = "CMI";
            registerName[5] = "CMI_2";

            LinearLayout layout_snr = (LinearLayout) findViewById(R.id.layout_snr);
            LinearLayout layout_snr_2 = (LinearLayout) findViewById(R.id.layout_snr2);
            layout_snr.setVisibility(View.GONE);
            layout_snr_2.setVisibility(View.GONE);
        }

        for (i = 0; i < MAX_REG_COUNT; i++) {
            mTextLabel[i] = (TextView) findViewById(mTextViewId[i]);
            mTextLabel[i].setText(registerName[i]);
            mEditReg[i] = (EditText) findViewById(mEditTextId[i]);
        }

      // if (!SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_HAS_INTENNA) {
            LinearLayout layout_rssi_2 = (LinearLayout) findViewById(R.id.layout_rssi2);
            LinearLayout layout_snr_2 = (LinearLayout) findViewById(R.id.layout_snr2);
            LinearLayout layout_cnt_2 = (LinearLayout) findViewById(R.id.layout_cnt2);

            layout_rssi_2.setVisibility(View.GONE);
            layout_snr_2.setVisibility(View.GONE);
            layout_cnt_2.setVisibility(View.GONE);
   //     }

    }

    private void uiclear() {
        for (i = 0; i < MAX_REG_COUNT; i++) {
          //  if (!SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_HAS_INTENNA) {
                if (i == 3 || i == 4 || i == 5)
                    continue;
     //       }

            // if(i%2 != 1 )
            {
                mEditReg[i].setText("");
            }
        }
    }

    private void readThreshold() {
        // try catch added by vanraj
        // any use of the FMPlayer needs to be covered with try catch block.
        try {
            int ret = mPlayer.getRSSI_th();
            Log.d(LOG_TAG, "Read : RSSI Threshold : " + ret);
            mEditReg[0].setText(Integer.toString(ret, 10));

            ret = mPlayer.getSNR_th();
            Log.d(LOG_TAG, "Read : SNR Threshold : " + ret);
            mEditReg[1].setText(Integer.toString(ret, 10));

            ret = mPlayer.getCnt_th();
            Log.d(LOG_TAG, "Read : CNT Threshold : " + ret);
            mEditReg[2].setText(Integer.toString(ret, 10));

          /*  if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_HAS_INTENNA) {
                ret = mPlayer.getRSSI_th_2();
                Log.d(LOG_TAG, "Read : RSSI_2 Threshold : " + ret);
                mEditReg[3].setText(Integer.toString(ret, 10));

                ret = mPlayer.getSNR_th_2();
                Log.d(LOG_TAG, "Read : SNR_2 Threshold : " + ret);
                mEditReg[4].setText(Integer.toString(ret, 10));

                ret = mPlayer.getCnt_th_2();
                Log.d(LOG_TAG, "Read : CNT_2 Threshold : " + ret);
                mEditReg[5].setText(Integer.toString(ret, 10));
            }*/

            // Add Source
            ret = mPlayer.GetAF_th();
            Log.d(LOG_TAG, "Read : AF Threshold : " + ret);
            mEditReg[6].setText(Integer.toString(ret, 10));

            ret = mPlayer.GetAFValid_th();
            Log.d(LOG_TAG, "Read : AFValid Threshold : " + ret);
            mEditReg[7].setText(Integer.toString(ret, 10));

            long retVal = mPlayer.getCurrentRSSI();
            Log.d(LOG_TAG, "Read : CurrentRSSI Threshold : " + (int) retVal);
            mEditReg[8].setText(Integer.toString((int) retVal, 10));

            long retSNR = -1;
            if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_SILICON))
            	retSNR = mPlayer.getCurrentSNR();
            Log.d(LOG_TAG, "Read : CurrentSNR Threshold : " + (int) retSNR);
            mEditReg[9].setText(Integer.toString((int) retSNR, 10));
        } catch (Exception e) {
            try {
                e.printStackTrace();
            } catch (Exception ex) {
                ;
            }
        }

        /*
         * ret = mPlayer.getCnt_th(); Log.d(LOG_TAG, "Read : CNT Threshold : "+
         * ret); mEditReg[2].setText(Integer.toString(ret , 16));
         */
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
          //  if (!SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_HAS_INTENNA) {
                if (i == 3 || i == 4 || i == 5)
                    continue;
        //    }

            mEditReg[i] = (EditText) findViewById(mEditTextId[i]);

            if (mEditReg[i].getText().length() != 0) {
                String ret = mEditReg[i].getText().toString();
                int Value = Integer.parseInt(ret, 10);

                switch (i) {
                case 0:
                    // try catch added by vanraj
                    // any use of the FMPlayer needs to be covered with try
                    // catch block.
                    try {
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
                    // try catch added by vanraj
                    // any use of the FMPlayer needs to be covered with try
                    // catch block.
                    try {
                        mPlayer.setSNR_th(Value);
                        Log.d(LOG_TAG, "Write : SNR Threadhold : " + Value);
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
                        mPlayer.setCnt_th(Value);
                        Log.d(LOG_TAG, "Write : CNT Threadhold : " + Value);
                    } catch (Exception e) {
                        try {
                            e.printStackTrace();
                        } catch (Exception ex) {
                            ;
                        }
                    }
                    break;

                case 3:
                    try {
                        mPlayer.setRSSI_th_2(Value);
                        Log.d(LOG_TAG, "Write : RSSI_2 Threadhold : " + Value);
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
                        mPlayer.setSNR_th_2(Value);
                        Log.d(LOG_TAG, "Write : SNR_2 Threadhold : " + Value);
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
                        mPlayer.setCnt_th_2(Value);
                        Log.d(LOG_TAG, "Write : CNT_2 Threadhold : " + Value);
                    } catch (Exception e) {
                        try {
                            e.printStackTrace();
                        } catch (Exception ex) {
                            ;
                        }
                    }
                    break;

                case 6:
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

                case 7:
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
                /*
                 * case 10 : try{ mPlayer.setCnt_th(Value); Log.d(LOG_TAG,
                 * "Write : CNT Threadhold : "+ Value); }catch(Exception e) {
                 * e.printStackTrace(); } break;
                 */
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

        setContentView(R.layout.hwtunning);
        // final FMPlayer player = (FMPlayer)
        // getSystemService(Context.FM_RADIO_SERVICE);
        initialize();
        readThreshold();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case (R.id.okbtn):
            try {
                if (mPlayer.isOn() == false) {
                    Toast.makeText(HwTunningActivity.this, "Play FMRadio before setting~!!",
                            Toast.LENGTH_SHORT).show();
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            writeThreshold();
            uiclear();
            readThreshold();

            Toast.makeText(HwTunningActivity.this, "Saved Complete~!!", Toast.LENGTH_SHORT).show();
            break;
        case (R.id.cancelbtn):
            uiclear();
            readThreshold();

            Toast.makeText(HwTunningActivity.this, "Cancel~!!", Toast.LENGTH_SHORT).show();
            break;
        case (R.id.skiptedtunningvalue):
            SkipTuning_Value();
            Toast.makeText(HwTunningActivity.this, "Applied value for scan!!", Toast.LENGTH_SHORT)
                    .show();
            break;
        case (R.id.thailandapplybtn):
            try {
                FMRadioFeature.ForceApply_ThailandFunction();
                if (!mPlayer.isOn()) {
                    Toast.makeText(HwTunningActivity.this, "Please turn on Radio",
                            Toast.LENGTH_LONG).show();
                } else {
                    mPlayer.setChannelSpacing(5);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Toast.makeText(HwTunningActivity.this, "ForceApply_ThailandFunction",
                    Toast.LENGTH_SHORT).show();
            break;
        case (R.id.thailandcancelbtn):
            FMRadioFeature.Recovery_ThailandFunction();
            try {
                if (!mPlayer.isOn()) {
                    Toast.makeText(HwTunningActivity.this, "Please turn on Radio",
                            Toast.LENGTH_LONG).show();
                } else {
                    mPlayer.setChannelSpacing(10);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Toast.makeText(HwTunningActivity.this, "Recovery_ThailandFunction", Toast.LENGTH_SHORT)
                    .show();
            break;
        default:
            break;

        }
    }

    // 2011.10.18 TOD_AHS : RSSI test. [
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
       /* if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_HAS_INTENNA) {
            menu.add(0, MENU_CONTEXT_GOTO_INTENNA_TEST, 0, "Intenna test");
        }*/
        if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_BRAODCOM)
                || SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_SPRD)) {
            menu.add(0, MENU_CONTEXT_GOTO_SOFTMUTE_TEST, 0, "Softmute test");
            menu.add(0, MENU_CONTEXT_GOTO_VOLUMESETTING_TEST, 0, "VolumeSetting test");
        }
        if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_MRVL)) {
            menu.add(0, MENU_CONTEXT_GOTO_SOFTMUTE_TEST_MRVL, 0, "Softmute test");
        }
      /*  if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_SUPPORT_STE) {
            menu.add(0, MENU_CONTEXT_GOTO_SOFTMUTE_TEST_STE, 0, "Softmute test for STE");
            menu.add(0, MENU_CONTEXT_GOTO_VOLUMESETTING_TEST, 0, "VolumeSetting test");
        }*/
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

       /* if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_HAS_INTENNA) {
            if (id == MENU_CONTEXT_GOTO_INTENNA_TEST) {
                Intent intent = new Intent(this, IntennaTestActivity.class);
                startActivity(intent);
                return true;
            }
        }*/

        if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_BRAODCOM)
                || SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_SPRD)) {
            if (id == MENU_CONTEXT_GOTO_SOFTMUTE_TEST) {
                Intent intent = new Intent(this, SoftmuteTestActivity.class);
                startActivity(intent);
                return true;
            }

            if (id == MENU_CONTEXT_GOTO_VOLUMESETTING_TEST) {
                Intent intent = new Intent(this, VolumeSettingTestActivity.class);
                startActivity(intent);
                return true;
            }
        }

        if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_CONFIG_CHIP_VENDOR.equals(FMRadioFeature.CHIP_MRVL)) {
            if (id == MENU_CONTEXT_GOTO_SOFTMUTE_TEST_MRVL) {
                Intent intent = new Intent(this, SoftmuteTestMrvlActivity.class);
                startActivity(intent);
                return true;
            }
        }

       /* if (SecProductFeature_FMRADIO.SEC_PRODUCT_FEATURE_FMRADIO_SUPPORT_STE) {
            if (id == MENU_CONTEXT_GOTO_SOFTMUTE_TEST_STE) {
                Intent intent = new Intent(this, SoftMuteTestSTEActivity.class);
                startActivity(intent);
                return true;
            }
            if (id == MENU_CONTEXT_GOTO_VOLUMESETTING_TEST) {
                Intent intent = new Intent(this, VolumeSettingTestActivity.class);
                startActivity(intent);
                return true;
            }
        }*/
        
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
    // 2011.10.18 TOD_AHS : RSSI test. ]
}

/*
 * public class HwTunningActivity extends Activity{
 * 
 * private EditText rssiEdit; private EditText snrEdit; private EditText
 * cntEdit; // Add Source private EditText afEdit; private EditText afvalidEdit;
 * private EditText currentEdit;
 * 
 * protected void onCreate(Bundle savedInstanceState) {
 * super.onCreate(savedInstanceState); setContentView(R.layout.hwtunning); final
 * FMPlayer player = (FMPlayer) getSystemService(Context.FM_RADIO_SERVICE);
 * rssiEdit = (EditText) findViewById(R.id.rssi); snrEdit = (EditText)
 * findViewById(R.id.snr); cntEdit = (EditText) findViewById(R.id.cnt); // Add
 * Source afEdit = (EditText) findViewById(R.id.af_threshold); afvalidEdit =
 * (EditText) findViewById(R.id.af_valid_threshold); currentEdit = (EditText)
 * findViewById(R.id.current_threshold);
 * 
 * Button button = (Button)findViewById(R.id.button); button.setText("Apply");
 * button.setOnClickListener(new View.OnClickListener() {
 * 
 * public void onClick(View arg0) { try{ System.out.println("RSSI set:" +
 * rssiEdit.getText()); //
 * player.setSeekRSSI(Long.parseLong(rssiEdit.getText().toString()));
 * player.setRSSI_th(Integer.parseInt(rssiEdit.getText().toString(), 16));
 * System.out.println("SNR set:" + snrEdit.getText()); //
 * player.setSeekSNR(Long.parseLong(snrEdit.getText().toString()));
 * player.setSNR_th(Integer.parseInt(snrEdit.getText().toString(), 16));
 * System.out.println("SNR set:" + cntEdit.getText()); //
 * player.setSeekSNR(Long.parseLong(cntEdit.getText().toString()));
 * player.setCnt_th(Integer.parseInt(cntEdit.getText().toString(), 16));
 * 
 * // Add Source System.out.println("Set AF Threshold:" + afEdit.getText());
 * player.SetAF_th(Integer.parseInt(afEdit.getText().toString(), 16));
 * 
 * System.out.println("AF Valid Threshold:" + afvalidEdit.getText());
 * player.SetAFValid_th (Integer.parseInt(afvalidEdit.getText().toString(),
 * 16));
 * 
 * System.out.println("Current Threshold:" + currentEdit.getText());
 * player.getCurrentRSSI();
 * 
 * }catch(Exception e){ e.printStackTrace();
 * Toast.makeText(HwTunningActivity.this, e.getMessage(),
 * Toast.LENGTH_SHORT).show(); } } }); }
 * 
 * }
 */
