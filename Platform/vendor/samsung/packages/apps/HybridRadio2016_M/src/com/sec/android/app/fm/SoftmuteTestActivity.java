/**
 * This is an activity for Softmute setting.
 */

package com.sec.android.app.fm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SoftmuteTestActivity extends Activity implements OnClickListener {
    String LOG_TAG = "### SoftmuteTestActivity ###";
    private int MAX_REG_COUNT = 8;
    private TextView[] mTextLabel = new TextView[MAX_REG_COUNT];
    private EditText[] mEditReg = new EditText[MAX_REG_COUNT];

    String registerName[] = { "Start SNR (0~63)", "Stop SNR (0~63)", "Start RSSI (-128~127)",
            "Stop RSSI (-128~127)", "Start MUTE (0~63) 255(Disable Softmute)",
            "Stop Atten(-128~127)", "Mute Rate (0~63)", "SNR40 (-128~127)" };

    int mTextViewId[] = { R.id.text_start_snr, R.id.text_stop_snr, R.id.text_start_rssi,
            R.id.text_stop_rssi, R.id.text_start_mute, R.id.text_stop_atten, R.id.text_mute_rate,
            R.id.text_snr40 };

    int mEditTextId[] = { R.id.start_snr, R.id.stop_snr, R.id.start_rssi, R.id.stop_rssi,
            R.id.start_mute, R.id.stop_atten, R.id.mute_rate, R.id.snr40 };

    String PropertyName[] = { "service.brcm.fm.start_snr", "service.brcm.fm.stop_snr",
            "service.brcm.fm.start_rssi", "service.brcm.fm.stop_rssi",
            "service.brcm.fm.start_mute", "service.brcm.fm.stop_atten",
            "service.brcm.fm.mute_rate", "service.brcm.fm.snr40" };
    String SetPropertyPermission = "com.sec.android.app.fm.permission.setproperty";

    int mDefaultSoftmute[] = { 41, 20, 0xff, 0xff, 8, 1, 0xff, 0xff }; // 0xff
                                                                       // is N/A

    private Button mApplyButton, mLoadButton;

    private void initialize() {
        // init apply button
        mApplyButton = (Button) findViewById(R.id.apply_softmute);
        mApplyButton.setOnClickListener(this);
        mApplyButton.setText("Apply");

        // init load button
        mLoadButton = (Button) findViewById(R.id.load_softmute);
        mLoadButton.setOnClickListener(this);
        mLoadButton.setText("Load default");

        // init textview, edittext
        for (int i = 0; i < MAX_REG_COUNT; i++) {
            mTextLabel[i] = (TextView) findViewById(mTextViewId[i]);
            mTextLabel[i].setText(registerName[i]);
            mEditReg[i] = (EditText) findViewById(mEditTextId[i]);
        }
    }

    private void uiclear() {
        for (int i = 0; i < MAX_REG_COUNT; i++) {
            mEditReg[i].setText("N/A");
        }
    }

    private void setPropertySoftmute(String key, int value) {
        Intent i = new Intent("com.sec.android.app.fm.set_property");
        i.putExtra("key", key);
        i.putExtra("value", value);
        this.sendBroadcast(i, SetPropertyPermission);
    }

    private void loadSoftmute() {
        for (int i = 0; i < MAX_REG_COUNT; i++) {
            if (mDefaultSoftmute[i] == 0xff)
                continue;
            mEditReg[i].setText(Integer.toString((int) mDefaultSoftmute[i], 10));
        }
    }

    private void applySoftmute() {
        for (int i = 0; i < MAX_REG_COUNT; i++) {
            String ret = mEditReg[i].getText().toString();
            if (ret.equals("N/A"))
                continue;

            int Value = Integer.parseInt(ret, 10);
            Log.d(LOG_TAG, "applySoftmute: value=" + Value);

            setPropertySoftmute(PropertyName[i], Value);
        }
        setPropertySoftmute("service.brcm.fm.set_blndmute", 1);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case (R.id.apply_softmute):
            applySoftmute();
            Toast.makeText(this, "Apply Complete except N/A~!!", Toast.LENGTH_SHORT).show();
            break;
        case (R.id.load_softmute):
            loadSoftmute();
            Toast.makeText(this, "Load default value~!!", Toast.LENGTH_SHORT).show();
            break;
        default:
            break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.softmute_test);
        initialize();
        uiclear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
