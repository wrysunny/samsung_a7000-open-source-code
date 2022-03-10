package com.sec.android.app.fm;

import java.io.File;
import java.util.Locale;

import com.sec.android.app.fm.ui.RenameDialog;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
public class RadioMediaStore {
    private static final String _TAG = "MediaStore";
    static final String RECORDING_DEFAULT_STORAGE_PATH = "/mnt/sdcard";
    static final String RECORDING_FOLDER_PATH = "/Radio";
    private static String sHiddenFilePath = null;
    static boolean deleteHiddenFile() {
        Log.v(_TAG, "deleteHiddenFile() - " + Log.filter(sHiddenFilePath));
        if (sHiddenFilePath == null || sHiddenFilePath.isEmpty())
            return false;
        File file = new File(sHiddenFilePath);
        if ((file == null) || !file.exists()) {
            Log.e(_TAG, "File doesn't exist");
            return false;
        }
        if (file.delete())
            return true;
        else {
            Log.e(_TAG, "It's failed to delete");
            return false;
        }
    }
    private static long getDuration(String fileName) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(fileName);
        String value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long duration = 0;
        if(value != null) {
            duration = Long.parseLong(value);
        }
        retriever.release();
        return duration;
    }
    static boolean isValid() {
        return getDuration(sHiddenFilePath) >= 1000;
    }
    static String makeFilePath(boolean isHidden) {
        String folderPath = makeFolderPath();
        String filePath = null;
        if (isHidden) {
            filePath = folderPath + "/.fmradio";
            sHiddenFilePath = filePath;
        } else {
            filePath = folderPath + "/" + makeFileTitle(folderPath) + ".m4a";
        }
        Log.d(_TAG, "makeFilePath() - path : " + Log.filter(filePath));
        return filePath;
    }

    public static boolean dirRecheck() {
        File dir = new File(sHiddenFilePath);
        if ((dir == null) || !dir.exists()) {
            Log.e(_TAG, "File doesn't exist");
            return false;
        }
        return true;
    }
    private static String makeFileSuffix(String path) {
        Context context = RadioApplication.getInstance();
        String appName = getAppName();
        int num = 1;
        String strIdx = "idx";
        String[] projection = { Audio.Media.DATA,
        "cast(replace(substr(_data, ?, 1000), '.m4a', '') as INTEGER) " + strIdx };
        String where = "_data like ? ";
        String[] selectionArgs = { String.format(Locale.UK,"%d", path.length() + appName.length() + 3),
        path + "/" + appName + " %" };
        String orderBy = strIdx + " DESC limit 1";
        Cursor cursor = context.getContentResolver().query(Audio.Media.EXTERNAL_CONTENT_URI,
        projection, where, selectionArgs, orderBy);
        if (cursor != null) {
            cursor.moveToFirst();
            int size = cursor.getCount();
            if (size > 0) {
                cursor.moveToFirst();
                num = cursor.getInt(cursor.getColumnIndex(strIdx)) + 1;
            }
            cursor.close();
        }
        return String.format(Locale.UK,"%03d",  num);
    }
    private static String makeFileTitle(String path) {
        Context context = RadioApplication.getInstance();
        String appName = getAppName();
        String suffix = makeFileSuffix(path);
        if (!path.contains(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            return appName + " " + context.getString(R.string.filename_sd, suffix);
        }
        return appName + " " + suffix;
    }
    public static String getRecordingFileTitle() {
        Context context = RadioApplication.getInstance();
        String appName = getAppName();
        String path = makeFolderPath();
        String suffix = makeFileSuffix(path);
        if (!path.contains(Environment.getExternalStorageDirectory().getAbsolutePath())) {
            return appName + " " + context.getString(R.string.filename_sd, suffix);
        }
        return appName + " " + suffix;
    }
    private static String makeFolderPath() {
        Context context = RadioApplication.getInstance();
        SharedPreferences prefs = context.getSharedPreferences(SettingsActivity.PREF_FILE,
        Context.MODE_PRIVATE);
        String folderPath = prefs.getString(SettingsActivity.KEY_STORAGE, Environment
        .getExternalStorageDirectory().getAbsolutePath());
        if (SettingsActivity.STORAGE_MOUNTED.equals(Environment.getExternalStorageState(new File(folderPath)))) {
            folderPath += RECORDING_FOLDER_PATH;
        } else {
            folderPath = Environment.getExternalStorageDirectory().getAbsolutePath()
            + RECORDING_FOLDER_PATH;
        }
        File dir = new File(folderPath);
        if (!dir.isDirectory())
            dir.mkdirs();
        return folderPath;
    }
    static boolean save(String perfomer, String album) {
        Log.v(_TAG, "save()");
        if (!isValid()) {
            deleteHiddenFile();
            return false;
        }
        String newFileName = makeFilePath(false);
        File saveFile = new File(newFileName);
        File hiddenFile = new File(sHiddenFilePath);
        if (!hiddenFile.renameTo(saveFile)) {
            Log.e(_TAG, "Rename failed");
            return false;
        }
        if (!setMetaData(newFileName, perfomer, album)) {
            Log.d(_TAG, "Content Resolver insert failed");
            if (newFileName != null) {
                saveFile.delete();
            }
            return false;
        }
        Log.v(_TAG, "save() - finish");
        return true;
    }
    private static boolean setMetaData(String fileName, String perfomer, String album) {
        Context context = RadioApplication.getInstance();
        File file = new File(fileName);
        String title = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf('.'));
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Audio.Media.TITLE, title);
        contentValues.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4");
        contentValues.put(MediaStore.Audio.Media.DATA, fileName);
        contentValues.put(MediaStore.Audio.Media.DURATION, getDuration(fileName));
        contentValues.put(MediaStore.Audio.Media.SIZE, file.length());
        contentValues.put(MediaStore.Audio.Media.DATE_MODIFIED, file.lastModified() / 1000l);
        contentValues.put(MediaStore.Audio.Media.IS_MUSIC, 0);
        contentValues.put("recordingtype", 2);
        if (!FMRadioFeature.FEATURE_DISABLERTPLUSINFO) {
            if (perfomer != null)
                contentValues.put(MediaStore.Audio.Media.ARTIST, perfomer);
            if (album != null)
                contentValues.put(MediaStore.Audio.Media.ALBUM, album);
        }
        
        boolean flag = false;
        try{
            flag = context.getContentResolver().insert(Audio.Media.EXTERNAL_CONTENT_URI, contentValues) != null;
        }
        catch (Exception e) {
            Log.d(_TAG, "setMetaData : recording error "+e.getMessage());
        }
        return  flag;
    }

    @SuppressLint("NewApi") // Upto JELLY_BEAN_MR1
    private static String getAppName() {
        Context context = RadioApplication.getInstance();
        String appName = context.getString(R.string.app_name);
        for(int i = 0 ; i < RenameDialog.FILE_INVALID_CHAR.length ; i++ ) {
            if(appName.contains(RenameDialog.FILE_INVALID_CHAR[i])) {
                Configuration config = new Configuration(context.getResources().getConfiguration());
                config.locale = Locale.UK;
                appName = context.createConfigurationContext(config).getString(R.string.app_name);
                break;
            }
        }
        return appName;
    }
}
