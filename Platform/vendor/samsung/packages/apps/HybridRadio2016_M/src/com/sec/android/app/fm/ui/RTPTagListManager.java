package com.sec.android.app.fm.ui;

import com.sec.android.app.fm.R;
import com.sec.android.app.fm.data.RTPTagDBAdapter;
import android.content.Context;
import com.sec.android.app.fm.Log;
import java.util.ArrayList;

public class RTPTagListManager {
    private static Context context = null;
    private static RTPTagListManager instance = null;
    public static final int TAG_LIST_ARRAY_SIZE = 10;
    private static final String TAG = "RTPTagListManager";

    public synchronized static RTPTagListManager getInstance(Context context) {
        if (instance == null) {
            instance = new RTPTagListManager(context);
            RTPTagListManager.context = context;
        }
        return instance;
    }

    // public static void setActiveContext(Context context) {
    // RTPTagListManager.context = context;
    // }

    private String curRadioText;
    private RTPTagList curTagList;
    private RTPTagDBAdapter dbAdapter;
    boolean mIsRtUpdated = false;
    private String preRadioText;
    private ArrayList<RTPTagList> tagListArray;

    private RTPTagListManager(Context context) {
        curTagList = new RTPTagList();
        dbAdapter = RTPTagDBAdapter.getInstance(context);
        // dbAdapter.clear();
        tagListArray = dbAdapter.getMusicTags();
        curRadioText = "";
        preRadioText = "";
    }

    synchronized public boolean addCurTagList(int tagCode, int startPos, int additionalLen) {
        Log.v(TAG, "addCurTagList()");
        if (curRadioText.isEmpty() || (getTagName(tagCode) == null))
            return false;

        Log.d(TAG, "curRadioText[" + curRadioText + "] startPos[" + startPos + "] additionalLen["
                + additionalLen + "]");

        boolean ret = false;
        try {
            String info = curRadioText.substring(startPos, startPos + additionalLen + 1);
            RTPTag tag = new RTPTag(tagCode, info);
            Log.d(TAG, tag.toString());
            ret = curTagList.addTag(tag);
        } catch (StringIndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        return ret;
    }

    synchronized private void addTagListArray(RTPTagList tagList) {
        Log.v(TAG, "addTagListArray()");
        if (!tagList.hasMusicTitle())
            return;

        RTPTagList newTagList = new RTPTagList(RTPTagList.TAG_CATEGORY_MUSIC, tagList);
        boolean removed = false;
        ArrayList<RTPTagList> removeList = new ArrayList<RTPTagList>();
        RTPTag tag = tagList.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_TITLE);

        if (tag != null) {
            String tagInfo = tag.getInfo();
            String info = null;
            RTPTag tmpTag = null;

            for (RTPTagList list : tagListArray) {
                tmpTag = list.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_TITLE);
                if ((tmpTag != null) && ((info = tmpTag.getInfo()) != null)
                        && (info.equals(tagInfo))) {
                    if (list.hasMusicSubContents() && !(tagList.hasMusicSubContents())) {
                        Log.v(TAG, "Remain with the much info " + list + "\n" + tagList);
                        return;
                    }
                    removeList.add(list);
                }
            }
        }

        for (RTPTagList list : removeList) {
            tagListArray.remove(list);
            removed = true;
        }

        if (removed) {
            Log.d(TAG, "curTagList is removed from history array");
            if (dbAdapter.delete(newTagList) > 0) {
                Log.d(TAG, "curTagList is removed from DB");
//                dbAdapter.append(newTagList);
            } else {
                Log.e(TAG, "curTagList is not in DB. Something is wrong!!!");
            }
        } else {
            Log.d(TAG, "New list. Nothing is removed.");
        }
        tagListArray.add(0, newTagList);
        dbAdapter.append(newTagList);
        if (tagListArray.size() > TAG_LIST_ARRAY_SIZE){
            tagListArray.remove(TAG_LIST_ARRAY_SIZE);
            dbAdapter.deleteLast();
        }
    }

    public void clearCurTagList() {
        Log.v(TAG, "clearCurTagList()");
        addTagListArray(curTagList);
        curTagList.clear();
    }

    public RTPTagList getCurTagList() {
        return curTagList;
    }

    public ArrayList<RTPTagList> getTagListArray() {
        return tagListArray;
    }

    public String getTagName(int tagCode) {
        if (context == null)
            return null;

        String name = null;
        switch (tagCode) {
        case RTPTagList.RTP_TAG_MUSIC_ITEM_TITLE:
            name = context.getResources().getString(R.string.tags_music_title);
            break;
        case RTPTagList.RTP_TAG_MUSIC_ITEM_ALBUM:
            name = context.getResources().getString(R.string.tags_music_album);
            break;
        case RTPTagList.RTP_TAG_MUSIC_ITEM_ARTIST:
            name = context.getResources().getString(R.string.tags_music_artist);
            break;
        case RTPTagList.RTP_TAG_MUSIC_ITEM_BAND:
            name = context.getResources().getString(R.string.tags_music_band);
            break;
        case RTPTagList.RTP_TAG_INFOMATION_INFO_URL:
            name = context.getResources().getString(R.string.tags_information_url);
            break;
        case RTPTagList.RTP_TAG_PROGRAMME_PROGRAMME_HOMEPAGE:
            name = context.getResources().getString(R.string.tags_programme_homepage);
            break;
        case RTPTagList.RTP_TAG_INTERACTIVITY_PHONE_HOTLINE:
        case RTPTagList.RTP_TAG_INTERACTIVITY_EMAIL_HOTLINE:
            name = context.getResources().getString(R.string.tags_interact_hotline);
            break;
        case RTPTagList.RTP_TAG_INTERACTIVITY_PHONE_STUDIO:
        case RTPTagList.RTP_TAG_INTERACTIVITY_SMS_STUDIO:
        case RTPTagList.RTP_TAG_INTERACTIVITY_EMAIL_STUDIO:
            name = context.getResources().getString(R.string.tags_interact_studio);
            break;
        case RTPTagList.RTP_TAG_INTERACTIVITY_PHONE_OTHER:
        case RTPTagList.RTP_TAG_INTERACTIVITY_SMS_OTHER:
        case RTPTagList.RTP_TAG_INTERACTIVITY_EMAIL_OTHER:
        case RTPTagList.RTP_TAG_INTERACTIVITY_MMS_OTHER:
            name = context.getResources().getString(R.string.tags_interact_other);
            break;
        case RTPTagList.RTP_TAG_INTERACTIVITY_CHAT_CENTRE:
        case RTPTagList.RTP_TAG_INTERACTIVITY_VOTE_CENTRE:
            name = context.getResources().getString(R.string.tags_interact_centre);
            break;
        default:
            break;
        }

        return name;
    }

    public boolean isRtUpdated() {
        return mIsRtUpdated;
    }

    public synchronized boolean setRadioText(String radioText) {
        Log.v(TAG, "setRadioText()");
        if ((radioText == null) || ((radioText != null) && (radioText.isEmpty())))
            return false;

        boolean ret = false;

        mIsRtUpdated = curRadioText.isEmpty() ? !preRadioText.equals(radioText) : !curRadioText
                .equals(radioText);
        if (mIsRtUpdated) {
            Log.d(TAG, "radioText[" + radioText + "] curRadioText[" + curRadioText
                    + "] preRadioText[" + preRadioText + "]");
            clearCurTagList();
            preRadioText = curRadioText;
            curRadioText = radioText;
            ret = true;
        } else {
            Log.d(TAG, "It's not new RT. So skipped.");
        }

        return ret;
    }
}
