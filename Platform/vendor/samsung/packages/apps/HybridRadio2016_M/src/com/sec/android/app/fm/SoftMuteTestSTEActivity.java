package com.sec.android.app.fm;

import com.samsung.media.fmradio.FMPlayer;
import com.samsung.media.fmradio.FMPlayerException;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SoftMuteTestSTEActivity extends Activity implements OnClickListener,
        OnCheckedChangeListener {
    private Switch msoftmute;
    private EditText mMin_RSSI, mMax_RSSI, mAttenuation;
    private int Min_RSSI, Max_RSSI, Attenuation;
    private Button mApplyButton;
    private boolean bSoftmuteOnOff = true, bCurrentSoftMuteMode = false;
    String LOG_TAG = "### SoftmuteTestSTEActivity ###";
    com.samsung.media.fmradio.FMPlayer mPlayer;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.softmute_test_ste);
        initialsetup();

    }

    private void initialsetup() {

        mPlayer = (FMPlayer) getSystemService(Context.FM_RADIO_SERVICE);

        inittextview();
        msoftmute = (Switch) findViewById(R.id.softmuteonoff);
        try {
            bCurrentSoftMuteMode = mPlayer.getSoftMuteMode();
        } catch (FMPlayerException e) {
            e.printStackTrace();
        }
        msoftmute.setChecked(bCurrentSoftMuteMode);
        msoftmute.setOnCheckedChangeListener(this);

        mMin_RSSI = (EditText) findViewById(R.id.min_rssi);
        mMax_RSSI = (EditText) findViewById(R.id.max_rssi);
        mAttenuation = (EditText) findViewById(R.id.Attenuation);

        String default_value = "0";
        mMin_RSSI.setText(default_value);
        mMax_RSSI.setText(default_value);
        mAttenuation.setText(default_value);

        mApplyButton = (Button) findViewById(R.id.applysoftmuteforste);
        mApplyButton.setOnClickListener(this);

    }

    private void inittextview() {
        TextView mtitletextview, msoftmuteswitchonofftextview, mminrssitextview, mmaxrssitextview, mattenuationtextview;
        Button mapplysoftmuteforste;

        mtitletextview = (TextView) findViewById(R.id.titletextview);
        msoftmuteswitchonofftextview = (TextView) findViewById(R.id.softmuteswitchonofftextview);
        mminrssitextview = (TextView) findViewById(R.id.minrssitextview);
        mmaxrssitextview = (TextView) findViewById(R.id.maxrssitextview);
        mattenuationtextview = (TextView) findViewById(R.id.attenuationtextview);
        mapplysoftmuteforste = (Button) findViewById(R.id.applysoftmuteforste);

        mtitletextview.setText("SoftMuteTest For STE Radio Chip");
        msoftmuteswitchonofftextview.setText("SoftMute On/Off");
        mminrssitextview.setText("min_RSSI");
        mmaxrssitextview.setText("max_RSSI");
        mattenuationtextview.setText("Attenuation");
        mapplysoftmuteforste.setText("Apply Values");

    }

    @Override
    public void onClick(View arg0) {
        Log.d(LOG_TAG, "onClick ");
        setsoftmuteValue();

    }

    int softmute_value = 0;

    private void setsoftmuteValue() {

        Min_RSSI = Integer.parseInt(mMin_RSSI.getText().toString(), 10);
        Max_RSSI = Integer.parseInt(mMax_RSSI.getText().toString(), 10);
        Attenuation = Integer.parseInt(mAttenuation.getText().toString(), 10);
        Log.d(LOG_TAG, "setsoftmuteValue MODE, Min_MAX RSSI, Attenuation " + bSoftmuteOnOff
                + Min_RSSI + Max_RSSI + softmute_value);

        try {
            mPlayer.setSoftmute(bSoftmuteOnOff);
            mPlayer.setSoftMuteControl(Min_RSSI, Max_RSSI, Attenuation);
        } catch (FMPlayerException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Apply Complete !!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
        Log.d(LOG_TAG, "onCheckedChanged " + arg1);
        bSoftmuteOnOff = arg1;

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

}
