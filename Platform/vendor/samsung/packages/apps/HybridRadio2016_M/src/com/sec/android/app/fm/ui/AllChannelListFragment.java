package com.sec.android.app.fm.ui;

import java.util.Calendar;
import java.util.Date;

import com.samsung.media.fmradio.FMPlayerException;
import com.sec.android.app.dns.DNSService;
import com.sec.android.app.dns.LogDns;
import com.sec.android.app.dns.radiodns.RadioDNSConnection;
import com.sec.android.app.dns.radioepg.PiData;
import com.sec.android.app.dns.ui.EpgDetailActivity;
import com.sec.android.app.fm.FMRadioFeature;
import com.sec.android.app.fm.Log;
import com.sec.android.app.fm.MainActivity;
import com.sec.android.app.fm.MainActivity.MyHandler;
import com.sec.android.app.fm.R;
import com.sec.android.app.fm.RadioPlayer;
import com.sec.android.app.fm.RadioToast;
import com.sec.android.app.fm.SettingsActivity;
import com.sec.android.app.fm.data.Channel;
import com.sec.android.app.fm.data.ChannelStore;
import com.sec.android.app.fm.data.PiDataManager;
import com.sec.android.app.fm.ui.RadioDialogFragment;
import com.sec.android.app.fm.ui.RenameDialog;
import com.sec.android.app.fm.util.FMUtil;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AllChannelListFragment extends Fragment {
    public static final String TAG = "AllChannelListFragment";

    public MyHandler myHandler = null;
    private View mChannelListFragmentview = null;
    private RadioItemAdapter mRadioArrayAdapter = null;
    private ListView mChannelListView = null;
    private PiDataManager mPiDataManager = null;
    protected Handler mHandler = null;
    private ChannelStore mChannelStore = null;
    private AudioManager mAudioManager = null;
    private RadioPlayer mPlayer = null;
    private Context mContext = null;

    private static final int MENU_REMOVE = 10;
    private static final int MENU_EDIT = 11;

    public static int MAX_FAVORITES_COUNT = 12;

    public static AllChannelListFragment newInstance() {
        AllChannelListFragment f = new AllChannelListFragment();
        return f;
    }

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach() is called");
        mContext = context;
        super.onAttach(context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() is called");
        if (mContext == null) {
            Log.e(TAG, "onCreate -mContext is null");
            return;
        }
        if (savedInstanceState != null) {
            Log.d(TAG, "onCreate savedinstancestate");
            mSavedSelectedFreq = savedInstanceState.getInt("selected_freq");
            mCurrentActionModeType = savedInstanceState.getInt("action_mode_type", MENU_NONE);
        }
        mChannelStore = ChannelStore.getInstance();
        mPlayer = RadioPlayer.getInstance();
        mModeCallback = new ModeCallback();
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mHandler = new Handler();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        if (mSelectedChannel != null || mSavedSelectedFreq != -1) {
            int freq = (mSelectedChannel != null) ? mSelectedChannel.mFreqency : mSavedSelectedFreq;
            outState.putInt("selected_freq", freq);
        }
        if(mCheckedcount > 0){
                outState.putInt("mCheckedcount", mCheckedcount);
        }
        if(mSelectAll){
                outState.putBoolean("mSelectAll", mSelectAll);
        }
        if (mIsActionMode) {
            outState.putInt("action_mode_type", mCurrentActionModeType);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() is called");
        mChannelListFragmentview = inflater.inflate(R.layout.allchannel_fragment, container, false);
        mChannelListView = (ListView) mChannelListFragmentview.findViewById(R.id.channelList);
        mChannelListView.setActivated(true);
        mChannelListView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mRadioArrayAdapter = new RadioItemAdapter();
        mChannelListView.setAdapter(mRadioArrayAdapter);
        setEmptView();
        mChannelListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
            if(!mIsActionMode) {
                if (mPlayer.isBusy()) {
                    Log.d(TAG, "RadioPlayer is busy. ignore it");
                    return;
                }
                if (FMUtil.isVoiceActive(mContext,FMUtil.NEED_TO_PLAY_FM))
                    return;
                final int index = arg2;
                if (mPlayer.isOn() && mChannelStore.getChannel(index).mFreqency == ((MainActivity)mContext).getCurrrentFrequency())
                    return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        String mute = "fm_radio_mute=1";
                        mAudioManager.setParameters(mute);
                        try {
                            boolean isOn = mPlayer.isOn();
                            if (mChannelStore.size() > index)
                                mPlayer.tuneAsyncEx(mChannelStore.getChannel(index).mFreqency);
                            if (!isOn)
                                SettingsActivity.activateTurnOffAlarm();
                        } catch (FMPlayerException e) {
                            RadioToast.showToast(getActivity().getApplicationContext(), e);
                        }
                    }
                });
            } else {
                if(mChannelListView.isItemChecked(arg2)) {
                    mChannelListView.setItemChecked(arg2, true);
                    mChannelStore.getChannel(arg2).mIsChecked = true;
                }
                else {
                    mChannelListView.setItemChecked(arg2, false);
                    mChannelStore.getChannel(arg2).mIsChecked = false;
                }
                checkSelection();
                }  
            }
        });

        mChannelListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                if (!mIsActionMode) {
                    mCurrentActionModeType = MENU_SELECT;
                    mActionMode = ((Activity)mContext).startActionMode(mModeCallback);
                    clearChoices();
                    mChannelListView.setItemChecked(arg2, true);
                    mChannelStore.getChannel(arg2).mIsChecked = true;
                    checkSelection();
                    return true;
                }
                return false;
            }
        });
        return mChannelListFragmentview;
    }

    public void startActionMode(){
        mCurrentActionModeType = MENU_SELECT;
        mActionMode = ((Activity)mContext).startActionMode(mModeCallback);
        checkSelection();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated() is called");
        mPiDataManager = PiDataManager.getInstance();
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart() is called");
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() is called");

        if (!FMRadioFeature.FEATURE_DISABLEDNS) {
            setDNSStackPriority(RadioDNSConnection.VIEW_HIGH);
        }

        if (mRadioArrayAdapter != null) {
            mRadioArrayAdapter.notifyDataSetChanged();
        }

        autoSmoothScrollChannelList();

        if (!mPlayer.isScanning()) {
            mChannelListView.setEnabled(true);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause() is called");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop() is called");
        if (!FMRadioFeature.FEATURE_DISABLEDNS) {
            setDNSStackPriority(RadioDNSConnection.CURRENT_HIGH);
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView() is called");
        if(mActionMode != null)
            mActionMode.finish();
        mChannelListFragmentview = null;
        mChannelListView = null;
        mRadioArrayAdapter = null;
       super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() is called");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach() is called");
        super.onDetach();
    }


    /**
     * Adapter class to hold Channel object for displaying all channel
     * information in listview.
     * 
     * @author vanrajvala
     */
    class RadioItemAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public RadioItemAdapter() {
            mInflater = LayoutInflater.from(mContext);
        }

        @Override
        public int getCount() {
            return mChannelStore.size();
        }

        @Override
        public Channel getItem(int position) {
            return mChannelStore.getChannel(position);
        }

        @Override
        public View getView(final int arg0, View view, ViewGroup viewGroup) {
            Channel channel = mChannelStore.getChannel(arg0);
            if (mChannelListView != null) {
            	mChannelListView.setItemChecked(arg0, channel.mIsChecked);
            }
            if (view == null) {
                if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                    view = mInflater.inflate(R.layout.channelallscreen_item_with_dns, null);
                } else {
                    view = mInflater.inflate(R.layout.channelallscreen_item, null);
                }
            }

            LinearLayout imgFavIcon_layout = (LinearLayout) view.findViewById(R.id.imgFavIcon_layout);
            final ImageView imgFavIcon = (ImageView) imgFavIcon_layout.findViewById(R.id.imgFavIcon);
            if (channel.mIsFavourite) {
                imgFavIcon.setImageResource(R.drawable.hybrid_radio_on_star);
                imgFavIcon.setContentDescription(getString(R.string.desc_button, getString(R.string.desc_favorite_button)) +", " + getString(R.string.desc_selected));
            } else {
                imgFavIcon.setImageResource(R.drawable.hybrid_radio_off_star);
                imgFavIcon.setContentDescription(getString(R.string.desc_button, getString(R.string.desc_favorite_button)) +", " + getString(R.string.desc_not_selected));
            }
            
            viewGroup.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View view, int keycode, KeyEvent event) {
                    View selectedView = mChannelListView.getSelectedView();
                    if (mIsActionMode || selectedView == null) {
                        return false;
                    }

                    LinearLayout imgFavIcon_layout = (LinearLayout) selectedView.findViewById(R.id.imgFavIcon_layout);
                    if (keycode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (event.getAction() == KeyEvent.ACTION_UP) {
                            imgFavIcon_layout.setPressed(true);
                            mChannelListView.setActivated(false);
                        }
                        return true;
                    } else if ((keycode == KeyEvent.KEYCODE_DPAD_CENTER) || (keycode == KeyEvent.KEYCODE_ENTER)){
                        if (imgFavIcon_layout.isPressed() && event.getAction() == KeyEvent.ACTION_UP) {
                            imgFavIcon_layout.performClick();
                        } else if (!imgFavIcon_layout.isPressed() && event.getAction() == KeyEvent.ACTION_UP) {
                            mChannelListView.performItemClick(selectedView, mChannelListView.getSelectedItemPosition(), mChannelListView.getSelectedItemId());
                        }
                        return true;
                    } else if (keycode == KeyEvent.KEYCODE_DPAD_LEFT){
                        imgFavIcon_layout.setPressed(false);
                        mChannelListView.setActivated(true);
                        return true;
                    } else {
                        imgFavIcon_layout.setPressed(false);
                        mChannelListView.setActivated(true);
                        return false;
                    }
                }
            });
            
            imgFavIcon_layout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                	Channel channel = mChannelStore.getChannel(arg0);
                    if (channel.mIsFavourite) {
                        remFav(channel);
                        imgFavIcon.setContentDescription(getString(R.string.desc_button, getString(R.string.desc_favorite_button)) +", " + getString(R.string.desc_selected));
                    } else {
                        addFav(channel);
                        imgFavIcon.setContentDescription(getString(R.string.desc_button, getString(R.string.desc_favorite_button)) +", " + getString(R.string.desc_not_selected));
                    }
                    mChannelStore.store();
                    notifyDataSetChanged();
                }
            });
            TextView txtChannelName = (TextView) view.findViewById(R.id.txtChanName);
            //ImageView imgSpkIcon = (ImageView) view.findViewById(R.id.imgSpkIcon);
            if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                TextView epgDetailIcon = (TextView) view.findViewById(R.id.epgDetailIcon);
                epgDetailIcon.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        Channel channel = mChannelStore.getChannel(arg0);
                        Intent intent = new Intent(getActivity().getApplicationContext(), EpgDetailActivity.class);
                        intent.putExtra("channel", channel);
                        startActivity(intent);

                    }
                });

                if (mPiDataManager.getPiData(channel.mFreqency) == null) {
                    epgDetailIcon.setVisibility(View.GONE);
                    new RequestPI(channel, epgDetailIcon).execute();
                } else {
                    epgDetailIcon.setVisibility(View.VISIBLE);
                }
            }
            TextView txtChannelFreq = (TextView) view.findViewById(R.id.txtChanFreq);
            String sfreq = RadioPlayer.convertToMhz(channel.mFreqency);
            txtChannelFreq.setText(sfreq + " MHz");
            txtChannelName.setText(channel.mFreqName);
            if (!(channel.mFreqName == null || channel.mFreqName.equals(""))) {
                txtChannelName.setVisibility(View.VISIBLE);
            } else {
                txtChannelName.setVisibility(View.GONE);
            }

            if (!FMRadioFeature.FEATURE_DISABLEDNS) {
                ImageView visIcon = (ImageView) view.findViewById(R.id.vis_icon);
                if (channel.mIsVisAvailable) {
                    visIcon.setVisibility(View.VISIBLE);
                    if(mPlayer.getFrequency() == channel.mFreqency) {
                        visIcon.setImageResource(R.drawable.hybrid_radio_info_icon_playing);
                    } else {
                        visIcon.setImageResource(R.drawable.hybrid_radio_info_icon);
                    }
                } else {
                    visIcon.setVisibility(View.GONE);
                }
            }

            try {
                if (mPlayer.getFrequency() == channel.mFreqency) {
                    txtChannelFreq.setPadding(0, 0, 0, 0);
                    txtChannelFreq.setTextColor(getResources().getColor(R.color.channel_frequency_text_playing,null));
                    txtChannelName.setTextColor(getResources().getColor(R.color.channel_frequency_text_playing,null));
                } else {
                    txtChannelFreq.setPadding(0, 0, 0, 0);
                    txtChannelFreq.setTextColor(getResources().getColor(R.color.channel_frequency_text,null));
                    txtChannelName.setTextColor(getResources().getColor(R.color.channel_name_text,null));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            CheckBox cbSelectChannel = (CheckBox) view.findViewById(R.id.channel_select_item_checkbox);
            cbSelectChannel.setChecked(channel.mIsChecked);
            if (mIsActionMode) {
                cbSelectChannel.setVisibility(View.VISIBLE);
                imgFavIcon.setVisibility(View.GONE);
                imgFavIcon_layout.setVisibility(View.GONE);
            } else {
                cbSelectChannel.setVisibility(View.GONE);
                imgFavIcon.setVisibility(View.VISIBLE);
                imgFavIcon_layout.setVisibility(View.VISIBLE);
                imgFavIcon_layout.setEnabled(true);
                imgFavIcon_layout.setClickable(true);
            }
            checkSelection();
            return view;
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }
    }

    private void setEmptView() {
        FrameLayout layout = (FrameLayout) mChannelListFragmentview.findViewById(android.R.id.empty);
        if(layout != null){
            layout.removeAllViews();
            View emptyView = LayoutInflater.from(mContext).inflate(R.layout.no_channels, null);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            layout.addView(emptyView, params);
            mChannelListView.setEmptyView(layout);
        }
    }

    private void setDNSStackPriority(int priority) {
        DNSService dnsService = ((MainActivity) mContext).getDNSService();
        if (dnsService != null) {
            dnsService.setStackPriority(priority);
        }
    }

    class RequestPI extends AsyncTask<Void, Void, Void> {

        private TextView mIcon = null;
        private Channel mChannel = null;

        public RequestPI(Channel channel, TextView icon) {
            mChannel = channel;
            mIcon = icon;
        }

        @Override
        protected Void doInBackground(Void... arg0) {

            DNSService dnsService = ((MainActivity) mContext).getDNSService();
            Date date = Calendar.getInstance().getTime();

            if (dnsService != null) {
                String freq = String.format("%05d", mChannel.mFreqency);
                dnsService.setStackPriority(RadioDNSConnection.VIEW_HIGH);
                LogDns.i("RequestPI", " request Program Info - start(" + LogDns.filter(freq) + ")");
                final PiData currentPiXml = dnsService.requestProgramInfo(freq,
                        Integer.toHexString(mChannel.mPi), date);
                Log.v("RequestPI",
                        "setCurrentTime - freq:" + Log.filter(freq) + " pi:"
                                + Integer.toHexString(mChannel.mPi) + " date:" + date);
                if (currentPiXml != null) {
                    if (mIcon != null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mIcon.setVisibility(View.VISIBLE);
                                mPiDataManager.putPiDataWithFreq(mChannel, currentPiXml);
                            }
                        });
                    }
                } else {
                    LogDns.e("RequestPI", "currentPiXml is Null!!!");
                }
            }
            return null;
        }

    }

    public void delete() {
        Log.d(TAG, "delete()");
        for (int i = mChannelListView.getCount() - 1; i >= 0; i--) {
            // get the last one and see it is deleted or not
            if (!mChannelListView.isItemChecked(i))
                continue;
            Channel channel = mChannelStore.getChannel(i);
            if (channel.mIsFavourite) {
                remFav(channel);
            }
            mChannelStore.removeChannel(channel);
        }
        mChannelStore.store();
        mRadioArrayAdapter.notifyDataSetChanged();
        if (mActionMode != null && mIsActionMode) {
            mActionMode.finish();
        }
     ((MainActivity)mContext).resetRDS(mPlayer.getFrequency());    
    }

    public void notifyDataSetChanged(){
        mRadioArrayAdapter.notifyDataSetChanged();
    }

    public void autoSmoothScrollChannelList() {
        if (mChannelListView == null || (mPlayer != null && !mPlayer.isOn()))
            return;

        mChannelListView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isVisible())
                    return;
                int mCurrentFreq = ((MainActivity) mContext).getCurrrentFrequency();
                Log.d(TAG, "mCurrentFreq : "+mCurrentFreq);
                int curPos = mChannelStore.getChannelIndexByFrequency(mCurrentFreq);
                Log.d(TAG, "curPos : "+curPos);
                if(curPos >= 0)
                    mChannelListView.smoothScrollToPosition(curPos);
            }
        }, 100);
    }

    public void addFavorite(int pos, int new_freq) {
        Channel favChannel = mChannelStore.getFavoriteChannel(pos);
        if (favChannel == null) {
            int formattedCurrentFreq = (int) (Float.parseFloat(RadioPlayer.convertToMhz(new_freq)) * 100);
            Log.d(TAG,"formattedCurrentFreq: "+formattedCurrentFreq);
            if (!mChannelStore.addFavoriteChannel(formattedCurrentFreq,"", pos)) {
                RadioToast.showToast(getActivity().getApplicationContext(), R.string.toast_already_added,Toast.LENGTH_SHORT);
                return;
            }
            ((MainActivity) mContext).refreshAddFavBtn(new_freq);
        }
    }

    public void addFav(Channel channel) {
        int newPosition = mChannelStore.getEmptyPositionOfFavorite();
        int new_freq=(int)(channel.getFrequency()*100);
        Log.d(TAG,"addFav  newPosition: "+newPosition +" new_freq: "+new_freq);
        if (newPosition == -1 || newPosition >= 12) {
            RadioToast.showToast(getActivity().getApplicationContext(),
                    getString(R.string.toast_max_favorite, 12), Toast.LENGTH_SHORT);
            return;
        }
        if (newPosition >= 0 && newPosition+1 <= MAX_FAVORITES_COUNT) {
            addFavorite(newPosition,new_freq);
        }
        channel.mPosition = newPosition;
        channel.mIsFavourite = true;
    }

    public void remFav(Channel channel) {
        mChannelStore.arrangFavChannelAfterRemove(channel);
        channel.mPosition = -1;
        channel.mIsFavourite = false;
        ((MainActivity) mContext).refreshAddFavBtn(channel.mFreqency);
    }

    public void setChannelListEnable(boolean value) {
        if (mChannelListView != null) {
            mChannelListView.setEnabled(value);
        }
    }

    private ActionMode mActionMode;
    private int mCheckedcount = 0;
    private boolean mSelectAll;
    private boolean mSelected;
    private Menu mActionModeMenu;
    private int mCurrentActionModeType = MENU_NONE;
    private TextView mTxtSelectedCount;
    private CheckBox mCheckboxSelectAll;
    private LinearLayout mCheckboxSelectAllLayout;
    private Channel mSelectedChannel = null;
    private ModeCallback mModeCallback;
    private boolean mIsActionMode = false;
    private static final int MENU_SCAN = Menu.FIRST;
    private static final int MENU_NONE = Menu.FIRST + 1;
    //private static final int MENU_REMOVE = Menu.FIRST + 1;
    private static final int MENU_SELECT = Menu.FIRST + 2;
    private static final int ACTIONMODE_MENU_SELECT_DELETE = Menu.FIRST + 5;
    private static final int ACTIONMODE_MENU_SELECT_RENAME = Menu.FIRST + 4;

    private class ModeCallback implements ActionMode.Callback {
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem menu) {
            switch (menu.getItemId()) {
            case ACTIONMODE_MENU_SELECT_RENAME:
                openDialog(RadioDialogFragment.ITEM_RENAME_DIALOG,
                        mRenameListener, mSelectedChannel);
                break;
            case ACTIONMODE_MENU_SELECT_DELETE:
                openDialog(RadioDialogFragment.ITEM_DELETE_DIALOG);
                break;
            default:
                break;
            }
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.v(TAG, "onCreateActionMode");
            mChannelListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            RelativeLayout.LayoutParams plControl = (RelativeLayout.LayoutParams) mChannelListView.getLayoutParams();
            plControl.bottomMargin = 0;
            mChannelListView.setLayoutParams(plControl);
            mActionMode = mode;
            ((MainActivity)mContext).setActionMode(mActionMode);
            mActionModeMenu = menu;
            mIsActionMode = true;
            mChannelListView.setDividerHeight((int)getResources().getDimension(R.dimen.station_favorite_divider_height));
            if(FMUtil.isRTL(getActivity())) {
                mChannelListView.setDivider(getActivity().getDrawable(R.drawable.list_divider_inset_rlt));
            } else
                mChannelListView.setDivider(getActivity().getDrawable(R.drawable.list_divider_inset));
            mChannelListView.setDividerHeight((int)getResources().getDimension(R.dimen.station_favorite_divider_height));

            MenuItem menuItem = menu.add(0, ACTIONMODE_MENU_SELECT_RENAME, ACTIONMODE_MENU_SELECT_RENAME,
                    R.string.rename);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menuItem = menu.add(0, ACTIONMODE_MENU_SELECT_DELETE, ACTIONMODE_MENU_SELECT_DELETE, R.string.delete);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            mRadioArrayAdapter.notifyDataSetChanged();

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.v(TAG, "onDestroyActionMode");
            mActionMode = null;
            mActionModeMenu = null;
            mIsActionMode = false;
            ((MainActivity)mContext).setActionMode(mActionMode);
            mCurrentActionModeType = MENU_NONE;
            mCheckboxSelectAll.setChecked(false);

            mChannelListView.setDivider(getActivity().getDrawable(R.drawable.list_divider_default));
            mChannelListView.setDividerHeight((int)getResources().getDimension(R.dimen.station_favorite_divider_height));
            mChannelListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
            for (int i = 0; i < mChannelListView.getCount(); i++) {
            	mChannelListView.setItemChecked(i, false);
            	mChannelStore.getChannel(i).mIsChecked = false;
            }
            notifyDataSetChanged();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Log.v(TAG, "onPrepareActionMode");
            View mMultiSelectActionBarView = ((Activity) mContext).getLayoutInflater().inflate(
                    R.layout.action_select_mode, null);
            mode.setCustomView(mMultiSelectActionBarView);

            mCheckboxSelectAll = (CheckBox)mMultiSelectActionBarView.findViewById(R.id.selectall_checkbox);
            mCheckboxSelectAllLayout = (LinearLayout)mMultiSelectActionBarView.findViewById(R.id.selectall_checkbox_layout);
            mCheckboxSelectAll.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            mCheckboxSelectAll.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View arg0) {
                	AccessibilityManager am = (AccessibilityManager) MainActivity._instance.getSystemService(MainActivity._instance.ACCESSIBILITY_SERVICE);
                    if(mSelectAll){
                    	makeAllSelection(false);
                    	if(am.isEnabled())
                    		MainActivity._instance.getTts().speak(getString(R.string.desc_not_selected), TextToSpeech.QUEUE_FLUSH, null, null);
                    } else {
                        makeAllSelection(true);
                        if(am.isEnabled())
                        	MainActivity._instance.getTts().speak(getString(R.string.desc_selected), TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                }
            });

            mTxtSelectedCount = (TextView)mMultiSelectActionBarView.findViewById(R.id.actionbar_title_dropdown);
            checkSelection();
            return true;
        }

        private void makeAllSelection(boolean b) {
        	if ( mChannelListView == null)
        		return ;
            if (b) {
                for (int i = 0; i < mChannelListView.getCount(); i++) {
                    if (mChannelListView.isItemChecked(i)) {
                        continue;
                    } else {
                        mChannelListView.setItemChecked(i, true);
                        mChannelStore.getChannel(i).mIsChecked = true;
                    }
                }
                if(mCheckboxSelectAll != null)
                    mCheckboxSelectAll.setChecked(true);
            } else {
                mChannelListView.clearChoices();
                for (int i = 0; i < mChannelListView.getCount(); i++) {
                    if (mChannelListView.isItemChecked(i)) {
                        continue;
                    } else {
                        mChannelListView.setItemChecked(i, false);
                        mChannelStore.getChannel(i).mIsChecked = false;
                    }
                }
                if(mCheckboxSelectAll != null)
                    mCheckboxSelectAll.setChecked(false);
            }
            mRadioArrayAdapter.notifyDataSetChanged();
            checkSelection();
            mSelectAll = b;
        }
    }

    private void checkSelection() {
        mCheckedcount = mChannelListView.getCheckedItemCount();
        if (mTxtSelectedCount != null) {
            if (mCheckedcount == 0){
                if (mIsActionMode)
                    mTxtSelectedCount.setText(getResources().getString(R.string.select_channels));
            }
            else
                mTxtSelectedCount.setText(getResources().getString(R.string.n_items, mCheckedcount));
        }

        if (mCheckedcount != 0)
            mSelected = true;
        else
            mSelected = false;

        if(mCurrentActionModeType == MENU_SELECT){
            if (mSelected) {
                if (mCheckedcount >= 2) {
                    if (mActionModeMenu != null) {
                        MenuItem menuItem = mActionModeMenu.findItem(ACTIONMODE_MENU_SELECT_RENAME);
                        menuItem.setVisible(false);
                    }
                } else {
                    mSelectedChannel = getSingleSelectedChannel();
                    if (mActionModeMenu != null) {
                        MenuItem menuItem = mActionModeMenu.findItem(ACTIONMODE_MENU_SELECT_RENAME);
                        menuItem.setVisible(true);
                    }
                }
                if (mActionModeMenu != null) {
                    MenuItem menuItem = mActionModeMenu.findItem(ACTIONMODE_MENU_SELECT_DELETE);
                    menuItem.setVisible(true);
                }
            } else {
                if (mActionModeMenu != null) {
                    MenuItem menuItem = mActionModeMenu.findItem(ACTIONMODE_MENU_SELECT_RENAME);
                    menuItem.setVisible(false);

                    menuItem = mActionModeMenu.findItem(ACTIONMODE_MENU_SELECT_DELETE);
                    menuItem.setVisible(false);
                }
            }
        } else {
            if (mActionModeMenu != null) {
                MenuItem menuItem = mActionModeMenu.findItem(ACTIONMODE_MENU_SELECT_RENAME);
                menuItem.setVisible(false);
                menuItem = mActionModeMenu.findItem(ACTIONMODE_MENU_SELECT_DELETE);
                menuItem.setVisible(false);
            }
        }

        // if all are selected make all selection true
        if(mCheckboxSelectAll != null) {
        	String content = "";
            if (mCheckedcount == mChannelListView.getCount()) {
                mSelectAll = true;
                mCheckboxSelectAll.setChecked(true);
                content = String.format(getString(R.string.desc_n_selected), mCheckedcount) + ", " + getString(R.string.desc_tick_box_t_tts) + ", " + getString(R.string.desc_ticked_t_tts) + ", " + getString(R.string.desc_double_tap_to_deselect_all);

            } else{
                mSelectAll = false;
                mCheckboxSelectAll.setChecked(false);
                if(mCheckedcount == 0)
                	content = getString(R.string.desc_nothing_selected) + ", " + getString(R.string.desc_tick_box_t_tts) + ", " + getString(R.string.desc_not_ticked_t_tts) + ", " + getString(R.string.desc_double_tap_to_select_all);
                else
                	content = String.format(getString(R.string.desc_n_selected), mCheckedcount) + ", " + getString(R.string.desc_tick_box_t_tts) + ", " + getString(R.string.desc_not_ticked_t_tts) + ", " + getString(R.string.desc_double_tap_to_select_all);

            }
            mCheckboxSelectAllLayout.setContentDescription(content);
        }
    }

    public int getCheckedcount() {
        return mCheckedcount;
    }

    public boolean isSelectAll() {
        return mSelectAll;
    }

    private Channel getSingleSelectedChannel() {
        Channel channel = null;
        for (int i = mChannelListView.getCount() - 1; i >= 0; i--) {
            // get the last one and see it is deleted or not
            if (mChannelListView.isItemChecked(i)) {
                channel = mChannelStore.getChannel(i);
                break;
            }
        }
        return channel;
    }

    private int mSavedSelectedFreq = -1;
    private DialogInterface.OnClickListener mRenameListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int buttonType) {

            String freqRename = ((RenameDialog) dialog).getText();
            if (buttonType == DialogInterface.BUTTON_POSITIVE) {
                int freq;
                if (mSelectedChannel != null)
                    freq = mSelectedChannel.mFreqency;
                else if (mSavedSelectedFreq != -1)
                    freq = mSavedSelectedFreq;
                else
                    freq = -1;

                if (freq != -1) {
                    Channel channel = mChannelStore.getChannelByFrequency(freq);
                    if (channel != null && freqRename != null) {
                        channel.mFreqName = freqRename.trim();
                        mChannelStore.store();
                        mRadioArrayAdapter.notifyDataSetChanged();
                    } else {
                        RadioToast.showToast(mContext, R.string.toast_rename_error,
                                Toast.LENGTH_SHORT);
                    }
                } else {
                    RadioToast.showToast(mContext, R.string.toast_rename_error,
                            Toast.LENGTH_SHORT);
                }
                if(mActionMode != null && mIsActionMode) {
                    mActionMode.finish();
                }
                mRadioArrayAdapter.notifyDataSetChanged();
                ((MainActivity)mContext).resetRDS(mPlayer.getFrequency());
            }
        }
    };

    public void openDialog(int type) {
        openDialog(type, null, null);
    }

    public void openDialog(int type, DialogInterface.OnClickListener clickListener) {
        openDialog(type, clickListener, null);
    }

    public void openDialog(int type, DialogInterface.OnClickListener clickListener,
            Channel selectedChannel) {
        RadioDialogFragment dialog = (RadioDialogFragment) getFragmentManager().findFragmentByTag(
                String.valueOf(type));

        if (dialog != null) {
            LogDns.e(TAG, "Dialog is already exist???");
            return;
        }

        if (type == RadioDialogFragment.SCAN_FINISH_DIALOG) {
            closeDialog(RadioDialogFragment.SCAN_PROGRESS_DIALOG);
        } else if (type == RadioDialogFragment.SCAN_PROGRESS_DIALOG) {
            closeDialog(RadioDialogFragment.SCAN_OPTION_DIALOG);
        }

        if (isResumed()) {
            if (type == RadioDialogFragment.ITEM_RENAME_DIALOG) {
                String name = null;
                if (selectedChannel != null) {
                    name = selectedChannel.mFreqName;
                }
                dialog = RadioDialogFragment.newInstance(type,
                    RenameDialog.RENAME_DIALOG_TYPE_STATION, name);
            }
            else {
                dialog = RadioDialogFragment.newInstance(type);
            }
            dialog.show(getFragmentManager(), String.valueOf(type));
        }
        if (dialog != null) {
            if (clickListener != null) {
                dialog.setOnClickListener(clickListener);
            }
        }
    }

    private void closeDialog(int type) {
        LogDns.v(TAG, "closeDialog() - start " + type);
        RadioDialogFragment dialog = (RadioDialogFragment) getFragmentManager().findFragmentByTag(
                String.valueOf(type));

        if (dialog != null) {
            try {
                LogDns.v(TAG, "removeDialog() - " + type);
                dialog.dismiss();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.remove(dialog);
                ft.commit();
            } catch (IllegalStateException e) {
                Log.d(TAG, "IllegalStateException in closeDialog");
            }
        }
    }

    public void invalidateActionMode() {
        if (mActionMode != null) {
           mActionMode.invalidate();
        }
    }

    public boolean isActionMode() {
        if (mActionMode != null) {
           return true;
        }
        return false;
    }

    public void clearChoices() {
        if (mModeCallback != null )
            mModeCallback.makeAllSelection(false);
    }
}
