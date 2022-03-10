package com.sec.android.app.dns.radioepg;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class XsiData implements Serializable {
    public static class Service implements Serializable {
        public static class Multimedia implements Serializable {
            //private static final String LOGO_TYPE_RECTANGLE = "logo_colour_rectangle";
            //private static final String LOGO_TYPE_SQUARE = "logo_colour_square";
            //private static final String LOGO_TYPE_UNRESTRICTED = "logo_unrestricted";
            private static final long serialVersionUID = -8474358996934530777L;

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

        public static class ServiceId implements Serializable {
            private static final long serialVersionUID = 1297044516226712507L;
            
            private int mCost = -1;
            private int mBitRate = -1;
            private String mId = null;
            private String mMime = null;
            private int mOffset = -1;

            public void setCost(String cost) {
                mCost = Integer.parseInt(cost);
            }

            public void setBitRate(String bitrate) {
                mBitRate = Integer.parseInt(bitrate);
            }

            public void setId(String id) {
                mId = id;
            }

            public void setMime(String mime) {
                mMime = mime;
            }

            public void setOffset(String offset) {
                mOffset = Integer.parseInt(offset);
            }

            public int getCost() {
                return mCost;
            }

            public int getBitRate() {
                return mBitRate;
            }

            public String getId() {
                return mId;
            }

            public String getMime() {
                return mMime;
            }

            public int getOffset() {
                return mOffset;
            }
        }

        private static final long serialVersionUID = -1547481994018722180L;

        private ArrayList<Multimedia> mArrayMultimedia;
        private ArrayList<ServiceId> mArrayServiceId;

        public Service() {
            mArrayServiceId = new ArrayList<ServiceId>();
            mArrayMultimedia = new ArrayList<Multimedia>();
        }

        public void addMultimedia(Multimedia mMultimedia) {
            mArrayMultimedia.add(mMultimedia);
        }

        public void addServiceId(ServiceId serviceId) {
            mArrayServiceId.add(serviceId);
        }

        public String getStreamUrl() {
            String url = null;
            int cost = Integer.MAX_VALUE;
            int bitrate = Integer.MAX_VALUE;
            for (ServiceId sid : mArrayServiceId) {
                if (STREAM_MIMETYPE.equals(sid.mMime) && sid.mId.startsWith("http://")
                        && cost > sid.mCost) {
                    url = sid.mId;
                    cost = sid.mCost;
                    bitrate = sid.mBitRate;

                } else if(STREAM_MIMETYPE.equals(sid.mMime) && sid.mId.startsWith("http://")
                        && cost == sid.mCost && bitrate >= sid.mBitRate) {
                    url = sid.mId;
                    cost = sid.mCost;
                    bitrate = sid.mBitRate;
                }
            }
            return url;
        }
    }

    private static final long serialVersionUID = 6517137360639437738L;
    public static final String STREAM_MIMETYPE = "audio/mpeg";
    public static final String TAG = "XSIData";

    private String mCreationTime = null;
    private HashMap<String, Service> mHashervice = null;

    public XsiData() {
        mHashervice = new HashMap<String, Service>();
    }

    public void addService(String key, Service s) {
        mHashervice.put(key, s);
    }

    public void clear() {
        mHashervice.clear();
    }

    public String getCreationTime() {
        return mCreationTime;
    }

    public Service getService(String pid, String freq) {
        return mHashervice.get(pid + freq);
    }

    public void setCreationTime(String time) {
        mCreationTime = time;
    }
}
