package com.sec.android.app.dns.ui;

import com.sec.android.app.dns.LogDns;
import com.sec.android.app.fm.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class DnsKoreaTestActivity extends Activity implements OnCheckedChangeListener {
    public static final boolean KOREA_TEST_WITHOUT_MCC = false;
    private static String sCountryCode = "gb";
    private static boolean sIsKoreaTest = false;
    private static String sPi = "c2a1";
    private static final String TAG = "DnsKoreaTestActivity";

    public static String getCountryCode() {
        return sCountryCode;
    }

    public static String getPi() {
        return sPi;
    }

    public static boolean isKoreaTest() {
        return sIsKoreaTest;
    }

    private EditText current_countrycode;
    private EditText current_pi;
    private Switch menableDnsTestMode;
    private Switch set_koreatest;

    private void initialsetup() {
        TextView koreaTestTextTitle;
        TextView setKoreaTestTextView;
        TextView currentPiKoreaTestTextView;
        TextView current_countrycodeView;
        koreaTestTextTitle = (TextView) findViewById(R.id.koreatest);
        koreaTestTextTitle.setText("KOREA Test");
        setKoreaTestTextView = (TextView) findViewById(R.id.set_koreatest_view);
        setKoreaTestTextView.setText("Set Korea Test");
        current_countrycodeView = (TextView) findViewById(R.id.korea_test_countrycode_view);
        current_countrycodeView.setText("Set Country Code");
        currentPiKoreaTestTextView = (TextView) findViewById(R.id.korea_test_pi_view);
        currentPiKoreaTestTextView.setText("Set RDS PI");
        menableDnsTestMode = (Switch) findViewById(R.id.enablednstestactivityvalue);
        menableDnsTestMode.setText("EnableDNSTestActivity");
        menableDnsTestMode.setChecked(DnsTestActivity.isDnsTest());
        menableDnsTestMode.setOnCheckedChangeListener(this);
        set_koreatest = (Switch) findViewById(R.id.set_koreatest);
        set_koreatest.setChecked(sIsKoreaTest);
        set_koreatest.setOnCheckedChangeListener(this);
        current_countrycode = (EditText) findViewById(R.id.cur_countrycode);
        current_countrycode.setText(sCountryCode);
        current_pi = (EditText) findViewById(R.id.cur_pi);
        current_pi.setText(sPi);
        Button applyKoreaTest = (Button) findViewById(R.id.applykoreatestvalue);
        View.OnClickListener mClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                LogDns.d(TAG, "SetOnclick");
                switch (arg0.getId()) {
                case R.id.applykoreatestvalue:
                    sCountryCode = current_countrycode.getText().toString();
                    sPi = current_pi.getText().toString();
                    LogDns.d(TAG, "set Current Country Code , RDS PI : " + " | " + sPi);
                    if (!sIsKoreaTest)
                        Toast.makeText(DnsKoreaTestActivity.this,
                                "please enable Korea Test Switch", Toast.LENGTH_SHORT).show();
                    else if (sCountryCode.isEmpty() || sPi.isEmpty())
                        Toast.makeText(DnsKoreaTestActivity.this,
                                "please set Country Code and RDS PI", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(DnsKoreaTestActivity.this,
                                "Apply Korea Test values Complete~!!", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
                }
            }
        };

        applyKoreaTest.setOnClickListener(mClickListener);
        applyKoreaTest.setText("Apply");
    }

    public void onCheckedChanged(CompoundButton button, boolean value) {
        LogDns.d(TAG, "onCheckedChanged " + value);
        switch (button.getId()) {
        case R.id.enablednstestactivityvalue:
            LogDns.d(TAG, "en/disable DNS TestActivity " + value);
            DnsTestActivity.setDnsTest(value);
            break;
        case R.id.set_koreatest:
            LogDns.d(TAG, "en/disable SetkoreaTest " + value);
            sIsKoreaTest = value;
            break;
        default:
            break;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hybridradio_testmode);
        initialsetup();
    }
}
