package com.sec.android.app.fm.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.samsung.media.fmradio.FMPlayerException;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.fm.FMRadioFeature;
import com.sec.android.app.fm.Log;
import com.sec.android.app.fm.MainActivity;
import com.sec.android.app.fm.R;
import com.sec.android.app.fm.RadioPlayer;
import com.sec.android.app.fm.RadioToast;
import com.sec.android.app.fm.RecordedFileListPlayerActivity;
import com.sec.android.app.fm.SettingsActivity;
import com.sec.android.app.fm.data.ChannelStore;
import com.sec.android.app.fm.util.FMPermissionUtil;
import com.sec.android.app.fm.util.FMUtil;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class RadioDialogFragment extends DialogFragment {
    private static final String TAG = "RadioDialogFragment";
    private static final String KEY_TYPE = "type";
    private static final String KEY_RENAME_TYPE = "rename_type";
    private static final String KEY_PARAM1 = "param1";

    public static final int SCAN_PROGRESS_DIALOG = 1;
    public static final int SCAN_FINISH_DIALOG = 2;
    public static final int SCAN_OPTION_DIALOG = 3;
    public static final int RECORD_CANCEL_DIALOG = 4;
    public static final int ITEM_DELETE_DIALOG = 5;
    public static final int ITEM_RENAME_DIALOG = 6;
    public static final int CHANGE_FREQ_DIALOG = 7;
    public static final int TTS_DIALOG = 8;

    private ChannelStore mChannelStore = null;
    private OnClickListener mListener = null;
    private String mSavedName = null;
    private EditText mChangeFreqEdit = null;
    Dialog dialog = null;
    ProgressDialog progressDialog = null;

    public static RadioDialogFragment newInstance(int type) {
        RadioDialogFragment radioDialog = new RadioDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(KEY_TYPE, type);

        radioDialog.setArguments(bundle);

        return radioDialog;
    }

    public static RadioDialogFragment newInstance(int type, int renameType, String name) {
        RadioDialogFragment radioDialog = new RadioDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(KEY_TYPE, type);
        bundle.putInt(KEY_RENAME_TYPE, renameType);

        radioDialog.setArguments(bundle);
        radioDialog.setPreviousName(name);

        return radioDialog;
    }

    public static RadioDialogFragment newInstance(int type, String param) {

        RadioDialogFragment radioDialog = new RadioDialogFragment();

        Bundle bundle = new Bundle();
        bundle.putInt(KEY_TYPE, type);
        bundle.putString(KEY_PARAM1, param);

        radioDialog.setArguments(bundle);

        return radioDialog;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mChannelStore = ChannelStore.getInstance();
        int type = getArguments().getInt(KEY_TYPE);
        View view = null;

        switch (type) {
        case SCAN_PROGRESS_DIALOG:
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(R.string.scan);
            progressDialog.setCancelable(true);
            progressDialog.setMessage(this.getString(R.string.dialog_scan));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    RadioPlayer player = RadioPlayer.getInstance();
                    player.cancelScan();
                }
            });
            progressDialog.show();
            break;
        case SCAN_OPTION_DIALOG:
            dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.scan)
                    .setItems(
                            new String[] { getString(R.string.menu_refersh_all),
                                    getString(R.string.menu_refresh) },
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (MainActivity._instance == null)
                                        return;
                                    if (FMUtil.isVoiceActive(getActivity().getApplicationContext(),FMUtil.NEED_TO_PLAY_FM)) {
                                        return;
                                    }

                                    if (which == 0) {
                                        if (MainActivity._instance.startScan(true)) {
                                            ((MainActivity) getActivity()).openDialog(SCAN_PROGRESS_DIALOG);
                                            mChannelStore.removeAllChannel();
                                            AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
                                            if(allChannelListFragment != null && allChannelListFragment.isVisible()) {
                                                allChannelListFragment.notifyDataSetChanged();
                                            }
                                            FavouriteListFragment favouriteListFragment = (FavouriteListFragment) getFragmentManager().findFragmentByTag(FavouriteListFragment.TAG);
                                            if(favouriteListFragment != null && favouriteListFragment.isVisible()) {
                                                favouriteListFragment.notifyDataSetChanged();
                                            }
                                        }
                                    } else {
                                        if (MainActivity._instance.startScan(true)) {
                                            // clear non fav channel
                                            ((MainActivity) getActivity()).openDialog(SCAN_PROGRESS_DIALOG);
                                            mChannelStore.clearNonFavoriteChannel();
                                            AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
                                            if(allChannelListFragment != null && allChannelListFragment.isVisible()) {
                                                allChannelListFragment.notifyDataSetChanged();
                                            }
                                        }
                                    }
                                }
                            }).create();
            ((AlertDialog) dialog).setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    dialog.dismiss();
                }
            });

            break;

        case RECORD_CANCEL_DIALOG:
            dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.cancel)
                    .setMessage(getString(R.string.recording_will_be_cancelled))
                    .setPositiveButton(getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    MainActivity._instance.cancelFMRecording();
                                }
                            })
                    .setNegativeButton(getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            }).create();

            break;

        case ITEM_DELETE_DIALOG:

            String deleteMsg = null;
            if (getActivity() instanceof MainActivity) {
                AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
                if (1 == allChannelListFragment.getCheckedcount())
                    deleteMsg = getString(R.string.dialog_one_station_will_be_deleted);
                else if (1 < allChannelListFragment.getCheckedcount())
                    deleteMsg = (getString(R.string.dialog_multiple_stations_will_be_deleted,
                            allChannelListFragment.getCheckedcount()));
            } else if (getActivity() instanceof RecordedFileListPlayerActivity) {
                RecordedFileListPlayerActivity activity = (RecordedFileListPlayerActivity) getActivity();
                if (1 == activity.getCheckedcount())
                    deleteMsg = getString(R.string.dialog_one_recording_will_be_deleted);
                else if (1 < activity.getCheckedcount())
                    deleteMsg = (getString(R.string.dialog_multiple_recordings_will_be_deleted,
                            activity.getCheckedcount()));
            } else {
                deleteMsg = getString(R.string.dialog_one_item_deleted);
            }
            dialog = new AlertDialog.Builder(getActivity())
                    .setMessage(deleteMsg)
                    .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (getActivity() instanceof RecordedFileListPlayerActivity) {
                                ((RecordedFileListPlayerActivity) getActivity()).deleteFile();
                            } else if (getActivity() instanceof MainActivity) {
                                AllChannelListFragment allChannelListFragment = (AllChannelListFragment) getFragmentManager().findFragmentByTag(AllChannelListFragment.TAG);
                                if(allChannelListFragment != null && allChannelListFragment.isVisible()) {
                                    allChannelListFragment.delete();
                                }
                            }
                        }
                    }).setNegativeButton(R.string.cancel, null).create();

            break;

        case ITEM_RENAME_DIALOG:
            if ((getArguments().getInt(KEY_RENAME_TYPE) == RenameDialog.RENAME_DIALOG_TYPE_FILE) ||
                (getArguments().getInt(KEY_RENAME_TYPE) == RenameDialog.RENAME_DIALOG_TYPE_STATION)) {

                dialog = new RenameDialog(getActivity(), getArguments().getInt(KEY_RENAME_TYPE), null,
                        mSavedName);
            } else {
                Log.e(TAG,"If you want to rename function, This activity have to regist activity in RadioDialogFragment.");
            }

            break;

        case CHANGE_FREQ_DIALOG:
            view = LayoutInflater.from(getActivity()).inflate(R.layout.change_freq_dialog, null);
            mChangeFreqEdit = (EditText) view.findViewById(R.id.edit_text);
            String curFreq = getArguments().getString(KEY_PARAM1);
            if (curFreq != null && curFreq.length() != 0) {
                mChangeFreqEdit.setText(curFreq);
                mChangeFreqEdit.selectAll();
            }
            else{
                mChangeFreqEdit.setHint(R.string.enter_number);
            }

            InputFilter[] filters = new InputFilter[1];
            filters[0] = new InputFilter() {

                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                        int dstart, int dend) {
                    StringBuilder newText = new StringBuilder();
                    newText.append(dest.subSequence(0, dstart));
                    newText.append(source.subSequence(start, end));
                    newText.append(dest.subSequence(dend, dest.length()));

                    String patternString = MainActivity.FREQ_INPUT_PATTERN;
                    if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
                        patternString = MainActivity.FREQ_INPUT_PATTERN_50_SPACE;
                    }
                    Pattern pattern = Pattern.compile(patternString);
                    Matcher matcher = pattern.matcher(newText);

                    patternString = MainActivity.FREQ_PATTERN;
                    if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
                        patternString = MainActivity.FREQ_PATTERN_50_SPACE;
                    }
                    Pattern enablePattern = Pattern.compile(patternString);

                    CharSequence returnText = "";
                    StringBuilder filteredText = new StringBuilder();
                    filteredText.append(dest.subSequence(0, dstart));
                    if (matcher.find()) {
                        returnText = source;
                        filteredText.append(source.subSequence(start, end));
                    }
                    filteredText.append(dest.subSequence(dend, dest.length()));

                    Matcher enableMatcher = enablePattern.matcher(filteredText);
                    if (getDialog() != null)
                        ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE)
                                .setEnabled(enableMatcher.find());

                    return returnText;
                }
            };
            mChangeFreqEdit.setFilters(filters);
            dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.change_frequency)
                    .setView(view)
                    .setPositiveButton(getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String str = mChangeFreqEdit.getText().toString();
                                    if (str.length() == 0)
                                        return;
                                    int inputFreq = -1;
                                    try {
                                        inputFreq = (int) (Float.parseFloat(str) * 100);
                                    } catch (NumberFormatException e) {
                                        Log.e(TAG, "NumberFormatException");
                                        return;
                                    }
                                    try {
                                        RadioPlayer player = RadioPlayer.getInstance();
                                        boolean isOn = player.isOn();
                                        player.tuneAsyncEx(RadioPlayer.getValidFrequency(inputFreq));
                                        if (!isOn)
                                            SettingsActivity.activateTurnOffAlarm();
                                    } catch (FMPlayerException e) {
                                        RadioToast.showToast(getActivity(), e);
                                    }
                                }
                            })
                    .setNegativeButton(getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.cancel();
                                }
                            }).create();

            if (dialog != null && mChangeFreqEdit.getText().toString().length() == 0) {
                dialog.show();
                ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }

            break;

        case TTS_DIALOG:
            dialog = new AlertDialog.Builder(getActivity()).setIcon(R.drawable.alert_dialog_icon)
                    .setTitle(R.string.alert).setMessage(getString(R.string.dialog_tts_confirm))
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (DialogInterface.BUTTON_POSITIVE == whichButton) {
                                Intent installIntent = new Intent("android.intent.action.MAIN");
                                installIntent.setClassName("com.android.settings",
                                        "com.android.settings.TextToSpeechSettings");
                                try {
                                    startActivity(installIntent);
                                } catch(ActivityNotFoundException e) {
                                    Log.d(TAG, "Activity Not Found");
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).setNegativeButton(R.string.cancel, null).create();

            break;

        default:
            break;
        }
        Log.d(TAG, "Dialog is created now");

        MainActivity.ISSIPVISIBLE = true;
        if (type == SCAN_PROGRESS_DIALOG)
            return progressDialog;
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        Log.v(TAG, "onDismiss - " + getTag());

        super.onDismiss(dialog);
    }

    public void setMessage(String message) {
        Log.v(TAG, "setMessage - " + getTag());
        LogDns.v(TAG, "setMessage : " + LogDns.filter(message));
        if (getDialog() != null) {
            ((ProgressDialog) getDialog()).setMessage(message);
        }
    }

    public void setTitle(int resId) {
        Log.v(TAG, "setTitle - " + getTag());
        Log.v(TAG, "setTitle - " + getActivity().getString(resId));
        if (getDialog() != null) {
            ((ProgressDialog) getDialog()).setTitle(resId);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Log.v(TAG, "onCancel - " + getTag());
        if (getTag() != null) {
            if (getTag().equals(String.valueOf(SCAN_PROGRESS_DIALOG))) {
                RadioPlayer player = RadioPlayer.getInstance();
                player.cancelScan();
            } else if (getTag().equals(String.valueOf(CHANGE_FREQ_DIALOG))) {
                mChangeFreqEdit.setText("");
            }
        }
        super.onCancel(dialog);
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        Log.d(TAG, "tag:" + getTag());
        this.mListener = onClickListener;
    }

    @Override
    public void onPause() {
        Log.v(TAG, "onPause - " + getTag());
        if (getTag().equals(String.valueOf(ITEM_RENAME_DIALOG))) {
            if (getDialog() != null) {
                ((RenameDialog) dialog).saveCurrentState();
            }
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(TAG, "onResume - " + getTag());
        if (getTag().equals(String.valueOf(ITEM_RENAME_DIALOG))) {
            if (dialog != null) {
                ((RenameDialog) dialog).show();

                if (getActivity() instanceof RecordedFileListPlayerActivity) {
                    if(!FMPermissionUtil.hasPermission(getActivity().getApplicationContext(), FMPermissionUtil.FM_PERMISSION_REQUEST_RECORDINGS_LIST_RESUME)) {
                        FMPermissionUtil.requestPermission(getActivity(), FMPermissionUtil.FM_PERMISSION_REQUEST_RECORDINGS_LIST_RESUME);
                    }
                }
            }
        }
        if (getTag().equals(String.valueOf(ITEM_DELETE_DIALOG))) {
            if (dialog != null) {
                if (getActivity() instanceof RecordedFileListPlayerActivity) {
                    if(!FMPermissionUtil.hasPermission(getActivity().getApplicationContext(), FMPermissionUtil.FM_PERMISSION_REQUEST_RECORDINGS_LIST_RESUME)) {
                        FMPermissionUtil.requestPermission(getActivity(), FMPermissionUtil.FM_PERMISSION_REQUEST_RECORDINGS_LIST_RESUME);
                    }
                }
            }
        }

        if (getTag().equals(String.valueOf(CHANGE_FREQ_DIALOG))) {
            ((MainActivity) getActivity()).registerBroadcastReceiverSip(true);
            mChangeFreqEdit.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!MainActivity.ISSIPVISIBLE)
                        return;

                    if (getActivity() != null) {
                        InputMethodManager mInputMethodManager = (InputMethodManager) getActivity()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (mChangeFreqEdit.isInTouchMode() && mInputMethodManager.isActive() && mInputMethodManager.isAccessoryKeyboardState() == 0)
                            mInputMethodManager.showSoftInput(mChangeFreqEdit, 0);
                    }
                }
            }, 400);
            mChangeFreqEdit.setOnEditorActionListener(new OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        if (((AlertDialog) getDialog()).getButton(Dialog.BUTTON_POSITIVE)
                                .isEnabled()) {
                            ((AlertDialog) getDialog()).getButton(Dialog.BUTTON_POSITIVE)
                                    .performClick();
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.v(TAG, "onStart - " + getTag());
        if (getTag().equals(String.valueOf(ITEM_RENAME_DIALOG))) {
            if (getDialog() == null) {
                Log.i(TAG, "getDialog() is null");
            }
            if (mListener != null) {
                ((RenameDialog) dialog).setOnclickListener(mListener);
            } else {
                if (getDialog() != null) {
                    getDialog().dismiss();
                }
                Log.d(TAG, "mListener is null");
            }
        }
    }

    private void setPreviousName(String name) {
        mSavedName = name;
    }
}