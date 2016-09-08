package com.cosmos.bgkiller;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.cosmos.bgkiller.utils.Utils;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by cosmos on 2016/8/27.
 */

public class BgKillerApplication extends Application {

    private static final String PREFERENCE_NAME = "common";

    private static BgKillerApplication sInstance;
    private SharedPreferences mPref;
    private Map<String, PackageInfo> mAllPackageInfo;
    private Map<String, ResolveInfo> mAllMainActivitys;
    private Set<String> mDisabledApps = new HashSet<>();
    private Intent mMainIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        setInstance(this);
        init();
        Utils.tryRoot();
    }

    private void startCoreService() {
        KillerCore.queueKiller(this, null, null, 0);
    }

    private void registerReceiver(){
        IntentFilter filter = new IntentFilter(Intent.ACTION_UNINSTALL_PACKAGE);
        filter.addAction(Intent.ACTION_INSTALL_PACKAGE);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long currentTimes = System.currentTimeMillis();
                mAllPackageInfo = null;
                getAllPackageInfo();
                mAllMainActivitys = null;
                getAllResolveInfo();
                Utils.logD("recieve action " + intent.getAction() + " usage: " + (System.currentTimeMillis() - currentTimes));
            }
        },filter);
    }


    private static void setInstance(BgKillerApplication application) {
        sInstance = application;
    }

    public static BgKillerApplication getsInstance() {
        return sInstance;
    }

    private void init() {
        mPref = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
        Settings.getInstance().init(PreferenceManager.getDefaultSharedPreferences(this));
        startCoreService();
        registerReceiver();
        syncDisableAppStatus();
    }

    private void syncDisableAppStatus(){
        getAllPackageInfo();
        Set<String> autoSettingDisabledApps = new HashSet<>();
        Set<String> manualSettingDisabledApps = new HashSet<>();
        Utils.getTypedStringSet(Utils.KEY_AUTO_ENABLE_DISABLED_USER_APPS, autoSettingDisabledApps);
        Utils.getTypedStringSet(Utils.KEY_NORMAL_DISABLED_USER_APPS, manualSettingDisabledApps);

        int size = manualSettingDisabledApps.size();

        mDisabledApps.removeAll(autoSettingDisabledApps);
        manualSettingDisabledApps.addAll(mDisabledApps);
        if(size != manualSettingDisabledApps.size()){
            Utils.putTypedStringSet(Utils.KEY_NORMAL_DISABLED_USER_APPS, manualSettingDisabledApps);
        }
    }

    public SharedPreferences getSharedPreferences() {
        return mPref;
    }

    public ActivityInfo getPackageMainActivityInfo(String packagename) {
        if (TextUtils.isEmpty(packagename)) {
            return null;
        }

        Map<String, ResolveInfo> resolveInfoMap = getAllResolveInfo();
        if (resolveInfoMap != null) {
            ResolveInfo info = resolveInfoMap.get(packagename);
            if (info == null) {
                mAllMainActivitys = null;
                resolveInfoMap = getAllResolveInfo();
                if (resolveInfoMap != null) {
                    info = resolveInfoMap.get(packagename);
                }
            }
            if (info != null) {
                return info.activityInfo;
            }
        }
        return null;
    }

    public Map<String, ResolveInfo> getAllResolveInfo() {
        Map<String, ResolveInfo> result = mAllMainActivitys;
        if (result == null) {
            result = new HashMap<>();
            if (mMainIntent == null) {
                mMainIntent = new Intent(Intent.ACTION_MAIN, null);
                mMainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            }
            List<ResolveInfo> infoList = getPackageManager().queryIntentActivities(mMainIntent, 0);
            Utils.logD("all resloving main info infoList: " + infoList);
            if (infoList != null) {
                for (ResolveInfo info : infoList) {
                    if (info != null &&info.activityInfo != null) {
                        result.put(info.activityInfo.packageName, info);
                    }
                }
                mAllMainActivitys = result;
            }

            Utils.logD("all resloving main info: " + result);
        }
        return result;
    }

    public Map<String, PackageInfo> getAllPackageInfo() {
        Map<String, PackageInfo> result = mAllPackageInfo;
        if (result == null) {
            List<PackageInfo> allInfo = Utils.getAllPackageList();
            result = new HashMap<>();
            if (allInfo != null) {
                for (PackageInfo info : allInfo) {
                    if (info != null) {
                        result.put(info.packageName, info);
                        if(info.applicationInfo != null && !info.applicationInfo.enabled){
                            mDisabledApps.add(info.packageName);
                        }
                    }
                }
            }
            mAllPackageInfo = result;
        }
        return result;
    }

    public Set<String> getmDisabledApps(){
        return mDisabledApps;
    }

}
