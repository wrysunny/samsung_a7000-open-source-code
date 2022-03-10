package com.sec.android.app.dns.radioepg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.media.MediaPlayer;

import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.ui.DnsKoreaTestActivity;
import com.sec.android.secmediarecorder.SecMediaRecorder;

public class EpgPlayer {
    public interface OnBufferingUpdateListener {
        public void onBufferingUpdate(final EpgPlayer ep, final int percent);
    }

    public interface OnInfoListener {
        public boolean onInfo(EpgPlayer player, int what, int extra);
    }

    private static final int BUFFERING_GAP = 2048;
    private static final int INITIAL_BUFFERING_SIZE = 16 * 1024;
    private static final byte STATE_DOWNLOADING = 0x01;
    private static final byte STATE_NONE = 0x00;
    private static final byte STATE_PLAYING = 0x02;
    private static final byte STATE_RECORDING = 0x04;
    private static final String TAG = "EpgPlayer";
    private static final int TEMP_BUFFER_SIZE = 8192;
    private static final String TEMP_FILE_NAME_DOWNLOAD1 = "tempFileDownload1";
    private static final String TEMP_FILE_NAME_DOWNLOAD2 = "tempFileDownload2";
    private static final long TEMP_FILE_SIZE_MAX = DnsKoreaTestActivity.isKoreaTest() ? 64 * 1024
            : 2 * 1024 * 1024;
    private AudioTrack mAudioTrack = null;
    private long mBufferBytesWrite = 0;
    private FileInputStream mBufferFileInputStream = null;
    private FileOutputStream mBufferFileOutputStream = null;
    private ArrayList<OnBufferingUpdateListener> mBufferingUpdateListeners = new ArrayList<OnBufferingUpdateListener>(
            2);
    private File mCurrentReadFile = null;
    private InputStream mDownloadInputStream = null;
    private Runnable mDownloadRunnable = new Runnable() {
        @Override
        public void run() {
            LogDns.v(TAG, "Download - run()");
            int readBytes = 0;
            int tryCount = 0;
            boolean timeout = false;
            boolean error = false;
            HttpURLConnection cn = null;
            mState |= STATE_DOWNLOADING;
            try {
                URL url = new URL(mPathSource);
                do {
                    try {
                        cn = (HttpURLConnection) url.openConnection();
                        cn.setConnectTimeout(3000);
                        cn.setReadTimeout(10000);
                        tryCount++;
                        cn.connect();
                        timeout = false;
                    } catch (SocketTimeoutException e) {
                        LogDns.d(TAG, "connection try count:" + tryCount);
                        timeout = true;
                    }
                } while (timeout && tryCount < 3);
                if (timeout && tryCount >= 3) {
                    throw new SocketTimeoutException();
                }
                mDownloadInputStream = cn.getInputStream();
                byte buf[] = new byte[TEMP_BUFFER_SIZE];
                mBufferBytesWrite = 0;
                mTotalBytesWrite = 0;
                mTempFileDownload1 = File.createTempFile(TEMP_FILE_NAME_DOWNLOAD1, null);
                mBufferFileOutputStream = new FileOutputStream(mTempFileDownload1);
                while (isDownloading()) {
                    readBytes = mDownloadInputStream.read(buf);
                    synchronized (mBufferFileOutputStream) {
                        if (!isDownloading() || readBytes <= 0) {
                            break;
                        } else {
                            // LogDns.d(TAG, "read bytes : " + readBytes);
                            if (mBufferBytesWrite > TEMP_FILE_SIZE_MAX) {
                                while (mTempFileDownload1 != null && mTempFileDownload2 != null) {
                                    LogDns.d(TAG, "It's too many buffered. Holding downloading :(");
                                    mBufferFileOutputStream.wait(1000);
                                }
                                mBufferBytesWrite = 0;
                                mBufferFileOutputStream.flush();
                                mBufferFileOutputStream.close();
                                if (mTempFileDownload1 == null) {
                                    mTempFileDownload1 = File.createTempFile(
                                            TEMP_FILE_NAME_DOWNLOAD1, null);
                                    mBufferFileOutputStream = new FileOutputStream(
                                            mTempFileDownload1);
                                } else if (mTempFileDownload2 == null) {
                                    mTempFileDownload2 = File.createTempFile(
                                            TEMP_FILE_NAME_DOWNLOAD2, null);
                                    mBufferFileOutputStream = new FileOutputStream(
                                            mTempFileDownload2);
                                } else {
                                    LogDns.e(TAG,
                                            "Check the playing part. One of them should be played and deleted. :(");
                                    break;
                                }
                            }
                            mBufferFileOutputStream.write(buf, 0, readBytes);
                            mBufferBytesWrite += readBytes;
                            mTotalBytesWrite += readBytes;
                        }
                    }
                }
            } catch (MalformedURLException e) {
                // from new URL(mPathSource)
                error = true;
                LogDns.e(TAG, e);
            } catch (SocketTimeoutException e) {
                // from new SocketTimeoutException()
                error = true;
                LogDns.d(TAG, e.toString());
            } catch (SocketException e) {
                // from stopDownload(){mDownloadInputStream.close()}
                // It's normal operation.
                LogDns.d(TAG, e.toString());
            } catch (IOException e) {
                // from cn.connect()
                error = true;
                LogDns.e(TAG, e);
            } catch (InterruptedException e) {
                // interrupt from stopDownload()
                // It's normal operation.
                LogDns.d(TAG, e.toString());
            } finally {
                if ((error || readBytes <= 0) && mInfoListener != null) {
                    mInfoListener.onInfo(EpgPlayer.this, MediaPlayer.MEDIA_ERROR_IO, 0);
                }
                mState &= ~STATE_DOWNLOADING;
                mTotalBytesWrite = 0;
                mBufferBytesWrite = 0;
                mDownloadThread = null;
                try {
                    if (mDownloadInputStream != null) {
                        mDownloadInputStream.close();
                    }
                } catch (IOException e) {
                    LogDns.e(TAG, e);
                } finally {
                    mDownloadInputStream = null;
                    try {
                        if (mBufferFileOutputStream != null) {
                            mBufferFileOutputStream.close();
                        }
                    } catch (IOException e) {
                        LogDns.e(TAG, e);
                    } finally {
                        mBufferFileOutputStream = null;
                        for (OnBufferingUpdateListener listener : mBufferingUpdateListeners) {
                            listener.onBufferingUpdate(EpgPlayer.this, -1);
                        }
                        LogDns.d(TAG, "Download runnable terminated.");
                    }
                }
            }
        }
    };

    private Thread mDownloadThread = null;
    private OnInfoListener mInfoListener = null;
    private String mPathSource = null;
    private String mPathTarget = null;

    private Runnable mPlayerMainRunnable = new Runnable() {
        public void run() {
            LogDns.v(TAG, "Main - run()");
            try {
                do {
                    if (mAudioTrack == null && mPlayerPlayThread == null) {
                        if (mTotalBytesWrite >= INITIAL_BUFFERING_SIZE) {
                            playAudioTrack();
                        }
                    }
                    if (isPlaying())
                        Thread.sleep(3000);
                    else
                        Thread.sleep(1000);
                    if (mBufferingUpdateListeners.size() > 0) {
                        int percent = (int) ((mTotalBytesWrite - mTotalBytesRead) * 100 / INITIAL_BUFFERING_SIZE);

                        @SuppressWarnings("unchecked")
                        ArrayList<OnBufferingUpdateListener> onBufferingUpdateListeners = (ArrayList<OnBufferingUpdateListener>) (mBufferingUpdateListeners
                                .clone());

                        for (OnBufferingUpdateListener listener : onBufferingUpdateListeners) {
                            listener.onBufferingUpdate(EpgPlayer.this, percent);
                        }
                    }
                } while (isDownloading());
            } catch (IllegalStateException e) {
                LogDns.e(TAG, e);
            } catch (InterruptedException e) {
                // LogDns.e(TAG, e);
            } finally {
                mPlayerMainThread = null;
                LogDns.d(TAG, "Main runnable terminated.");
            }
        }
    };

    private Thread mPlayerMainThread = null;

    private Runnable mPlayerPlayRunnable = new Runnable() {
        @Override
        public void run() {
            LogDns.v(TAG, "Play - run()");
            Object sBuffer = null;
            Object bitStream = null;
            int playerBufferSize = 0;
            long tempBytesRead = 0;
            long previousBytesRead = 0;
            try {
                mCurrentReadFile = mTempFileDownload1;
                mBufferFileInputStream = new FileInputStream(mTempFileDownload1);
                bitStream = JlayerLibrary.Bitstream.newInstance(mBufferFileInputStream);
                Object header = JlayerLibrary.Bitstream.readFrame(bitStream);
                Object decoder = JlayerLibrary.Decoder.newInstance();
                sBuffer = JlayerLibrary.Decoder.decodeFrame(decoder, header, bitStream);
                JlayerLibrary.Bitstream.closeFrame(bitStream);
                int channelConfig = JlayerLibrary.SampleBuffer.getChannelCount(sBuffer) == 1 ? AudioFormat.CHANNEL_OUT_MONO
                        : AudioFormat.CHANNEL_OUT_STEREO;
                int sampleRateInHz = JlayerLibrary.SampleBuffer.getSampleFrequency(sBuffer);
                mSampleRateInHz = sampleRateInHz > 0 ? sampleRateInHz
                        : (mSampleRateInHz != 0 ? mSampleRateInHz : 32000);
                playerBufferSize = AudioTrack.getMinBufferSize(mSampleRateInHz, channelConfig,
                        AudioFormat.ENCODING_PCM_16BIT);
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRateInHz,
                        channelConfig, AudioFormat.ENCODING_PCM_16BIT, playerBufferSize,
                        AudioTrack.MODE_STREAM);
                mTotalBytesRead = 0;
                mAudioTrack
                        .setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListener() {
                            @Override
                            public void onMarkerReached(AudioTrack arg0) {
                            }

                            @Override
                            public void onPeriodicNotification(AudioTrack arg0) {
                                if (isRecording() && mRecordInfoListener != null) {
                                    mRecordInfoListener.onInfo(null,
                                            SecMediaRecorder.MEDIA_RECORDER_INFO_DURATION_PROGRESS,
                                            ++mRecordTime * 1000);
                                }
                            }
                        });
                mAudioTrack.setPositionNotificationPeriod(mSampleRateInHz);
                mState |= STATE_PLAYING;
                mAudioTrack.play();
                while (isPlaying()) {
                    mAudioTrack.write(JlayerLibrary.SampleBuffer.getBuffer(sBuffer), 0,
                            JlayerLibrary.SampleBuffer.getBufferLength(sBuffer));
                    if (mTotalBytesWrite - mTotalBytesRead < BUFFERING_GAP)
                        waitBuffering();
                    header = JlayerLibrary.Bitstream.readFrame(bitStream);
                    if (header == null) {
                        JlayerLibrary.Bitstream.closeFrame(bitStream);
                        exchangeBuffer();
                        LogDns.i(TAG, "Exchange another buffer");
                        tempBytesRead = mTotalBytesRead;
                        bitStream = JlayerLibrary.Bitstream.newInstance(mBufferFileInputStream);
                        header = JlayerLibrary.Bitstream.readFrame(bitStream);
                        decoder = JlayerLibrary.Decoder.newInstance();
                    }
                    sBuffer = JlayerLibrary.Decoder.decodeFrame(decoder, header, bitStream);
                    JlayerLibrary.Bitstream.closeFrame(bitStream);
                    previousBytesRead = mTotalBytesRead;
                    mTotalBytesRead = tempBytesRead
                            + mBufferFileInputStream.getChannel().position();
                    synchronized (this) {
                        if (isRecording() && previousBytesRead < mTotalBytesRead) {
                            byte[] buf = new byte[(int) (mTotalBytesRead - previousBytesRead)];
                            int readByte = 0;
                            readByte = mRecordFileInputStream.read(buf, 0, buf.length);
                            LogDns.d(TAG, "record - write byte:" + readByte);
                            mRecordFileOutputStream.write(buf, 0, readByte);
                        }
                    }
                }
            } catch (InterruptedException e) {
                LogDns.e(TAG, e);
            } catch (IOException e) {
                LogDns.e(TAG, e);
            } finally {
                if (isRecording()) {
                    stopRecord();
                    releaseRecord();
                }
                mTotalBytesRead = 0;
                mPlayerPlayThread = null;
                if (mAudioTrack != null) {
                    mAudioTrack.release();
                    mAudioTrack = null;
                }
                if (mBufferFileInputStream != null) {
                    try {
                        mBufferFileInputStream.close();
                    } catch (IOException e) {
                        LogDns.e(TAG, e);
                    } finally {
                        mBufferFileInputStream = null;
                        if (bitStream != null) {
                            JlayerLibrary.Bitstream.close(bitStream);
                        }
                    }
                }
                LogDns.d(TAG, "Playing runnable terminated.");
            }
        }
    };
    private Thread mPlayerPlayThread = null;
    private FileInputStream mRecordFileInputStream = null;
    private FileOutputStream mRecordFileOutputStream = null;
    private com.sec.android.secmediarecorder.SecMediaRecorder.OnInfoListener mRecordInfoListener = null;
    private int mRecordTime = 0;
    private int mSampleRateInHz = 0;
    private byte mState = STATE_NONE;
    private File mTempFileDownload1 = null;
    private File mTempFileDownload2 = null;
    private long mTotalBytesRead = 0;
    private long mTotalBytesWrite = 0;

    private void exchangeBuffer() throws IOException {
        LogDns.v(TAG, "exchangeBuffer()");
        if (mTempFileDownload1 == null || mTempFileDownload2 == null) {
            LogDns.e(TAG, "Something is wrong!!");
            return;
        }
        if (isRecording()) {
            byte[] buf = new byte[TEMP_BUFFER_SIZE];
            int readByte = 0;
            while ((readByte = mRecordFileInputStream.read(buf, 0, buf.length)) > 0) {
                LogDns.d(TAG, "record - write byte:" + readByte);
                mRecordFileOutputStream.write(buf, 0, readByte);
            }
            mRecordFileInputStream.close();
            mRecordFileInputStream = null;
        }
        mBufferFileInputStream.close();
        if (mCurrentReadFile.equals(mTempFileDownload1)) {
            mTempFileDownload1.delete();
            mTempFileDownload1 = null;
            mCurrentReadFile = mTempFileDownload2;
        } else if (mCurrentReadFile.equals(mTempFileDownload2)) {
            mTempFileDownload2.delete();
            mTempFileDownload2 = null;
            mCurrentReadFile = mTempFileDownload1;
        } else {
            LogDns.e(TAG, "Wrong currentReadFile:" + mCurrentReadFile);
            mCurrentReadFile = null;
            throw new IOException();
        }
        mBufferFileInputStream = new FileInputStream(mCurrentReadFile);
        if (isRecording()) {
            mRecordFileInputStream = new FileInputStream(mCurrentReadFile);
        }
    }

    public void initialize() {
        LogDns.d(TAG, "initialize()");
        mPathSource = null;
        mPathTarget = null;
        mBufferingUpdateListeners.clear();
        mState = STATE_NONE;
    }

    private boolean isDownloading() {
        return (mState & STATE_DOWNLOADING) == STATE_DOWNLOADING;
    }

    private boolean isPlaying() {
        return (mState & STATE_PLAYING) == STATE_PLAYING;
    }

    private boolean isRecording() {
        return (mState & STATE_RECORDING) == STATE_RECORDING;
    }

    public void pauseRecord() {
        LogDns.d(TAG, "pauseRecord()");
        mState &= ~STATE_RECORDING;
        try {
            mRecordFileInputStream.close();
        } catch (IOException e) {
            LogDns.e(TAG, e);
        } finally {
            mRecordFileInputStream = null;
        }
    }

    public void play() throws IllegalStateException {
        synchronized (this) {
            LogDns.d(TAG, "play()");
            if (mPathSource != null && mState == STATE_NONE) {
                if ((mDownloadThread == null) || mDownloadThread.isInterrupted()) {
                    while (mDownloadThread != null) {
                        try {
                            LogDns.d(TAG, "wait mDownloadThread is terminated");
                            this.wait(100);
                        } catch (InterruptedException e) {
                            LogDns.e(TAG, e);
                        }
                    }
                    mDownloadThread = new Thread(mDownloadRunnable);
                    mDownloadThread.start();
                } else {
                    LogDns.e(TAG, "play() - mDownloadThread is running.");
                    throw new IllegalStateException();
                }
                if ((mPlayerMainThread == null) || mPlayerMainThread.isInterrupted()) {
                    while (mPlayerMainThread != null) {
                        try {
                            LogDns.d(TAG, "wait mPlayerMainThread is terminated");
                            this.wait(100);
                        } catch (InterruptedException e) {
                            LogDns.e(TAG, e);
                        }
                    }
                    mPlayerMainThread = new Thread(mPlayerMainRunnable);
                    mPlayerMainThread.start();
                } else {
                    LogDns.e(TAG, "play() - mRunningMediaPlayerThread is running.");
                    throw new IllegalStateException();
                }
            } else {
                LogDns.e(TAG, "play() - mState:" + mState);
                throw new IllegalStateException();
            }
        }
    }

    private void playAudioTrack() throws IllegalStateException {
        if (mPlayerPlayThread == null && !isPlaying() && isDownloading()) {
            mPlayerPlayThread = new Thread(mPlayerPlayRunnable);
            mPlayerPlayThread.start();
            if (mInfoListener != null) {
                mInfoListener.onInfo(EpgPlayer.this, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public void record() throws IllegalStateException {
        LogDns.d(TAG, "startRecord()");
        if (mPathTarget != null && mAudioTrack != null && isPlaying()) {
            try {
                if (mRecordFileOutputStream == null) {
                    mRecordFileOutputStream = new FileOutputStream(new File(mPathTarget));
                    mRecordTime = 0;
                }
                if (mRecordFileInputStream != null) {
                    mRecordFileInputStream.close();
                }
                mRecordFileInputStream = new FileInputStream(mCurrentReadFile);
                mRecordFileInputStream.skip(mBufferFileInputStream.getChannel().position());
                mState |= STATE_RECORDING;
            } catch (FileNotFoundException e) {
                LogDns.e(TAG, e);
                mState &= ~STATE_RECORDING;
            } catch (IOException e) {
                LogDns.e(TAG, e);
                mState &= ~STATE_RECORDING;
            } finally {
                if (!isRecording())
                    releaseRecord();
            }
        } else {
            throw new IllegalStateException();
        }
    }

    public void releaseRecord() {
        LogDns.d(TAG, "releaseRecord()");
        try {
            mRecordInfoListener = null;
            if (mRecordFileInputStream != null) {
                mRecordFileInputStream.close();
            }
        } catch (IOException e) {
            LogDns.e(TAG, e);
        } finally {
            mRecordFileInputStream = null;
            try {
                if (mRecordFileOutputStream != null) {
                    mRecordFileOutputStream.close();
                }
            } catch (IOException e) {
                LogDns.e(TAG, e);
            } finally {
                mRecordFileOutputStream = null;
            }
        }
    }

    public void removeOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        LogDns.v(TAG, "removeOnBufferingUpdateListener()");
        mBufferingUpdateListeners.remove(listener);
    }

    public void setDataSource(String path) {
        mPathSource = path;
    }

    public void setDataTarget(String path) {
        mPathTarget = path;
    }

    public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
        LogDns.v(TAG, "setOnBufferingUpdateListener()");
        if (!mBufferingUpdateListeners.contains(listener)) {
            mBufferingUpdateListeners.add(listener);
        }
    }

    public void setOnInfoListener(OnInfoListener listener) {
        mInfoListener = listener;
    }

    public void setOnRecordInfoListener(
            com.sec.android.secmediarecorder.SecMediaRecorder.OnInfoListener listener) {
        mRecordInfoListener = listener;
    }

    public void stop() {
        synchronized (this) {
            LogDns.d(TAG, "stop()");
            mState &= ~STATE_PLAYING;
            if (mAudioTrack != null && mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                mAudioTrack.stop();
            }
            stopDownload();
            stopPlayerMainThread();
        }
    }

    private void stopDownload() {
        LogDns.d(TAG, "stopDownload()");
        mState &= ~STATE_DOWNLOADING;
        if (mDownloadThread != null && mDownloadThread.isAlive()) {
            mDownloadThread.interrupt();
        }
        try {
            if (mDownloadInputStream != null) {
                mDownloadInputStream.close();
            }
        } catch (IOException e) {
            LogDns.e(TAG, e);
        } finally {
            mDownloadInputStream = null;
            try {
                if (mBufferFileOutputStream != null) {
                    mBufferFileOutputStream.close();
                }
            } catch (IOException e) {
                LogDns.e(TAG, e);
            } finally {
                mBufferFileOutputStream = null;
                if (mTempFileDownload1 != null && mTempFileDownload1.exists()) {
                    mTempFileDownload1.delete();
                    mTempFileDownload1 = null;
                }
                if (mTempFileDownload2 != null && mTempFileDownload2.exists()) {
                    mTempFileDownload2.delete();
                    mTempFileDownload2 = null;
                }
                LogDns.d(TAG, "stopDownload() - finish");
            }
        }
    }

    private void stopPlayerMainThread() {
        LogDns.d(TAG, "stopPlayerMainThread()");
        if (mPlayerMainThread != null && mPlayerMainThread.isAlive()) {
            mPlayerMainThread.interrupt();
        }
    }

    public void stopRecord() {
        LogDns.d(TAG, "stopRecord()");
        synchronized (this) {
            mState &= ~STATE_RECORDING;
            try {
                if (mRecordFileOutputStream != null) {
                    mRecordFileOutputStream.close();
                }
            } catch (IOException e) {
                LogDns.e(TAG, e);
            } finally {
                mRecordFileOutputStream = null;
                try {
                    if (mRecordFileInputStream != null) {
                        mRecordFileInputStream.close();
                    }
                } catch (IOException e) {
                    LogDns.e(TAG, e);
                } finally {
                    mRecordFileInputStream = null;
                }
            }
        }
    }

    private void waitBuffering() throws InterruptedException {
        boolean inform = false;
        if (mInfoListener != null) {
            mInfoListener.onInfo(EpgPlayer.this, MediaPlayer.MEDIA_INFO_BUFFERING_START, 0);
            inform = true;
        }
        mAudioTrack.pause();
        try {
            while (isPlaying() && (mTotalBytesWrite - mTotalBytesRead < INITIAL_BUFFERING_SIZE)) {
                LogDns.d(TAG, "Wait buffering");
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            LogDns.e(TAG, e);
        } finally {
            if (mInfoListener != null && inform)
                mInfoListener.onInfo(EpgPlayer.this, MediaPlayer.MEDIA_INFO_BUFFERING_END, 0);
        }
        mAudioTrack.play();
    }
}
