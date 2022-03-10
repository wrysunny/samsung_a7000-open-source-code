package com.sec.android.app.fm.data;

import com.sec.android.app.fm.Log;
import com.sec.android.app.fm.ui.RTPTag;
import com.sec.android.app.fm.ui.RTPTagList;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class RTPTagDBAdapter {
    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DB_TABLE_CREATE);
            // FmRadioPlayer.getInstance(mContext).setConditionFlag(
            // FmRadioPlayer.CONDITION_INITIAL_START, true);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS RTPHistory");
            onCreate(db);
        }
    }

    public static final String DB_NAME = "RTPHistory.db";
    public static final String DB_TABLE = "RTPHistory";

    /*
     * private static final String DB_TABLE_CREATE =
     * "create table RTPHistory (_id integer primary key autoincrement, title text not null, album text not null, artist text not null, band text not null);"
     * ;
     */

    private static final String DB_TABLE_CREATE = "create table RTPHistory (_id integer primary key autoincrement, title text, album text, artist text, band text);";
    public static final int DB_VERSION = 2;
    public static final int INDEX_ALBUM = 2;
    public static final int INDEX_ARTIST = 3;
    public static final int INDEX_BAND = 4;
    public static final int INDEX_ROWID = 0;
    public static final int INDEX_TITLE = 1;
    public static final String KEY_ALBUM = "album";
    public static final String KEY_ARTIST = "artist";
    public static final String KEY_BAND = "band";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_TITLE = "title";
    private static RTPTagDBAdapter sInstance = null;
    private static final String TAG = "RTPTagDBAdapter";

    public synchronized static RTPTagDBAdapter getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RTPTagDBAdapter();
            sInstance.open(context);
        }
        return sInstance;
    }

    private SQLiteDatabase mDb;
    private DatabaseHelper mDbHelper;
    private ArrayList<RTPTagList> mMusicTagList;

//    public void append(ArrayList<RTPTagList> musicTagList) {
//        for (RTPTagList tag : musicTagList) {
//            append(tag);
//        }
//    }

    public void append(RTPTagList musicTags) {
        RTPTag tag;
        String title, album, artist, band;

        Log.v(TAG, "append : " + musicTags.toString());

        tag = musicTags.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_TITLE);
        if ((tag == null) || ((title = tag.getInfo()) == null) || title.isEmpty()) {
            Log.d(TAG, "title is null. so skip to append");
            return;
        }
        tag = musicTags.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_ALBUM);
        album = tag != null ? tag.getInfo() : "";
        tag = musicTags.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_ARTIST);
        artist = tag != null ? tag.getInfo() : "";
        tag = musicTags.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_BAND);
        band = tag != null ? tag.getInfo() : "";
        append(title, album, artist, band);
    }

    private void append(String title, String album, String artist, String band) {
        ContentValues values = new ContentValues();
        values.put(KEY_TITLE, title);
        values.put(KEY_ALBUM, album);
        values.put(KEY_ARTIST, artist);
        values.put(KEY_BAND, band);
        mDb.beginTransaction();
        try {
            mDb.insert(DB_TABLE, null, values);
            mDb.setTransactionSuccessful();
        } catch (SQLException anyDbError) {
            anyDbError.printStackTrace();
        } finally {
            mDb.endTransaction();
        }
    }

    public boolean clear() {
        int rows = 0;
        mDb.beginTransaction();
        try {
            rows = mDb.delete(DB_TABLE, null, null);
            mDb.setTransactionSuccessful();
        } catch (SQLException anyDbError) {
            anyDbError.printStackTrace();
        } finally {
            mDb.endTransaction();
        }
        return rows > 0;
    }

    public void close() {
        if (mDb.isOpen()) {
            mDb.close();
        }

        if (mDbHelper != null) {
            mDbHelper.close();
        }

        sInstance = null;
    }

    public int count() {
        Cursor cursor = mDb.query(DB_TABLE, new String[] { KEY_ROWID, KEY_TITLE, KEY_ALBUM,
                KEY_ARTIST, KEY_BAND }, null, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        cursor = null;
        return count;
    }

    public int delete(RTPTagList tagList) {
        String whereClause = KEY_TITLE + "=?";
        // and " + KEY_ALBUM + "=? and " + KEY_ARTIST + "=? and "
        // + KEY_BAND + "=?";
        String[] whereArgs = { "" };// , "", "", "" };
        RTPTag tag;
        int ret = 0;
        tag = tagList.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_TITLE);
        whereArgs[0] = tag != null ? tag.getInfo() : "";
        // tag = tagList.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_ALBUM);
        // whereArgs[1] = tag != null ? tag.getInfo() : "";
        // tag = tagList.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_ARTIST);
        // whereArgs[2] = tag != null ? tag.getInfo() : "";
        // tag = tagList.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_BAND);
        // whereArgs[3] = tag != null ? tag.getInfo() : "";
        mDb.beginTransaction();
        try {
            ret = mDb.delete(DB_TABLE, whereClause, whereArgs);
            mDb.setTransactionSuccessful();
        } catch (SQLException anyDbError) {
            anyDbError.printStackTrace();
        } finally {
            mDb.endTransaction();
        }
        return ret;
    }

    public void deleteLast() {
        String whereClause = KEY_ROWID + "=?";
        String[] whereArgs = { Integer.toString(getDBLastIndex()) };
        mDb.beginTransaction();
        try {
            mDb.delete(DB_TABLE, whereClause, whereArgs);
            mDb.setTransactionSuccessful();
        } catch (SQLException anyDbError) {
            anyDbError.printStackTrace();
        } finally {
            mDb.endTransaction();
        }
    }

    private int getDBFirstIndex() {
        int index = 0;
        Cursor cursor = mDb.query(DB_TABLE, new String[] { KEY_ROWID, KEY_TITLE, KEY_ALBUM,
                KEY_ARTIST, KEY_BAND }, null, null, null, null, KEY_ROWID + " DESC");
        if (cursor == null)
            return -1;
        else if (cursor.getCount() == 0) {
            cursor.close();
            return -1;
        }
        cursor.moveToFirst();
        index = cursor.getInt(INDEX_ROWID);
        cursor.close();
        return index;
    }

    private int getDBLastIndex() {
        int index = 0;
        Cursor cursor = mDb.query(DB_TABLE, new String[] { KEY_ROWID, KEY_TITLE, KEY_ALBUM,
                KEY_ARTIST, KEY_BAND }, null, null, null, null, KEY_ROWID + " DESC");
        if (cursor == null)
            return -1;
        else if (cursor.getCount() == 0) {
            cursor.close();
            return -1;
        }
        cursor.moveToLast();
        index = cursor.getInt(INDEX_ROWID);
        cursor.close();
        return index;
    }

    public ArrayList<RTPTagList> getMusicTags() {
        mMusicTagList = new ArrayList<RTPTagList>();

        Cursor cursor = mDb.query(DB_TABLE, new String[] { KEY_ROWID, KEY_TITLE, KEY_ALBUM,
                KEY_ARTIST, KEY_BAND }, null, null, null, null, KEY_ROWID + " DESC");
        if (cursor == null)
            return mMusicTagList;

        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToNext();
            RTPTagList musicTags = new RTPTagList();
            musicTags.addTag(new RTPTag(RTPTagList.RTP_TAG_MUSIC_ITEM_TITLE, cursor
                    .getString(INDEX_TITLE)));
            musicTags.addTag(new RTPTag(RTPTagList.RTP_TAG_MUSIC_ITEM_ALBUM, cursor
                    .getString(INDEX_ALBUM)));
            musicTags.addTag(new RTPTag(RTPTagList.RTP_TAG_MUSIC_ITEM_ARTIST, cursor
                    .getString(INDEX_ARTIST)));
            musicTags.addTag(new RTPTag(RTPTagList.RTP_TAG_MUSIC_ITEM_BAND, cursor
                    .getString(INDEX_BAND)));
            mMusicTagList.add(musicTags);
        }
        cursor.close();
        cursor = null;
        return mMusicTagList;
    }

    public boolean isEmpty() {
        return count() == 0;
    }

    private void open(Context context) throws SQLException {
        mDbHelper = new DatabaseHelper(context);
        try {
            mDb = mDbHelper.getWritableDatabase();
        } catch (SQLiteException exception) {
            exception.printStackTrace();
        }
    }

    public void updateFirst(RTPTagList tagList) {
        ContentValues values = new ContentValues();
        String whereClause = KEY_ROWID + "=?";
        String[] whereArgs = { "" };
        int firstIndex = getDBFirstIndex();
        String title, album, artist, band;
        RTPTag tag;
        if (firstIndex < 0)
            return;
        whereArgs[0] = Integer.toString(firstIndex);
        tag = tagList.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_TITLE);
        if ((tag == null) || ((title = tag.getInfo()) == null) || title.isEmpty()) {
            Log.d(TAG, "title is null. so skip to update");
            return;
        }

        tag = tagList.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_ALBUM);
        album = tag != null ? tag.getInfo() : "";
        tag = tagList.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_ARTIST);
        artist = tag != null ? tag.getInfo() : "";
        tag = tagList.getTagWithCode(RTPTagList.RTP_TAG_MUSIC_ITEM_BAND);
        band = tag != null ? tag.getInfo() : "";
        values.put(KEY_TITLE, title);
        values.put(KEY_ALBUM, album);
        values.put(KEY_ARTIST, artist);
        values.put(KEY_BAND, band);
        mDb.beginTransaction();
        try {
            mDb.update(DB_TABLE, values, whereClause, whereArgs);
            mDb.setTransactionSuccessful();
        } catch (SQLException anyDbError) {
            anyDbError.printStackTrace();
        } finally {
            mDb.endTransaction();
        }
    }
}
