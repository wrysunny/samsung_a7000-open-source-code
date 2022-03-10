package com.sec.android.app.fm.ui;

import java.util.ArrayList;

import com.samsung.media.fmradio.FMPlayerException;
import com.sec.android.app.fm.Log;
import com.sec.android.app.fm.MainActivity;
import com.sec.android.app.fm.R;
import com.sec.android.app.fm.RadioPlayer;
import com.sec.android.app.fm.RadioToast;
import com.sec.android.app.fm.SettingsActivity;
import com.sec.android.app.fm.data.Channel;
import com.sec.android.app.fm.data.ChannelStore;
import com.sec.android.app.fm.ui.FavoriteButton;
import com.sec.android.app.fm.util.FMUtil;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FavouriteListFragment extends Fragment {

    public static final String TAG = "FavouriteListFragment";

    private View mFavouriteFragmentview = null;
    private GridView mFavouriteChannelGridView = null;
    private AudioManager mAudioManager = null;
    private ChannelStore mChannelStore = null;
    private Context mContext = null;
    protected Handler mHandler = null;
    private RadioPlayer mPlayer = null;
    private boolean mIsFavPressed = false;
    public static ArrayList<Channel> mBtnFavoritesArray = new ArrayList<Channel>();

    private int mCurrentFreq = RadioPlayer.FREQ_DEFAULT;
    private int mFavFreq[] = new int[MAX_FAVORITES_COUNT];
    public static int MAX_FAVORITES_COUNT = 12;

    public static FavouriteListFragment newInstance() {
        FavouriteListFragment f = new FavouriteListFragment();
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
        Log.d(TAG, "onCreate() is called");
        mAudioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        mChannelStore = ChannelStore.getInstance();
        mPlayer = RadioPlayer.getInstance();
        mModeCallback = new ModeCallback();
        mHandler = new Handler();

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() is called");
        mFavouriteFragmentview = inflater.inflate(R.layout.favorite_fragment,container, false);
        mFavouriteChannelGridView = (GridView) mFavouriteFragmentview.findViewById(R.id.fav_list_gridview);
        mFavouriteChannelGridView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mFavoriteListAdapter = new FavoriteListAdapter();
        mFavouriteChannelGridView.setAdapter(mFavoriteListAdapter);
        setEmptView();
        return mFavouriteFragmentview;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated() is called");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart() is called");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume() is called");
        mCurrentFreq = ((MainActivity) mContext).getCurrrentFrequency();
        refreshFavList();
        mIsFavPressed = false;
        mFavoriteListAdapter.notifyDataSetChanged();
        if (mActionMode != null)
            checkSelection();
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause() is called");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop() is called");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView() is called");
        if(mActionMode != null)
            mActionMode.finish();
        mFavouriteFragmentview = null;
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

    private void setEmptView() {
        FrameLayout layout = (FrameLayout) mFavouriteFragmentview.findViewById(android.R.id.empty);
        if(layout != null){
            layout.removeAllViews();
            View emptyView = LayoutInflater.from(mContext).inflate(R.layout.no_favourites, null);
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            layout.addView(emptyView, params);
            mFavouriteChannelGridView.setEmptyView(layout);
        }
    }

    public FavoriteListAdapter mFavoriteListAdapter;

    public class FavoriteListAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        public FavoriteListAdapter() {
            mInflater = LayoutInflater.from(mContext);
        }

        @Override
        public int getCount() {
            return mBtnFavoritesArray.size();
        }

        @Override
        public Object getItem(int arg0) {
            return mBtnFavoritesArray.get(arg0);
        }

        @Override
        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int arg0, View view, ViewGroup viewGroup) {
            Channel channel = (Channel) getItem(arg0);
            Log.d(TAG, "adapter  mPosition " + channel.mPosition + " mFreqency " + channel.mFreqency);
            final RadioPlayer mPlayer = RadioPlayer.getInstance();
            int freq = mPlayer.getFrequency();
            final int index = arg0;
            if (view == null) {
                view = mInflater.inflate(R.layout.channelfavscreen_item,null);
            }

            FavoriteButton favChannel = (FavoriteButton) view.findViewById(R.id.favchannel);
            favChannel.setState(FavoriteButton.STATE_CHANNEL);
            favChannel.setFrequencyText(RadioPlayer.convertToMhz(mBtnFavoritesArray.get(arg0).mFreqency),mBtnFavoritesArray.get(arg0).mFreqName);
            if (channel.mFreqency == freq && mPlayer.isOn()) {
                favChannel.setFreqTextColorAsPlay(getFavouritesCount());
                favChannel.setPressed(mIsFavPressed);
            } else {
                favChannel.setFreqTextColorAsNormal(getFavouritesCount());
            }
            favChannel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View paramView) {
                    if (mIsActionMode) {
                        if (!mBtnFavoritesArray.get(index).mIsChecked)
                            mBtnFavoritesArray.get(index).mIsChecked = true;
                        else
                            mBtnFavoritesArray.get(index).mIsChecked = false;
                        checkSelection();
                        return;
                    }
                    if (mPlayer.isBusy()) {
                        Log.d(TAG, "RadioPlayer is busy. ignore it");
                        return;
                    }
                    if (FMUtil.isVoiceActive(mContext,FMUtil.NEED_TO_PLAY_FM))
                        return;
                    if (mPlayer.isOn()
                            && mBtnFavoritesArray.get(index).mFreqency == ((MainActivity) mContext)
                                    .getCurrrentFrequency())
                        return;

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            String mute = "fm_radio_mute=1";
                            mAudioManager.setParameters(mute);
                            try {
                                boolean isOn = mPlayer.isOn();
                                mPlayer.tuneAsyncEx(mBtnFavoritesArray.get(index).mFreqency);
                                if (!isOn)
                                    SettingsActivity.activateTurnOffAlarm();
                            } catch (FMPlayerException e) {
                                RadioToast.showToast(getActivity().getApplicationContext(), e);
                            }
                        }
                    });
                }
            });

            favChannel.setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View arg0) {
                    if (!mIsActionMode) {
                        mCurrentActionModeType = MENU_SELECT;
                        clearChoices();
                        mActionMode = ((Activity) mContext).startActionMode(mModeCallback);
                        mBtnFavoritesArray.get(index).mIsChecked = true;
                        checkSelection();
                        return true;
                    }
                    return false;
                }
            });

            CheckBox cbSelectChannel = (CheckBox) view.findViewById(R.id.favchannel_select_item_checkbox);
            if (mIsActionMode) {
            	cbSelectChannel.bringToFront();
                cbSelectChannel.setVisibility(View.VISIBLE);
                cbSelectChannel.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View ar0) {
                        if (!mBtnFavoritesArray.get(index).mIsChecked)
                            mBtnFavoritesArray.get(index).mIsChecked = true;
                        else
                            mBtnFavoritesArray.get(index).mIsChecked = false;
                        checkSelection();
                    }
                });
                if (mBtnFavoritesArray.get(index).mIsChecked)
                    cbSelectChannel.setChecked(true);
                else
                    cbSelectChannel.setChecked(false);
            } else {
                cbSelectChannel.setVisibility(View.INVISIBLE);
            }

            viewGroup.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keycode, KeyEvent event) {
                    View selectedView = mFavouriteChannelGridView.getSelectedView();
                    if (selectedView == null)
                        return false;
                    FavoriteButton selectedFavChannel = (FavoriteButton) selectedView.findViewById(R.id.favchannel);
                    CheckBox selectedCheckBox = (CheckBox) selectedView.findViewById(R.id.favchannel_select_item_checkbox);
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        if (mIsActionMode) {
                            selectedCheckBox.setPressed(true);
                        } else {
                            selectedFavChannel.setPressed(true);
                        }
                        return false;
                    }
                    else if (keycode == KeyEvent.KEYCODE_ENTER) {
                        if (mIsActionMode) {
                            selectedCheckBox.performClick();
                        } else {
                            selectedFavChannel.performClick();
                        }
                        return true;
                    }
                    return false;
                }
            });
            return view;
        }
    }

    public void notifyDataSetChanged(){
        refreshFavList();
        mFavoriteListAdapter.notifyDataSetChanged();
        if (mActionMode != null)
            checkSelection();
    }

    private ActionMode mActionMode;
    private boolean mIsActionMode;
    private int mCheckedcount = 0;
    private boolean mSelectAll;
    private boolean mSelected;
    private Menu mActionModeMenu;
    private int mCurrentActionModeType = MENU_NONE;
    private TextView mTxtSelectedCount;
    private CheckBox mCheckboxSelectAll;
    private LinearLayout mCheckboxSelectAllLayout;
    private ModeCallback mModeCallback;
    private static final int MENU_NONE = Menu.FIRST + 1;
    private static final int MENU_REMOVE = Menu.FIRST + 2;
    private static final int MENU_SELECT = Menu.FIRST + 2;
    private static final int ACTIONMODE_MENU_SELECT_DELETE = Menu.FIRST + 3;

    private class ModeCallback implements ActionMode.Callback {
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem menu) {
            switch (menu.getItemId()) {
            case ACTIONMODE_MENU_SELECT_DELETE:
                removeFavouriteChannels();
                break;
            default:
                break;
            }
            return true;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Log.v(TAG, "onCreateActionMode");
            mFavouriteChannelGridView.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE);
            mActionMode = mode;
            ((MainActivity)mContext).setActionMode(mActionMode);
            mActionModeMenu = menu;
            mIsActionMode = true;
            MenuItem menuItem = menu.add(0, ACTIONMODE_MENU_SELECT_DELETE, ACTIONMODE_MENU_SELECT_DELETE, R.string.remove);
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            mIsFavPressed = false;
            mFavoriteListAdapter.notifyDataSetChanged();

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
            mFavouriteChannelGridView.setChoiceMode(GridView.CHOICE_MODE_NONE);
            notifyDataSetChanged();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            Log.v(TAG, "onPrepareActionMode");
            View mMultiSelectActionBarView = ((Activity) mContext).getLayoutInflater().inflate(
                    R.layout.action_select_mode, null);
            mode.setCustomView(mMultiSelectActionBarView);

            mCheckboxSelectAll = (CheckBox)mMultiSelectActionBarView.findViewById(R.id.selectall_checkbox);
            mCheckboxSelectAll.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            mCheckboxSelectAllLayout=(LinearLayout)mMultiSelectActionBarView.findViewById(R.id.selectall_checkbox_layout);
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
            if (b) {
                for (int i = 0; i < mBtnFavoritesArray.size(); i++) {
                    mBtnFavoritesArray.get(i).mIsChecked = true;
                    mFavouriteChannelGridView.setItemChecked(i, true);
                }
                if(mCheckboxSelectAll != null)
                    mCheckboxSelectAll.setChecked(true);
            } else {
                mFavouriteChannelGridView.clearChoices();
                for (int i = 0; i < mBtnFavoritesArray.size(); i++) {
                    mBtnFavoritesArray.get(i).mIsChecked = false;
                    mFavouriteChannelGridView.setItemChecked(i, false);
                }
                if(mCheckboxSelectAll != null)
                    mCheckboxSelectAll.setChecked(false);
            }
            mFavoriteListAdapter.notifyDataSetChanged();
            checkSelection();
            mSelectAll = b;
        }
    }

    private void checkSelection() {
        for (int i=0; i<mBtnFavoritesArray.size(); i++) {
            mFavouriteChannelGridView.setItemChecked(i, mBtnFavoritesArray.get(i).mIsChecked);
        }
        mCheckedcount = mFavouriteChannelGridView.getCheckedItemCount();
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
                if (mActionModeMenu != null) {
                    MenuItem menuItem = mActionModeMenu.findItem(ACTIONMODE_MENU_SELECT_DELETE);
                    menuItem.setVisible(true);
                }
            } else {
                if (mActionModeMenu != null) {
                    MenuItem menuItem = mActionModeMenu.findItem(ACTIONMODE_MENU_SELECT_DELETE);
                    menuItem.setVisible(false);
                }
            }
        } else {
            if (mActionModeMenu != null) {
               MenuItem menuItem = mActionModeMenu.findItem(ACTIONMODE_MENU_SELECT_DELETE);
                menuItem.setVisible(false);
            }
        }

        // if all are selected make all selection true
        if(mCheckboxSelectAll != null) {
        	String content = "";
            if (mCheckedcount == mFavouriteChannelGridView.getCount()){
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

    public void refreshFavList() {
        mBtnFavoritesArray.clear();
        for (int i = 0; i < MAX_FAVORITES_COUNT; i++)
            mFavFreq[i] = -1;

        if (mChannelStore != null) {
            for (int i = 0; i < mChannelStore.size(); i++) {
                Channel channel = mChannelStore.getChannel(i);
                if (channel != null && channel.mIsFavourite) {
                    mFavFreq[channel.mPosition] = channel.mFreqency;
                }
            }
            for (int i = 0; i < MAX_FAVORITES_COUNT; i++) {
                if (mFavFreq[i] != -1)
                    mBtnFavoritesArray.add(mChannelStore
                            .getChannelByFrequency(mFavFreq[i]));
            }
        }
        View selectedView = mFavouriteChannelGridView.getSelectedView();
        if(selectedView != null) {
            FavoriteButton favChannel = (FavoriteButton) selectedView.findViewById(R.id.favchannel);
            mIsFavPressed = favChannel.isPressed();
        } else {
            mIsFavPressed = false;
        }

        GridView gridView = (GridView) mFavouriteFragmentview.findViewById(R.id.fav_list_gridview);
        android.view.ViewGroup.LayoutParams gridViewLayoutParam = gridView.getLayoutParams();

        if (getFavouritesCount() > 4) {
            gridView.setPadding(0, getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_padding_top_5), 0, 0);
            gridView.setColumnWidth(getResources().getDimensionPixelSize(R.dimen.fav_channels_gridview_columnwidth_5));
            gridView.setHorizontalSpacing(getResources().getDimensionPixelSize(R.dimen.horizantal_spacing_fav_channel_5));
            gridView.setVerticalSpacing(getResources().getDimensionPixelSize(R.dimen.vertical_spacing_fav_channel_5));
            gridView.setNumColumns(getResources().getInteger(R.integer.fav_more_than_4_column_number));
            gridViewLayoutParam.width = getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_width_5);
            gridViewLayoutParam.height = getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_height_5);
            gridView.setLayoutParams(gridViewLayoutParam);
        } else if (getFavouritesCount() > 2) {
            gridView.setPadding(0, getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_padding_top_4), 0, 0);
            gridView.setColumnWidth(getResources().getDimensionPixelSize(R.dimen.fav_channels_gridview_columnwidth_4));
            gridView.setHorizontalSpacing(getResources().getDimensionPixelSize(R.dimen.horizantal_spacing_fav_channel_4));
            gridView.setVerticalSpacing(getResources().getDimensionPixelSize(R.dimen.vertical_spacing_fav_channel_4));
            gridViewLayoutParam.width = getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_width_4);
            gridViewLayoutParam.height = getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_height_4);
            gridView.setNumColumns(getResources().getInteger(R.integer.fav_more_than_2_column_number));
            gridView.setLayoutParams(gridViewLayoutParam);
        } else if(getFavouritesCount() > 1){
            gridView.setPadding(0, getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_padding_top_2), 0, 0);
            gridView.setColumnWidth(getResources().getDimensionPixelSize(R.dimen.fav_channels_gridview_columnwidth_4));
            gridView.setHorizontalSpacing(getResources().getDimensionPixelSize(R.dimen.horizantal_spacing_fav_channel_4));
            gridView.setVerticalSpacing(getResources().getDimensionPixelSize(R.dimen.vertical_spacing_fav_channel_4));
            gridView.setNumColumns(getResources().getInteger(R.integer.fav_more_than_1_column_number));
            gridViewLayoutParam.width = getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_width_2);
            gridViewLayoutParam.height = getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_height_2);
            gridView.setLayoutParams(gridViewLayoutParam);
        } else {
            gridView.setPadding(0, getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_padding_top_1), 0, 0);
            gridView.setColumnWidth(getResources().getDimensionPixelSize(R.dimen.fav_channels_gridview_columnwidth_4));
            gridView.setHorizontalSpacing(getResources().getDimensionPixelSize(R.dimen.horizantal_spacing_fav_channel_4));
            gridView.setVerticalSpacing(getResources().getDimensionPixelSize(R.dimen.vertical_spacing_fav_channel_4));
            gridView.setNumColumns(getResources().getInteger(R.integer.fav_1_column_number));
            gridViewLayoutParam.width = getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_width_1);
            gridViewLayoutParam.height = getResources().getDimensionPixelSize(R.dimen.fav_channel_gridview_height_1);
            gridView.setLayoutParams(gridViewLayoutParam);
        }
    }

    public void startActionMode(){
        mCurrentActionModeType = MENU_SELECT;
        mActionMode = ((Activity)mContext).startActionMode(mModeCallback);
        checkSelection();
    }

    public void removeFavouriteChannels() {
        for (int i = 0 ; i<mBtnFavoritesArray.size() ; i++) {
            if (!mBtnFavoritesArray.get(i).mIsChecked)
                continue;
            Channel channel = mBtnFavoritesArray.get(i);
            if (channel.mIsFavourite) {
                mChannelStore.arrangFavChannelAfterRemove(channel);
                channel.mPosition = -1;
                channel.mIsFavourite = false;
                channel.mIsChecked = false;
                ((MainActivity) mContext).refreshAddFavBtn(channel.mFreqency);
            }
        }
        refreshFavList();
        for (int i=0; i<mBtnFavoritesArray.size(); i++) {
            mBtnFavoritesArray.get(i).mIsChecked = false;
            mFavouriteChannelGridView.setItemChecked(i, false);
        }
        if (mActionMode != null && mIsActionMode) {
            mActionMode.finish();
        }
    }

    public int getFavouritesCount() {
        int count = 0;
        if(mBtnFavoritesArray != null)
            count = mBtnFavoritesArray.size();
        return count;
    }

    public boolean isActionMode() {
        if (mActionMode != null) {
           return true;
        }
        return false;
    }

    public void clearChoices(){
        if (mModeCallback != null)
            mModeCallback.makeAllSelection(false);
    }

    public void rearrangeCheckedItems(Channel channel){
        for (int i=0; i<mBtnFavoritesArray.size(); i++) {
            mFavouriteChannelGridView.setItemChecked(i, mBtnFavoritesArray.get(i).mIsChecked);
        }
        //only one favourite is to be deleted
        if(mBtnFavoritesArray.size() == 1) {
            if (mActionMode != null && mIsActionMode) {
                mActionMode.finish();
            }
        }
    }
}
