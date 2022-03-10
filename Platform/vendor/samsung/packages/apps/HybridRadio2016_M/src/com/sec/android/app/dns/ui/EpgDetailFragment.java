package com.sec.android.app.dns.ui;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import com.samsung.media.fmradio.FMEventListener;
import com.samsung.media.fmradio.FMPlayerException;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.RadioDNSUtil;
import com.sec.android.app.dns.radioepg.PiData;
import com.sec.android.app.dns.radioepg.PiData.Programme;
import com.sec.android.app.fm.FMRadioFeature;
import com.sec.android.app.fm.Log;
import com.sec.android.app.fm.MainActivity;
import com.sec.android.app.fm.R;
import com.sec.android.app.fm.RadioPlayer;
import com.sec.android.app.fm.RadioToast;
import com.sec.android.app.fm.SettingsActivity;
import com.sec.android.app.fm.data.PiDataManager;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class EpgDetailFragment extends Fragment {
    private static final String TAG = "EpgDetailFragment";

    private PiDataManager mPiDataManager = PiDataManager.getInstance();
    private ImageView mLogoImage = null;
    private TextView mTitle = null;
    private TextView mFrequency = null;
    private TextView mDescription = null;
    private LinearLayout mListenBtn = null;
    private LinearLayout mListeningLayout = null;
    private ImageView mPlayingAni = null;
    private FrameLayout mListenFrame = null;
    private int mCurrentFrequency = -1;
    private int mIndex = 0;
    private DisplayTask mDisplayTask = null;
    private RadioPlayer mPlayer = RadioPlayer.getInstance();

    private static final int DISPLAY_BUTTON = 1;
    private static final int DISPLAY_PLAYING_ANI = 2;
    private static final int DISPLAY_NOTHING = 3;

    private FMEventListener mFmListener = new FMEventListener() {

        @Override
        public void onOff(int reasonCode) {
            Log.d(TAG, "onOff");
            if (isCurrentContents()) {
                showNowPlayingLayout(DISPLAY_BUTTON);
            } else {
                showNowPlayingLayout(DISPLAY_NOTHING);
            }
        }

        @Override
        public void onTune(long frequency) {
            Log.d(TAG, "onTune");
            if (isCurrentContents()) {
                Log.d(TAG, "test mPlayer.getFrequency()-" + mPlayer.getFrequency());
                Log.d(TAG, "test mCurrentFrequency-" + mCurrentFrequency);
                if (mPlayer.getFrequency() == mCurrentFrequency) {
                    showNowPlayingLayout(DISPLAY_PLAYING_ANI);
                } else {
                    showNowPlayingLayout(DISPLAY_BUTTON);
                }
            }
        }
    };

    public static EpgDetailFragment newInstance(final int frequency, final int index) {
        EpgDetailFragment frag = new EpgDetailFragment();
        Bundle args = new Bundle();
        args.putInt("channel", frequency);
        args.putInt("index", index);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        mIndex = getArguments().getInt("index");
        mCurrentFrequency = getArguments().getInt("channel");
        PiData currentPi = mPiDataManager.getPiData(mCurrentFrequency);
        if (currentPi == null) {
            LogDns.e(TAG, "current PI is null -" + mCurrentFrequency);
            return null;
        }

        int countOfProgram = currentPi.getNumberOfPrograms();
        if (countOfProgram <= mIndex) {
            LogDns.e(TAG, "Out of Index - index:" + mIndex);
            return null;
        }
        Programme programme = currentPi.getProgram(mIndex);
        View v = inflater.inflate(R.layout.epg_detail_item, null);
        mLogoImage = (ImageView) v.findViewById(R.id.selected_image);
        mLogoImage.setContentDescription(programme.getName());
        if (programme.checkExistedImage()) {
            LogDns.d(TAG, "get Image - exist");
            if (FMRadioFeature.EPG_PI_IMAGE_CACHE) {
                mLogoImage.setImageBitmap(programme.getCachedImage());
            } else {
                mLogoImage.setImageBitmap(programme.getImage());
            }
        } else {
            LogDns.d(TAG, "get Image - null");
            mLogoImage.setImageResource(R.drawable.hybrid_radio_dns_img);
            mDisplayTask = (DisplayTask) new DisplayTask(getActivity().getApplicationContext(),
                    programme).execute();
        }

        mTitle = (TextView) v.findViewById(R.id.selected_channel_title);
        mTitle.setText(programme.getName());

        mFrequency = (TextView) v.findViewById(R.id.selected_channel_frequency);
        mFrequency.setText(RadioPlayer.convertToMhz(mCurrentFrequency) + "Mhz");

        mDescription = (TextView) v.findViewById(R.id.selected_channel_description);
        mDescription.setText(programme.getDescription());

        mListenFrame = (FrameLayout) v.findViewById(R.id.epg_listen_frame);
        mListeningLayout = (LinearLayout) v.findViewById(R.id.listening_now);
        mPlayingAni = (ImageView) v.findViewById(R.id.now_playing_ani);
        mListenBtn = (LinearLayout) v.findViewById(R.id.listen_button);
        mListenBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Log.d(TAG, "onClick - listen Button : " + mFrequency.getText());
                String sFreq = (String) mFrequency.getText();
                int frequency = (int) (Float.parseFloat(sFreq.substring(0, sFreq.length() - 3)) * 100);
                try {
                    RadioPlayer player = RadioPlayer.getInstance();
                    boolean isOn = player.isOn();
                    player.tuneAsyncEx(frequency);
                    if (!isOn)
                        SettingsActivity.activateTurnOffAlarm();
                } catch (FMPlayerException e) {
                    RadioToast.showToast(getActivity(), e);
                } finally {
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        });

        if (isCurrentContents()) {
            mListenFrame.setVisibility(View.VISIBLE);

            if (mPlayer.isOn()) {
                if (mPlayer.getFrequency() == mCurrentFrequency) {
                    showNowPlayingLayout(DISPLAY_PLAYING_ANI);
                } else {
                    showNowPlayingLayout(DISPLAY_BUTTON);
                }
            } else {
                showNowPlayingLayout(DISPLAY_BUTTON);
            }
        } else {
            showNowPlayingLayout(DISPLAY_NOTHING);
        }

        return v;
    }

    @Override
    public void onAttach(Context context) {
        mPlayer.registerListener(mFmListener);
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        if (mDisplayTask != null) {
            LogDns.d(TAG, "display task cancel");
            mDisplayTask.cancel(true);
        }
        mPlayer.unregisterListener(mFmListener);
        super.onDetach();
    }

    public String getPlayTime() {
        PiData currentPi = mPiDataManager.getPiData(mCurrentFrequency);
        if (currentPi != null) {
            Programme programme = currentPi.getProgram(mIndex);
            if (programme != null) {
                return programme.getPlayTime();
            }
        }
        return "";
    }

    public void changeListenButton(int displayedIndex) {
        if (mIndex == displayedIndex) {
            mListenBtn.setVisibility(View.VISIBLE);
        } else {
            mListenBtn.setVisibility(View.GONE);
        }
    }

    private void showNowPlayingLayout(int command) {
        Log.d(TAG, "showNowPlayingLayout - " + Log.filter(command));
        switch (command) {
        case DISPLAY_BUTTON:
            mListenBtn.setVisibility(View.VISIBLE);
            mListeningLayout.setVisibility(View.GONE);
            mListenFrame.setVisibility(View.VISIBLE);
            mListenFrame.invalidate();
            break;
        case DISPLAY_PLAYING_ANI:
            mListenBtn.setVisibility(View.GONE);
            mListeningLayout.setVisibility(View.VISIBLE);
            AnimationDrawable nowPlayingAni = (AnimationDrawable) mPlayingAni.getDrawable();
            nowPlayingAni.start();
            mListenFrame.setVisibility(View.VISIBLE);
            mListenFrame.invalidate();
            break;
        case DISPLAY_NOTHING:
            mListenBtn.setVisibility(View.GONE);
            mListeningLayout.setVisibility(View.GONE);
            mListenFrame.setVisibility(View.GONE);
            mListenFrame.invalidate();
            break;
        default:
            break;
        }
    }

    private boolean isCurrentContents() {
        PiData currentPi = mPiDataManager.getPiData(mCurrentFrequency);
        if (currentPi != null) {
            Programme programme = currentPi.getProgram(mIndex);
            if (programme != null) {
                int startTime = programme.getStartTime();
                int endTime = startTime + programme.getDuration();
                int currentTime = RadioDNSUtil.timeToInt(Calendar.getInstance().getTime());

                LogDns.v(TAG, "start:" + startTime + " end:" + endTime + " now:" + currentTime);
                if (startTime <= currentTime && endTime > currentTime) {
                    return true;
                }
            }
        }
        return false;
    }

    private class DisplayTask extends AsyncTask<Void, Void, Bitmap> {

        private Programme mProgramme = null;
        private Context mContext = null;

        public DisplayTask(Context context, Programme programme) {
            mContext = context;
            mProgramme = programme;
        }

        @Override
        protected Bitmap doInBackground(Void... arg0) {
            LogDns.v(TAG, "DisplayTask - start");
            Bitmap image = null;
            BufferedInputStream input = null;
            // final boolean IMAGE_LOAD_BY_BYTE = false;

            if (mProgramme.getMinimumUrl() != null && !mProgramme.getMinimumUrl().isEmpty()) {
                try {
                    if (isCancelled()) {
                        return null;
                    }
                    URL url = new URL(mProgramme.getMinimumUrl());
                    URLConnection conn = url.openConnection();
                    conn.connect();
                    int size = conn.getContentLength();
                    input = new BufferedInputStream(conn.getInputStream(), size);
                    image = BitmapFactory.decodeStream(input);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (input != null) {
                            input.close();
                        }
                    } catch (IOException e) {
                        LogDns.e(TAG, e);
                    }
                }
            }
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                mLogoImage.setImageBitmap(result);
                mPiDataManager.setImage(mContext, mCurrentFrequency, result,
                        mProgramme.getStartTime());
            }
        }
    }
}
