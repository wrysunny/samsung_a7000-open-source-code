package com.sec.android.app.fm.ui;

import com.sec.android.app.fm.Log;
import com.sec.android.app.fm.MainActivity;
import com.sec.android.app.fm.R;
import com.sec.android.app.fm.util.EmojiList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class RenameDialog extends AlertDialog implements OnClickListener {

    public static final int RENAME_DIALOG_TYPE_STATION = 0;
    public static final int RENAME_DIALOG_TYPE_FILE = 1;
    public static final int RENAME_DIALOG_TYPE_MAX = 2;
    private static final int STATION_NAME_MAX = 12;
    private static final int FILE_NAME_MAX = 50;
    public static final String FILE_INVALID_CHAR[] = { "\\", "/", ":", "*", "?", "\"", "<", ">",
            "|", "\n" };
    private static final String TAG = RenameDialog.class.getSimpleName();

    private Context mContext;
    private EditText mEditText;
    private TextInputLayout mTextInputLayout;
    private String mPrevEditText;
    private OnClickListener mListener;
    private InputMethodManager mInputManager;
    private int mType;

    public RenameDialog(final Context context, final int type, final OnClickListener listener,
            String name) {
        super(context);

        if (MainActivity._instance != null)
            MainActivity._instance.registerBroadcastReceiverSip(true);

        this.mContext = context;

        View layout = LayoutInflater.from(context).inflate(R.layout.rename, null);
        mTextInputLayout = (TextInputLayout) layout.findViewById(R.id.input_name);

        //TextView textView = (TextView) layout.findViewById(R.id.text_view);

        mEditText = (EditText) layout.findViewById(R.id.edit_text);
        mEditText.setFilters(getInputFilters(type));
        mEditText.setSingleLine();
        mEditText.setFocusable(true);
        mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        if (name != null && name.length() != 0) {
            mEditText.setText(name);
            mEditText.selectAll();
        }
        mPrevEditText = name;

        this.mType = type;
        switch (type) {
        case RENAME_DIALOG_TYPE_STATION:
            //textView.setText(context.getString(R.string.dialog_station_name));
            mEditText.setHint(R.string.rename_text);
            mEditText.setPrivateImeOptions("inputType=PredictionOff;inputType=filename;disableEmoticonInput=true");
            break;
        case RENAME_DIALOG_TYPE_FILE:
            //textView.setText(context.getString(R.string.dialog_new_file_name));
            mEditText.setHint(R.string.rename_text);
            mEditText.setPrivateImeOptions("inputType=filename");
            break;
        default:
            break;
        }

        mEditText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public synchronized void afterTextChanged(Editable s) {
                refreshPositiveBtn(s.toString(), type);
            }
        });

        this.mListener = listener;

        setTitle(R.string.rename);
        setView(layout);

        mInputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        setButton(BUTTON_POSITIVE, getContext().getString(R.string.rename), this);
        setButton(BUTTON_NEGATIVE, getContext().getString(R.string.cancel), this);
        setIcon(0);

        MainActivity.ISSIPVISIBLE = true;
    }

    public void setErrorMessage(String message) {
        mTextInputLayout.setErrorEnabled(true);
        mTextInputLayout.setError(message);
    }

    private InputFilter[] getInputFilters(final int type) {
        InputFilter[] filterArray = null;
        switch (type) {
        case RENAME_DIALOG_TYPE_STATION: {
            filterArray = new InputFilter[2];
            filterArray[0] = new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                        int dstart, int dend) {
                    String expected = new String();
                    expected += dest.subSequence(0, dstart);
                    expected += source.subSequence(start, end);
                    expected += dest.subSequence(dend, dest.length());

                    if (expected.length() > STATION_NAME_MAX) {
                        setErrorMessage(mContext.getString(R.string.toast_maximum_file_name, STATION_NAME_MAX));
                        if (source.length() > STATION_NAME_MAX) {
                            int selection = mEditText.getSelectionEnd();
                            // editText.setText(dest);
                            mEditText.setSelection(selection);
                            return source.toString().subSequence(0, STATION_NAME_MAX);
                        } else
                            return source.subSequence(0, source.length());
                    }
                    mTextInputLayout.setError(null);
                    mTextInputLayout.setErrorEnabled(false);
                    return null;
                }
            };
            filterArray[1] = new InputFilter.LengthFilter(12);
        }
            break;
        case RENAME_DIALOG_TYPE_FILE: {
            filterArray = new InputFilter[2];
            filterArray[0] = new InputFilter() {

                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                        int dstart, int dend) {
                    if (EmojiList.hasEmojiString(source)) {
                        setErrorMessage(mContext.getString(R.string.toast_invalid_character));
                        return "";
                    }
                    if (end != 0) {
                        for (int i = 0; i < end; i++) {
                            int value = source.toString().codePointAt(i);
                            if ((value >= 0xFE000 && value < 0xFF000)
                                    || (value >= 0xE63E && value <= 0xE757)) {
                                setErrorMessage(mContext.getString(R.string.toast_invalid_character));
                                return "";
                            }
                        }
                    }

                    String origTxt = source.subSequence(start, end).toString();
                    String validTxt = origTxt;
                    boolean invalidFlag = false;
                    String exceptEmojiTxt = EmojiList.exceptEmojiString(validTxt);
                    if (!validTxt.equals(exceptEmojiTxt)) {
                        invalidFlag = true;
                        validTxt = exceptEmojiTxt;
                    }
                    for (int i = 0; i < FILE_INVALID_CHAR.length; ++i) {
                        int validTxtLength = validTxt.length();
                        for (int j = 0; j < validTxtLength; ++j) {
                            int index = validTxt.indexOf(FILE_INVALID_CHAR[i]);
                            if (index >= 0) {
                                invalidFlag = true;
                                if (index < validTxt.length()) {
                                    validTxt = validTxt.substring(0, index)
                                            + validTxt.substring(index + 1);
                                }
                            } else {
                                char c = FILE_INVALID_CHAR[i].charAt(0);
                                if (c >= 0x21 && c < 0x7e && c != 0x3f) {
                                    c += 0xfee0;
                                    int iDBC = validTxt.indexOf(c);
                                    if (iDBC >= 0) {
                                        invalidFlag = true;
                                        if (iDBC < validTxt.length()) {
                                            validTxt = validTxt.substring(0, iDBC)
                                                    + validTxt.substring(iDBC + 1);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (invalidFlag) {
                        setErrorMessage(mContext.getString(R.string.toast_invalid_character));
                        return validTxt;
                    }

                    String expected = new String();
                    expected += dest.subSequence(0, dstart);
                    expected += source.subSequence(start, end);
                    expected += dest.subSequence(dend, dest.length());

                    if (expected.length() > FILE_NAME_MAX) {
                        setErrorMessage(mContext.getString(R.string.toast_maximum_file_name, FILE_NAME_MAX));
                        if (validTxt.length() > FILE_NAME_MAX) {
                            int selection = mEditText.getSelectionEnd();
                            // editText.setText(dest);
                            mEditText.setSelection(selection);
                            return validTxt.toString().subSequence(0, FILE_NAME_MAX);
                        } else
                            return source.subSequence(0, source.length());
                    }
                    mTextInputLayout.setError(null);
                    mTextInputLayout.setErrorEnabled(false);
                    return null;
                }
            };
            filterArray[1] = new InputFilter.LengthFilter(FILE_NAME_MAX);
        }
            break;
        default:
            break;
        }
        return filterArray;
    }

    public String getText() {
        return mEditText.getText().toString();
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        dismiss();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mListener == null) {
            Log.e(TAG, "onClick listener is null!!");
            return;
        }
        mListener.onClick(dialog, which);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mType == RENAME_DIALOG_TYPE_FILE) {
            // Prevent to close dialog.
            getButton(BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    mListener.onClick(RenameDialog.this, BUTTON_POSITIVE);
                }
            });
        }
    }

    @Override
    public void show() {
        super.show();
        refreshPositiveBtn(mEditText.getText().toString().trim(), mType);
        mEditText.requestFocus();
        mEditText.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!MainActivity.ISSIPVISIBLE || mInputManager.isAccessoryKeyboardState() != 0)
                    return;

                if (mEditText.isInTouchMode() && MainActivity.ISSIPVISIBLE)
                    mInputManager.showSoftInput(mEditText, 0);
            }
        }, 300);
    }

    public void setText(String text) {
        Log.d(TAG, "setChannel()");
        if (text == null) {
            Log.w(TAG, "null text");
            return;
        }
        mEditText.setText(text);
    }

    public void saveCurrentState() {
        Log.d(TAG, "saveCurrentState()");
    }

    public void setOnclickListener(OnClickListener listener) {
        Log.d(TAG, "setOnClickListener()");
        this.mListener = listener;
    }

    private void refreshPositiveBtn(String str, int type) {
        if (isShowing()) {
            Button btn = getButton(BUTTON_POSITIVE);

            if (btn != null) {
                switch (type) {
                case RENAME_DIALOG_TYPE_STATION:
                    if (str.toString().trim().equals(mPrevEditText))
                        btn.setEnabled(false);
                    else
                        btn.setEnabled(true);
                    break;

                case RENAME_DIALOG_TYPE_FILE:
                    if (str.toString().trim().equals("")
                            || str.toString().trim().equals(mPrevEditText))
                        btn.setEnabled(false);
                    else
                        btn.setEnabled(true);
                    break;

                default:
                    break;
                }
            }
        }
    }
}
