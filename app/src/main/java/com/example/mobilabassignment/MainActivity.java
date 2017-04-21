package com.example.mobilabassignment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;

import static com.example.mobilabassignment.AppConstant.GallyImage_List;
import static com.example.mobilabassignment.AppConstant.Prefs_Flag_Section;
import static com.example.mobilabassignment.AppConstant.Prefs_Flag_Sort;
import static com.example.mobilabassignment.AppConstant.Prefs_Flag_View;
import static com.example.mobilabassignment.AppConstant.Prefs_Flag_Window;
import static com.example.mobilabassignment.AppConstant.Request_cleanCache;
import static com.example.mobilabassignment.AppConstant.Request_getGalleryInfo;
import static com.example.mobilabassignment.AppConstant.Request_loadImage_forManAct;
import static com.example.mobilabassignment.AppConstant.Response_getGalleryInfo;
import static com.example.mobilabassignment.AppConstant.Response_loadImage_forManAct;

public class MainActivity extends AppCompatActivity {
    private String LOG_TAG = this.getClass().getName();
    enum Section{hot,top,user_include_viral,user_exclude_viral};
    enum Window{day,week,month,year};
    enum Sort{viral,top,time,rising};

    private ArrayList<GalleryImage> list = new ArrayList<GalleryImage>();
    SharedPreferences sharedPreferences;
    Context mContext;
    RecyclerView recyclerView;
    ImageAdapter imgAdapter;
    ProgressDialog pDialog;
    Spinner sectionSP,windowSP,sortSP;
    boolean userIsInteracting = false;
    ImageLoadingManager imgLoaderHandler;
    UIHandler uiHandler;

    /*Setting, and default value*/
    final String[] optionSection = {Section.hot.name(),Section.top.name(),Section.user_include_viral.name(),
            Section.user_exclude_viral.name()};
    final String[] optionWindow = {Window.day.name(),Window.week.name(),Window.month.name(),
            Window.year.name()};
    final String[] optionSort = {Sort.viral.name(),Sort.top.name(),Sort.time.name(),Sort.rising.name()};
    int setting_view;//0: gridView; 1:listView; 2:staggeredGridView
    String setting_section, setting_window, setting_sort;
    Section default_section= Section.hot;
    Window default_window= Window.day;
    Sort default_sort = Sort.viral;
    LooperThread mylooper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getApplicationContext();
        sharedPreferences = getSharedPreferences(AppConstant.SharedPreferences_Name, MODE_PRIVATE);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        if(null==mylooper){
            mylooper = new LooperThread();
            mylooper.start();
        }
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView_id);
        sectionSP = (Spinner) findViewById(R.id.spinner_section);
        windowSP = (Spinner) findViewById(R.id.spinner_window);
        sortSP = (Spinner) findViewById(R.id.spinner_sort);

        setting_view = sharedPreferences.getInt(Prefs_Flag_View,0);
        setting_section = sharedPreferences.getString(Prefs_Flag_Section, default_section.name());
        setting_window = sharedPreferences.getString(Prefs_Flag_Window, default_window.name());
        setting_sort = sharedPreferences.getString(Prefs_Flag_Sort, default_sort.name());
        if(uiHandler==null) uiHandler = new UIHandler();
        if(imgLoaderHandler==null) imgLoaderHandler = ImageLoadingManager.getInstance(mContext);
        imgLoaderHandler.addUIHander(uiHandler);
        if(savedInstanceState==null) filterUpdate();
        else list = savedInstanceState.getParcelableArrayList(GallyImage_List);

        updateView();
        sectionSP.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                R.layout.simple_spinner_item, optionSection));
        windowSP.setAdapter(new ArrayAdapter<String>(MainActivity.this,
                R.layout.simple_spinner_item, optionWindow));
        ArrayAdapter<String> sortArrayAdapter = new ArrayAdapter<String>(this,
                R.layout.simple_spinner_item, optionSort) {
            @Override
            public boolean isEnabled(int position) {
                //Todo: It should look like unclickable.
                if(position==3) {
                    // sort rising couldn't be choose while section is not user.
                    if (Section.valueOf(setting_section).ordinal() >= 2) return true;
                    else return false;
                }
                return true;
            }
        };
        sortSP.setAdapter(sortArrayAdapter);
        sectionSP.setSelection(Section.valueOf(setting_section).ordinal());
        windowSP.setSelection(Window.valueOf(setting_window).ordinal());
        sortSP.setSelection(Sort.valueOf(setting_sort).ordinal());
        pDialog = new ProgressDialog(MainActivity.this);
        pDialog.setMessage("Loading...");
        pDialog.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(GallyImage_List,list);
    }

    @Override
    protected void onStart(){
        super.onStart();
        if(setting_section.equals(Section.top.name())) {
            windowSP.setClickable(true);
            windowSP.setEnabled(true);
        } else {
            windowSP.setClickable(false);
            windowSP.setEnabled(false);
        }
        sectionSP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                setting_section = optionSection[adapterView.getSelectedItemPosition()];
                if(userIsInteracting) filterUpdate();
                if(setting_section.equals(Section.top.name())) {
                    windowSP.setClickable(true);
                    windowSP.setEnabled(true);
                } else {
                    windowSP.setClickable(false);
                    windowSP.setEnabled(false);
                }
            }
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        windowSP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                setting_window = optionWindow[adapterView.getSelectedItemPosition()];
                if(userIsInteracting) filterUpdate();
            }
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        sortSP.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                setting_sort = optionSort[adapterView.getSelectedItemPosition()];
                if(userIsInteracting) filterUpdate();
            }
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    private void filterUpdate(){
        MyLog.d("filterUpdate()");
        if(pDialog!=null && !pDialog.isShowing()) pDialog.show();
        String[] obj = {setting_section,setting_window,setting_sort};
        // Use new setting as result, so remove perior pending event.
        imgLoaderHandler.getHandler().removeMessages(Request_loadImage_forManAct);
        imgLoaderHandler.getHandler().removeMessages(Request_getGalleryInfo);
        Message msg = Message.obtain(imgLoaderHandler.getHandler(), Request_getGalleryInfo,obj);
        msg.sendToTarget();
    }

    // Avoid initial spinner UI to listen a event.
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        MyLog.d("onUserInteraction");
        userIsInteracting = true;
    }

    @Override
    protected void onStop(){
        super.onStop();
        sharedPreferences.edit().putInt(Prefs_Flag_View,setting_view).commit();
        sharedPreferences.edit().putString(Prefs_Flag_Section, setting_section).commit();
        sharedPreferences.edit().putString(Prefs_Flag_Window, setting_window).commit();
        sharedPreferences.edit().putString(Prefs_Flag_Sort, setting_sort).commit();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(uiHandler!=null) imgLoaderHandler.removeUIHandler(uiHandler);
        //imgLoaderHandler.quit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.setting_displayed_view:
                switch(setting_view){
                    case 1://listView
                        item.getSubMenu().findItem(R.id.view_option_listView).setChecked(true);
                        return true;
                    case 2://staggeredGridView
                        item.getSubMenu().findItem(R.id.view_option_staggeredGridView).setChecked(true);
                        return true;
                    case 0://gridView
                    default:
                        item.getSubMenu().findItem(R.id.view_option_gridView).setChecked(true);
                        return true;
                }
            case R.id.view_option_gridView:
                setting_view=0;
                updateView();
                return true;
            case R.id.view_option_listView:
                setting_view=1;
                updateView();
                return true;
            case R.id.view_option_staggeredGridView:
                setting_view=2;
                updateView();
                return true;
            case R.id.clear_cache:
                imgLoaderHandler.getHandler().obtainMessage(Request_cleanCache).sendToTarget();
                return true;
            case R.id.about:
                AlertDialog.Builder aboutDialog = new AlertDialog.Builder(MainActivity.this,R.style.AboutDialogTheme);
                aboutDialog.setTitle(getResources().getString(R.string.about_title));
                aboutDialog.setMessage(getResources().getString(R.string.about_field_version)+AppConstant.app_version+"\n"
                        +getResources().getString(R.string.about_field_developed_time)+AppConstant.development_time+"\n"
                        +getResources().getString(R.string.about_field_author)+AppConstant.author_info);
                aboutDialog.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateView(){
        if(recyclerView==null) return;
        switch(setting_view){
            case 1://listView
                imgAdapter = new ImageAdapter(list,1);
                recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
                break;
            case 2://staggeredGridView
                imgAdapter = new ImageAdapter(list,2);
                recyclerView.setLayoutManager(new StaggeredGridLayoutManager(3,
                        StaggeredGridLayoutManager.VERTICAL));
                break;
            case 0://gridView
            default:
                imgAdapter = new ImageAdapter(list,0);
                recyclerView.setLayoutManager(new GridLayoutManager(MainActivity.this, 3));
                break;
        }
        recyclerView.setAdapter(imgAdapter);
    }

    class LooperThread extends Thread {
        public void run() {
            Looper.prepare();
            Looper.loop();
        }
    }

    class UIHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            MyLog.d("msg.what="+msg.what);
            if(pDialog.isShowing()) pDialog.dismiss();
            if(msg.obj==null) return;
            list = (ArrayList<GalleryImage>) msg.obj;
            switch (msg.what){
                case Response_getGalleryInfo:
                    updateView();
                    break;
                case Response_loadImage_forManAct:
                    imgAdapter.notifyDataSetChanged();
                    break;
                default: super.handleMessage(msg);
            }
        }
    }
}
