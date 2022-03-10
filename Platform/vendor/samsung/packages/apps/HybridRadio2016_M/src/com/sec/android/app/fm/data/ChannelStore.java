package com.sec.android.app.fm.data;

import com.sec.android.app.fm.Log;
import com.sec.android.app.fm.RadioPlayer;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Storing channel information persistently.
 * 
 * @author vanrajvala
 */
public class ChannelStore {
    private static ChannelStore _instance = new ChannelStore();
    public static final String FILE_NAME = "ChannelStorage";
    public static final int MAX_FAVORITES_COUNT = 12;
    private static final String TAG = "ChannelStore";

    /**
     * Creates singleton instance.
     * 
     * @return ChannelStore.
     */
    public static ChannelStore getInstance() {
        return _instance;
    };

    private ArrayList<Channel> mChannelList = new ArrayList<Channel>(200);
    private Context mContext = null;
    private ObjectInputStream mInputStream = null;
    private ObjectOutputStream mOutputStream = null;

    public void addChannel(final Channel channel) {
        synchronized (mChannelList) {
            mChannelList.add(channel);
            Collections.sort(mChannelList);
        }
        store();
    }

    public boolean addFavoriteChannel(final int frequency, final String channelName,
            final int position) {
        boolean ret = false;
        int freq = RadioPlayer.getValidFrequency(frequency);
        Channel channel = getChannelByFrequency(freq);
        if (channel == null) {
            channel = new Channel(freq, channelName);
            channel.mIsFavourite = true;
            channel.mPosition = position;
            addChannel(channel);
            ret = true;
        } else if (!channel.mIsFavourite) {
            channel.mIsFavourite = true;
            if (channel.mFreqName == null || channel.mFreqName.isEmpty())
                channel.mFreqName = channelName;
            channel.mPosition = position;
            store();
            ret = true;
        }
        return ret;
    }

    public void clearNonFavoriteChannel() {
        synchronized (mChannelList) {
            int size = mChannelList.size();
            for (int i = size - 1; i >= 0; i--) {
                Channel channel = mChannelList.get(i);
                if (!channel.mIsFavourite) {
                    mChannelList.remove(i);
                }
            }
        }
        store();
    }

    public Channel getChannel(final int index) {
        if (mChannelList.size() > index) {
            return mChannelList.get(index);
        } else {
            return new Channel(RadioPlayer.FREQ_DEFAULT);
        }
    }

    public Channel getChannelByFrequency(final int freq) {
        if (freq < 0)
            return null;
        synchronized (mChannelList) {
            for (Channel channel : mChannelList) {
                if (channel.mFreqency == freq) {
                    return channel;
                }
            }
        }
        return null;
    }

    public int getChannelIndexByFrequency(final int freq) {
        int index = 0;
        synchronized (mChannelList) {
            for (Channel channel : mChannelList) {
                if (channel.mFreqency == freq)
                    return index;
                else
                    index++;
            }
        }
        return -1;
    }

    public int getEmptyPositionOfFavorite() {
        int position = -1;
        int count = 0;
        int[] positions = new int[MAX_FAVORITES_COUNT];
        Arrays.fill(positions, -1);
        synchronized (mChannelList) {
            for (Channel channel : mChannelList) {
                if (channel.mPosition >= 0) {
                    positions[channel.mPosition] = channel.mPosition;
                    count++;
                }
            }
        }
        if (count >= MAX_FAVORITES_COUNT)
            return -1;
        for (int i = 0; i < MAX_FAVORITES_COUNT; i++) {
            if (positions[i] < 0) {
                position = i;
                break;
            }
        }
        return position;
    }

    public Channel getFavoriteChannel(final int position) {
        synchronized (mChannelList) {
            for (Channel channel : mChannelList) {
                if ((channel.mPosition == position) && channel.mIsFavourite) {
                    return channel;
                }
            }
        }
        return null;
    }

    public int getNearestFrequency(final int baseFreq) {
        int nearestFreq = 0;
        int tmpDiff = 0;
        int smallestDiff = 50000;
        synchronized (mChannelList) {
            for (Channel c : mChannelList) {
                tmpDiff = Math.abs(baseFreq - c.mFreqency);
                if ((tmpDiff - smallestDiff) < 0) {
                    smallestDiff = tmpDiff;
                    nearestFreq = c.mFreqency;
                }
            }
        }
        return nearestFreq;
    }

    public boolean getFavoriteFrequency(int freq) {
        synchronized (mChannelList) {
            for (Channel channel : mChannelList) {
                if ((channel.mFreqency == freq) && channel.mIsFavourite) {
                    return true;
                }
            }
        }
        return false;
    }

    public void initialize(final Context context) {
        if ((mContext == null) && (context != null))
            mContext = context.getApplicationContext();
    }

    /**
     * Load the value in memory.
     */
    @SuppressWarnings("unchecked")
    public synchronized void load() {
        Log.d(TAG, "load()");
        if (mContext == null) {
            Log.e(TAG, "load() - context is null!!");
            return;
        }
        try {
            mInputStream = new ObjectInputStream(mContext.openFileInput(FILE_NAME));
            mChannelList = (ArrayList<Channel>) mInputStream.readObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (mChannelList == null) {
                mChannelList = new ArrayList<Channel>(100);
            }
            Channel c = null;
            for (int i = mChannelList.size() - 1; i >= 0; i--) {
                c = mChannelList.get(i);
                if (c.mFreqency == 0 && 87.5f <= c.mFreqMHz && c.mFreqMHz <= 108.0f) {
                    c.mFreqency = (int) (c.mFreqMHz * 100);
                } else if (c.mFreqency < RadioPlayer.FREQ_MIN || c.mFreqency > RadioPlayer.FREQ_MAX) {
                    mChannelList.remove(i);
                }
                c.mFreqMHz = 0f;
            }
            try {
                if (mInputStream != null) {
                    mInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeAllChannel() {
        synchronized (mChannelList) {
            mChannelList.clear();
        }
        store();
    }

    public void removeChannel(final Channel channel) {
        synchronized (mChannelList) {
            mChannelList.remove(channel);
        }
        store();
    }

    public void removeChannel(final int position) {
        synchronized (mChannelList) {
            mChannelList.remove(position);
        }
        store();
    }

    public int size() {
        return mChannelList.size();
    }

    public void sort() {
        Collections.sort(mChannelList);
    }

    /**
     * Store the value.
     */
    public synchronized void store() {
        Log.v(TAG, "store()");
        try {
            mOutputStream = new ObjectOutputStream(mContext.openFileOutput(FILE_NAME,
                    Context.MODE_PRIVATE));
            mOutputStream.writeObject(mChannelList);
            mOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (mOutputStream != null) {
                    mOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public int removeFavoriteChannel(int frequency) {
        int freq = RadioPlayer.getValidFrequency(frequency);
        Channel channel = getChannelByFrequency(freq);
        int removePos = -1;

        if (channel != null) {
            removePos = channel.mPosition;
            arrangFavChannelAfterRemove(channel);
            channel.mIsFavourite = false;
            channel.mPosition = -1;
            store();
        }

        return removePos;
    }

    public void arrangFavChannelAfterRemove(Channel channel) {
        if (channel.mIsFavourite) {
            int size = mChannelList.size();
            int removePos = channel.mPosition;
            for (int i = 0; i < size; i++) {
                if (getChannel(i).mPosition > removePos) {
                    getChannel(i).mPosition--;
                }
            }
        }
    }

    public void deleteEmptyFile() {
        load();
        if(size() == 0) {
            mContext.deleteFile(FILE_NAME);
        }
    }
}
