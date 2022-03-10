package com.sec.android.app.dns.radioepg;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;

import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.RadioDNSUtil;
import com.sec.android.app.fm.FMRadioFeature;

public class PiData implements Serializable {
    public static class Programme implements Serializable {
        public static class Multimedia implements Serializable {
            //private static final String LOGO_TYPE_RECTANGLE = "logo_colour_rectangle";
            //private static final String LOGO_TYPE_SQUARE = "logo_colour_square";
            //private static final String LOGO_TYPE_UNRESTRICTED = "logo_unrestricted";
            private static final long serialVersionUID = -6661519526736735862L;

            private int mHeight = -1;
            private String mType = null;
            private String mUrl = null;
            private int mWidth = -1;

            public void setHeight(String height) {
                mHeight = Integer.parseInt(height);
            }

            public void setType(String type) {
                mType = type;
            }

            public void setUrl(String url) {
                mUrl = url;
            }

            public void setWidth(String width) {
                mWidth = Integer.parseInt(width);
            }

            public int getHeight() {
                return mHeight;
            }

            public String getType() {
                return mType;
            }

            public String getUrl() {
                return mUrl;
            }

            public int getWidth() {
                return mWidth;
            }
        }

        private static final long serialVersionUID = 7080333542540358096L;
        private static final String TAG = "PiData-Programme";
        private static final String DIR_PATH = "data/data/com.sec.android.app.fm/files/";

        private String mActualDuration = null;
        private String mActualTime = null;
        private ArrayList<Multimedia> mArrayMultimedia;
        private String mDescription = null;
        private String mDuration = null;
        private String mLongName = null;
        private String mMediumName = null;
        private String mTime = null;
        private Bitmap mImage = null;
        private String mCachedImageName = null;

        public Programme() {
            mArrayMultimedia = new ArrayList<Multimedia>();
        }

        public Programme(Programme p) {
            this.mActualDuration = p.mActualDuration;
            this.mActualTime = p.mActualTime;
            this.mDuration = p.mDuration;
            this.mTime = p.mTime;
            this.mLongName = p.mLongName;
            this.mMediumName = p.mMediumName;
            this.mDescription = p.mDescription;
            mArrayMultimedia = new ArrayList<Multimedia>(p.mArrayMultimedia);
        }

        public void addMultimedia() {
            mArrayMultimedia.add(new Multimedia());
        }

        public void addMultimedia(Multimedia multimedia) {
            mArrayMultimedia.add(multimedia);
        }

        public String getMinimumUrl() {
            String url = null;
            int minWidth = Integer.MAX_VALUE;
            for (Multimedia media:mArrayMultimedia) {
                LogDns.v("TEST- URL", media.mUrl);
                if (media.mWidth<minWidth) {
                    minWidth = media.mWidth;
                    url = media.mUrl;
                }
            }
            return url;
        }

        public int getDuration() {
            String s = null;
            int hi = 0, mi = 0, si = 0;
            int d = 0;
            s = (mActualDuration != null) ? mActualDuration : ((mDuration != null) ? mDuration
                    : null);
            if (s != null) {
                hi = s.indexOf("H");
                if (hi > 2) {
                    d += Integer.parseInt(s.substring(2, hi) + "0000");
                }
                if (hi == -1) {
                    hi = 1;
                }
                mi = s.indexOf("M");
                if (mi > hi) {
                    d += Integer.parseInt(s.substring(hi + 1, mi) + "00");
                }
                if (mi == -1) {
                    mi = hi;
                }
                si = s.indexOf("S");
                if (si > mi) {
                    d += Integer.parseInt(s.substring(mi + 1, si));
                }
            }
            return d;
        }

        public String getName() {
            return (mLongName != null) ? mLongName : ((mMediumName != null) ? mMediumName : null);
        }

        public String getShortName() {
            return (mMediumName != null) ? mMediumName : ((mLongName != null) ? mLongName : null);
        }

        public int getStartTime() {
            int time = 0;
            String t = (mActualTime != null) ? mActualTime : ((mTime != null) ? mTime : null);
            if (t != null) {
                try {
                    time = RadioDNSUtil.timeToInt(RadioDNSUtil.stringToDate(t));
                } catch (ParseException e) {
                    LogDns.e(TAG, e);
                }
            }
            return time;
        }

        public String getPlayTime() {
            int hour = getStartTime()/10000;
            int min  = (getStartTime()/100)%100;
            String startTime = String.format("%02d:%02d", hour, min);
            hour = ((getStartTime()+getDuration())/10000)%24;
            min  = ((getStartTime()+getDuration())/100)%100;
            String endTime = String.format("%02d:%02d", hour, min);
            return startTime + " - " + endTime;
        }

        public String getDescription() {
            return mDescription;
        }

        public void setActualDuration(String actualDuration) {
            mActualDuration = actualDuration;
        }

        public void setActualTime(String actualTime) {
            mActualTime = actualTime;
        }

        public void setDescription(String description) {
            mDescription = description;
        }

        public void setDuration(String duration) {
            mDuration = duration;
        }

        public void setLongName(String longName) {
            mLongName = longName;
        }

        public void setMediumName(String mediumName) {
            mMediumName = mediumName;
        }

        public void setTime(String time) {
            mTime = time;
        }

        public void setImage(Bitmap image) {
            mImage = image;
        }

        public Bitmap getImage() {
            return mImage;
        }

        public void setCachedImage(Context context, Bitmap image) {
            String fileName = getName()+"_image.png";
            try {

                FileOutputStream fos = context.openFileOutput(fileName, 0);
                image.compress(CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();

                mCachedImageName = fileName;
                LogDns.d(TAG, mCachedImageName+"is saved" );
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public Bitmap getCachedImage() {
            if (mCachedImageName == null)
                return null;
            String filePath = DIR_PATH + mCachedImageName;
            Bitmap bitmapImage = BitmapFactory.decodeFile(filePath);

            return bitmapImage;
        }

        public boolean checkExistedImage() {

            if (FMRadioFeature.EPG_PI_IMAGE_CACHE) {
                if (mCachedImageName != null) {
                    LogDns.d(TAG, "image file name - " + mCachedImageName);
                    return true;
                } else {
                    return false;
                }
            } else {
                if (mImage != null) {
                    return true;
                } else {
                    return false;
                }
           }
        }

        public void deleteImage() {
            if (mCachedImageName == null)
                return;
            File imageFile = new File(DIR_PATH + mCachedImageName);
            imageFile.delete();
            mCachedImageName = null;
        }
    }

    private static final long serialVersionUID = 3176139083345959085L;
    //private static final String TAG = "PIData";

    private ArrayList<Programme> mArrayProgramme;

    public PiData() {
        mArrayProgramme = new ArrayList<Programme>();
    }

    public void addMultimedia() {
        Programme p = mArrayProgramme.get(mArrayProgramme.size() - 1);
        p.addMultimedia();
    }

    public void addProgramme(Programme p) {
        if (p != null) {
            mArrayProgramme.add(new Programme(p));
        }
    }

    public void sortProgram() {
        Collections.sort(mArrayProgramme, new Comparator<Programme>() {

            @Override
            public int compare(Programme arg0, Programme arg1) {
                if (arg0.getStartTime() > arg1.getStartTime())
                    return 1;
                else if (arg0.getStartTime() < arg1.getStartTime())
                    return -1;
                else
                    return 0;
            }
        });
    }

    public Programme getProgram(int index) {
        return mArrayProgramme.get(index);
    }

    public String getProgramDescription(Date date) {
        Programme p = getProgram(RadioDNSUtil.timeToInt(date));
        return p != null ? p.mDescription : null;
    }

    public String getProgramName(Date date) {
        Programme program = null;
        int time = RadioDNSUtil.timeToInt(date);
        for (Programme p : mArrayProgramme) {
            int startTime = p.getStartTime();
            int endTime = startTime + p.getDuration();
            if (startTime <= time && endTime > time) {
                program = p;
                break;
            }
        }
        return program != null ? program.getName() : null;
    }

    public int getNumberOfPrograms() {
        return mArrayProgramme.size();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (Programme p : mArrayProgramme) {
            s.append("time:").append(p.mTime).append(" duration:").append(p.mDuration)
                    .append(" actualTime").append(p.mActualTime).append(" actualDuration")
                    .append(p.mActualDuration).append("\n").append(" mediumName:")
                    .append(p.mMediumName).append(" longName:").append(p.mLongName).append("\n")
                    .append(" Description").append(p.mDescription).append("\n");
            for (Programme.Multimedia m : p.mArrayMultimedia) {
                s.append(" url:").append(m.mUrl).append("\n").append(" height:").append(m.mHeight)
                        .append(" width:").append(m.mWidth).append("\n");
            }
        }
        return s.toString();
    }

    public void update(int index, Programme program) {
        mArrayProgramme.set(index, program);        
    }

    public void deleteImages() {
        for (Programme program:mArrayProgramme) {
            program.deleteImage();
        }
    }
}
