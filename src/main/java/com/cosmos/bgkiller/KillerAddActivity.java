package com.cosmos.bgkiller;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import com.cosmos.bgkiller.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by cosmos on 2016/8/28.
 */

public class KillerAddActivity extends Activity {

    private ListView mSystemAppsView;
    private ListView mNormalAppsView;

    private PackageManager mPackageManager;

    private static final int TYPE_SYSTEM = 1;
    private static final int TYPE_NORMAL = 0;

    private static final int TYPE_AUTO= 1 << 1;

    private int mType = TYPE_NORMAL;

    private ArrayList<String> mOriginManualDisableApps;
    private ArrayList<String> mOriginAutoDisableApps;

    private Set<String> mManualDisabledApps = new HashSet<>();
    private Set<String> mAutoDisabledApps = new HashSet<>();

    private AddAdapter mSystemAdapter;
    private AddAdapter mNormalAdapter;

    private String mQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_killer_add);
        Utils.getTypedStringSet(Utils.KEY_AUTO_ENABLE_DISABLED_USER_APPS, mAutoDisabledApps);
        Utils.getTypedStringSet(Utils.KEY_NORMAL_DISABLED_USER_APPS, mManualDisabledApps);
        mOriginManualDisableApps = new ArrayList<>(mManualDisabledApps);
        mOriginAutoDisableApps = new ArrayList<>(mAutoDisabledApps);


        mPackageManager = getPackageManager();
        mNormalAppsView = (ListView) findViewById(R.id.activity_add_package_list);
        mNormalAppsView.setEmptyView(findViewById(R.id.empty_view1));
        mSystemAppsView = (ListView) findViewById(R.id.activity_add_package_list_system);
        mSystemAppsView.setEmptyView(findViewById(R.id.empty_view2));
        mSystemAdapter = new AddAdapter(Utils.getSystemPackageList());
        mSystemAppsView.setAdapter(mSystemAdapter);

        mNormalAdapter = new AddAdapter(Utils.getUserInstalledPackageList());
        mNormalAppsView.setAdapter(mNormalAdapter);

        AbsListView.OnItemClickListener listener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PackageInfo info = ((PackageInfo)parent.getAdapter().getItem(position));
                if(info != null && info.applicationInfo != null){
                    AddViewHolder holder = (AddViewHolder) view.getTag();
                    boolean isSelected = holder.mSelect.isChecked();
                    holder.mSelect.setChecked(!isSelected);
                    boolean isAuto = isAuto();
                    if(isSelected){
                        (isAuto ? mAutoDisabledApps : mManualDisabledApps).remove(info.packageName);
                    }else{
                        (isAuto ? mAutoDisabledApps : mManualDisabledApps).add(info.packageName);
                    }
                }
                notifyDataSetChange();
            }

        };

        mNormalAppsView.setOnItemClickListener(listener);
        mSystemAppsView.setOnItemClickListener(listener);
        refreshUI();
    }

    public void refreshUI(){
        boolean isSystem = isSystem();
        if(isSystem){
            ((View)mNormalAppsView.getParent()).setVisibility(View.GONE);
            ((View)mSystemAppsView.getParent()).setVisibility(View.VISIBLE);
        }else{
            ((View)mSystemAppsView.getParent()).setVisibility(View.GONE);
            ((View)mNormalAppsView.getParent()).setVisibility(View.VISIBLE);
        }
        notifyDataSetChange();
    }

    private void notifyDataSetChange(){
        mSystemAdapter.notifyDataSetChanged();
        mNormalAdapter.notifyDataSetChanged();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_add_menu, menu);
        boolean isSystem = isSystem();
        menu.findItem(R.id.menu_system).setTitle(getString(!isSystem ? R.string.menu_system : R.string.menu_normal));
        ((SearchView)menu.findItem(R.id.menu_search).getActionView()).setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mNormalAdapter.setQuery(newText);
                mSystemAdapter.setQuery(newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_system:
                mSystemAdapter.changeToSelected(false);
                mNormalAdapter.changeToSelected(false);
                boolean isSystem = isSystem();
                if(isSystem){
                    item.setTitle(getString(R.string.menu_system));
                    mType &= (~TYPE_SYSTEM);
                }else{
                    item.setTitle(getString(R.string.menu_normal));
                    mType |= (TYPE_SYSTEM);
                }
                refreshUI();
                break;
            case R.id.menu_search:
                item.collapseActionView();
                break;
            case R.id.menu_selected:
                mSystemAdapter.changeToSelected(true);
                mNormalAdapter.changeToSelected(true);
                notifyDataSetChange();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isAuto(){
        return (mType & TYPE_AUTO) != 0;
    }

    private boolean isSystem(){
        return (mType & TYPE_SYSTEM) != 0;
    }

    class AddAdapter extends BaseAdapter{

        private String mQuery;
        List<PackageInfo> mFilteredPackageList = new ArrayList<>();
        private List<PackageInfo> mPackageList;
        private List<PackageInfo> mPackageListTemp;
        boolean mIsSelected = false;
        public void setQuery(String query){
            if(!TextUtils.equals(mQuery, query)){
                mQuery = query;
                reSyncList();
                notifyDataSetChanged();
            }
        }

        public AddAdapter(List<PackageInfo> data){
            mPackageList = data;
            if(mPackageList == null){
                mPackageList = new ArrayList<>();
            }
            mPackageListTemp = mPackageList;
            reSyncList();
        }

        private void reSyncList(){
            if(mIsSelected){
                mPackageList = new ArrayList<>();
                Map<String, PackageInfo> allPackagesList = BgKillerApplication.getsInstance().getAllPackageInfo();
                for(String pname : mAutoDisabledApps){
                    mPackageList.add(allPackagesList.get(pname));
                }
                for(String pname : mManualDisabledApps){
                    mPackageList.add(allPackagesList.get(pname));
                }
            }else{
                mPackageList = mPackageListTemp;
            }

            mFilteredPackageList.clear();
            String query = mQuery == null ? "" : mQuery.replace(" ", "").toLowerCase();
            if(TextUtils.isEmpty(query)){
                mFilteredPackageList.addAll(mPackageList);
            }else {
                for(PackageInfo info : mPackageList){
                    if(info != null && info.packageName != null && info.packageName.contains(query)){
                        mFilteredPackageList.add(info);
                    }
                }
            }
        }


        @Override
        public int getCount() {
            return mFilteredPackageList.size();
        }

        @Override
        public PackageInfo getItem(int position) {
            return mFilteredPackageList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.layout_add_item, null);
            }
            AddViewHolder holder = (AddViewHolder) convertView.getTag();
            if (holder == null) {
                holder = new AddViewHolder();
                holder.mIcon = (ImageView) convertView.findViewById(R.id.add_item_icon);
                holder.mLabel = (TextView) convertView.findViewById(R.id.add_item_label);
                holder.mPackageName = (TextView) convertView.findViewById(R.id.add_item_packagename);
                holder.mSelect = (CheckBox) convertView.findViewById(R.id.add_item_select);
                holder.mSpinner = (Spinner) convertView.findViewById(R.id.add_item_enable_types);
                convertView.setTag(holder);
                holder.mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String packageName = (String) parent.getTag(R.id.add_item_enable_types);
                        processSelectionChange(position, packageName);
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });
            }
            PackageInfo info = getItem(position);
            if(info != null && info.applicationInfo != null){
                holder.mIcon.setImageDrawable(Utils.getDrawable(info));
                holder.mLabel.setText(mPackageManager.getApplicationLabel(info.applicationInfo));
                holder.mPackageName.setText(info.packageName);
                boolean isAuto = isAuto();
                Collection<String> cl = isAuto ? mAutoDisabledApps : mManualDisabledApps;
                holder.mSelect.setChecked(cl.contains(info.packageName));
                holder.mSpinner.setSelection(findSelection(info.packageName), true);
                holder.mSpinner.setTag(R.id.add_item_enable_types, info.packageName);
            }
            return convertView;
        }

        public void changeToSelected(boolean selected) {
            if(mIsSelected == selected){
                return;
            }
            mIsSelected = selected;
            reSyncList();
        }
    }

    private static final int SELECTION_NONE = 0;
    private static final int SELECTION_MANUAL = 1;
    private static final int SELECTION_AUTO = 2;

    private int findSelection(String packageName){
        int selection = SELECTION_NONE;
        if(!TextUtils.isEmpty(packageName)){
            if(mAutoDisabledApps.contains(packageName)){
                selection = SELECTION_AUTO;
            }else if(mManualDisabledApps.contains(packageName)){
                selection = SELECTION_MANUAL;
            }
        }
        return selection;
    }

    private void processSelectionChange(int selection, String packageName){
        switch (selection){
            case SELECTION_MANUAL:
                mAutoDisabledApps.remove(packageName);
                mManualDisabledApps.add(packageName);
                break;
            case SELECTION_AUTO:
                mManualDisabledApps.remove(packageName);
                mAutoDisabledApps.add(packageName);
                break;
            default:
                mManualDisabledApps.remove(packageName);
                mAutoDisabledApps.remove(packageName);
                break;
        }
        notifyDataSetChange();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mOriginManualDisableApps.removeAll(mManualDisabledApps);

        mOriginAutoDisableApps.removeAll(mAutoDisabledApps);
        mOriginManualDisableApps.addAll(mAutoDisabledApps);

        KillerCore.queueKiller(this,new ArrayList<>(mManualDisabledApps), mOriginManualDisableApps , 0);
        Utils.putTypedStringSet(Utils.KEY_AUTO_ENABLE_DISABLED_USER_APPS, mAutoDisabledApps);
        Utils.putTypedStringSet(Utils.KEY_NORMAL_DISABLED_USER_APPS, mManualDisabledApps);

        mOriginManualDisableApps = new ArrayList<>(mManualDisabledApps);
        mOriginAutoDisableApps = new ArrayList<>(mAutoDisabledApps);
    }

    class AddViewHolder {
        ImageView mIcon;
        TextView mLabel;
        TextView mPackageName;
        CheckBox mSelect;
        Spinner mSpinner;
    }
}
