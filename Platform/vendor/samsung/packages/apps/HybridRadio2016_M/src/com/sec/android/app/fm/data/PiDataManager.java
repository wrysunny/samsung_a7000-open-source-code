package com.sec.android.app.fm.data;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;

import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.radioepg.PiData;
import com.sec.android.app.dns.radioepg.PiData.Programme;
import com.sec.android.app.fm.FMRadioFeature;
import com.sec.android.app.fm.RadioApplication;

public class PiDataManager {

    private static final String TAG = "PiDataManager";
    public static final String FILE_NAME = "PiDataStorage";

    private Map<Channel, PiData> mPis = null;
    private ObjectOutputStream mOutputStream = null;
    private ObjectInputStream mInputStream = null;

    private static PiDataManager mManager = null;

    public synchronized static PiDataManager getInstance() {
        if (mManager == null) {
            mManager = new PiDataManager();
        }
        return mManager;
    }

    private PiDataManager() {
        mPis = new HashMap<Channel, PiData>();
        loadData();
    }

    public void putPiDataWithFreq(Channel channel, PiData pi) {
        mPis.put(channel, pi);
        storeData();
    }

    private PiData getPiData(Channel channel) {
        if (mPis.containsKey(channel)) {
            LogDns.d(TAG, "getPiDataByFreq - freq:" + channel.mFreqency + "pi:" + mPis.get(channel));
            return mPis.get(channel);
        } else {
            LogDns.d(TAG, "getPiDataByFreq - Pi is null");
            return null;
        }
    }

    public void setImage(Context context, final int freq, Bitmap image, int time) {
        Channel channel = null;
        Iterator<Channel> iter = mPis.keySet().iterator();
        while (iter.hasNext()) {
            channel = iter.next();
            if (freq == channel.mFreqency) {
                break;
            }
        }
        if (channel == null || freq != channel.mFreqency) {
            LogDns.d(TAG, "save image - fail");
            return;
        } else {
            PiData piData = getPiData(channel);

            if (piData != null) {
                Programme program = null;
                int index = 0;
                for (index = 0; index < piData.getNumberOfPrograms(); index++) {
                    program = piData.getProgram(index);
                    if (program.getStartTime() == time) {
                        if (FMRadioFeature.EPG_PI_IMAGE_CACHE) {
                            program.setCachedImage(context, image);
                        } else {
                            program.setImage(image);
                        }
                        break;
                    }
                }
                piData.update(index, program);
                putPiDataWithFreq(channel, piData);
            }
        }
    }

    public int getNumberOfProgram(Channel channel) {
        PiData piData = mPis.get(channel);
        
        if(piData == null) {
            return 0;
        } else {
            return piData.getNumberOfPrograms();            
        }
    }

    public PiData getPiData(int frequency) {
        Channel channel = null;
        Iterator<Channel> iter = mPis.keySet().iterator();
        while (iter.hasNext()) {
            channel = iter.next();
            if (frequency == channel.mFreqency) {
                break;
            }
        }
        if (channel == null || frequency != channel.mFreqency) {
            return null;
        } else {
            return getPiData(channel);
        }
    }

    public void storeData() {
        if (mPis == null)
            return;
        try {
            mOutputStream = new ObjectOutputStream(RadioApplication.getInstance()
                    .openFileOutput(FILE_NAME, Context.MODE_PRIVATE));
            Date date = Calendar.getInstance().getTime();
            String today = new SimpleDateFormat("yyyyMMdd").format(date);
            mOutputStream.writeObject(today);
            mOutputStream.writeObject(mPis);
            mOutputStream.flush();
        } catch (Exception e) {
            try {
                e.printStackTrace();
            } catch (Exception ex) {
                ;
            }
        } finally {
            try {
                if (mOutputStream != null)
                    mOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadData() {
        FileInputStream fileInputStream = null;
        boolean err = false;
        try {
            fileInputStream = RadioApplication.getInstance().openFileInput(FILE_NAME);
            mInputStream = new ObjectInputStream(fileInputStream);
            Date date = Calendar.getInstance().getTime();
            String today = new SimpleDateFormat("yyyyMMdd").format(date);
            String savedDate = (String) mInputStream.readObject();
            LogDns.d(TAG, "loadData!!");
            LogDns.d(TAG, "today - " + today);
            LogDns.d(TAG, "savedDate - " + savedDate);
            mPis = (Map<Channel, PiData>) mInputStream.readObject();
            if (today.equals(savedDate)) {
                LogDns.d(TAG, "load PiData - " + savedDate);
            } else {
                LogDns.d(TAG, "load PiData - saved Pi is old");
                Set<Channel> channels = mPis.keySet();
                for (Channel channel : channels) {
                    mPis.get(channel).deleteImages();
                }
                mPis.clear();
                mPis = null;
            }
            if (mPis == null) {
                mPis = new HashMap<Channel, PiData>();
            } else {
                LogDns.d(TAG, mPis.toString());
            }
        } catch (FileNotFoundException e) {
            err = true;
            LogDns.e(TAG, e);
        } catch (StreamCorruptedException e) {
            err = true;
            LogDns.e(TAG, e);
        } catch (IOException e) {
            err = true;
            LogDns.e(TAG, e);
        } catch (ClassNotFoundException e) {
            err = true;
            LogDns.e(TAG, e);
        } finally {
            if (err) {
                if (mPis != null) {
                    LogDns.v(TAG, "Creating new PiDataManager");
                    mPis = new HashMap<Channel, PiData>();
                }
            }
            try {
                if (mInputStream != null)
                    mInputStream.close();
            } catch (IOException e) {
                LogDns.e(TAG, e);
            }
            try {
                if (fileInputStream != null)
                    fileInputStream.close();
            } catch (IOException e) {
                LogDns.d(TAG, "IOException");
            }
        }
    }
}
