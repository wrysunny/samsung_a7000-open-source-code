package com.sec.android.app.fm.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.sec.android.app.fm.FMRadioFeature;
import com.sec.android.app.fm.Log;
import com.sec.android.app.fm.R;
import com.sec.android.app.fm.RadioPlayer;

public class FrequencyDisplayBgView extends View {

    private static final String COLOR_RIPPLE_HEX = "#20000000";
    private static final String TAG = "FrequencyDisplayBgView";
    private Drawable mBarImage;
    public double location = 0;
    private double mFrequencyPosition;
    private int mFrequencybarTopOffset;
    int value = 0;
    private Paint mDrawRipple;
    private double mFrequencyStartPosition;
    private double mFrequencyEndPosition;
    private int mFrequencyBarHeight;
    private int mFrequencyBarWidth;
    private final int MIN_FREQUENCY = 8750;
    private final int MAX_FREQUENCY = 10800;

    private OnFrequencyChangeListener mFrequencyChangeListener;
    private OnPositionChangeListener mPositionChangeListener;
    public int frequency;

    public interface OnFrequencyChangeListener {
        public void onFrequencyChanged(long frequency);
    }

    public interface OnPositionChangeListener {
        public void onPositionChanged(double position);
    }

    public void setOnFrequencyChangeListener(OnFrequencyChangeListener listener) {
        Log.d(TAG, "setOnFrequencyChangeListener : ");
        value = 1;
        mFrequencyChangeListener = listener;
    }

    public void setOnPositioinChangeListener(OnPositionChangeListener listener) {
        Log.d(TAG, "setOnPositioinChangeListener : ");
        value = 2;
        mPositionChangeListener = listener;
    }

    public FrequencyDisplayBgView(Context context) {
        super(context);
        setFocusable(true);
    }

    public FrequencyDisplayBgView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.frequencyBar);
        try {
            mBarImage = a.getDrawable(R.styleable.frequencyBar_barImage);
            mFrequencyEndPosition = a.getDimension(R.styleable.frequencyBar_frequencyMax,301F);
            mFrequencyStartPosition = a.getDimension(R.styleable.frequencyBar_frequencyMin, 55F);
            mFrequencyPosition = a.getInt(R.styleable.frequencyBar_frequencyPosition, 0);
            mFrequencybarTopOffset = (int) getContext().getResources().getDimensionPixelSize(R.dimen.frequencybar_top_offset);
            mFrequencyBarHeight = (int)a.getDimension(R.styleable.frequencyBar_frequencyBarHeight, 53F);
            mFrequencyBarWidth = (int)a.getDimension(R.styleable.frequencyBar_frequencyBarWidth, 15F);
            mDrawRipple = new Paint();
            mDrawRipple.setColor(Color.parseColor(COLOR_RIPPLE_HEX));
            mDrawRipple.setAntiAlias(true);
        } finally{
            a.recycle();
        }
    }

    public void setFrequency(int mfrequency) {
        Log.d(TAG, "setFrequency : "+mfrequency);
        frequency = mfrequency;
        mFrequencyPosition = ((frequency - MIN_FREQUENCY) * (mFrequencyEndPosition - mFrequencyStartPosition))/ (MAX_FREQUENCY - MIN_FREQUENCY);;
        invalidate();
    }

    public boolean hasFrequencyChangeListener(){
        return mFrequencyChangeListener != null;
    }

    public void getFrequencyPosition(int pos) {
        Log.d(TAG, "getFrequencyPosition : "+pos);
        if(pos < mFrequencyStartPosition){
            pos = (int)(mFrequencyStartPosition);
        }
        else if(pos > mFrequencyEndPosition){
            pos = (int)(mFrequencyEndPosition);
        }
        mFrequencyPosition = pos - mFrequencyStartPosition;
        frequency = (int) ((mFrequencyPosition) * (MAX_FREQUENCY - MIN_FREQUENCY)/ (mFrequencyEndPosition - mFrequencyStartPosition) + MIN_FREQUENCY);
        if (FMRadioFeature.GetFrequencySpace() == FMRadioFeature.NARROW_WIDTH_SCANSPACE) {
            calculateFrequncyNarrowScan();
        }
        Log.d(TAG, "mFrequencyPosition : "+mFrequencyPosition);
        if (frequency < MIN_FREQUENCY) {
            frequency = MIN_FREQUENCY;
        } else if(frequency > MAX_FREQUENCY){
            frequency = MAX_FREQUENCY;
        }
        mPositionChangeListener.onPositionChanged(mFrequencyPosition);
        mBarInFocus = true;
        invalidate();
    }

    public void frequencyChange() {
        if(mFrequencyChangeListener != null){
            mFrequencyChangeListener.onFrequencyChanged(MAX_FREQUENCY);
        }
        mBarInFocus = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "onDraw : mFrequencybarTopOffset:- "+mFrequencybarTopOffset);
        super.onDraw(canvas);
        RadioPlayer player = RadioPlayer.getInstance();
        mFrequencybarTopOffset = (canvas.getHeight() - mFrequencyBarHeight)/2;
        location = calculateFrequencyPosition();
        if (player.isOn()) {
            mBarImage.setBounds((int) location, mFrequencybarTopOffset, (int) (location + mFrequencyBarWidth),mFrequencyBarHeight + mFrequencybarTopOffset);
        } else {
            mBarImage.setBounds(0, 0, 0, 0);
        }
        if(mBarInFocus)
            canvas.drawCircle((float) (location+mFrequencyBarWidth/2), mFrequencybarTopOffset+mFrequencyBarHeight/2, mFrequencyBarHeight/2, mDrawRipple);
        mBarImage.draw(canvas);
    }

    private double calculateFrequencyPosition() {
        double distance = 0;
        double resultedDistance = 0;
        Log.d(TAG, "value:   "+value);
        if (value == 2) {
            distance = mFrequencyPosition;
            Log.d(TAG, "mFrequencyPosition : "+mFrequencyPosition);
            frequency = (int) ((distance) * (MAX_FREQUENCY - MIN_FREQUENCY)/ (mFrequencyEndPosition - mFrequencyStartPosition) + MIN_FREQUENCY);
            if (frequency < MIN_FREQUENCY) {
                frequency = MIN_FREQUENCY;
            } else if(frequency > MAX_FREQUENCY){
                frequency = MAX_FREQUENCY;
            }
            Log.d(TAG, "calculateFrequencyPosition: frequency  "+frequency);
        } else if (value == 1) {
              distance = ((frequency - MIN_FREQUENCY) * (mFrequencyEndPosition - mFrequencyStartPosition))/ (MAX_FREQUENCY - MIN_FREQUENCY);
        }
        Log.d(TAG, "calculateFrequencyPosition: distance  "+distance);
        Log.d(TAG, "mFrequencyStartPosition:   "+mFrequencyStartPosition);
        resultedDistance = mFrequencyStartPosition + distance;
        return resultedDistance;
    }

    private void calculateFrequncyNarrowScan() {
        int lastNumber = frequency % 10;

        frequency = frequency - lastNumber;
        if ((lastNumber >= 3) && (lastNumber <= 7)) {
            frequency = frequency + 5;

        } else if (lastNumber >= 8) {
            frequency = frequency + 10;
        }
    }

    private boolean mBarInFocus;

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction,
       Rect previouslyFocusedRect) {
       Log.d(TAG, "onFocusChanged : "+gainFocus);
       mBarInFocus = gainFocus;
       invalidate();
       super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
   }

    public void initializeFrequencyBar() {
        mBarInFocus = false;
        invalidate();
    }
}
