package com.cosmos.bgkiller.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.widget.Toast;

import com.cosmos.bgkiller.BgKillerApplication;
import com.cosmos.bgkiller.Settings;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by cosmos on 2016/8/27.
 */

public final class Utils {

    public static final String PACKAGE_NAME = "com.cosmos.bgkiller";

    public static final String TAG = "BG_KILLER";

    private static final String DISABLE_PREFIX = "pm disable ";
    private static final String ENABLE_PREFIX = "pm enable ";

    private static final int TYPE_ALL = 0;
    private static final int TYPE_SYSTEM = 1;
    private static final int TYPE_USER_INSTALL = 2;
    private static final int TYPE_DISABLE = 3;

    public static final String KEY_NORMAL_DISABLED_USER_APPS = "normal_disabled_user_apps";
    public static final String KEY_AUTO_ENABLE_DISABLED_USER_APPS = "auto_enable_disabled_user_apps";

    public static PrintStream sPrintStream;

    public static List<PackageInfo> getAllPackageList() {
        return getPackageInfoList(TYPE_ALL);
    }

    public static List<PackageInfo> getSystemPackageList() {
        return getPackageInfoList(TYPE_SYSTEM);
    }

    public static List<PackageInfo> getUserInstalledPackageList() {
        return getPackageInfoList(TYPE_USER_INSTALL);
    }

    private static List<PackageInfo> getPackageInfoList(int type) {
        PackageManager pm = BgKillerApplication.getsInstance().getPackageManager();
        List<PackageInfo> infos = pm.getInstalledPackages(0);
        if (infos == null || infos.size() == 0) {
            return infos;
        }
        infos = new ArrayList<>(infos);
        List<PackageInfo> whiteList = new ArrayList<>();
        for (PackageInfo info : infos) {
            if (info != null) {
                if (isInWhiteList(info)) {
                    whiteList.add(info);
                    break;
                }
            }
        }
        infos.removeAll(whiteList);

        List<PackageInfo> result = new ArrayList<>();
        boolean system = false;
        switch (type) {
            case TYPE_ALL:
                result = infos;
                break;
            case TYPE_SYSTEM:
                system = true;
            case TYPE_USER_INSTALL:
                for (PackageInfo info : infos) {
                    if (info != null) {
                        if (isSystem(info) == system) {
                            result.add(info);
                        }
                    }
                }
                break;
            case TYPE_DISABLE:
                break;
            default:
                break;

        }
        return result;
    }

    private static boolean isInWhiteList(PackageInfo info) {
        return info != null && TextUtils.equals(info.packageName, PACKAGE_NAME);
    }

    private static boolean isSystem(PackageInfo info) {
        return info.applicationInfo != null
                && (info.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }


    public static void getTypedStringSet(String typeKey, Set<String> result) {
        if (result == null || TextUtils.isEmpty(typeKey)) {
            return;
        }
        result.clear();
        result.addAll(BgKillerApplication.getsInstance().getSharedPreferences().getStringSet(typeKey, Collections.EMPTY_SET));
    }

    public static void putTypedStringSet(String typeKey, Set<String> result) {
        if (TextUtils.isEmpty(typeKey)) {
            return;
        }
        SharedPreferences.Editor editor = BgKillerApplication.getsInstance().getSharedPreferences().edit();
        editor.putStringSet(typeKey, result == null ? Collections.<String>emptySet() : result);
        editor.commit();
    }

    private static void appendSettignsCommand(String[] pkgs, String prefix, DataOutputStream ds) throws IOException {
        if (pkgs != null) {
            for (String pkg : pkgs) {
                if (!TextUtils.isEmpty(pkg)) {
                    ds.writeBytes(prefix + pkg + " \n");
                    ds.flush();
                }
            }
        }
    }

    public static void settingsPackage(String[] enablePkgs, String[] disablePkgs) {
        if (!isArrayReallyEmpty(enablePkgs) || !isArrayReallyEmpty(disablePkgs)) {
            DataOutputStream ds = null;
            try {

                Runtime runtime = Runtime.getRuntime();
                Process process = runtime.exec("su root");
                ds = new DataOutputStream(process.getOutputStream());
                appendSettignsCommand(enablePkgs, ENABLE_PREFIX, ds);
                appendSettignsCommand(disablePkgs, DISABLE_PREFIX, ds);
                ds.writeBytes("exit \n");
                ds.flush();
                process.waitFor();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (ds != null) {
                    try {
                        ds.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void tryRoot(){
        DataOutputStream ds = null;
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("su root");
            ds = new DataOutputStream(process.getOutputStream());
            ds.writeBytes("exit \n");
            ds.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }  finally {
            if (ds != null) {
                try {
                    ds.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void settingsPackage(boolean enable, String... pkgName) {
        if (enable) {
            settingsPackage(pkgName, null);
        } else {
            settingsPackage(null, pkgName);
        }
    }

    static boolean isArrayReallyEmpty(String... pkgName) {
        String[] params = pkgName;
        if (params != null) {
            for (String s : params) {
                if (!TextUtils.isEmpty(s)) {
                    return false;
                }
            }
        }
        return true;
    }

    static String loadStream(InputStream in) throws IOException {
        int ptr;
        in = new BufferedInputStream(in);
        StringBuffer buffer = new StringBuffer();
        while ((ptr = in.read()) != -1) {
            buffer.append((char) ptr);
        }
        return buffer.toString();
    }

    public synchronized static void logD(String message) {
        Log.e(TAG, message);
        if(Settings.getInstance().debugMode()){
            if(sPrintStream == null){
                try {
                    sPrintStream = new PrintStream(new FileOutputStream(new File(BgKillerApplication.getsInstance().getFilesDir(), System.currentTimeMillis() + ".log")));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if(sPrintStream != null){
                sPrintStream.println(new Date() + ": " + message) ;
            }
        }

        toastDebug(BgKillerApplication.getsInstance(), message);
    }

    private static final Map<String, SoftReference<Drawable>> sDrawableCache = new HashMap<>();

    public static Drawable getDrawable(PackageInfo info) {
        if (info != null && info.applicationInfo != null) {
            PackageManager pm = BgKillerApplication.getsInstance().getPackageManager();
            SoftReference<Drawable> drawableSf = sDrawableCache.get(info.packageName);
            Drawable d = drawableSf != null ? drawableSf.get() : null;
            if (d == null) {
                d = pm.getApplicationIcon(info.applicationInfo);
                sDrawableCache.put(info.packageName, new SoftReference<Drawable>(d));
            }
            return d;
        }
        return null;
    }

    public static String appendWifiInfo(String ssid, String bssid){
        return ssid/* + "_wifi_" + bssid*/;
    }
    public static void toastDebug(Context context, String message){
        if(Settings.getInstance().debugMode()){
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

}
