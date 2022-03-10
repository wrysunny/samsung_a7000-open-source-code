package com.sec.android.app.fm.ui;

import java.util.ArrayList;

public class RTPTagList {
    public static final int RTP_TAG_MUSIC_ITEM_TITLE = 1;
    public static final int RTP_TAG_MUSIC_ITEM_ALBUM = 2;
    public static final int RTP_TAG_MUSIC_ITEM_ARTIST = 4;
    public static final int RTP_TAG_MUSIC_ITEM_BAND = 9;
    public static final int RTP_TAG_INFOMATION_INFO_URL = 29;
    public static final int RTP_TAG_PROGRAMME_PROGRAMME_HOMEPAGE = 39;
    public static final int RTP_TAG_INTERACTIVITY_PHONE_HOTLINE = 41;
    public static final int RTP_TAG_INTERACTIVITY_PHONE_STUDIO = 42;
    public static final int RTP_TAG_INTERACTIVITY_PHONE_OTHER = 43;
    public static final int RTP_TAG_INTERACTIVITY_SMS_STUDIO = 44;
    public static final int RTP_TAG_INTERACTIVITY_SMS_OTHER = 45;
    public static final int RTP_TAG_INTERACTIVITY_EMAIL_HOTLINE = 46;
    public static final int RTP_TAG_INTERACTIVITY_EMAIL_STUDIO = 47;
    public static final int RTP_TAG_INTERACTIVITY_EMAIL_OTHER = 48;
    public static final int RTP_TAG_INTERACTIVITY_MMS_OTHER = 49;
    public static final int RTP_TAG_INTERACTIVITY_CHAT_CENTRE = 51;
    public static final int RTP_TAG_INTERACTIVITY_VOTE_CENTRE = 53;
    public static final int TAG_CATEGORY_MUSIC = 0x01;
    public static final int TAG_CATEGORY_INFORMATION = 0x02;
    public static final int TAG_CATEGORY_PROGRAMME = 0x04;
    public static final int TAG_CATEGORY_INTERACTIVITY = 0x08;
    public static final int TAG_CATEGORY_ALL = 0x0F;

    private ArrayList<RTPTag> tagList;

    public RTPTagList() {
        tagList = new ArrayList<RTPTag>();
    }

    public RTPTagList(int category, RTPTagList baseTagList) {
        tagList = new ArrayList<RTPTag>();

        for (RTPTag t : baseTagList.tagList) {
            switch (t.getTagCode()) {
            case RTP_TAG_MUSIC_ITEM_TITLE:
            case RTP_TAG_MUSIC_ITEM_ALBUM:
            case RTP_TAG_MUSIC_ITEM_ARTIST:
            case RTP_TAG_MUSIC_ITEM_BAND:
                if ((category & TAG_CATEGORY_MUSIC) == TAG_CATEGORY_MUSIC)
                    tagList.add(t);
                break;
            case RTP_TAG_INFOMATION_INFO_URL:
                if ((category & TAG_CATEGORY_INFORMATION) == TAG_CATEGORY_INFORMATION)
                    tagList.add(t);
                break;
            case RTP_TAG_PROGRAMME_PROGRAMME_HOMEPAGE:
                if ((category & TAG_CATEGORY_PROGRAMME) == TAG_CATEGORY_PROGRAMME)
                    tagList.add(t);
                break;
            case RTP_TAG_INTERACTIVITY_PHONE_HOTLINE:
            case RTP_TAG_INTERACTIVITY_PHONE_STUDIO:
            case RTP_TAG_INTERACTIVITY_PHONE_OTHER:
            case RTP_TAG_INTERACTIVITY_SMS_STUDIO:
            case RTP_TAG_INTERACTIVITY_SMS_OTHER:
            case RTP_TAG_INTERACTIVITY_EMAIL_HOTLINE:
            case RTP_TAG_INTERACTIVITY_EMAIL_STUDIO:
            case RTP_TAG_INTERACTIVITY_EMAIL_OTHER:
            case RTP_TAG_INTERACTIVITY_MMS_OTHER:
            case RTP_TAG_INTERACTIVITY_CHAT_CENTRE:
            case RTP_TAG_INTERACTIVITY_VOTE_CENTRE:
                if ((category & TAG_CATEGORY_INTERACTIVITY) == TAG_CATEGORY_INTERACTIVITY)
                    tagList.add(t);
                break;
            default:
                break;
            }
        }
    }

    public RTPTagList(RTPTagList baseTagList) {
        tagList = new ArrayList<RTPTag>();
        tagList.addAll(baseTagList.tagList);
    }

    public boolean addTag(RTPTag tag) {
        for (RTPTag t : tagList) {
            if (t.getTagCode() == tag.getTagCode()) {
                if (t.getInfo().equals(tag.getInfo())) {
                    return false;
                } else {
                    tagList.remove(t);
                    break;
                }
            }
        }
        tagList.add(tag);
        return true;
    }

    public void clear() {
        tagList.clear();
    }

    public RTPTag getTag(int index) {
        return (RTPTag) tagList.get(index);
    }

    public RTPTag getTagWithCode(int tagCode) {
        for (RTPTag t : tagList) {
            if (t.getTagCode() == tagCode)
                return t;
        }
        return null;
    }

    /*
     * public boolean haveInformationTag() { RTPTag t; Iterator<RTPTag> i =
     * tagList.iterator(); while (i.hasNext()) { t = i.next(); switch
     * (t.getTagCode()) { case RTP_TAG_INFOMATION_INFO_URL: return true;
     * default: break; } } return false; }
     */

    /*
     * public boolean haveInteractivityTag() { RTPTag t; Iterator<RTPTag> i =
     * tagList.iterator(); while (i.hasNext()) { t = i.next(); switch
     * (t.getTagCode()) { case RTP_TAG_INTERACTIVITY_PHONE_HOTLINE: case
     * RTP_TAG_INTERACTIVITY_PHONE_STUDIO: case
     * RTP_TAG_INTERACTIVITY_PHONE_OTHER: case RTP_TAG_INTERACTIVITY_SMS_STUDIO:
     * case RTP_TAG_INTERACTIVITY_SMS_OTHER: case
     * RTP_TAG_INTERACTIVITY_EMAIL_HOTLINE: case
     * RTP_TAG_INTERACTIVITY_EMAIL_STUDIO: case
     * RTP_TAG_INTERACTIVITY_EMAIL_OTHER: case RTP_TAG_INTERACTIVITY_MMS_OTHER:
     * case RTP_TAG_INTERACTIVITY_CHAT_CENTRE: case
     * RTP_TAG_INTERACTIVITY_VOTE_CENTRE: return true; default: break; } }
     * return false; }
     */

    /*
     * public boolean haveMusicTag() { RTPTag t; Iterator<RTPTag> i =
     * tagList.iterator(); while (i.hasNext()) { t = i.next(); switch
     * (t.getTagCode()) { case RTP_TAG_MUSIC_ITEM_TITLE: case
     * RTP_TAG_MUSIC_ITEM_ALBUM: case RTP_TAG_MUSIC_ITEM_ARTIST: case
     * RTP_TAG_MUSIC_ITEM_BAND: return true; default: break; } } return false; }
     */

    /*
     * public boolean haveProgrammeTag() { RTPTag t; Iterator<RTPTag> i =
     * tagList.iterator(); while (i.hasNext()) { t = i.next(); switch
     * (t.getTagCode()) { case RTP_TAG_PROGRAMME_PROGRAMME_HOMEPAGE: return
     * true; default: break; } } return false; }
     */

    public boolean haveTag(int category) {
        int tagCode;
        for (RTPTag t : tagList) {
            tagCode = t.getTagCode();

            switch (tagCode) {
            case RTP_TAG_MUSIC_ITEM_TITLE:
            case RTP_TAG_MUSIC_ITEM_ALBUM:
            case RTP_TAG_MUSIC_ITEM_ARTIST:
            case RTP_TAG_MUSIC_ITEM_BAND:
                if ((category & TAG_CATEGORY_MUSIC) == TAG_CATEGORY_MUSIC)
                    return true;
                break;
            case RTP_TAG_INFOMATION_INFO_URL:
                if ((category & TAG_CATEGORY_INFORMATION) == TAG_CATEGORY_INFORMATION)
                    return true;
                break;
            case RTP_TAG_PROGRAMME_PROGRAMME_HOMEPAGE:
                if ((category & TAG_CATEGORY_PROGRAMME) == TAG_CATEGORY_PROGRAMME)
                    return true;
                break;
            case RTP_TAG_INTERACTIVITY_PHONE_HOTLINE:
            case RTP_TAG_INTERACTIVITY_PHONE_STUDIO:
            case RTP_TAG_INTERACTIVITY_PHONE_OTHER:
            case RTP_TAG_INTERACTIVITY_SMS_STUDIO:
            case RTP_TAG_INTERACTIVITY_SMS_OTHER:
            case RTP_TAG_INTERACTIVITY_EMAIL_HOTLINE:
            case RTP_TAG_INTERACTIVITY_EMAIL_STUDIO:
            case RTP_TAG_INTERACTIVITY_EMAIL_OTHER:
            case RTP_TAG_INTERACTIVITY_MMS_OTHER:
            case RTP_TAG_INTERACTIVITY_CHAT_CENTRE:
            case RTP_TAG_INTERACTIVITY_VOTE_CENTRE:
                if ((category & TAG_CATEGORY_INTERACTIVITY) == TAG_CATEGORY_INTERACTIVITY)
                    return true;
                break;
            default:
                break;
            }

        }
        return false;
    }

    public int size() {
        return tagList.size();
    }

    @Override
    public boolean equals(Object o) {
        int countOfEquals = 0;

        if (!(o instanceof RTPTagList))
            return false;

        RTPTagList rList = (RTPTagList) o;
        int lSize = this.tagList.size();
        int rSize = rList.tagList.size();
        boolean isEqual = false;

        if (lSize == rSize) {
            for (RTPTag lTag : this.tagList) {
                for (RTPTag rTag : rList.tagList) {
                    if (lTag.equals(rTag)) {
                        countOfEquals++;
                        break;
                    }
                }
            }
            if (lSize == countOfEquals) {
                return true;
            }
        } else if (lSize > rSize) {
            for (RTPTag lTag : this.tagList) {
                for (RTPTag rTag : rList.tagList) {
                    if (lTag.equals(rTag)) {
                        isEqual = true;
                        countOfEquals++;
                        break;
                    }
                }
                if (!isEqual) {
                    if (!lTag.getInfo().isEmpty())
                        return false;
                }
                isEqual = false;
            }
            if (rSize == countOfEquals)
                return true;
        } else {
            for (RTPTag rTag : rList.tagList) {
                for (RTPTag lTag : this.tagList) {
                    if (lTag.equals(rTag)) {
                        isEqual = true;
                        countOfEquals++;
                        break;
                    }
                }
                if (!isEqual) {
                    if (!rTag.getInfo().isEmpty())
                        return false;
                }
                isEqual = false;
            }
            if (lSize == countOfEquals)
                return true;
        }
        return false;
    }

    public boolean hasMusicTitle() {
        for (RTPTag tag : tagList) {
            if (hasMusicTitle(tag))
                return true;
        }
        return false;
    }

    static public boolean hasMusicTitle(RTPTag tag) {
        switch (tag.getTagCode()) {
        case RTP_TAG_MUSIC_ITEM_TITLE:
            return true;
        default:
            return false;
        }
    }
    
    public boolean hasMusicSubContents() {
        String info = null;
        for (RTPTag t : tagList) {
            switch (t.getTagCode()) {
            case RTP_TAG_MUSIC_ITEM_ALBUM:
            case RTP_TAG_MUSIC_ITEM_ARTIST:
            case RTP_TAG_MUSIC_ITEM_BAND:
                info = t.getInfo();
                if ((info != null) && (info.length() != 0))
                    return true;
            default:
                break;
            }
        }
        return false;
    }

    static public boolean isMusicTag(RTPTag tag) {
        switch (tag.getTagCode()) {
        case RTP_TAG_MUSIC_ITEM_TITLE:
        case RTP_TAG_MUSIC_ITEM_ALBUM:
        case RTP_TAG_MUSIC_ITEM_ARTIST:
        case RTP_TAG_MUSIC_ITEM_BAND:
            return true;
        default:
            return false;
        }
    }

    @Override
    public String toString() {
        return "RTPTagList [tagList=" + tagList + "]";
    }
}
