package com.example.mobilabassignment;

/**
 * Created by Jean on 2017/4/13.
 */

public class AppConstant {
    static final double app_version = 1.0;
    static final String development_time="April 2017";
    static final String author_info = "XiuJun Yang(proverb70@gmail.com)";

    static final int HTTP_StatusCodes_OK = 200;

    /* Support to load Bitmap from Dish cache or Server.*/
    static final int Request_getGalleryInfo = 1000;
    static final int Response_getGalleryInfo = 1001;
    static final int Request_checkWholeList_bitmapExisted = 1002;
    static final int Request_loadImage_forManAct = 1003;
    static final int Response_loadImage_forManAct = 1004;
    static final int Request_loadImage_forDetAct=1005;
    static final int Response_loadImage_forDetAct = 1006;
    static final int Request_InitDiskCacheDir = 2000;
    static final int Request_cleanCache = 2001;
    static final String Intent_action_displayDetail = "intent.action.showDetail";
    static final String flag_galleryImage = "galleryImage";

    /*For SharedPreferences*/
    public static final String SharedPreferences_Name = "SettingConfig";
    static final String Prefs_Flag_View = "prefs_view";//0: gridView; 1:listView; 2:staggeredGridView
    static final String Prefs_Flag_Section = "prefs_section";
    static final String Prefs_Flag_Window = "prefs_window";
    static final String Prefs_Flag_Sort = "prefs_sort";
    /* The flag use to onSaveInstanceState() on list, it's for rotation*/
    static final String GallyImage_List = "gallyImage_list";

    /*Imgur basic info*/
    static final String base_url = "https://api.imgur.com/3/gallery/";
    static final String imgur_app_auth_flag = "Authorization";
    static final String imgur_app_clientId_content = "Client-ID eef5fec2cf0d376";
    static final String imgur_app_userAgent_flag = "User-Agent";
    static final String imgur_app_userAgent_content = "MobiLabAssignment";
    static final int url_page = 0;
    static final String JSON_key_data="data";
    static final String JSON_key_type="type";
    static final String JSON_key_id="id";
    static final String JSON_key_title="title";
    static final String JSON_key_description="description";
    static final String JSON_key_link="link";
    static final String JSON_key_score="score";
    static final String JSON_key_ups="ups";
    static final String JSON_key_downs="downs";

    /*Disk cache's folder name*/
    static final String cache_fileName = "bitmap";
}
