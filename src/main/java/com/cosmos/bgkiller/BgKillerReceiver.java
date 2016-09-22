package com.cosmos.bgkiller;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;

import com.cosmos.bgkiller.BgKillerApplication;
import com.cosmos.bgkiller.utils.Utils;
import com.cosmos.bgkiller.xposed.NetworkAutoSwitcher;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by zhouyuhang on 2016/8/29.
 */
public class BgKillerReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if(intent == null){
            return ;
        }
        HashSet<String> autoDisabledSet = new HashSet<>();
        Utils.getTypedStringSet(Utils.KEY_AUTO_ENABLE_DISABLED_USER_APPS, autoDisabledSet);
        HashSet<String> manualDisabledSet = new HashSet<>();
        Utils.getTypedStringSet(Utils.KEY_NORMAL_DISABLED_USER_APPS, manualDisabledSet);
        ArrayList<String> disable = null;
        ArrayList<String> enable = null;
        String action = intent.getAction();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        Utils.logD("onReceive " + action+ "  " + pm.isInteractive());
        boolean switchSS = false;
        int command = 0;
        int type = -1;
        if (NetworkAutoSwitcher.ACTION_SCREEN_LOCKED_STATUS_CHANGE.equals(action)) { // 锁屏
            disable = new ArrayList<>(autoDisabledSet);
            disable.addAll(manualDisabledSet);
            command = KillerCore.COMMAND_DELAYED;
            WifiManager wifiM = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            type = 1;
        } else if (Intent.ACTION_USER_PRESENT.equals(action)) { // 解锁
            enable = new ArrayList<>(autoDisabledSet);
            switchSS = Settings.getInstance().getAutoEnableSS();
            type = 22;
        }else {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean(KillerCore.KEY_ENABLE_SS, switchSS);
        if(Settings.getInstance().autoSwitchNetwork()){
            bundle.putInt(NetworkAutoSwitcher.KEY_TYPE, type);
        }
        KillerCore.queueKiller(context, disable, enable, command, bundle);
    }
}
