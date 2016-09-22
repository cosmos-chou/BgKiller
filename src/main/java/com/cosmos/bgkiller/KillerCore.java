package com.cosmos.bgkiller;

import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;

import com.cosmos.bgkiller.utils.Utils;
import com.cosmos.bgkiller.xposed.NetworkAutoSwitcher;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by cosmos on 2016/8/27.
 */

public class KillerCore extends Service {

    public static final String KEY_PACKAGES_LIST_ENABLE = "packageList_enable";
    public static final String KEY_PACKAGES_LIST_DISABLE = "packageList_disable";
    public static final String KEY_ENABLE_SS = "enable_ss";
    public static final String KEY_COMMAND = "command";


    public static final int MSG_HANDLE = 10000;
    public static final int MSG_ENABLE_SS = 10001;
    public static final int MSG_START_COMMAND = 10002;
    public static final int MSG_SET_NETWORK_TYPE = 10003;


    public static final int COMMAND_DELAYED = 1 << 3;
    public static final int COMMAND_ALARM_PROCESS = 1 << 2;
    public static final int COMMAND_REFRESH_FOREGROUND_STATUS = 1 << 4;
    private HandlerThread mThread;
    private Handler mProcessHandler;

    private Set<String> mPendingDisabledPackages = new HashSet<>();
    private Set<String> mPendingEnabledPackages = new HashSet<>();

    private PowerManager.WakeLock mWakeLock;
    private PowerManager mPowerManager;

    private Settings mSettings = Settings.getInstance();

    private BroadcastReceiver mReceiver;
    private Object mSyncLock = new Object();

    private Intent mAlarmProcessIntent;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        refreshForegroundStatus();
        mThread = new HandlerThread("killer_core");
        mThread.setPriority(Thread.MAX_PRIORITY);
        mThread.start();
        mProcessHandler = new KillerHandler(mThread.getLooper());
        mPowerManager = (PowerManager) BgKillerApplication.getsInstance().getSystemService(POWER_SERVICE);
        registerReceiver();
    }

    private void refreshForegroundStatus(){
        Resources res = getResources();
        Notification notification;
        if(Settings.getInstance().showNotification()){
            Intent intent = new Intent(this, KillerMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Notification.Builder nb = new Notification.Builder(this);
            nb.setSmallIcon(R.drawable.ic_launcher);
            nb.setContentIntent(PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT));
            nb.setContentTitle(res.getString(R.string.app_name));
            nb.setContentText(res.getString(R.string.notification_text));
            notification = nb.build();
        }else{
            notification = new Notification();
        }
        startForeground(1000, notification);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }
        if (intent.hasExtra(KEY_COMMAND)) {

            Utils.logD("onStartCommand");
            int command = intent.getIntExtra(KEY_COMMAND, 0);

            switch (command){
                case COMMAND_REFRESH_FOREGROUND_STATUS:
                    refreshForegroundStatus();
                    break;
                default:
                    Message msg = Message.obtain();
                    msg.what = MSG_START_COMMAND;
                    msg.arg1 = command;
                    msg.obj = new Intent(intent);
                    mProcessHandler.sendMessage(msg);
                    break;
            }
        }
        return START_STICKY;
    }

    private void resetMessage() {
        mProcessHandler.removeCallbacksAndMessages(null);
    }

    private void registerReceiver() {
        if (mReceiver == null) {
            mReceiver = new BgKillerReceiver();
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            filter.addAction(NetworkAutoSwitcher.ACTION_SCREEN_LOCKED_STATUS_CHANGE);
            registerReceiver(mReceiver, filter);
        }
    }

    private void unRegisterReceiver() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private void syncList(List<String> stringList, boolean enable) {
        if (stringList != null && stringList.size() > 0) {
            (enable ? mPendingDisabledPackages : mPendingEnabledPackages).removeAll(stringList);
            (enable ? mPendingEnabledPackages : mPendingDisabledPackages).addAll(stringList);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterReceiver();
        mProcessHandler.removeCallbacksAndMessages(null);
        mThread.quitSafely();
    }

    private void acquire(long mills) {
        if (mWakeLock == null) {
            mWakeLock = createWakeLock();
        }
        if (mills >= 0) {
            mWakeLock.acquire(mills);
        } else {
            mWakeLock.acquire();
        }
    }

    private void release() {
        if (mWakeLock != null) {
            mWakeLock.release();
        }
    }

    private PowerManager.WakeLock createWakeLock() {
        return mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "killer_core_wakelock");
    }

    public static void queueKiller(Context context, ArrayList<String> disableList, ArrayList<String> enableList, int command) {
        queueKiller(context, disableList, enableList, command, null);
    }

    public static void queueKiller(Context context, ArrayList<String> disableList, ArrayList<String> enableList, int command, Bundle extras) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(BgKillerApplication.getsInstance(), KillerCore.class);
        intent.putExtra(KillerCore.KEY_PACKAGES_LIST_ENABLE, enableList);
        intent.putExtra(KillerCore.KEY_PACKAGES_LIST_DISABLE, disableList);
        intent.putExtra(KillerCore.KEY_COMMAND, command);
        if (extras != null) {
            intent.putExtras(extras);
        }
        Utils.logD("start_service killer");
        context.startService(intent);
    }

    private Set<String> findStatePackageNameSet(boolean enable, Set<String> pkgSet) {
        Set<String> set = new HashSet<>();
        PackageManager pm = BgKillerApplication.getsInstance().getPackageManager();
        for (String s : pkgSet) {
            try {
                int state = pm.getApplicationEnabledSetting(s);
                if ((state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED && enable)
                        || (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED && !enable)) {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();

            }
            set.add(s);
        }
        return set;
    }

    class KillerHandler extends Handler {
        KillerHandler(Looper looper) {
            super(looper == null ? Looper.getMainLooper() : looper);
        }

        @Override
        public void handleMessage(Message msg) {
            acquire(-1);
            long currenttime = System.currentTimeMillis();
            int what = msg.what;
            switch (what) {
                case MSG_HANDLE:
                    PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                    String[] disableArrays  = null;
                    if(!pm.isInteractive()){
                        disableArrays = mPendingDisabledPackages.toArray(new String[mPendingDisabledPackages.size()]);
                        mPendingDisabledPackages.clear();
                    }
                    Utils.settingsPackage(mPendingEnabledPackages.toArray(new String[mPendingEnabledPackages.size()]), disableArrays);
                    mPendingEnabledPackages.clear();
                    break;
                case MSG_ENABLE_SS:
                    Set<String> ignoreSet = Settings.getInstance().getUnBlockedWifiList();
                    WifiInfo info = ((WifiManager)getSystemService(WIFI_SERVICE)).getConnectionInfo();
                    if(info != null){
                        String key = Utils.appendWifiInfo(info.getSSID(), info.getBSSID());
                        Utils.logD("current connected wifi : " + key + "  " + ignoreSet.contains(key));
                        if(ignoreSet.contains(key)){
                            break;
                        }
                    }
                    sendBroadcast(new Intent(Settings.getInstance().getSSEnableAction()));
                    break;

                case MSG_START_COMMAND:
                    Intent intent = (Intent) msg.obj;
                    long timeout = mSettings.getProcessDelay();
                    int command = msg.arg1;
                    boolean delayed = (command & COMMAND_DELAYED) != 0 && timeout != 0;
                    Utils.logD("MSG_START_COMMAND: " + command + " " + delayed);
                    if(command != COMMAND_ALARM_PROCESS){
                        syncList(intent.getStringArrayListExtra(KEY_PACKAGES_LIST_ENABLE), true);
                        syncList(intent.getStringArrayListExtra(KEY_PACKAGES_LIST_DISABLE), false);
                        Set<String> disableSet = findStatePackageNameSet(false, mPendingDisabledPackages);
                        Set<String> enableSet = findStatePackageNameSet(true, mPendingEnabledPackages);
                        disableSet.removeAll(enableSet);
                        Utils.logD("enable origin: " + mPendingEnabledPackages);
                        Utils.logD("enable fixed: " + enableSet);
                        Utils.logD("disable origin: " + mPendingDisabledPackages);
                        Utils.logD("disable fixed: " + disableSet);
                        mPendingDisabledPackages = disableSet;
                        mPendingEnabledPackages = enableSet;
                    }
                    boolean notEmpty = mPendingDisabledPackages.size() != 0 || mPendingEnabledPackages.size() != 0;
                    AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                    removeMessages(MSG_ENABLE_SS);
                    removeMessages(MSG_HANDLE);
                    mAlarmProcessIntent = new Intent(intent);

                    mAlarmProcessIntent.putExtra(KEY_COMMAND, COMMAND_ALARM_PROCESS);
                    PendingIntent pendingIntent = PendingIntent.getService(KillerCore.this, 0, mAlarmProcessIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    am.cancel(pendingIntent);
                    boolean enableSS = intent.getBooleanExtra(KEY_ENABLE_SS, false);
                    boolean changeNetWork = intent.getIntExtra(NetworkAutoSwitcher.KEY_TYPE, -1) != -1;
                    if (delayed && (notEmpty || enableSS || changeNetWork)) {
                        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + Settings.getInstance().getProcessDelay(), pendingIntent);
                        break;
                    }
                    if (notEmpty) {
                        mProcessHandler.sendEmptyMessage(MSG_HANDLE);
                    }
                    if (enableSS) {
                        mProcessHandler.sendEmptyMessage(MSG_ENABLE_SS);
                    }
                    if(changeNetWork){
                        Message.obtain(mProcessHandler, MSG_SET_NETWORK_TYPE, intent.getIntExtra(NetworkAutoSwitcher.KEY_TYPE, -1), 0).sendToTarget();
                    }
                    break;
                case MSG_SET_NETWORK_TYPE:
                    PowerManager pm1 = (PowerManager) getSystemService(POWER_SERVICE);
                    Utils.logD("MSG_SET_NETWORK_TYPE: " + pm1.isInteractive() + "  " + msg.arg1);
                    if(!pm1.isInteractive() || msg.arg1 == 22) {
                        Intent intent3 = new Intent(NetworkAutoSwitcher.ACTION_CHANGE_NETWORK_TYPE);
                        intent3.putExtra(NetworkAutoSwitcher.KEY_TYPE, msg.arg1);
                        sendBroadcast(intent3);
                        if(Settings.getInstance().autoSwitchWifi()){
                            WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);
                            boolean shouldEnable = 22 == msg.arg1;
                            boolean current = wifi.isWifiEnabled();
                            if(!shouldEnable){
                                Settings.getInstance().saveWifiState(current);
                            }else{
                                shouldEnable = Settings.getInstance().isWifiEnableBefore();
                            }
                            if(shouldEnable != current){
                                wifi.setWifiEnabled(shouldEnable);
                            }
                        }
                    }
                default:
                    break;
            }
            Utils.logD(" KillerHandler process message: " + what + ", usage: " + (System.currentTimeMillis() - currenttime));
            release();
        }
    }
}
