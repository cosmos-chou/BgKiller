package com.cosmos.bgkiller.xposed;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;

import com.cosmos.bgkiller.utils.Utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class NetworkAutoSwitcher implements IXposedHookLoadPackage {

    private static final String PHONE_PACKAGE_NAME = "com.android.phone";
    private static final String SYSTEM_UI_PACKAGE_NAME = "com.android.systemui";
    public static final String ACTION_CHANGE_NETWORK_TYPE = "com.cosmos.bgkiller.change_net_worktype";
    public static final String ACTION_SCREEN_LOCKED_STATUS_CHANGE = "com.cosmos.bgkiller.change_screen_lock_state";
    public static final String KEY_TYPE = "type";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        if (loadPackageParam != null && TextUtils.equals(PHONE_PACKAGE_NAME, loadPackageParam.packageName)) {
            XposedBridge.log(Utils.TAG + ": " + loadPackageParam.packageName + " | " + loadPackageParam.processName);
            findAndHookMethod("com.android.phone.PhoneApp", loadPackageParam.classLoader, "onCreate", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    Context context = (Context) param.thisObject;
                    if (context != null && TextUtils.equals(PHONE_PACKAGE_NAME, context.getPackageName())) {
                        System.out.println(Utils.TAG + ": context" + context.getPackageName());
                        IntentFilter filter = new IntentFilter(ACTION_CHANGE_NETWORK_TYPE);
                        context.registerReceiver(new AutoReceiver(), filter);
                    }
                }
            });
        }

        if(loadPackageParam != null
                &&(TextUtils.equals(SYSTEM_UI_PACKAGE_NAME, loadPackageParam.packageName))){
            findAndHookMethod("com.android.systemui.keyguard.KeyguardViewMediator ",loadPackageParam.classLoader, "handleShow", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    super.afterHookedMethod(param);
                    Field field = param.thisObject.getClass().getField("mContext");
                    field.setAccessible(true);
                    XposedBridge.log(Utils.TAG + ": " + field.get(param.thisObject));
                    ((Context)field.get(param.thisObject)).sendBroadcast(new Intent(ACTION_SCREEN_LOCKED_STATUS_CHANGE));

                }
            });
        }

    }

    public static class AutoReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            XposedBridge.log(Utils.TAG + ": AutoReceiver onReceive" + intent);
            if (intent != null) {
                int type = intent.getIntExtra(KEY_TYPE, -1);
                XposedBridge.log(Utils.TAG + ": " + ": AutoReceiver onReceive" + type + "  " + Settings.Global.getInt(context.getContentResolver(), "preferred_network_mode" + 1, -1));
                if (type != -1 && Settings.Global.getInt(context.getContentResolver(), "preferred_network_mode" + 1, -1) != type) {
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    editor.putString("preferred_network_mode_key", String.valueOf(type)).commit();
                    editor.putString("enabled_networks_key", String.valueOf(type)).commit();
                    Settings.Global.putInt(context.getContentResolver(), "preferred_network_mode" + 1, type);
                    try{
                        Field f = context.getApplicationContext().getClass().getDeclaredField("mPhoneGlobals");
                        f.setAccessible(true);
                        Object o = f.get(context.getApplicationContext());
                        Method m = o.getClass().getDeclaredMethod("getPhone");
                        m.setAccessible(true);
                        o = m.invoke(null);
                        m = o.getClass().getDeclaredMethod("setPreferredNetworkType", int.class, Message.class);
                        m.invoke(o, type, new MyHandler().obtainMessage(0));
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }
            }
        }
    }

    public static class MyHandler extends Handler {

        public void handleMessage(Message paramMessage) {
            XposedBridge.log(Utils.TAG + ": " +  "MyHandler: handle" + paramMessage);
        }
    }
}
