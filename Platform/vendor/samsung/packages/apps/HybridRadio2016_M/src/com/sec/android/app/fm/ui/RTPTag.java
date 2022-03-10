package com.sec.android.app.fm.ui;

public class RTPTag {
    private String info;
    private int tagCode;

    public RTPTag(int tagCode, String info) {
        this.tagCode = tagCode;
        this.info = (info == null ? "" : info);
    }

    public String getInfo() {
        return info;
    }

    public int getTagCode() {
        return tagCode;
    }

    public void setInfo(String info) {
        this.info = (info == null ? "" : info);
    }

    public void setTagCode(int tagCode) {
        this.tagCode = tagCode;
    }

    @Override
    public String toString() {
        return "tagCode : " + tagCode + " info : " + info;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RTPTag) {
            RTPTag tag = (RTPTag) o;
            return (this.tagCode == tag.tagCode) && (this.info.equals(tag.info));
        }

        return false;
    }
}