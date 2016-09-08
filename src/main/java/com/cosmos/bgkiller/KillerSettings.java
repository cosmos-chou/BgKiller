package com.cosmos.bgkiller;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.cosmos.bgkiller.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static com.cosmos.bgkiller.Settings.*;

public class KillerSettings extends PreferenceActivity {

    private ListPreference mDelayedProcessPref;
    private MultiSelectListPreference mMultiSelectListPreference;
    private WifiManager mWifiManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        mDelayedProcessPref = (ListPreference) findPreference(KEY_PROCESS_DELAYED);
        setupListPreference(Settings.getInstance().getProcessDelayString());
        mDelayedProcessPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setupListPreference(String.valueOf(newValue));
                return true;
            }
        });

        mMultiSelectListPreference = ((MultiSelectListPreference)findPreference(KEY_UN_BLOCKED_WIFI_LIST));

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        List<WifiConfiguration> savedWifis = mWifiManager.getConfiguredNetworks();
        Utils.logD("scan result: " + savedWifis);
        List<String> entries = new ArrayList<>();
        List<String> entrieValues = new ArrayList<>();
        if(savedWifis != null && savedWifis.size() > 0){
            for(WifiConfiguration wc : savedWifis){
                entries.add(wc.SSID);
                entrieValues.add(Utils.appendWifiInfo(wc.SSID, wc.BSSID));
            }
        }

        Utils.logD("wifi saved result: " + entries + "| "  + entrieValues);
        MultiSelectListPreference multiSelectListPreference = mMultiSelectListPreference;
        multiSelectListPreference.setEnabled(mWifiManager.isWifiEnabled() && entries.size() > 0);
        multiSelectListPreference.setEntries(entries.toArray(new String[entries.size()]));
        multiSelectListPreference.setEntryValues(entrieValues.toArray(new String[entrieValues.size()]));

        findPreference(KEY_SHOW_NOTIFICATION).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                KillerCore.queueKiller(KillerSettings.this, null, null , KillerCore.COMMAND_REFRESH_FOREGROUND_STATUS);
                return true;
            }
        });
    }


    private void setupListPreference(String value){
        int index = mDelayedProcessPref.findIndexOfValue(String.valueOf(value));
        if(index < 0){
            index = 1;
        }
        CharSequence[] values = mDelayedProcessPref.getEntries();
        if(values != null && values.length > index){
            mDelayedProcessPref.setSummary(values[index]);
        }
    }
}
