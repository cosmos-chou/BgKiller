package com.cosmos.bgkiller;

import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.Set;

/**
 * Created by cosmos on 2016/8/27.
 */

public final class Settings {
    public static final long MILLS_FOR_MINUTE = 1000 * 60;

    public static final String KEY_UN_BLOCKED_WIFI_LIST = "unblocked_wifi_list";

    public static final String KEY_PROCESS_DELAYED = "key_process_delayed";

    public static final String KEY_AUTO_ENABLE_SS = "key_auto_enable_ss";

    public static final String KEY_SHOW_NOTIFICATION = "show_notification";

    public static final String KEY_AUTO_SWITCH_NETWORK = "auto_switch_network";

    public static final String KEY_AUTO_SWITCH_WIFI = "auto_switch_wifi";

    public static final String KEY_DEBUG_MODE = "debug_mode";

    public static final String KEY_WIFI_STATE = "wifi_state";

    private static final Settings INSTANCE = new Settings();

    private SharedPreferences mPref;

    public static Settings getInstance(){
        return INSTANCE;
    }

    private Settings(){}

    public void init(SharedPreferences prefs){
        mPref = prefs;
    }

    public long getProcessDelay(){
        return MILLS_FOR_MINUTE * Integer.parseInt(getProcessDelayString());
    }
    public String getProcessDelayString(){
        return mPref.getString(KEY_PROCESS_DELAYED, "0");
    }

    public String getSSPackageName(){
        return "com.github.shadowsocks";
    }

    public Intent getSSEnableAction(){
        return new Intent("com.twofortyfouram.locale.intent.action.FIRE_SETTING");
    }

    public boolean getAutoEnableSS(){
        return mPref.getBoolean(KEY_AUTO_ENABLE_SS, true);
    }

    public Set<String> getUnBlockedWifiList(){
        return mPref.getStringSet(KEY_UN_BLOCKED_WIFI_LIST, Collections.EMPTY_SET);
    }

    public boolean showNotification(){
        return mPref.getBoolean(KEY_SHOW_NOTIFICATION, true);
    }

    public boolean autoSwitchNetwork(){
        return mPref.getBoolean(KEY_AUTO_SWITCH_NETWORK, true);
    }

    public boolean autoSwitchWifi(){
        return mPref.getBoolean(KEY_AUTO_SWITCH_WIFI, true);
    }

    public boolean debugMode(){
        return mPref.getBoolean(KEY_DEBUG_MODE, false);
    }

    public void saveWifiState(boolean wifiEnable){
        mPref.edit().putBoolean(KEY_WIFI_STATE, wifiEnable);
    }

    public boolean isWifiEnableBefore(){
        return mPref.getBoolean(KEY_WIFI_STATE, false);
    }
}
