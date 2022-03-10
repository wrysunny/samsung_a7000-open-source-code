package com.sec.android.app.fm.ui;

import com.sec.android.app.fm.MainActivity;
import com.sec.android.app.fm.R;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;


public class FavoriteButton extends FrameLayout {

    private MainActivity mActivity;

    private TextView mFrequencyTextView , mStationTextView;
    private ImageView mAddImageView;
    private LinearLayout mChannelButton;

    private String mFrequencyText = "";
    private String mStationText = "";

    public static final int STATE_ADD = 0;
    public static final int STATE_CHANNEL = 1;
    private int mState;

    private static final int FREQUENCY_TEXT_ID = 10;
    private static final int STATION_TEXT_ID = 20;

    private float mBigFreqTxtSize;
    private float mSmallFreqTxtSize;
    private float mStationTxtSize;

    private Context mContext;

    private static int sTextFrequencyNormalColor;
    private static int sTextFocusColor;

    public FavoriteButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FavoriteButton);

        mBigFreqTxtSize = a.getDimension(R.styleable.FavoriteButton_fav_more_than_4_frq_txt_size, 16);
        mSmallFreqTxtSize = a.getDimension(R.styleable.FavoriteButton_fav_less_than_4_frq_txt_size, 16);
        mStationTxtSize = a.getDimension(R.styleable.FavoriteButton_station_text_size, 16);

        a.recycle();

        mFrequencyTextView = new TextView(context);
        mFrequencyTextView.setId(FREQUENCY_TEXT_ID);
        mFrequencyTextView.setSingleLine();
        mFrequencyTextView.setFocusable(false);
        mFrequencyTextView.setTypeface(Typeface.SANS_SERIF);
        mFrequencyTextView.setGravity(Gravity.CENTER_VERTICAL);
        mFrequencyTextView.setDuplicateParentStateEnabled(true);

        mStationTextView = new TextView(context);
        mStationTextView.setId(STATION_TEXT_ID);
        mStationTextView.setSingleLine();
        mStationTextView.setFocusable(false);
        mStationTextView.setEllipsize(TruncateAt.END);
        mStationTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mStationTxtSize);
        mStationTextView.setTypeface(Typeface.SANS_SERIF);
        mStationTextView.setGravity(Gravity.CENTER);
        mStationTextView.setDuplicateParentStateEnabled(true);
        mChannelButton = new LinearLayout(context);
        mChannelButton.setGravity(Gravity.CENTER);
        mChannelButton.setOrientation(LinearLayout.VERTICAL);
        mChannelButton.setDuplicateParentStateEnabled(true);

        setFocusable(true);
        setClickable(true);

        mAddImageView = new ImageView(context);
        mAddImageView.setFocusable(false);
        mAddImageView.setFocusableInTouchMode(false);
        mAddImageView.setDuplicateParentStateEnabled(true);
        mAddImageView.setImageDrawable(context.getResources().getDrawable(
                R.drawable.fmradio_stations_num_normal_bg,null));

        setBackgroundResource(0);

        mActivity = (MainActivity) context;

        makeFavoriteButtonTextColor();

        setOnClickListener(mActivity.mClickListener);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mChannelButton.addView(mFrequencyTextView, new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mChannelButton.addView(mStationTextView, new LinearLayout.LayoutParams(
        		getResources().getDimensionPixelSize(R.dimen.station_favorite_text_view_width), LayoutParams.WRAP_CONTENT));
        addView(mAddImageView, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT, Gravity.CENTER));
        addView(mChannelButton, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT));
        setFreqTextColorAsNormal();
        getDataFromDb();

        if (mFrequencyText.length() == 0) {
            setState(STATE_ADD);
        } else {
            setState(STATE_CHANNEL);
        }

        setFrequencyText();
    }

    public void makeFavoriteButtonTextColor() {
        sTextFrequencyNormalColor = mContext.getResources().getColor(R.color.favorite_text_normal_frequency_color,null);
        sTextFocusColor = mContext.getResources().getColor(R.color.favorite_text_playing_frequency_color,null);
    }

    private void getDataFromDb() {
        if (mFrequencyText == null) {
            mFrequencyText = "";
        }
    }

    public void setState(int state) {
        mState = state;
        changeVisibility();
        invalidate();
    }

    public int getState() {
        return mState;
    }

    public String getFrequencyText() {
        return mFrequencyText;
    }

    public void setFrequencyText(String frequencyText) {
        mFrequencyText = frequencyText;
        mFrequencyTextView.setText(frequencyText);
        String desc = mContext.getString(R.string.desc_button,
                mContext.getString(R.string.desc_favorite, frequencyText));
        setContentDescription(desc);
        mFrequencyTextView.setContentDescription(desc);
        this.setFocusable(false);
        this.setEnabled(true);
        invalidate();
    }

    public void setFrequencyText(String frequencyText,String stationText) {
        mFrequencyText = frequencyText;
        mStationText = stationText;
        mFrequencyTextView.setText(frequencyText);
        mStationTextView.setText(stationText);
        String desc = mContext.getString(R.string.desc_button,
                mContext.getString(R.string.desc_favorite, frequencyText+stationText));
        setContentDescription(desc);
        mFrequencyTextView.setContentDescription(desc);
        this.setFocusable(false);
        this.setEnabled(true);
        invalidate();
    }

    private void setFrequencyText() {
        mFrequencyTextView.setText(mFrequencyText);
        String desc = mContext.getString(R.string.desc_button,
                mContext.getString(R.string.desc_favorite, mFrequencyText));
        setContentDescription(desc);
        mFrequencyTextView.setContentDescription(desc);
        this.setFocusable(false);
        this.setAlpha(1.0F);
        this.setEnabled(true);
        invalidate();
    }

    public void setFreqTextColorAsPlay() {
        mFrequencyTextView.setTextColor(sTextFocusColor);
        mAddImageView.setImageDrawable(mContext.getResources().getDrawable(
                R.drawable.favchannel_btn_play_bg,null));

    }

    public void setFreqTextColorAsNormal() {
        mFrequencyTextView.setTextColor(sTextFrequencyNormalColor);
        mAddImageView.setImageDrawable(mContext.getResources().getDrawable(
                R.drawable.favchannel_btn_normal_bg,null));

    }

    public void setFreqTextColorAsPlay(int fav_channels) {
        mFrequencyTextView.setTextColor(sTextFocusColor);
        mStationTextView.setTextColor(sTextFocusColor);
        android.view.ViewGroup.LayoutParams favChannelLayout = this.getLayoutParams();
        if (fav_channels > 4) {
            mAddImageView.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.favchannel_btn_play_bg,null));
            favChannelLayout.width = getResources().getDimensionPixelSize(R.dimen.station_favorite_icon_size_5);
            favChannelLayout.height = getResources().getDimensionPixelSize(R.dimen.station_favorite_icon_size_5);
            mFrequencyTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mBigFreqTxtSize);
            mStationTextView.setVisibility(View.GONE);
        } else {
            mAddImageView.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.favchannel_btn_play_bg_1,null));
            favChannelLayout.width = getResources().getDimensionPixelSize(R.dimen.station_favorite_icon_size_4);
            favChannelLayout.height = getResources().getDimensionPixelSize(R.dimen.station_favorite_icon_size_4);
            mFrequencyTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSmallFreqTxtSize);
            mStationTextView.setVisibility(mStationText.equals("")?View.GONE:View.VISIBLE);
        }
        this.setLayoutParams(favChannelLayout);
    }

    public void setFreqTextColorAsNormal(int fav_channels) {
        mFrequencyTextView.setTextColor(sTextFrequencyNormalColor);
        mStationTextView.setTextColor(sTextFrequencyNormalColor);
        android.view.ViewGroup.LayoutParams favChannelLayout = this.getLayoutParams();
        if (fav_channels > 4) {
            mAddImageView.setImageDrawable(mContext.getResources().getDrawable(
                    R.drawable.favchannel_btn_normal_bg,null));
            favChannelLayout.width = getResources().getDimensionPixelSize(R.dimen.station_favorite_icon_size_5);
            favChannelLayout.height = getResources().getDimensionPixelSize(R.dimen.station_favorite_icon_size_5);
            mFrequencyTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mBigFreqTxtSize);
            mStationTextView.setVisibility(View.GONE);
        } else {
            try {
                mAddImageView.setImageDrawable(mContext.getResources().getDrawable(
                        R.drawable.favchannel_btn_normal_bg_1,null));
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
            favChannelLayout.width = getResources().getDimensionPixelSize(R.dimen.station_favorite_icon_size_4);
            favChannelLayout.height = getResources().getDimensionPixelSize(R.dimen.station_favorite_icon_size_4);
            mFrequencyTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mSmallFreqTxtSize);
            mStationTextView.setVisibility(mStationText.equals("")?View.GONE:View.VISIBLE);
        }
    }

    private void changeVisibility() {
        switch (mState) {
        case STATE_ADD:
            this.setVisibility(View.INVISIBLE);
            setEnabled(false);
            setFocusable(false);
            break;

        case STATE_CHANNEL:
            this.setVisibility(View.VISIBLE);
            setEnabled(true);
            setFocusable(false);
            break;

        default:
            break;
        }
    }
}
