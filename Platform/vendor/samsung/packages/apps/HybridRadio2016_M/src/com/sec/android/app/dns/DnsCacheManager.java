package com.sec.android.app.dns;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.ArrayList;

import android.content.Context;

import com.sec.android.app.dns.data.DnsCache;
import com.sec.android.app.dns.data.InternalDnsData;

public class DnsCacheManager {
    private static final String FILE_NAME_CACHE = "dns_cache";
    private static DnsCacheManager sInstance = new DnsCacheManager();
    private static final String TAG = "DnsCacheManager";

    public static DnsCacheManager getInstance() {
        return sInstance;
    }

    private ArrayList<DnsCache> mArrayCache = new ArrayList<DnsCache>();

    public void addCache(DnsCache cache) {
        LogDns.v(TAG, "addCache - " + cache.getDnsData());
        InternalDnsData data = cache.getDnsData();
        String freq = data.getFrequency();
        String pid = data.getPi();
        synchronized (mArrayCache) {
            for (DnsCache c : mArrayCache) {
                if (c.getXsiData().getService(pid, freq) != null) {
                    mArrayCache.remove(c);
                    break;
                }
            }
            mArrayCache.add(cache);
        }
        saveCache();
    }

    public DnsCache getCache(String pid, String freq) {
        synchronized (mArrayCache) {
            for (DnsCache cache : mArrayCache) {
                if (cache.getXsiData().getService(pid, freq) != null) {
                    return cache;
                }
            }
        }
        return null;
    }

    public void loadCache() {
        new Thread() {
            @Override
            public void run() {
                ObjectInputStream ois = null;
                DNSService dnsService = DNSService.getInstance();
                if (dnsService != null) {
                    try {
                        ois = new ObjectInputStream(new BufferedInputStream(dnsService
                                .getApplicationContext().openFileInput(FILE_NAME_CACHE)));
                        String currentCountry = dnsService.getIsoCountryCode();
                        String cachedCountry = ois.readObject().toString();
                        LogDns.v(TAG, "loadCache() " + currentCountry + " " + cachedCountry);
                        if ((currentCountry != null)
                                && (currentCountry.equalsIgnoreCase(cachedCountry))) {
                            loadCache(ois);
                        }
                    } catch (StreamCorruptedException e) {
                        LogDns.e(TAG, e);
                    } catch (FileNotFoundException e) {
                        LogDns.d(TAG, e.getMessage());
                    } catch (IOException e) {
                        LogDns.e(TAG, e);
                    } catch (ClassNotFoundException e) {
                        LogDns.e(TAG, e);
                    } finally {
                        try {
                            if (ois != null) {
                                ois.close();
                            }
                        } catch (IOException e) {
                            LogDns.e(TAG, e);
                        }
                    }
                }
            }
        }.start();
    }

    private void loadCache(ObjectInputStream ois) {
        LogDns.d(TAG, "loadCache() - " + DnsInternalDataManager.getInstance().getSize());
        try {
            Object o = null;
            synchronized (mArrayCache) {
                while ((o = ois.readObject()) != null) {
                    if (o instanceof DnsCache) {
                        DnsCache cache = (DnsCache) o;
                        mArrayCache.add(cache);

                        if (cache.getDnsData().getFrequency() != null) {
                            DnsInternalDataManager.getInstance().updateInternalData(
                                    cache.getDnsData(), false);
                        } else {
                            LogDns.e(TAG, "Cache has null data!!");
                        }
                    }
                }
            }
        } catch (EOFException e) {
            LogDns.d(TAG, "loadCache() - successful");
        } catch (OptionalDataException e) {
            LogDns.e(TAG, e);
        } catch (ClassNotFoundException e) {
            LogDns.e(TAG, e);
        } catch (IOException e) {
            LogDns.e(TAG, e);
        }
    }

    public void removeCache(DnsCache cache) {
        synchronized (mArrayCache) {
            mArrayCache.remove(cache);
        }
        saveCache();
    }

    private void saveCache() {
        new Thread() {
            @Override
            public void run() {
                ObjectOutputStream oos = null;
                DNSService dnsService = DNSService.getInstance();
                if (dnsService != null) {
                    try {
                        oos = new ObjectOutputStream(new BufferedOutputStream(dnsService
                                .getApplicationContext().openFileOutput(FILE_NAME_CACHE,
                                        Context.MODE_PRIVATE)));                        if (oos != null)                            saveCache(oos);
                    } catch (IOException e) {
                        LogDns.e(TAG, e);
                    } finally {
                        try {
                            if (oos != null) {
                                oos.close();
                            }
                        } catch (IOException e) {
                            LogDns.e(TAG, e);
                        }
                    }
                }
            }
        }.start();
    }

    private void saveCache(ObjectOutputStream oos) {
        DNSService dnsService = DNSService.getInstance();
        if (dnsService != null) {
            try {
                String currentCountry = dnsService.getIsoCountryCode();
                LogDns.d(TAG, "saveCache() " + currentCountry);
                oos.writeObject(currentCountry);
                synchronized (mArrayCache) {
                    for (DnsCache cache : mArrayCache) {
                        LogDns.v(TAG, "saveCache() - " + cache.getDnsData());
                        oos.writeObject(cache);
                    }
                }
                oos.flush();
            } catch (IOException e) {
                LogDns.e(TAG, e);
            }
        }
    }
}