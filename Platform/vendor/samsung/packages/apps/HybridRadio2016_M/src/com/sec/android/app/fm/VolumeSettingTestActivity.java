/**
 * This is an activity for Volume setting.
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

public class VolumeSettingTestActivity extends Activity implements OnClickListener {
    String LOG_TAG = "### VolumeSettingTestActivity ###";
    private int MAX_REG_COUNT = 15;
    private TextView[] mTextLabel = new TextView[MAX_REG_COUNT];
    private EditText[] mEditReg = new EditText[MAX_REG_COUNT];

    String registerName[] = { "Volume level 1", "Volume level 2", "Volume level 3",
            "Volume level 4", "Volume level 5", "Volume level 6", "Volume level 7",
            "Volume level 8", "Volume level 9", "Volume level 10", "Volume level 11",
            "Volume level 12", "Volume level 13", "Volume level 14", "Volume level 15" };

    int mTextViewId[] = { R.id.text_fmvolume_1, R.id.text_fmvolume_2, R.id.text_fmvolume_3,
            R.id.text_fmvolume_4, R.id.text_fmvolume_5, R.id.text_fmvolume_6, R.id.text_fmvolume_7,
            R.id.text_fmvolume_8, R.id.text_fmvolume_9, R.id.text_fmvolume_10,
            R.id.text_fmvolume_11, R.id.text_fmvolume_12, R.id.text_fmvolume_13,
            R.id.text_fmvolume_14, R.id.text_fmvolume_15 };

    int mEditTextId[] = { R.id.fmvolume_1, R.id.fmvolume_2, R.id.fmvolume_3, R.id.fmvolume_4,
            R.id.fmvolume_5, R.id.fmvolume_6, R.id.fmvolume_7, R.id.fmvolume_8, R.id.fmvolume_9,
            R.id.fmvolume_10, R.id.fmvolume_11, R.id.fmvolume_12, R.id.fmvolume_13,
            R.id.fmvolume_14, R.id.fmvolume_15 };

    private String mVolumeTable;

    String VolumePropertyname = "service.brcm.fm.volumetable";
    String SetPropertyPermission = "com.sec.android.app.fm.permission.setproperty";

    private Button mApplyButton;

    private void initialize() {
        // init apply button
        mApplyButton = (Button) findViewById(R.id.apply_volumetable);
        mApplyButton.setOnClickListener(this);
        mApplyButton.setText("Apply");

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

    private void setPropertyVolumeTable(String key, String value) {
        Intent i = new Intent("com.sec.android.app.fm.set_volume");
        i.putExtra("key", key);
        i.putExtra("volumetable", value);
        this.sendBroadcast(i, SetPropertyPermission);
    }

    private void loadVolumeTable() {
        try {
            mVolumeTable = SystemPropertiesWrapper.getInstance().get(VolumePropertyname);
            String[] volumeTable = mVolumeTable.split(",");

            Log.d(LOG_TAG, "loadVolumeTable: Volumetable=" + mVolumeTable);
            
            for (int i = 0; i < MAX_REG_COUNT; i++) {
                // if (Integer.valueOf(volumeTable[i+1]) == 0xff)
                // continue;
                mEditReg[i].setText(volumeTable[i + 1]);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            Toast.makeText(this, "Frist, start FM Radio ~!!", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private void applyVolumeTable() {
        String Volumetable = "0";
        int Value;

        for (int i = 0; i < MAX_REG_COUNT; i++) {
            String ret = mEditReg[i].getText().toString();
            if ("N/A".equals(ret) || Integer.valueOf(ret) < 0 || Integer.valueOf(ret) > 255) {
                Toast.makeText(this, "Fill the volumetable 0-255 ~!!", Toast.LENGTH_SHORT).show();
                return;
            }
            Value = Integer.parseInt(ret, 10);
            Volumetable = Volumetable + "," + Value;
        }
        Log.d(LOG_TAG, "applyVolumeTable: Volumetable=" + Volumetable);
        setPropertyVolumeTable(VolumePropertyname, Volumetable);
        Toast.makeText(this, "Apply Complete. ReStart FM Radio ~!!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case (R.id.apply_volumetable):
            applyVolumeTable();
            break;
        default:
            break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.volumesetting_test);
        initialize();
        uiclear();
        loadVolumeTable();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
