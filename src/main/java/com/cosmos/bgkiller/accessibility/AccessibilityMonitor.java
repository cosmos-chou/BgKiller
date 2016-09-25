package com.cosmos.bgkiller.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.view.accessibility.AccessibilityEvent;

/**
 * Created by cosmos on 2016/9/25.
 */

public class AccessibilityMonitor extends AccessibilityService{

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        System.out.println(" cosmos_chou: " + event);
        switch (event.getEventType()){
            case AccessibilityEvent.TYPE_WINDOWS_CHANGED:
        }

    }

    @Override
    public void onInterrupt() {

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    public static final void beginMonitor(Context context){
        context.startService(new Intent(context, AccessibilityMonitor.class));
    }

}
