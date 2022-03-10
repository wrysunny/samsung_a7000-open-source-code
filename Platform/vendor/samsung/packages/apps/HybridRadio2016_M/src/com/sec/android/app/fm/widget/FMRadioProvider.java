package com.sec.android.app.fm.widget;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.samsung.media.fmradio.FMEventListener;
import com.sec.android.app.fm.Log;
import com.sec.android.app.fm.MainActivity;
import com.sec.android.app.fm.NotificationReceiver;
import com.sec.android.app.fm.NotificationService;
import com.sec.android.app.fm.R;
import com.sec.android.app.fm.RadioApplication;
import com.sec.android.app.fm.RadioPlayer;
import com.sec.android.app.fm.RadioToast;
import com.sec.android.app.fm.data.Channel;
import com.sec.android.app.fm.data.ChannelStore;

public class FMRadioProvider extends AppWidgetProvider {
    private static final String ACTION_RADIO_WIDGET_FAV = "com.sec.android.app.fm.widget.fav";
    private static final String ACTION_RADIO_WIDGET_NEXT = "com.sec.android.app.fm.widget.next";
    private static final String ACTION_RADIO_WIDGET_POWER_OFF = "com.sec.android.app.fm.widget.off";
    private static final String ACTION_RADIO_WIDGET_POWER_ON = "com.sec.android.app.fm.widget.on";
    private static final String ACTION_RADIO_WIDGET_PREV = "com.sec.android.app.fm.widget.prev";
    public static final String ACTION_RADIO_WIDGET_REFRESH = "com.sec.android.app.fm.widget.refresh";
    private static final String ACTION_RADIO_WIDGET_REFRESH_FORCE = "com.sec.android.fm.player_lock.status.off";
    private static final String ACTION_RADIO_WALLPAPER_CHANGED = "com.sec.android.intent.action.WALLPAPER_CHANGED";
    public static final String ACTION_REGISTER_MEDIA_SESSION = "com.sec.android.app.fm.REGISTER_MEDIA_SESSION";
    public static final String ACTION_UNREGISTER_MEDIA_SESSION = "com.sec.android.app.fm.UNREGISTER_MEDIA_SESSION";
    
    public static final int SHOW_SEEKING = 0;
    public static final int SHOW_TURNING_ON = 1;
    public static final int SHOW_EMPTY = 2;

    private static String sInfomationViewState;
    private static final String APP_PACKAGE_NAME = "com.sec.android.app.fm";
    private static final int sAddImgs[] = { R.id.widget_fav01_add, R.id.widget_fav02_add,
            R.id.widget_fav03_add, R.id.widget_fav04_add, R.id.widget_fav05_add,
            R.id.widget_fav06_add, R.id.widget_fav07_add, R.id.widget_fav08_add,
            R.id.widget_fav09_add, R.id.widget_fav10_add, R.id.widget_fav11_add,
            R.id.widget_fav12_add };
    private static ChannelStore sChannelStore = ChannelStore.getInstance();
    private static Context sContext = null;
    private static final int sFavClick[] = { R.id.widget_fav01, R.id.widget_fav02,
            R.id.widget_fav03, R.id.widget_fav04, R.id.widget_fav05, R.id.widget_fav06,
            R.id.widget_fav07, R.id.widget_fav08, R.id.widget_fav09, R.id.widget_fav10,
            R.id.widget_fav11, R.id.widget_fav12 };

    // don't use. the size increases whenever updateWidget
    // private static RemoteViews rv = null;

    private static final int sFreqMHzs[] = { R.id.widget_fav01_FreqMHz, R.id.widget_fav02_FreqMHz,
            R.id.widget_fav03_FreqMHz, R.id.widget_fav04_FreqMHz, R.id.widget_fav05_FreqMHz,
            R.id.widget_fav06_FreqMHz, R.id.widget_fav07_FreqMHz, R.id.widget_fav08_FreqMHz,
            R.id.widget_fav09_FreqMHz, R.id.widget_fav10_FreqMHz, R.id.widget_fav11_FreqMHz,
            R.id.widget_fav12_FreqMHz };
    private static final int sFreqNames[] = { R.id.widget_fav01_FreqName,
            R.id.widget_fav02_FreqName, R.id.widget_fav03_FreqName, R.id.widget_fav04_FreqName,
            R.id.widget_fav05_FreqName, R.id.widget_fav06_FreqName, R.id.widget_fav07_FreqName,
            R.id.widget_fav08_FreqName, R.id.widget_fav09_FreqName, R.id.widget_fav10_FreqName,
            R.id.widget_fav11_FreqName, R.id.widget_fav12_FreqName };
    private static FMEventListener sListener = new FMEventListener() {
        @Override
        public void onOff(int reasonCode) {
            sContext = RadioApplication.getInstance();
            sReceivedChannelName = null;
            sReceivedRadioText = null;
            sInfomationViewState = null;
            update(UPDATE_REMOTEVIEWS_FLAG_ALL);
        }

        @Override
        public void onOn() {
            sContext = RadioApplication.getInstance();
            boolean isInitialAccess = RadioApplication.isInitialAccess();
            if (isInitialAccess) {
                RadioApplication.setInitialAccess(false);
            }
            update(UPDATE_REMOTEVIEWS_FLAG_POWER);
            sInfomationViewState = null;
        }

        @Override
        public void onRDSReceived(long freq, String channelName, String radioText) {
            sContext = RadioApplication.getInstance();
            boolean update = false;
            if (channelName != null && !channelName.equals("")) {
                if (sReceivedChannelName == null || !sReceivedChannelName.equals(channelName)) {
                    sReceivedChannelName = channelName;
                    update = true;
                }
            }
            if (radioText != null && !radioText.equals("")) {
                if (sReceivedRadioText == null || !sReceivedRadioText.equals(radioText)) {
                    sReceivedRadioText = radioText;
                    update = true;
                }
            }
            if (update)
                update(UPDATE_REMOTEVIEWS_FLAG_RDS);
        }

        @Override
        public void onTune(long frequency) {
            sContext = RadioApplication.getInstance();
            sReceivedChannelName = null;
            sReceivedRadioText = null;
            sInfomationViewState = null;
            update(UPDATE_REMOTEVIEWS_FLAG_FAVORITES | UPDATE_REMOTEVIEWS_FLAG_FREQUENCY
                    | UPDATE_REMOTEVIEWS_FLAG_RDS);
        }
    };
    private static FutureTask<ConcurrentHashMap<Integer, Integer>> sLoadSpanYTask = new FutureTask<ConcurrentHashMap<Integer, Integer>>(
            new Runnable() {

                @SuppressWarnings("unchecked")
                @Override
                public synchronized void run() {
                    Log.d(TAG, "sLoadSpanYTask run");
                    if (sSpanYMap != null)
                        return;
                    if (sContext == null) {
                        Log.e(TAG, "sLoadSpanYTask - context is null!!");
                        return;
                    }
                    ObjectInputStream ois = null;
                    try {
                        ois = new ObjectInputStream(sContext.openFileInput(WIDGET_FILE_NAME));
                        sSpanYMap = (ConcurrentHashMap<Integer, Integer>) ois.readObject();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (StreamCorruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        if (sSpanYMap == null) {
                            sSpanYMap = new ConcurrentHashMap<Integer, Integer>();
                        }
                        try {
                            if (ois != null) {
                                ois.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "sLoadSpanYTask end");
                    }
                }
            }, null);
    private static Thread sLoadSpanYThread = null;
    private static RadioPlayer sPlayer = null;

    private static String sReceivedChannelName;
    private static String sReceivedRadioText;

    private static ConcurrentHashMap<Integer, Integer> sSpanYMap = null;
    private static final String TAG = "FMRadioProvider";

    private static final int UPDATE_REMOTEVIEWS_FLAG_FAVORITES = 0x1;
    private static final int UPDATE_REMOTEVIEWS_FLAG_FREQUENCY = 0x2;
    private static final int UPDATE_REMOTEVIEWS_FLAG_POWER = 0x4;
    private static final int UPDATE_REMOTEVIEWS_FLAG_RDS = 0x8;
    private static final int UPDATE_REMOTEVIEWS_FLAG_SPANY = 0x10;
    private static final int UPDATE_REMOTEVIEWS_FLAG_ALL = UPDATE_REMOTEVIEWS_FLAG_FAVORITES
            | UPDATE_REMOTEVIEWS_FLAG_FREQUENCY | UPDATE_REMOTEVIEWS_FLAG_POWER
            | UPDATE_REMOTEVIEWS_FLAG_RDS | UPDATE_REMOTEVIEWS_FLAG_SPANY;

    private static final String WIDGET_FILE_NAME = "widgetspany";

    private static void appendBaseTo(RemoteViews rv) {
        Intent intentOff = new Intent(ACTION_RADIO_WIDGET_POWER_OFF);
        if (intentOff != null) {
            PendingIntent pi = PendingIntent.getBroadcast(sContext, 0, intentOff,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            rv.setOnClickPendingIntent(R.id.ic_power_on, pi);
        }
        Intent intentOn = new Intent(ACTION_RADIO_WIDGET_POWER_ON);
        if (intentOn != null) {
            PendingIntent pi = PendingIntent.getBroadcast(sContext, 0, intentOn,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.ic_power_off, pi);
        }
        Intent intentPrev = new Intent(ACTION_RADIO_WIDGET_PREV);
        if (intentPrev != null) {
            PendingIntent pi = PendingIntent.getBroadcast(sContext, 0, intentPrev,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.ic_arrow_left, pi);
        }
        Intent intentNext = new Intent(ACTION_RADIO_WIDGET_NEXT);
        if (intentNext != null) {
            PendingIntent pi = PendingIntent.getBroadcast(sContext, 0, intentNext,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.ic_arrow_right, pi);
        }
        Intent intentFrequency = new Intent(Intent.ACTION_MAIN);
        intentFrequency.addCategory(Intent.CATEGORY_LAUNCHER);
        intentFrequency.setComponent(new ComponentName(MainActivity.class.getPackage().getName(),
                MainActivity.class.getName()));
        intentFrequency.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intentFrequency.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intentFrequency != null) {
            PendingIntent pi = PendingIntent.getActivity(sContext, 0, intentFrequency, 0);
            rv.setOnClickPendingIntent(R.id.layout_frequency, pi);
        }
        Intent intentFavStar = new Intent(ACTION_RADIO_WIDGET_FAV);
        if (intentFavStar != null) {
            Log.d(TAG, "intentFavStar is called");
            PendingIntent pi = PendingIntent.getBroadcast(sContext, 0, intentFavStar, 0);
            rv.setOnClickPendingIntent(R.id.widget_star, pi);
        }
        Intent intentFav = null;
        for (int i = 0; i < sFavClick.length; i++) {
            intentFav = new Intent(ACTION_RADIO_WIDGET_FAV + i);
            if (intentFav != null) {
                PendingIntent pi = PendingIntent.getBroadcast(sContext, 0, intentFav, 0);
                rv.setOnClickPendingIntent(sFavClick[i], pi);
            }
            rv.setTextViewText(sFreqMHzs[i], "");
            rv.setTextViewText(sFreqNames[i], "");
            rv.setViewVisibility(sAddImgs[i], View.VISIBLE);
            rv.setContentDescription(
                    sFreqMHzs[i],
                    sContext.getString(R.string.desc_button,
                            sContext.getString(R.string.desc_add_favorite)));
        }
        rv.setTextViewText(R.id.txt_turn_on,
                sContext.getString(R.string.turn_on_radio, sContext.getString(R.string.app_name)));

        rv.setImageViewResource(R.id.widget_star, R.drawable.radio_favorite_btn_normal);

        rv.setContentDescription(R.id.widget_star, sContext.getString(R.string.desc_add_favorite));

        rv.setContentDescription(R.id.txt_mhz, " ");
        rv.setContentDescription(R.id.ic_power_on,
                sContext.getString(R.string.desc_quick_pannel_power));
        rv.setContentDescription(R.id.ic_power_off,
                sContext.getString(R.string.desc_quick_pannel_power));
        rv.setContentDescription(R.id.ic_arrow_left, sContext.getString(R.string.desc_prev));
        rv.setContentDescription(R.id.ic_arrow_right, sContext.getString(R.string.desc_next));
    }

    private static void appendFavoritesTo(RemoteViews rv) {
        if (rv == null) {
            Log.e(TAG, "RemoteView is null!!");
            return;
        }
        for (int i = 0; i < sFreqMHzs.length; i++) {
            rv.setTextViewText(sFreqMHzs[i], "");
            rv.setTextViewText(sFreqNames[i], "");
            rv.setViewVisibility(sAddImgs[i], View.VISIBLE);
            rv.setContentDescription(
                    sFreqMHzs[i],
                    sContext.getString(R.string.desc_button,
                            sContext.getString(R.string.desc_add_favorite)));
        }
        Channel c = null;
        int freq = RadioApplication.getInitialFrequency();
        for (int i = sChannelStore.size() - 1; i >= 0; i--) {
            c = sChannelStore.getChannel(i);
            int position = c.mPosition;
            if (position != -1) {
                if (sPlayer.isOn() && c.mFreqency == freq) {
                    if(getNeedDarkColor(sContext) == true){
                        rv.setTextColor(sFreqMHzs[position],
                                sContext.getResources().getColor(R.color.need_dark_select_bg,null));
                        rv.setTextColor(sFreqNames[position],
                                sContext.getResources().getColor(R.color.need_dark_select_bg,null));
                    }else {
                        rv.setTextColor(sFreqMHzs[position],
                                sContext.getResources().getColor(R.color.widget_text_freq_focus,null));
                        rv.setTextColor(sFreqNames[position],
                                sContext.getResources().getColor(R.color.widget_text_freq_focus,null));
                    }
                } else {
                    if(getNeedDarkColor(sContext) == true){
                        rv.setTextColor(sFreqMHzs[position],
                                sContext.getResources().getColor(R.color.need_dark_bg,null));
                        rv.setTextColor(sFreqNames[position],
                                sContext.getResources().getColor(R.color.need_dark_bg,null));
                    }else {
                        rv.setTextColor(sFreqMHzs[position],
                                sContext.getResources().getColor(R.color.widget_text_freq_normal,null));
                        rv.setTextColor(sFreqNames[position],
                                sContext.getResources().getColor(R.color.widget_text_freq_normal,null));
                    }
                }
                if (c.mFreqName == null || c.mFreqName.length() == 0) {
                    rv.setViewVisibility(sFreqNames[position], View.GONE);
                } else {
                    rv.setViewVisibility(sFreqNames[position], View.VISIBLE);
                }
                String sfreq = RadioPlayer.convertToMhz(c.mFreqency);
                rv.setTextViewText(sFreqMHzs[position], sfreq);
                rv.setTextViewText(sFreqNames[position], c.mFreqName);
                rv.setViewVisibility(sAddImgs[position], View.INVISIBLE);
                String desc = sContext.getString(R.string.desc_button,
                        sContext.getString(R.string.desc_favorite, sfreq + ", " + c.mFreqName));
                rv.setContentDescription(sFreqMHzs[position], desc);

                // to avoid read radio station name second time.
                rv.setContentDescription(sFreqNames[position], "\u00A0");
            }
        }

    }

    private static void appendFrequencyTo(RemoteViews rv) {
        int ifreq = 0;
        String sfreq = null;
        String desc = null;
        rv.setTextViewTextSize(R.id.txt_frequency, TypedValue.COMPLEX_UNIT_PX, sContext.getResources().getDimension(R.dimen.widget_txt_frequency_textsize));
        rv.setTextViewText(R.id.txt_mhz, sContext.getResources().getString(R.string.mhz));
        if (sInfomationViewState != null && sInfomationViewState.length() != 0) {
            rv.setTextViewText(R.id.txt_freq_name, sInfomationViewState);
            rv.setViewVisibility(R.id.txt_below_frequency_layout, View.VISIBLE);
            rv.setViewVisibility(R.id.txt_freq_name_layout, View.VISIBLE);
            rv.setViewVisibility(R.id.txt_freq_name, View.VISIBLE);
        }
        if (sPlayer.isOn()) {
            ifreq = RadioPlayer.getValidFrequency(RadioApplication.getInitialFrequency());
            if(getNeedDarkColor(sContext) == true){
                rv.setTextColor(R.id.txt_frequency,
                        sContext.getResources().getColor(R.color.need_dark_bg,null));
                rv.setTextColor(R.id.txt_mhz,
                        sContext.getResources().getColor(R.color.need_dark_bg,null));
            }else {
                rv.setTextColor(R.id.txt_frequency,
                        sContext.getResources().getColor(R.color.widget_text_freq_normal,null));
                rv.setTextColor(R.id.txt_mhz,
                        sContext.getResources().getColor(R.color.widget_text_freq_normal,null));
            }
            rv.setViewVisibility(R.id.widget_star, View.VISIBLE);
            rv.setViewVisibility(R.id.ic_arrow_left, View.VISIBLE);
            rv.setViewVisibility(R.id.ic_arrow_right, View.VISIBLE);
        } else {
            if(getNeedDarkColor(sContext) == true){
                rv.setTextColor(R.id.txt_frequency,
                        sContext.getResources().getColor(R.color.need_dark_bg_50,null));
                rv.setTextColor(R.id.txt_mhz,
                        sContext.getResources().getColor(R.color.need_dark_bg_50,null));
            }else {
                rv.setTextColor(R.id.txt_frequency,
                        sContext.getResources().getColor(R.color.widget_text_freq_normal_50,null));
                rv.setTextColor(R.id.txt_mhz,
                        sContext.getResources().getColor(R.color.widget_text_freq_normal_50,null));
            }
            rv.setViewVisibility(R.id.widget_star, View.INVISIBLE);
            rv.setViewVisibility(R.id.ic_arrow_left, View.INVISIBLE);
            rv.setViewVisibility(R.id.ic_arrow_right, View.INVISIBLE);
        }

        sfreq = RadioPlayer.convertToMhz(ifreq);
        rv.setTextViewText(R.id.txt_frequency, sfreq);

        desc = sfreq + " " + sContext.getString(R.string.mhz_content_des);
        Channel c = sChannelStore.getChannelByFrequency(ifreq);
        if (c != null && c.mIsFavourite){
            rv.setImageViewResource(R.id.widget_star, R.drawable.radio_favorite_btn_press);

            rv.setContentDescription(R.id.widget_star, sContext.getString(R.string.desc_rem_favorite));
        }
        else{
            if(getNeedDarkColor(sContext) == true){
                rv.setImageViewResource(R.id.widget_star, R.drawable.radio_favorite_btn_normal_dark);
            } else {
                rv.setImageViewResource(R.id.widget_star, R.drawable.radio_favorite_btn_normal);
            }
            rv.setContentDescription(R.id.widget_star, sContext.getString(R.string.desc_add_favorite));
        }
        if (c != null && c.mFreqName != null && !c.mFreqName.isEmpty())
            desc += " " + c.mFreqName;
        rv.setContentDescription(R.id.layout_frequency, desc);
        rv.setContentDescription(R.id.txt_frequency, desc);
        rv.setContentDescription(R.id.txt_freq_name, desc);
    }

    private static void appendPowerTo(RemoteViews rv) {
        if (sPlayer.isOn()) {
            rv.setViewVisibility(R.id.ic_power_on, View.VISIBLE);
            rv.setViewVisibility(R.id.ic_power_off, View.GONE);
        } else {
            rv.setViewVisibility(R.id.ic_power_on, View.GONE);
            rv.setViewVisibility(R.id.ic_power_off, View.VISIBLE);
        }
    }

    private static void appendRdsTo(RemoteViews rv) {
        if (rv == null) {
            Log.d(TAG, "updateWidget() :: rv is null.");
            return;
        }
        if (sPlayer == null) {
            Log.d(TAG, "updateWidget() :: sPlayer is null.");
            return;
        }
        if (sPlayer.isOn()) {
            rv.setViewVisibility(R.id.txt_below_frequency_layout, View.GONE);
            rv.setViewVisibility(R.id.txt_turn_on, View.GONE);
            boolean hasChannelName = false;
            if (sReceivedChannelName != null && sReceivedChannelName.length() != 0) {
                hasChannelName = true;
                rv.setTextViewText(R.id.txt_freq_name, sReceivedChannelName);
                rv.setViewVisibility(R.id.txt_below_frequency_layout, View.VISIBLE);
                rv.setViewVisibility(R.id.txt_freq_name_layout, View.VISIBLE);
            } else {
                int freq = sPlayer.getFrequency();
                Channel c = sChannelStore.getChannelByFrequency(freq);
                if ((c != null) && (c.mFreqName != null) && !c.mFreqName.isEmpty()) {
                    hasChannelName = true;
                    rv.setViewVisibility(R.id.txt_below_frequency_layout, View.VISIBLE);
                    rv.setViewVisibility(R.id.txt_freq_name_layout, View.VISIBLE);
                    rv.setTextViewText(R.id.txt_freq_name, c.mFreqName);
                } else {
                    rv.setViewVisibility(R.id.txt_below_frequency_layout, View.GONE);
                    rv.setViewVisibility(R.id.txt_freq_name_layout, View.GONE);
                }
            }
            if (sReceivedRadioText != null && sReceivedRadioText.length() != 0) {
                rv.setViewVisibility(R.id.txt_below_frequency_layout, View.VISIBLE);
                rv.setViewVisibility(R.id.txt_freq_name_layout, View.VISIBLE);
                rv.setTextViewText(R.id.txt_freq_rt, sReceivedRadioText);
                rv.setViewVisibility(R.id.txt_freq_rt, View.VISIBLE);
                if (hasChannelName)
                    rv.setViewVisibility(R.id.txt_freq_divider, View.VISIBLE);
            } else {
                rv.setViewVisibility(R.id.txt_freq_divider, View.GONE);
                rv.setViewVisibility(R.id.txt_freq_rt, View.GONE);
            }
        } else {
            rv.setViewVisibility(R.id.txt_below_frequency_layout, View.VISIBLE);
            rv.setViewVisibility(R.id.txt_turn_on, View.VISIBLE);
            rv.setViewVisibility(R.id.txt_freq_name_layout, View.GONE);
        }
    }

    private static void appendSpanYTo(final int widgetId, RemoteViews rv) {
        loadSpanYMap(sContext);
        Integer spanY = null;
        if(sSpanYMap != null){
            spanY = sSpanYMap.get(widgetId);
        }
        if (spanY == null) {
            spanY = 1;
            if(sSpanYMap != null){
                sSpanYMap.put(widgetId, spanY);
            }
        }
        switch (spanY) {
        case 0:
        case 1:
            rv.setViewVisibility(R.id.widget_span1, View.GONE);
            rv.setViewVisibility(R.id.widget_span2, View.GONE);
            rv.setViewVisibility(R.id.widget_span3, View.GONE);
            break;
        case 2:
            rv.setViewVisibility(R.id.widget_span1, View.VISIBLE);
            rv.setViewVisibility(R.id.widget_span2, View.GONE);
            rv.setViewVisibility(R.id.widget_span3, View.GONE);
            break;
        case 3:
            rv.setViewVisibility(R.id.widget_span1, View.VISIBLE);
            rv.setViewVisibility(R.id.widget_span2, View.VISIBLE);
            rv.setViewVisibility(R.id.widget_span3, View.GONE);
            break;
        case 4:
            rv.setViewVisibility(R.id.widget_span1, View.VISIBLE);
            rv.setViewVisibility(R.id.widget_span2, View.VISIBLE);
            rv.setViewVisibility(R.id.widget_span3, View.VISIBLE);
            break;
        case 5:
            rv.setViewVisibility(R.id.widget_span1, View.VISIBLE);
            rv.setViewVisibility(R.id.widget_span2, View.VISIBLE);
            rv.setViewVisibility(R.id.widget_span3, View.VISIBLE);
            break;
        default:
            Log.e(TAG, "Unknown Widget SpanY = " + spanY);
            break;
        }
    }

    private static String convertFlagToString(final int flags) {
        StringBuilder sb = new StringBuilder();
        if ((UPDATE_REMOTEVIEWS_FLAG_ALL & flags) == UPDATE_REMOTEVIEWS_FLAG_ALL) {
            sb.append("ALL, ");
        } else {
            if ((UPDATE_REMOTEVIEWS_FLAG_FAVORITES & flags) == UPDATE_REMOTEVIEWS_FLAG_FAVORITES) {
                sb.append("FAVORITES, ");
            }
            if ((UPDATE_REMOTEVIEWS_FLAG_FREQUENCY & flags) == UPDATE_REMOTEVIEWS_FLAG_FREQUENCY) {
                sb.append("FREQUENCY, ");
            }
            if ((UPDATE_REMOTEVIEWS_FLAG_POWER & flags) == UPDATE_REMOTEVIEWS_FLAG_POWER) {
                sb.append("POWER, ");
            }
            if ((UPDATE_REMOTEVIEWS_FLAG_RDS & flags) == UPDATE_REMOTEVIEWS_FLAG_RDS) {
                sb.append("RDS, ");
            }
            if ((UPDATE_REMOTEVIEWS_FLAG_SPANY & flags) == UPDATE_REMOTEVIEWS_FLAG_SPANY) {
                sb.append("SPANY, ");
            }
        }
        return sb.toString();
    }

    public static void loadSpanYMap(Context context) {
        if (sSpanYMap != null)
            return;
        sContext = context;
        boolean err = false;
        try {
            if (sLoadSpanYThread != null) {
                Log.v(TAG, "loadSpanYMap() - wait");
                sLoadSpanYTask.get(100, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            err = true;
            e.printStackTrace();
        } catch (ExecutionException e) {
            err = true;
            e.printStackTrace();
        } catch (TimeoutException e) {
            err = true;
            e.printStackTrace();
        } finally {
            if (sLoadSpanYThread == null || err) {
                Log.v(TAG, "loadSpanYMap() - start");
                sLoadSpanYThread = new Thread(sLoadSpanYTask);
                sLoadSpanYThread.start();
            }
            Log.v(TAG, "loadSpanYMap() - end");
        }
    }

    private static RemoteViews makeRemoteViews(final int widgetId, final int flags) {
        if (sPlayer == null) {
            Log.d(TAG, "updateWidget() :: sPlayer is null.");
            return null;
        }

        RemoteViews rv;
        if(getNeedDarkColor(sContext) == true){
            rv = new RemoteViews(sContext.getPackageName(), R.layout.widget_dark);
        }else {
            rv = new RemoteViews(sContext.getPackageName(), R.layout.widget);
        }
        if ((UPDATE_REMOTEVIEWS_FLAG_ALL & flags) == UPDATE_REMOTEVIEWS_FLAG_ALL) {
            appendBaseTo(rv);
        }
        if ((UPDATE_REMOTEVIEWS_FLAG_FAVORITES & flags) == UPDATE_REMOTEVIEWS_FLAG_FAVORITES) {
            appendFavoritesTo(rv);
        }
        if ((UPDATE_REMOTEVIEWS_FLAG_FREQUENCY & flags) == UPDATE_REMOTEVIEWS_FLAG_FREQUENCY) {
            appendFrequencyTo(rv);
        }
        if ((UPDATE_REMOTEVIEWS_FLAG_POWER & flags) == UPDATE_REMOTEVIEWS_FLAG_POWER) {
            appendPowerTo(rv);
        }
        if ((UPDATE_REMOTEVIEWS_FLAG_RDS & flags) == UPDATE_REMOTEVIEWS_FLAG_RDS) {
            appendRdsTo(rv);
        }
        if ((UPDATE_REMOTEVIEWS_FLAG_SPANY & flags) == UPDATE_REMOTEVIEWS_FLAG_SPANY) {
            appendSpanYTo(widgetId, rv);
        }
        return rv;
    }

    private static void storeSpanYMap() {
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                Log.v(TAG, "storeSpanYMap thread run");
                if (sContext == null) {
                    Log.e(TAG, "load() - context is null!!");
                    return;
                }
                if (sSpanYMap == null) {
                    Log.e(TAG, "load() - sSpanYMap is null!!");
                    return;
                }
                ObjectOutputStream oos = null;
                try {
                    oos = new ObjectOutputStream(sContext.openFileOutput(WIDGET_FILE_NAME,
                            Context.MODE_PRIVATE));
                    oos.writeObject(sSpanYMap);
                    oos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (oos != null) {
                            oos.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.v(TAG, "storeSpanYMap thread - end");
                }
            }
        });
        t.start();
    }

    private static void update(final int flags) {
        Log.d(TAG, "update() - flags : " + convertFlagToString(flags));
        int[] appWidgetIds = null;
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(sContext);
        if (appWidgetManager == null) {
            Log.e(TAG, "update() :: AppWidgetManager is null.");
            return;
        }
        appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(sContext,
                "com.sec.android.app.fm.widget.FMRadioProvider"));
        if (appWidgetIds == null) {
            Log.e(TAG, "update() :: appWidgetIds is null.");
            return;
        }
        for (int widgetId : appWidgetIds) {
            RemoteViews rv = makeRemoteViews(widgetId, flags);
            if (flags == UPDATE_REMOTEVIEWS_FLAG_ALL)
                appWidgetManager.updateAppWidget(widgetId, rv);
            else
                appWidgetManager.partiallyUpdateAppWidget(widgetId, rv);
        }
    }

    private static void update(final int widgetId, final int flags) {
        Log.d(TAG, "update() - id:" + widgetId + " flags:" + convertFlagToString(flags));
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(sContext);
        if (appWidgetManager == null) {
            Log.e(TAG, "update() :: mAppWidgetManager is null.");
            return;
        }
        RemoteViews rv = makeRemoteViews(widgetId, flags);
        if (flags == UPDATE_REMOTEVIEWS_FLAG_ALL)
            appWidgetManager.updateAppWidget(widgetId, rv);
        else
            appWidgetManager.partiallyUpdateAppWidget(widgetId, rv);
    }

    private synchronized void initPlayer() {
        if (sPlayer == null) {
            sPlayer = RadioPlayer.getInstance();
            sPlayer.registerListener(sListener);
        }
    }

    private boolean isStartedService(Context context) {
        ActivityManager mActivityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningServiceInfo> rsiList = mActivityManager.getRunningServices(100);
        boolean isExist = false;
        if (rsiList != null) {
            for (RunningServiceInfo rsi : rsiList) {
                if (APP_PACKAGE_NAME.equals(rsi.service.getPackageName())) {
                    boolean mNotification = rsi.service.getClassName().equals(
                            "com.sec.android.app.fm.NotificationService");
                    if (!mNotification) {
                        isExist = false;
                        return isExist;
                    }
                    Log.d("run service", "Package Name : " + rsi.service.getPackageName());
                    isExist = true;
                }
            }
        }
        return isExist;
    }

    private void onClickFavorite(int pos) {
        if (sPlayer == null) {
            Log.e("FMRadioProvider", "mPlayer is null.");
            return;
        }
        Log.d(TAG, "onClickFavorite() pos:" + pos);
        int freq = sPlayer.isOn() ? sPlayer.getFrequency() : -1;
        Channel channel = sChannelStore.getFavoriteChannel(pos);
        if (channel != null) {
            if (freq == channel.mFreqency) {
                return;
            }
            if (!isStartedService(sContext)) {
                Intent serviceIntent = new Intent(sContext, NotificationService.class);
                serviceIntent.setAction(NotificationReceiver.TUNE_ACTION);
                serviceIntent.putExtra(NotificationReceiver.FREQ, channel.mFreqency);
                sContext.startService(serviceIntent);
                Log.d(TAG, "NotificationService start");
            } else {
                Intent intent = new Intent(NotificationReceiver.TUNE_ACTION);
                intent.putExtra(NotificationReceiver.FREQ, channel.mFreqency);
                sContext.sendBroadcast(intent);
            }
            return;
        }
        // if (!sPlayer.isOn()) {
        // RadioToast.showToast(sContext, R.string.toast_on_alert,
        // Toast.LENGTH_SHORT);
        // } else if (!sChannelStore.addFavoriteChannel(freq,
        // sReceivedChannelName, pos)) {
        // RadioToast.showToast(sContext, R.string.toast_already_added,
        // Toast.LENGTH_SHORT);
        // } else {
        // update(UPDATE_REMOTEVIEWS_FLAG_FAVORITES);
        // }
    }

    private void onClickStar() {
        if (!sPlayer.isOn()) {
            RadioToast.showToast(sContext, R.string.toast_on_alert, Toast.LENGTH_SHORT);
        } else {
            int emptyPos = sChannelStore.getEmptyPositionOfFavorite();
            int freq = sPlayer.isOn() ? sPlayer.getFrequency() : -1;
            if (sChannelStore.getFavoriteFrequency(freq)) {
                sChannelStore.removeFavoriteChannel(freq);
            } else {
                if (emptyPos == -1) {
                    RadioToast.showToast(sContext, sContext.getString(R.string.toast_max_favorite, 12), Toast.LENGTH_SHORT);
                    return;
                }
                if(sReceivedChannelName == null)
                {
                    sReceivedChannelName = "";
                }
                sChannelStore.addFavoriteChannel(freq, sReceivedChannelName, emptyPos);
            }
        }
        update(UPDATE_REMOTEVIEWS_FLAG_FAVORITES | UPDATE_REMOTEVIEWS_FLAG_FREQUENCY);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        Log.v(TAG, "onDeleted() is called");
        sContext = context;
        if (sSpanYMap != null) {
            for (int widgetId : appWidgetIds) {
                sSpanYMap.remove(widgetId);
            }
        }
        storeSpanYMap();
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context,
            AppWidgetManager appWidgetManager, int appWidgetId,
            Bundle newOptions) {
        Log.v(TAG, "onAppWidgetOptionsChanged");

        if (sPlayer == null) {
            initPlayer();
        }

        int widgetspany;
        
        widgetspany = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT) / (sContext.getResources().getInteger(R.integer.widget_cell_height));

        int newSpanY = widgetspany;
        int id = appWidgetId;
        if (sSpanYMap != null ) {
            Integer prevSpanY = sSpanYMap.get(id);
            if (prevSpanY == null) {
                prevSpanY = 1;
                sSpanYMap.put(id, prevSpanY);
            }
            Log.d(TAG, "prevSpanY:" + prevSpanY + " newSpanY :" + newSpanY + " id:" + id);
            if (newSpanY != prevSpanY) {
                sSpanYMap.replace(id, newSpanY);
                update(id, UPDATE_REMOTEVIEWS_FLAG_SPANY);
                storeSpanYMap();
            }
        }
        update(id, UPDATE_REMOTEVIEWS_FLAG_FREQUENCY);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId,
                newOptions);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive() is called : "+intent.getAction());
        sContext = context;
        super.onReceive(context, intent);
        initPlayer();
        String action = intent.getAction();
        if (action == null) {
            Log.e(TAG, "action is null!");
            return;
        }
        Log.d(TAG, "action:" + action.substring(action.lastIndexOf(".")));
        loadSpanYMap(context);
        // 2012.02.15 TOD [P120214-2942] The service is checked to be alive
        // before operating by all intent. [
        if (!isStartedService(context)) {
            Log.d(TAG, "Service is not started");
            if (ACTION_RADIO_WIDGET_POWER_ON.equals(action)) {
                Intent mediaSessionIntent = new Intent(ACTION_REGISTER_MEDIA_SESSION);
                context.sendBroadcast(mediaSessionIntent);
                
                Intent serviceIntent = new Intent(context, NotificationService.class);
                serviceIntent.setAction(NotificationReceiver.ON_ACTION);
                context.startService(serviceIntent);
            } else if (ACTION_RADIO_WIDGET_POWER_OFF.equals(action)) {
                Intent serviceIntent = new Intent(context, NotificationService.class);
                serviceIntent.setAction(NotificationReceiver.OFF_ACTION);
                context.startService(serviceIntent);
            } else if (ACTION_RADIO_WIDGET_PREV.equals(action)) {
                updateStateView(SHOW_SEEKING);
                Intent serviceIntent = new Intent(context, NotificationService.class);
                serviceIntent.setAction(NotificationReceiver.PREV_ACTION);
                context.startService(serviceIntent);
                update(UPDATE_REMOTEVIEWS_FLAG_FREQUENCY);
            } else if (ACTION_RADIO_WIDGET_NEXT.equals(action)) {
                updateStateView(SHOW_SEEKING);
                Intent serviceIntent = new Intent(context, NotificationService.class);
                serviceIntent.setAction(NotificationReceiver.NEXT_ACTION);
                context.startService(serviceIntent);
                update(UPDATE_REMOTEVIEWS_FLAG_FREQUENCY);
            }
        } else {
            if (ACTION_RADIO_WIDGET_POWER_ON.equals(action)) {
                context.sendBroadcast(new Intent(NotificationReceiver.ON_ACTION));
            } else if (ACTION_RADIO_WIDGET_POWER_OFF.equals(action)) {
                context.sendBroadcast(new Intent(NotificationReceiver.OFF_ACTION));
            } else if (ACTION_RADIO_WIDGET_PREV.equals(action)) {
                context.sendBroadcast(new Intent(NotificationReceiver.PREV_ACTION));
            } else if (ACTION_RADIO_WIDGET_NEXT.equals(action)) {
                context.sendBroadcast(new Intent(NotificationReceiver.NEXT_ACTION));
            }
        }
        // 2012.02.15 TOD [P120214-2942] The service is checked to be alive
        // before operating by all intent. ]

        if (action.contains(ACTION_RADIO_WIDGET_FAV)) {
            if (action.equals(ACTION_RADIO_WIDGET_FAV)) {
                onClickStar();
            } else {
                String pos = intent.getAction().substring(ACTION_RADIO_WIDGET_FAV.length());
                onClickFavorite(Integer.parseInt(pos));
            }
        } else if (ACTION_RADIO_WIDGET_REFRESH.equals(action)
                || ACTION_RADIO_WIDGET_REFRESH_FORCE.equals(action)
                || ACTION_RADIO_WALLPAPER_CHANGED.equals(action)) {
            update(UPDATE_REMOTEVIEWS_FLAG_ALL);
        }
    }

    private void updateStateView(int state) {
        if(sContext == null || sPlayer == null) {
            return;
        }
        switch(state) {
            case SHOW_SEEKING :
                if(sPlayer.isOn()){
                    sInfomationViewState = sContext.getString(R.string.seeking);
                }
                break;
            case SHOW_EMPTY :
                sInfomationViewState = "";
                break;
            default :
                sInfomationViewState = "";
                break;
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate() is called");
        sContext = context;
        loadSpanYMap(context);
        initPlayer();
        update(UPDATE_REMOTEVIEWS_FLAG_ALL);
    }

    public static boolean getNeedDarkColor(Context context) {
        int need_dark_font = Settings.System.getInt(context.getContentResolver(), "need_dark_font", 0);
        if (need_dark_font == 1)
            return true;
        else
            return false;
    }
}
