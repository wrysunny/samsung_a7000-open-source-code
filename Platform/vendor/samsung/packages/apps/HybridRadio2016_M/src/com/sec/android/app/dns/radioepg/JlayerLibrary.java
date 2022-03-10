package com.sec.android.app.dns.radioepg;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.content.Context;

import com.sec.android.app.dns.LogDns;

import dalvik.system.DexClassLoader;

public class JlayerLibrary {

    public static class Bitstream {
        private static Method sCloseFrameMethod = null;
        private static Method sCloseMethod = null;
        private static Constructor<?> sConstructor = null;
        private static Method sReadFrameMethod = null;

        public static void close(Object obj) {
            if (sBitstreamCls != null) {
                try {
                    if (sCloseMethod == null)
                        sCloseMethod = sBitstreamCls.getMethod("close", (Class[]) null);
                    sCloseMethod.invoke(obj);
                } catch (Exception e) {
                    LogDns.e(TAG, e);
                }
            }
        }

        public static void closeFrame(Object obj) {
            if (sBitstreamCls != null) {
                try {
                    if (sCloseFrameMethod == null)
                        sCloseFrameMethod = sBitstreamCls.getMethod("closeFrame", (Class[]) null);
                    sCloseFrameMethod.invoke(obj);
                } catch (Exception e) {
                    LogDns.e(TAG, e);
                }
            }
        }

        public static Object newInstance(InputStream fis) {
            Object instance = null;
            if (sBitstreamCls != null) {
                try {
                    if (sConstructor == null)
                        sConstructor = sBitstreamCls.getConstructor(InputStream.class);
                    instance = sConstructor.newInstance(fis);
                } catch (Exception e) {
                    LogDns.e(TAG, e);
                }
            }
            return instance;
        }

        public static Object readFrame(Object obj) {
            Object header = null;
            if (sBitstreamCls != null) {
                try {
                    if (sReadFrameMethod == null)
                        sReadFrameMethod = sBitstreamCls.getMethod("readFrame", (Class[]) null);
                    header = sReadFrameMethod.invoke(obj);
                } catch (Exception e) {
                    LogDns.e(TAG, e);
                }
            }
            return header;
        }
    }

    public static class Decoder {
        private static Constructor<?> sConstructor = null;
        private static Method sDecodeFrameMethod = null;

        public static Object decodeFrame(Object decoder, Object header, Object bitStream) {
            Object buffer = null;
            if (sDecoderCls != null) {
                try {
                    if (sDecodeFrameMethod == null) {
                        Class<?>[] clss = { sHeaderCls, sBitstreamCls };
                        sDecodeFrameMethod = sDecoderCls.getMethod("decodeFrame", clss);
                    }
                    buffer = sDecodeFrameMethod.invoke(decoder, header, bitStream);
                } catch (Exception e) {
                    LogDns.e(TAG, e);
                }
            }
            return buffer;
        }

        public static Object newInstance() {
            Object instance = null;
            if (sDecoderCls != null) {
                try {
                    if (sConstructor == null)
                        sConstructor = sDecoderCls.getConstructor();
                    instance = sConstructor.newInstance();
                } catch (Exception e) {
                    LogDns.e(TAG, e);
                }
            }
            return instance;
        }
    }

    public static class SampleBuffer {
        private static Method sGetBufferLengthMethod = null;
        private static Method sGetBufferMethod = null;
        private static Method sGetChannelCountMethod = null;
        private static Method sGetSampleFrequencyMethod = null;

        public static short[] getBuffer(Object sBuffer) {
            short[] buffer = null;
            if (sSampleBufferCls != null) {
                try {
                    if (sGetBufferMethod == null)
                        sGetBufferMethod = sSampleBufferCls.getMethod("getBuffer", (Class[]) null);
                    buffer = (short[]) sGetBufferMethod.invoke(sBuffer);
                } catch (Exception e) {
                    LogDns.e(TAG, e);
                }
            }
            return buffer;
        }

        public static int getBufferLength(Object sBuffer) {
            int length = 0;
            if (sSampleBufferCls != null) {
                try {
                    if (sGetBufferLengthMethod == null)
                        sGetBufferLengthMethod = sSampleBufferCls.getMethod("getBufferLength",
                                (Class[]) null);
                    length = (Integer) sGetBufferLengthMethod.invoke(sBuffer);
                } catch (Exception e) {
                    LogDns.e(TAG, e);
                }
            }
            return length;
        }

        public static int getChannelCount(Object sBuffer) {
            int channelCount = 0;
            if (sSampleBufferCls != null) {
                try {
                    if (sGetChannelCountMethod == null)
                        sGetChannelCountMethod = sSampleBufferCls.getMethod("getChannelCount",
                                (Class[]) null);
                    channelCount = (Integer) sGetChannelCountMethod.invoke(sBuffer);
                } catch (Exception e) {
                    LogDns.e(TAG, e);
                }
            }
            return channelCount;
        }

        public static int getSampleFrequency(Object sBuffer) {
            int sampleFrequency = 0;
            if (sSampleBufferCls != null) {
                try {
                    if (sGetSampleFrequencyMethod == null)
                        sGetSampleFrequencyMethod = sSampleBufferCls.getMethod(
                                "getSampleFrequency", (Class[]) null);
                    sampleFrequency = (Integer) sGetSampleFrequencyMethod.invoke(sBuffer);
                } catch (Exception e) {
                    LogDns.e(TAG, e);
                }
            }
            return sampleFrequency;
        }
    }

    private static final String CLASS_NAME_BITSTREAM = "javazoom.jl.decoder.Bitstream";
    private static final String CLASS_NAME_DECODER = "javazoom.jl.decoder.Decoder";
    private static final String CLASS_NAME_HEADER = "javazoom.jl.decoder.Header";
    private static final String CLASS_NAME_SAMPLEBUFFER = "javazoom.jl.decoder.SampleBuffer";
    private static final String APK_FILE_PATH = "jl1.0.1.apk";
    private static Class<?> sBitstreamCls = null;
    private static Class<?> sHeaderCls = null;
    private static ClassLoader sDexClassLoader = null;
    private static Class<?> sDecoderCls = null;
    private static Class<?> sSampleBufferCls = null;
    private static final String TAG = "JlayerLibrary";
    private static JlayerLibrary sInstance = null;

    public static void load(Context context) {
        LogDns.v(TAG, "load()");
        if (sInstance == null) {
            sInstance = new JlayerLibrary();
            File apkFile = new File(context.getDir("apk", Context.MODE_PRIVATE), APK_FILE_PATH);
            if (!apkFile.exists()) {
                BufferedInputStream bis = null;
                OutputStream dexWriter = null;
                final int BUF_SIZE = 8 * 1024;
                try {
                    bis = new BufferedInputStream(context.getAssets().open(APK_FILE_PATH));
                    dexWriter = new BufferedOutputStream(new FileOutputStream(apkFile));
                    byte[] buf = new byte[BUF_SIZE];
                    int len;
                    while ((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
                        dexWriter.write(buf, 0, len);
                    }
                } catch (IOException e) {
                    LogDns.e(TAG, e);
                } finally {
                    try {
                        if (dexWriter != null) {
                            dexWriter.close();
                        }
                    } catch (IOException e) {
                        LogDns.e(TAG, e);
                    } finally {
                        try {
                            if (bis != null) {
                                bis.close();
                            }
                        } catch (IOException e) {
                            LogDns.e(TAG, e);
                        }
                    }
                }
            }
            final File optDir = context.getDir("outdex", Context.MODE_PRIVATE);
            sDexClassLoader = new DexClassLoader(apkFile.getAbsolutePath(),
                    optDir.getAbsolutePath(), null, ClassLoader.getSystemClassLoader());
            if (sDexClassLoader != null) {
                try {
                    sBitstreamCls = sDexClassLoader.loadClass(CLASS_NAME_BITSTREAM);
                    sDecoderCls = sDexClassLoader.loadClass(CLASS_NAME_DECODER);
                    sSampleBufferCls = sDexClassLoader.loadClass(CLASS_NAME_SAMPLEBUFFER);
                    sHeaderCls = sDexClassLoader.loadClass(CLASS_NAME_HEADER);
                } catch (ClassNotFoundException e) {
                    LogDns.e(TAG, e);
                    sDexClassLoader = null;
                } catch (IllegalArgumentException e) {
                    LogDns.e(TAG, e);
                }
                LogDns.v(TAG, "load() - success");
            } else {
                LogDns.e(TAG, "Loading library fail.");
            }
        }
    }
}
