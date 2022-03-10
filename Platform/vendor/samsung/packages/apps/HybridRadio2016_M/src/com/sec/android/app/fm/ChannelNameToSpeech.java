/**
 *
 */

package com.sec.android.app.fm;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;

import java.util.Locale;

import com.sec.android.app.fm.ui.RadioDialogFragment;

/**
 * Singleton class to speak information of currently playing channel to user.
 * 
 * @author vanrajvala
 */
public class ChannelNameToSpeech implements OnInitListener {
    TextToSpeech mTts;
    private String[] mSpeech;
    private static ChannelNameToSpeech _instance = null;
    public static final int TEXT_TO_SPEECH = 7;
    private static final String TAG = "ChannelNameToSpeech";

    private ChannelNameToSpeech() {
    }

    public static synchronized ChannelNameToSpeech getInstance() {
        if (_instance == null)
            _instance = new ChannelNameToSpeech();
        return _instance;
    }

    public void speakUp(String[] speech) {
        mSpeech = speech;
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        try {
            MainActivity._instance.startActivityForResult(checkIntent, TEXT_TO_SPEECH);
        } catch(ActivityNotFoundException e) {
            Log.d(TAG, "Activity Not Found");
            e.printStackTrace();
        }
    }

    protected void activityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TEXT_TO_SPEECH) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                mTts = new TextToSpeech(MainActivity._instance, this);
                Log.d(TAG, "[TextToSpeech]check voice data pass");
            } else {
                // missing data, install it
                Log.d(TAG, "[TextToSpeech]we dont have any data lets go for installation");
                MainActivity._instance.openDialog(RadioDialogFragment.TTS_DIALOG);
            }
        }
    }

    @Override
    public void onInit(int status) {
        Log.v(TAG, "[TextToSpeech]on init");
        mTts.setLanguage(Locale.ENGLISH);
        for (int i = 0; i < mSpeech.length; i++) {
            if (mSpeech[i] != null)
                mTts.speak(mSpeech[i], TextToSpeech.QUEUE_ADD, null, null);
        }
        Log.v(TAG, "[TextToSpeech]speech is over..");
    }

    /**
     * shutdown the TTS.
     */
    protected void destroy() {
        if (mTts != null)
            mTts.shutdown();
    }
}
