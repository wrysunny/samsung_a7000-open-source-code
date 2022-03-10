package com.sec.android.app.fm;

import com.sec.android.app.fm.data.Channel;
import com.sec.android.app.fm.data.ChannelStore;
import com.sec.android.app.fm.util.FMPermissionUtil;
import com.sec.android.app.fm.Log;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class BackupAndRestoreService extends BroadcastReceiver {
    public static final String TAG = "BackupAndRestoreService";

    public static final String BACKUP_AND_RESTORE_PERMISSION = "com.wssnps.permission.COM_WSSNPS";
    private static final String BACKUP_CHANNEL_FILE_NAME = ChannelStore.FILE_NAME;
    private static final String CHNNEL_STORE_FILE_PATH = "./data/data/com.sec.android.app.fm/files/"
            + BACKUP_CHANNEL_FILE_NAME;
    private static final String PREF_FILE = SettingsActivity.PREF_FILE;
    private static final String BACKUP_PREFER_FILE_NAME = PREF_FILE + ".xml";
    private static final String SHARED_PREF_FILE_PATH = "./data/data/com.sec.android.app.fm/shared_prefs/"
            + BACKUP_PREFER_FILE_NAME;
    public static final String RESTORE_FINISH = "com.sec.android.app.fm.RESTORE_FINISH";
    public static final String BACKUP_FINISH = "com.sec.android.app.fm.BACKUP_FINISH";

    private static final String REQUEST_BACKUP_RADIO = "com.samsung.android.intent.action.REQUEST_BACKUP_RADIO";
    private static final String REQUEST_RESTORE_RADIO = "com.samsung.android.intent.action.REQUEST_RESTORE_RADIO";
    private static final String RESPONSE_BACKUP_RADIO = "com.samsung.android.intent.action.RESPONSE_BACKUP_RADIO";
    private static final String RESPONSE_RESTORE_RADIO = "com.samsung.android.intent.action.RESPONSE_RESTORE_RADIO";

    private static int RESTORE = 1;
    private static int BACKUP = 2;
    private static int UNKNOWN_ERROR = 1;
    private static int PERMISSION_FAIL = 4;

    private static Context mContext = null;

    private Cipher cipher;
    private SecretKeySpec secretKey;
    private byte[] salt;
//    private String securityPassword;

    public SecretKeySpec generateSHA256SecretKey(String super_key) throws Exception {

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(super_key.getBytes("UTF-8"));
        byte[] keyBytes = new byte[16];
        System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);

        return new SecretKeySpec(keyBytes, "AES");
    }

    public byte[] generateEncryptSalt() throws NoSuchAlgorithmException {

        SecureRandom sr = new SecureRandom();
        byte[] salt = new byte[16];
        sr.nextBytes(salt);

        return salt;
    }

    public SecretKeySpec generatePBKDF2SecretKey(String securityPassword) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException {

        int iterationCount = 1000;
        int keyLength = 256;
        char[] chars = securityPassword.toCharArray();

        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec keySpec = new PBEKeySpec(chars, salt, iterationCount, keyLength);
        SecretKey key = keyFactory.generateSecret(keySpec);

        return new SecretKeySpec(key.getEncoded(), "AES");
    }

    public OutputStream encryptStream(OutputStream out, String saveKey, boolean isSecurityPassword) throws Exception {

        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        byte[] iv = new byte[cipher.getBlockSize()];
        new SecureRandom().nextBytes(iv);
        AlgorithmParameterSpec spec = new IvParameterSpec(iv);
        out.write(iv);

        if(isSecurityPassword) { // intent.getIntExtra("SECURITY_LEVEL", 0); // 1 -> "HIGH", 0 -> "NORMAL"
            salt = generateEncryptSalt();
            out.write(salt);
            secretKey = generatePBKDF2SecretKey(saveKey);
        }
        else {
            secretKey = generateSHA256SecretKey(saveKey);
        }

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
        return new CipherOutputStream(out, cipher);

    }

    public InputStream decryptStream(InputStream in, String saveKey, boolean isSecurityPassword) throws Exception {

        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

        byte[] iv = new byte[cipher.getBlockSize()];
        in.read(iv);
        AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);

        if(isSecurityPassword) { // intent.getIntExtra("SECURITY_LEVEL", 0); // 1 -> "HIGH", 0 -> "NORMAL"
            salt = new byte[16];
            in.read(salt);
            secretKey = generatePBKDF2SecretKey(saveKey);
        } else {
            secretKey = generateSHA256SecretKey(saveKey);
        }

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        return new CipherInputStream(in, cipher);
    }

    public void copy(String target_File, String result_File, boolean isBackup, String saveKey, boolean isSecurityPassword) {
        File targetFile = new File(target_File);
        File resultFile = new File(result_File);
        File tmpFile = new File(result_File + "_");

        InputStream in = null;
        InputStream in_tmp = null;
        OutputStream out = null;
        OutputStream out_tmp = null;

        if ((targetFile != null) && (targetFile.exists())) {
            if ((resultFile != null) && (resultFile.exists())) {
                resultFile.renameTo(new File(result_File + "_"));
            }
            int nRead = 0;
            byte[] buffer = new byte[1024 * 8];
            try {
                if (isBackup) {
                    in = new FileInputStream(targetFile);
                    out_tmp = new FileOutputStream(resultFile);
                    out = encryptStream(out_tmp, saveKey, isSecurityPassword);
                } else {
                    in_tmp = new FileInputStream(targetFile);
                    in = decryptStream(in_tmp, saveKey, isSecurityPassword);
                    out = new FileOutputStream(resultFile);
                }

                while ((nRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, nRead);
                }

            } catch (IOException e) {
                if (tmpFile.exists() && !(tmpFile.renameTo(new File(result_File)))) {
                    Log.e(TAG, "Rename fail");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (in_tmp != null) {
                    try {
                        in_tmp.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out_tmp != null) {
                    try {
                        out_tmp.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (tmpFile.exists() && !(tmpFile.delete())) {
                Log.e(TAG, "Remove temp file fail");
            }

            resultFile.setReadable(true, false);
            resultFile.setWritable(true, false);
            resultFile.setExecutable(true, false);
        } else {
            Log.v(TAG, "targetFile targetfile not exist");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        mContext = context;

        Log.v("FmRadio", "onReceive : action = " + action);

        if (action != null) {
            final String filepath = intent.getStringExtra("SAVE_PATH") + "/";
            final String source = intent.getStringExtra("SOURCE");
            final String saveKey = intent.getStringExtra("SESSION_KEY");
            final String sessiontime = intent.getStringExtra("EXPORT_SESSION_TIME");
            final int extraAction = intent.getIntExtra("ACTION", 0);
            final int securityLevel = intent.getIntExtra("SECURITY_LEVEL", 0); // 1 -> "HIGH", 0 -> "NORMAL"

            final boolean isSecurityPassword;
            if (securityLevel == 1)
                isSecurityPassword = true;
            else
                isSecurityPassword = false;

            if (action.equals(REQUEST_RESTORE_RADIO)) { // is restore

                if(FMPermissionUtil.hasPermission(mContext, FMPermissionUtil.FM_PERMISSION_REQUEST_BACKUP_RESTORE) == false) {
                    sendResponse(RESTORE, PERMISSION_FAIL, source, sessiontime);
                } else {
                    Thread t = new Thread(new Runnable() {
                        public void run() {
                            Log.v("FmRadio", "onReceive : RESTORE is running");

                            initFilePath(mContext);

                            String targetfile = filepath + BACKUP_PREFER_FILE_NAME;
                            String channleStore = filepath + BACKUP_CHANNEL_FILE_NAME;
                            String channelRestorePath = CHNNEL_STORE_FILE_PATH;

                            mContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);

                            Intent restore = new Intent(RESTORE_FINISH);
                            mContext.sendBroadcast(restore, BACKUP_AND_RESTORE_PERMISSION);

                            int result = loadRadioSettingPreferencesFromFile(targetfile, mContext, saveKey, isSecurityPassword);
                            copy(channleStore, channelRestorePath, false, saveKey, isSecurityPassword);
                            ChannelStore.getInstance().load();

                            sendResponse(RESTORE, result, source, sessiontime);
                        }
                    });
                    t.start();
                }
            } else if (action.equals(REQUEST_BACKUP_RADIO)) { // is backup
                if(extraAction == 2) {// cancel case
                    // stop backup working thread
                } else {
                    if(FMPermissionUtil.hasPermission(mContext, FMPermissionUtil.FM_PERMISSION_REQUEST_BACKUP_RESTORE) == false) {
                        sendResponse(BACKUP, PERMISSION_FAIL, source, sessiontime);
                    } else {
                        Thread t = new Thread(new Runnable() {
                            public void run() {
                                Log.v("FmRadio", "onReceive : BACKUP is running");

                                String restoreOrBackup = filepath + BACKUP_PREFER_FILE_NAME;
                                ChannelStore.getInstance().deleteEmptyFile();
                                String channleStore = CHNNEL_STORE_FILE_PATH;
                                String channelRestorePath = filepath + BACKUP_CHANNEL_FILE_NAME;

                                Log.v("FmRadio", "onReceive : filepath = " + filepath);
                                Log.v("FmRadio", "onReceive : restoreOrBackup = " + restoreOrBackup);

                                saveRadioSettingPreferencesToFile(restoreOrBackup, mContext, saveKey, isSecurityPassword);
                                copy(channleStore, channelRestorePath, true, saveKey, isSecurityPassword);

                                sendResponse(BACKUP, 0, source, sessiontime);
                            }
                        });
                        t.start();
                    }
                }
            }
        }
    }

    private void sendResponse(int type, int err_code, String source, String sessiontime) {
        Intent response;
        int result = 0;

        if (type == RESTORE) { // restore
            response = new Intent(RESPONSE_RESTORE_RADIO);
        } else {
            response = new Intent(RESPONSE_BACKUP_RADIO);
        }

        if (err_code != 0) {
            result = 1; // fail
        }

        response.putExtra("RESULT", result);
        response.putExtra("ERR_CODE", err_code);
        response.putExtra("REQ_SIZE", 0);
        response.putExtra("SOURCE", source);
        response.putExtra("EXPORT_SESSION_TIME", sessiontime);

        mContext.sendBroadcast(response, BACKUP_AND_RESTORE_PERMISSION);
    }

    private void initFilePath(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_FILE,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("TEMP", false);
        editor.commit();

        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(context.openFileOutput(ChannelStore.FILE_NAME,
                    Context.MODE_PRIVATE));
            outputStream.writeObject(new ArrayList<Channel>());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void saveRadioSettingPreferencesToFile(String dst, Context context, String saveKey, boolean isSecurityPassword) {

        File srcFile = new File(SHARED_PREF_FILE_PATH);
        File dstFile = new File(dst);
        ObjectOutputStream output = null;
        OutputStream out_tmp = null;

        if (srcFile != null && srcFile.exists()) {
            try {
                out_tmp = new FileOutputStream(dstFile);
                OutputStream newfos = encryptStream(out_tmp, saveKey, isSecurityPassword);
                output = new ObjectOutputStream(newfos);
                SharedPreferences pref = context.getSharedPreferences(PREF_FILE,
                        Context.MODE_PRIVATE);
                output.writeObject(pref.getAll());
                output.flush();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (output != null) {
                        output.close();
                    }
                    if (out_tmp != null) {
                        out_tmp.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            Log.v(TAG, "Preference file not exist");
        }
    }

    @SuppressWarnings("unchecked")
    private int loadRadioSettingPreferencesFromFile(String src, Context context, String saveKey, boolean isSecurityPassword) {

        File srcFile = new File(src);
        ObjectInputStream input = null;
        InputStream in_tmp = null;
        int result = UNKNOWN_ERROR;

        try {
            in_tmp = new FileInputStream(srcFile);
            InputStream fis = decryptStream(in_tmp, saveKey, isSecurityPassword);
            input = new ObjectInputStream(fis);
            SharedPreferences.Editor prefEdit = context.getSharedPreferences(PREF_FILE,
                    Context.MODE_PRIVATE).edit();
            prefEdit.clear();
            Map<String, Object> entries = (Map<String, Object>) input.readObject();
            for (Entry<String, Object> entry : entries.entrySet()) {
                Object obj = entry.getValue();
                String key = entry.getKey();

                if (obj instanceof Boolean) {
                    prefEdit.putBoolean(key, ((Boolean) obj).booleanValue());
                } else if (obj instanceof Float) {
                    prefEdit.putFloat(key, ((Float) obj).floatValue());
                } else if (obj instanceof Integer) {
                    prefEdit.putInt(key, ((Integer) obj).intValue());
                } else if (obj instanceof Long) {
                    prefEdit.putLong(key, ((Long) obj).longValue());
                } else if (obj instanceof String) {
                    prefEdit.putString(key, ((String) obj));
                }
            }
            prefEdit.commit();
            result = 0;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
                if (in_tmp != null) {
                    in_tmp.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }
}
