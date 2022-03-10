package com.sec.android.app.fm;

/**
 * Some calls to driver are time consuming operation. If its happen in UI thread
 * then UI will be blocked.Worker thread does the operation in new thread.
 * 
 * @author vanrajvala
 */
public class WorkerThread extends Thread {
    private static final String _TAG = "WorkerThread";
    public static final int OPERATION_SEEKDOWN = 3;
    public static final int OPERATION_SEEKUP = 2;
    public static final int OPERATION_TUNE = 1;

    private int mFreq;
    private boolean mIsBusy = false;
    private boolean mIsRunning = false;
    private int mType = 0;

    /**
     * Posting operation to worker thread.
     * 
     * @param type
     *            - Type of the operation
     * @param freq
     *            - Given frequency
     */
    synchronized public void doOperation(int type, final int freq) {
        Log.d(_TAG, "######### doOperation #### type :" + type);
        mType = type;
        mFreq = freq;
        notify();
    }

    public boolean isBusy() {
        return mIsBusy;
    }

    @Override
    public void run() {
        RadioPlayer player = RadioPlayer.getInstance();
        mIsRunning = true;
        try {
            while (mIsRunning) {
                synchronized (this) {
                    Log.d(_TAG, "[WorkerThread] waiting for Job..");
                    wait();
                }
                if (!mIsRunning)
                    break;
                synchronized (this) {
                    Log.d(_TAG, "[WorkerThread] got job " + mType + ":" + Log.filter(mFreq));
                    switch (mType) {
                    case OPERATION_TUNE:
                        Log.d(_TAG, "######### OPERATION_TUNE#### mFreq:" + Log.filter(mFreq));
                        mIsBusy = true;
                        mFreq = RadioPlayer.getValidFrequency(mFreq);
                        player.tune(mFreq);
                        break;
                    case OPERATION_SEEKUP:
                        Log.d(_TAG, "####### NEXT BUTTON SELECTED ####");
                        mIsBusy = true;
                        player.seekUp();
                        break;
                    case OPERATION_SEEKDOWN:
                        Log.d(_TAG, "####### PREVIOUS BUTTON SELECTED ####");
                        mIsBusy = true;
                        player.seekDown();
                        break;
                    default:
                        break;
                    }
                    mIsBusy = false;
                }
            }
        } catch (InterruptedException e) {
            Log.d(_TAG, e.toString());
        } finally {
            Log.d(_TAG, "[WorkerThread] terminated..");
        }
    }

    public void terminate() {
        mIsRunning = false;
        if (isAlive())
            interrupt();
    }
}
