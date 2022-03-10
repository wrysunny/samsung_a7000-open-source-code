package com.sec.android.app.fm.ui;

import com.sec.android.app.fm.R;
import com.sec.android.app.fm.SettingsActivity;
import com.sec.android.app.fm.R.styleable;
import com.sec.android.app.fm.SettingsActivity.SettingsFragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class SpinnerPreference extends Preference {
    private static final String TAG = "SpinnerPreference";
    private String mValue;
    private String mKey;
    private Spinner mSpinner;

    private Context mContext;
    private ArrayAdapter<String> mAdapter;
    private String[] mEntries;
    private String[] mEntryValues;
    private String mDefaultValues;

    public SpinnerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }
    
    public void initSpinnerPreference(String[] entries, String[] entryValues, String defaultValues) {
        mEntries = entries;
        mEntryValues = entryValues;
        mDefaultValues = defaultValues;

        mAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, mEntries);

        mKey = getKey();
        mValue = getValue();

        mSpinner = new Spinner(mContext);

        mSpinner.setVisibility(View.INVISIBLE);
        mSpinner.setAdapter(mAdapter);
        if (mEntryValues == null) {
            mSpinner.setSelection(0);
        } else {
            mSpinner.setSelection(findIndexOfValue(getValue()));
        }
        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3) {
                Log.d(TAG, "onItemSelected : " + position);
                if (mValue != mEntryValues[position]) {
                    if (!callChangeListener(mEntryValues[position])) {
                        return;
                    }
                }
                mValue = mEntryValues[position];
                setSummary(mEntries[position]);
                persistString(mValue);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
        setPersistent(true);
        setOnPreferenceClickListener(new OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference arg0) {
                mSpinner.setSoundEffectsEnabled(false);
                mSpinner.performClick();
                return true;
            }
        });
        setDefaultValue(mDefaultValues);
    }

    public String getEntry() {
        return mEntries[findIndexOfValue(getValue())].toString();
    }

    public String getValue() {
        mValue = PreferenceManager.getDefaultSharedPreferences(mContext).getString(mKey, mDefaultValues);
        return mValue;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        if (view.equals(mSpinner.getParent()))
            return;
        if (mSpinner.getParent() != null) {
            ((ViewGroup) mSpinner.getParent()).removeView(mSpinner);
        }
        ViewGroup vg = (ViewGroup) view;
        vg.addView(mSpinner, 0);
        ViewGroup.LayoutParams lp = mSpinner.getLayoutParams();
        lp.width = 0;
        mSpinner.setLayoutParams(lp);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (restoreValue) {
            mValue = getPersistedString(mValue);
        } else {
            String temp;
            temp = defaultValue.toString();
            persistString(temp);
            mValue = temp;
        }

    }

    public int findIndexOfValue(String value) {
        for (int i = 0, n = mEntryValues.length; i < n; ++i) {
            if (mEntryValues[i].equals(value))
                return i;
        }
        return -1;
    }
    public void setSelection(int position) {
        mSpinner.setSelection(position);
        setSummary(mEntries[position]);
    }

    public void setEntries(String[] entries) {
        mEntries = entries;
    }

    public void setEntryValues(String[] entryValues) {
        mEntryValues = entryValues;

        mAdapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_dropdown_item, mEntries);
        mSpinner.setAdapter(mAdapter);
        mSpinner.setSelection(findIndexOfValue(getValue()));

    }
}