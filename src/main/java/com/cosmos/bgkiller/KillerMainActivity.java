package com.cosmos.bgkiller;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.cosmos.bgkiller.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by cosmos on 2016/8/28.
 */

public class KillerMainActivity extends Activity {
    private static final int REQUEST_CODE_ADD = 100;

    private GridView mGridView;
    private List<PackageInfo> mControlledItemList = new ArrayList<>();
    PackageManager mPackageManager;
    private MainItemAdapter mAdapter;
    private Set<String> mDisabledApps = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_killer_main);
        mGridView = (GridView) findViewById(R.id.activity_killer_package_list);
        View empty = findViewById(R.id.empty_view);
        mGridView.setEmptyView(empty);
        mPackageManager = getPackageManager();
        mAdapter = new MainItemAdapter();
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PackageInfo info = (PackageInfo) parent.getAdapter().getItem(position);
                if(info != null){
                    try{
                        Utils.settingsPackage(true, info.packageName);
                        ActivityInfo activityInfo = BgKillerApplication.getsInstance().getPackageMainActivityInfo(info.packageName);
                        if(activityInfo != null){
                            Intent intent = new Intent();
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            intent.setComponent(new ComponentName(info.packageName, activityInfo.name));
                            System.out.println(info.packageName + activityInfo.name);
                            startActivity(intent);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    private void loadDisabledApps() {
        Utils.getTypedStringSet(Utils.KEY_NORMAL_DISABLED_USER_APPS, mDisabledApps);
        Utils.logD("loadDisabledApps: " + mDisabledApps);
        PackageInfo info;
        Map<String, PackageInfo> map = BgKillerApplication.getsInstance().getAllPackageInfo();
        mControlledItemList.clear();
        if (map != null) {
            for (String key : mDisabledApps) {
                info = map.get(key);
                if (info != null && info.applicationInfo != null) {
                    mControlledItemList.add(info);
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_add:
                startActivityForResult(new Intent(this, KillerAddActivity.class), REQUEST_CODE_ADD);
                break;
            case R.id.menu_settings:
                startActivity(new Intent(this, KillerSettings.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDisabledApps();
        mAdapter.notifyDataSetChanged();
    }

    class MainItemAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mControlledItemList.size();
        }

        @Override
        public PackageInfo getItem(int position) {
            return mControlledItemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_main_item, null);
            }

            MainViewHolder holder = (MainViewHolder) convertView.getTag();
            if (holder == null) {
                holder = new MainViewHolder();
                holder.mLabelView = (TextView) convertView.findViewById(R.id.main_item_label);
                holder.mIconView = (ImageView) convertView.findViewById(R.id.main_item_icon);
                convertView.setTag(holder);
            }

            PackageInfo info = getItem(position);
            if (info != null && info.applicationInfo != null) {
                holder.mLabelView.setText(mPackageManager.getApplicationLabel(info.applicationInfo));
                holder.mIconView.setImageDrawable(Utils.getDrawable(info));
            }
            return convertView;
        }
    }

    class MainViewHolder {
        TextView mLabelView;
        ImageView mIconView;
    }
}
