package com.sec.android.app.fm.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public final class SystemPropertiesWrapper {
    private static SystemPropertiesWrapper sInstance = null;
    private static Class<?> c;

    private SystemPropertiesWrapper() {
        try {
            c = Class.forName("android.os.SystemProperties");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static SystemPropertiesWrapper getInstance() {
        if (sInstance == null) {
            sInstance = new SystemPropertiesWrapper();
        }
        return sInstance;
    }

    public String get(String key) {
        try {
            Method get = c.getMethod("get", String.class);
            Object ret = get.invoke(c, key);
            return (String) ret;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String get(String key, String def) {
        try {
            Method get = c.getMethod("get", String.class, String.class);
            Object ret = get.invoke(c, key, def);
            return (String) ret;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public int getInt(String key, int def) {
        try {
            Method getInt = c.getMethod("getInt", String.class, int.class);
            Object ret = getInt.invoke(c, key, def);
            return (Integer) ret;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean getBoolean(String key, boolean def) {
    	try {
    		Method getBoolean = c.getMethod("getBoolean", String.class, boolean.class);
    		Object ret = getBoolean.invoke(c, key, def);
    		return (Boolean) ret;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    	return false;
    }
}
