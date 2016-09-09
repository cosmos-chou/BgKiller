package com.cosmos.bgkiller;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;

import com.cosmos.bgkiller.BgKillerApplication;
import com.cosmos.bgkiller.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by zhouyuhang on 2016/8/29.
 */
public class BgKillerReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        if(intent == null || (!Settings.getInstance().enableUntilUserPresent() && Intent.ACTION_USER_PRESENT.equals(intent.getAction() ))
            || (( km.isDeviceLocked() && Settings.getInstance().enableUntilUserPresent())&& Intent.ACTION_SCREEN_ON.equals(intent.getAction() ))){
            return ;
        }
        HashSet<String> autoDisabledSet = new HashSet<>();
        Utils.getTypedStringSet(Utils.KEY_AUTO_ENABLE_DISABLED_USER_APPS, autoDisabledSet);
        HashSet<String> manualDisabledSet = new HashSet<>();
        Utils.getTypedStringSet(Utils.KEY_NORMAL_DISABLED_USER_APPS, manualDisabledSet);
        ArrayList<String> disable = null;
        ArrayList<String> enable = null;
        String action = intent.getAction();
        Utils.logD("onReceive " + action);

        boolean switchSS = false;
        int command = 0;
        if (Intent.ACTION_SCREEN_OFF.equals(action)) { // 锁屏
            disable = new ArrayList<>(autoDisabledSet);
            disable.addAll(manualDisabledSet);
            command = KillerCore.COMMAND_DELAYED;
        } else if (Intent.ACTION_USER_PRESENT.equals(action)
                || Intent.ACTION_SCREEN_ON.equals(action)) { // 解锁
            enable = new ArrayList<>(autoDisabledSet);
            switchSS = /*enable.contains(Settings.getInstance().getSSPackageName()) &&*/ Settings.getInstance().getAutoEnableSS();
        }else {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean(KillerCore.KEY_ENABLE_SS, switchSS);
        KillerCore.queueKiller(context, disable, enable, command, bundle);
    }
}
