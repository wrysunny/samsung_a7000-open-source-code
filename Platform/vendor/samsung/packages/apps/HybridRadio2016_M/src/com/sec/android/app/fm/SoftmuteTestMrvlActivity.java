/**
 * This is an activity for Softmute setting.
 */

package com.sec.android.app.fm;

import com.sec.android.app.fm.util.SystemPropertiesWrapper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class SoftmuteTestMrvlActivity extends Activity implements OnClickListener {
    String LOG_TAG = "### SoftmuteTestMrvlActivity ###";
    private int MAX_REG_COUNT = 3;
    private TextView[] mTextLabel = new TextView[MAX_REG_COUNT];
    private EditText[] mEditReg = new EditText[MAX_REG_COUNT];

    String registerName[] = { "Mute_Thresh(dBm)", "Attenuation", "Enable SWMute (1 is on, 0 is off)" };

    int mTextViewId[] = { R.id.text_mute_thresh, R.id.text_attenuation, R.id.text_start_mute_mrvl};

    int mEditTextId[] = { R.id.mute_thresh, R.id.attenuation, R.id.start_mute_mrvl};

    String PropertyName[] = { "service.mrvl.fm.mute_thresh", "service.mrvl.fm.attenuation", "service.mrvl.fm.enable_swmute"};
    String SetPropertyPermission = "com.sec.android.app.fm.permission.setproperty";

    int mDefaultSoftmute[] = {80, 15, 0xff}; // 0xff is NULL

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
        String PropBuff = null ;

        for (int i = 0; i < MAX_REG_COUNT; i++) {
            PropBuff = SystemPropertiesWrapper.getInstance().get(PropertyName[i], "N/A");
            mEditReg[i].setText(PropBuff);
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
            if ("N/A".equals(ret))
                continue;

            int Value = Integer.parseInt(ret, 10);
            Log.d(LOG_TAG, "applySoftmute: value=" + Value);

            setPropertySoftmute(PropertyName[i], Value);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case (R.id.apply_softmute):
            applySoftmute();
            Toast.makeText(this, "Apply Complete !! plz restart FMRadio.", Toast.LENGTH_SHORT).show();
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
        setContentView(R.layout.softmute_test_mrvl);
        initialize();
        uiclear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
