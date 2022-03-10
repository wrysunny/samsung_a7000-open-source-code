package com.sec.android.app.dns.radiovis;

import android.graphics.Bitmap;

public class RadioVISFrame {
    private static RadioVISFrame sFrame = new RadioVISFrame();

    public static RadioVISFrame obtain() {
        return sFrame;
    }

    private int mCommand = -1;
    private Bitmap mImage = null;
    private String mImageUrl = null;
    private String mLink = null;
    private String mText = null;
    private String mType = null;

    public void clear() {
        mCommand = -1;
        mLink = null;
        mImage = null;
        mType = null;
        mText = null;
        mImageUrl = null;
    }

    public int getCommand() {
        return mCommand;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public String getLink() {
        return mLink;
    }

    public String getText() {
        return mText;
    }

    public String getType() {
        return mType;
    }

    public void setCommand(int command) {
        mCommand = command;
    }

    public void setImage(Bitmap image) {
        mImage = image;
    }

    public void setImageUrl(String url) {
        mImageUrl = url;
    }

    public void setLink(String link) {
        mLink = link;
    }

    public void setText(String text) {
        mText = text;
    }

    public void setType(String type) {
        mType = type;
    }
}
