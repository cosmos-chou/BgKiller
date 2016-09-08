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

    public static final String KEY_ENABLE_UNTIL_USER_PRESENT = "key_until_present";

    public static final String KEY_SHOW_NOTIFICATION = "show_notification";

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

    public boolean enableUntilUserPresent(){
        return mPref.getBoolean(KEY_ENABLE_UNTIL_USER_PRESENT, true);
    }

    public Set<String> getUnBlockedWifiList(){
        return mPref.getStringSet(KEY_UN_BLOCKED_WIFI_LIST, Collections.EMPTY_SET);
    }

    public boolean showNotification(){
        return mPref.getBoolean(KEY_SHOW_NOTIFICATION, true);
    }
}
