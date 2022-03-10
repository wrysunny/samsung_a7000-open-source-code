package com.sec.android.app.dns.ui;

import java.util.Calendar;

import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.RadioDNSUtil;
import com.sec.android.app.dns.radiodns.RadioDNSConnection;
import com.sec.android.app.dns.radioepg.PiData;
import com.sec.android.app.dns.radioepg.PiData.Programme;
import com.sec.android.app.fm.FMNotificationManager;
import com.sec.android.app.fm.MainActivity;
import com.sec.android.app.fm.R;
import com.sec.android.app.fm.RadioToast;
import com.sec.android.app.fm.data.Channel;
import com.sec.android.app.fm.data.ChannelStore;
import com.sec.android.app.fm.data.PiDataManager;
import com.sec.android.app.fm.util.FMUtil;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class EpgDetailActivity extends FragmentActivity {
    private static final String TAG = "EpgDetailActivity";
    private static final int MENU_FAVORITE = Menu.FIRST;
    private PiDataManager mPiDataManager = PiDataManager.getInstance();
    private Channel mCurrentChannel = null;
    private ImageView mPrevTime = null;
    private ImageView mNextTime = null;
    private TextView mCurrentProgram = null;
    private ViewPager mPager = null;
    private int mDisplayedIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogDns.d(TAG, "onCreate");
        setContentView(R.layout.epgdetailview_screen);
        mCurrentChannel = (Channel) getIntent().getSerializableExtra("channel");
        String title = getString(R.string.epg) + "-" + (float) (mCurrentChannel.mFreqency) / 100.f
                + getString(R.string.mhz);
        setTitle(title);

        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(false);

        mPrevTime = (ImageView) findViewById(R.id.prev_time);
        mNextTime = (ImageView) findViewById(R.id.next_time);
        mCurrentProgram = (TextView) findViewById(R.id.current_time);
        mPrevTime.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                LogDns.d(TAG, "onClick - previous time");
                int prevIndex = mPager.getCurrentItem() - 1;
                if (prevIndex >= 0) {
                    mDisplayedIndex = prevIndex;
                    mPager.setCurrentItem(prevIndex);
                }
            }
        });

        mNextTime.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                LogDns.d(TAG, "onClick - next time");
                int nextIndex = mPager.getCurrentItem() + 1;
                EpgPagerAdapter adapter = (EpgPagerAdapter) mPager.getAdapter();
                if (nextIndex < adapter.getCount()) {
                    mDisplayedIndex = nextIndex;
                    mPager.setCurrentItem(nextIndex);
                }
            }
        });

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setPageMargin(convertDpToPixel(getResources().getInteger(R.integer.epg_pager_margin)));
        mPager.setAdapter(new EpgPagerAdapter(getSupportFragmentManager()));
        mPager.setOffscreenPageLimit(2);
        mPager.addOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixel) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager.SCROLL_STATE_SETTLING) {
                    mDisplayedIndex = mPager.getCurrentItem();
                    setPlayTime(mDisplayedIndex);
                }
            }
        });
        displayCurrentPage(getCurrentProgramIndex());
    }

    private int getCurrentProgramIndex() {
        PiData currentPi = mPiDataManager.getPiData(mCurrentChannel.mFreqency);
        if (currentPi == null) {
            LogDns.d(TAG, "current Pi is null");
            return 0;
        }

        int countOfProgram = currentPi.getNumberOfPrograms();
        for (int i = 0; i < countOfProgram; i++) {
            Programme programme = currentPi.getProgram(i);
            int startTime = programme.getStartTime();
            int endTime = startTime + programme.getDuration();
            int currentTime = RadioDNSUtil.timeToInt(Calendar.getInstance().getTime());
            LogDns.v(TAG, "start:" + startTime + " end:" + endTime + " now:" + currentTime);
            if (startTime <= currentTime && endTime > currentTime) {
                return i;
            }
        }
        return 0;
    }

    protected void setPlayTime(int displayedIndex) {
        String playTime = ((EpgPagerAdapter) mPager.getAdapter()).getPlayTime(displayedIndex);
        mCurrentProgram.setText(playTime);
    }

    public void displayCurrentPage(int index) {
        if (index != mDisplayedIndex) {
            mDisplayedIndex = index;
            mPager.setCurrentItem(mDisplayedIndex);
            setPlayTime(mDisplayedIndex);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuItem menuItem = menu.add(0, MENU_FAVORITE, MENU_FAVORITE, R.string.fav_channel);
        if (mCurrentChannel.mIsFavourite) {
            menuItem.setIcon(R.drawable.hybrid_radio_on_star);
        } else {
            menuItem.setIcon(R.drawable.hybrid_radio_off_star);
        }
        menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_FAVORITE) {
            ChannelStore channelStore = ChannelStore.getInstance();
            Channel channel = channelStore.getChannelByFrequency(mCurrentChannel.mFreqency);
            if (channel == null) {
                LogDns.e(
                        TAG,
                        "onOptionsItemSelected() - channel("
                                + LogDns.filter(mCurrentChannel.mFreqency) + ") is null");
                return false;
            }
            if (channel.mIsFavourite) {
                channel.mPosition = -1;
                channel.mIsFavourite = false;
                item.setIcon(R.drawable.hybrid_radio_off_star);
                item.setTitle(getString(R.string.desc_add_favorite));
            } else {
                int newPosition = channelStore.getEmptyPositionOfFavorite();
                if (newPosition == -1) {
                    String maxMsg = getString(R.string.toast_max_favorite, 12);
                    RadioToast.showToast(getApplicationContext(), maxMsg, Toast.LENGTH_SHORT);
                    return false;
                }
                channel.mPosition = newPosition;
                channel.mIsFavourite = true;
                item.setIcon(R.drawable.hybrid_radio_on_star);
                item.setTitle(getString(R.string.desc_rem_favorite));
            }
            channelStore.store();
        }

        switch (item.getItemId()) {

        case android.R.id.home:
            finish();
            break;
        default:
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!FMUtil.isKeyguardLocked(getApplicationContext()))
            FMNotificationManager.getInstance().removeNotification(true);
        DNSService service = MainActivity._instance.getDNSService();
        if (service != null) {
            service.setStackPriority(RadioDNSConnection.VIEW_HIGH);
        }
    }

    @Override
    protected void onStop() {
        FMNotificationManager.getInstance().registerNotification(false);
        DNSService service = MainActivity._instance.getDNSService();
        if (service != null) {
            service.setStackPriority(RadioDNSConnection.CURRENT_HIGH);
        }
        super.onStop();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mPager.setPageMargin(convertDpToPixel(getResources().getInteger(R.integer.epg_pager_margin)));
        refreshPager();
        super.onConfigurationChanged(newConfig);
    }

    public int getCurrentTime() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    public void refreshPager() {
        mPager.getAdapter().notifyDataSetChanged();
    }

    private class EpgPagerAdapter extends FragmentPagerAdapter {
        private PiData mCurrentPi = null;

        public EpgPagerAdapter(FragmentManager fm) {
            super(fm);
            mCurrentPi = mPiDataManager.getPiData(mCurrentChannel.mFreqency);
        }

        @Override
        public Fragment getItem(int position) {
            return EpgDetailFragment.newInstance(mCurrentChannel.mFreqency, position);
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return mCurrentPi.getNumberOfPrograms();
        }

        public String getPlayTime(int index) {
            return mCurrentPi.getProgram(index).getPlayTime();
        }
    }

    private int convertDpToPixel(int dp) {
        int pixel = 0;
        final float scale = getResources().getDisplayMetrics().density;
        pixel = (int) (((float) dp) * scale + 0.5f);
        return pixel;
    }
}
