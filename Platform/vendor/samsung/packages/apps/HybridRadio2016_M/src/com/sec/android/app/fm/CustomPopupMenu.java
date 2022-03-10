package com.sec.android.app.fm;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListPopupWindow;
import android.widget.TextView;

import com.sec.android.app.fm.R;

import java.util.ArrayList;

// to make custom width pop up    a.agnihotri

public class CustomPopupMenu {

    private Context mContext;

    private ListPopupWindow mPopupWindow;

    private SparseArray<String> mMenuKeys = new SparseArray<String>();

    private SelectionMenuAdapter mAdapter = new SelectionMenuAdapter();

    private OnMenuItemClickListener mOnItemClickListener;

    private View mAnchorView = null;

    public CustomPopupMenu(Context c, View anchorView) {
        mContext = c;
        mPopupWindow = new ListPopupWindow(mContext, null);
        mAnchorView = anchorView;
        mPopupWindow.setAnchorView(mAnchorView);
        mPopupWindow.setAdapter(mAdapter);
        mPopupWindow.setModal(true);
        mPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClicked((int) id);
                }
            }
        });
        mPopupWindow.setVerticalOffset(mContext.getResources().getDimensionPixelSize(R.dimen.actionbar_title_dropdown_y_offset));
    }

    public void addMenu(int id, String menu) {
        String exist = mMenuKeys.get(id, null);
        if (exist != null) {
            mAdapter.removeMenu(exist);
        }
        mMenuKeys.append(id, menu);
        mAdapter.addMenu(menu);
    }

    public void removeMenu(int id) {
        String exist = mMenuKeys.get(id, null);
        if (exist != null) {
            mAdapter.removeMenu(exist);
        }
    }

    public void clearMenu() {
        mMenuKeys.clear();
        mAdapter.clearMenu();
    }

    public View getAnchorView() {
        return mAnchorView;
    }

    public void show() {
        mPopupWindow.show();
    }

    public boolean isShowing() {
        return mPopupWindow.isShowing();
    }

    public void setOnMenuItemClickListener(OnMenuItemClickListener l) {
        mOnItemClickListener = l;
    }

    public void dismiss() {
        mPopupWindow.dismiss();
    }

    private class SelectionMenuAdapter implements ListAdapter {

        private ArrayList<String> mMenuItems = new ArrayList<String>();

        void addMenu(String menu) {
            mMenuItems.add(menu);
        }

        void removeMenu(String menu) {
            mMenuItems.remove(menu);
        }

        void clearMenu() {
            mMenuItems.clear();
        }

        @Override
        public int getCount() {
            return mMenuItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mMenuItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mMenuKeys.keyAt(position);
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(mContext, R.layout.popup_menu_item_layout, null);
            }
            TextView tv = (TextView) convertView.findViewById(R.id.title);
            tv.setText(mMenuItems.get(position));
            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
        }
    }

    ;

    public interface OnMenuItemClickListener {
        void onItemClicked(int id);
    }
}
