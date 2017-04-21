package com.example.mobilabassignment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.widget.Toast;

import com.jakewharton.disklrucache.DiskLruCache;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

import static com.example.mobilabassignment.AppConstant.HTTP_StatusCodes_OK;
import static com.example.mobilabassignment.AppConstant.JSON_key_data;
import static com.example.mobilabassignment.AppConstant.JSON_key_description;
import static com.example.mobilabassignment.AppConstant.JSON_key_downs;
import static com.example.mobilabassignment.AppConstant.JSON_key_id;
import static com.example.mobilabassignment.AppConstant.JSON_key_link;
import static com.example.mobilabassignment.AppConstant.JSON_key_score;
import static com.example.mobilabassignment.AppConstant.JSON_key_title;
import static com.example.mobilabassignment.AppConstant.JSON_key_type;
import static com.example.mobilabassignment.AppConstant.JSON_key_ups;
import static com.example.mobilabassignment.AppConstant.Request_InitDiskCacheDir;
import static com.example.mobilabassignment.AppConstant.Request_checkWholeList_bitmapExisted;
import static com.example.mobilabassignment.AppConstant.Request_cleanCache;
import static com.example.mobilabassignment.AppConstant.Request_getGalleryInfo;
import static com.example.mobilabassignment.AppConstant.Request_loadImage_forDetAct;
import static com.example.mobilabassignment.AppConstant.Request_loadImage_forManAct;
import static com.example.mobilabassignment.AppConstant.Response_getGalleryInfo;
import static com.example.mobilabassignment.AppConstant.Response_loadImage_forDetAct;
import static com.example.mobilabassignment.AppConstant.Response_loadImage_forManAct;
import static com.example.mobilabassignment.AppConstant.base_url;
import static com.example.mobilabassignment.AppConstant.cache_fileName;
import static com.example.mobilabassignment.AppConstant.imgur_app_auth_flag;
import static com.example.mobilabassignment.AppConstant.imgur_app_clientId_content;
import static com.example.mobilabassignment.AppConstant.imgur_app_userAgent_content;
import static com.example.mobilabassignment.AppConstant.imgur_app_userAgent_flag;
import static com.example.mobilabassignment.AppConstant.url_page;

/**
 * Created by Jean on 2017/4/19.
 */

public class ImageLoadingManager {
    //Gets the number of available cores (not always the same as the maximum number of cores)
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    // A queue of Runnables
    ThreadPoolExecutor mThreadPool;
    BlockingQueue<Runnable> mWorkQueue;

    private Context mContext;
    private static ImageLoadingManager instance;
    private HandlerThread handlerThread;
    private Handler bgdHander;
    HashMap<Integer,Handler> uiHandlers = new HashMap<Integer,Handler>();//UI thread
    ArrayList<GalleryImage> image_list = new ArrayList<GalleryImage>();
    DiskLruCache mDiskLruCache = null;
    String filter_section,filter_window,filter_sort;
    NetworkChangedReceiver networkChangedReceiver = new NetworkChangedReceiver();
    IntentFilter networkFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

    public static ImageLoadingManager getInstance(Context context){
        if(instance==null) instance = new ImageLoadingManager(context);
        return instance;
    }

    public static ImageLoadingManager getInstance() {return instance;}

    public ImageLoadingManager(Context context){
        mContext = context;
        initalBgdThread();
        bgdHander.obtainMessage(Request_InitDiskCacheDir).sendToTarget();
    }

    public void addUIHander(Handler handler){
        //0 is used for MainActivity.UIHandler; i is for used for DetailedImageActivity.
        if(handler instanceof MainActivity.UIHandler) uiHandlers.put(0,handler);
        else if(handler instanceof DetailedImageActivity.UIHandler) uiHandlers.put(1,handler);
    }

    public void removeUIHandler(Handler handler){
        if(handler instanceof MainActivity.UIHandler) uiHandlers.remove(0);
        else if(handler instanceof DetailedImageActivity.UIHandler) uiHandlers.remove(1);
    }

    public Handler getHandler(){return bgdHander;}

    // Release resource
    public void quit(){
        if(handlerThread!=null) handlerThread.quit();
    }

    private void initalBgdThread(){
        handlerThread = new HandlerThread("ImageLoader");
        handlerThread.start();
        bgdHander = new Handler(handlerThread.getLooper()){
            @Override
            public void handleMessage(Message msg){
                MyLog.d("msg.what="+msg.what);
                switch (msg.what){
                    case Request_InitDiskCacheDir:
                        initalDiskCacheDir();
                        break;
                    case Request_getGalleryInfo:
                        int page=0;
                        Object obj= msg.obj;
                        if(obj==null||uiHandlers.get(0)==null) break;
                        if(msg.arg1==0){//msg.arg1 is page, default is 0.
                            image_list.clear();
                            filter_section = ((String[])obj)[0];
                            filter_window = ((String[])obj)[1];
                            filter_sort = ((String[])obj)[2];
                            if(mWorkQueue!=null) mWorkQueue.clear();
                            if(mThreadPool!=null) mThreadPool.shutdownNow();
                        } else if(msg.arg1>0 && msg.arg1<1)
                            page =msg.arg1;
                        else page = msg.arg1;

                        // Todo: It could use endless list by listen screen end to bottom.
                        if(getGallyInfo(filter_section,filter_window,filter_sort,page)) {
                            if(image_list.size()<=0) break;
                            if(uiHandlers.get(0)!=null)
                                uiHandlers.get(0).obtainMessage(Response_getGalleryInfo, image_list).sendToTarget();
                            obtainMessage(Request_loadImage_forManAct).sendToTarget();
                        } else
                            if(uiHandlers.get(0)!=null)
                                uiHandlers.get(0).obtainMessage(Response_getGalleryInfo).sendToTarget();

                        if(msg.arg1<1){
                            String[] params = {filter_section,filter_window,filter_sort};
                            obtainMessage(Request_getGalleryInfo, page+1, -1, params).sendToTarget();
                        }
                        break;
                    case Request_checkWholeList_bitmapExisted:
                        obtainMessage(Request_loadImage_forManAct).sendToTarget();
                        break;
                    case Request_loadImage_forManAct:
                        //Only loading image use muti-thread and run in parallel. Its advantage is efficient in image loading,
                        // and also avoid race condition with Request_getGalleryInfo.
                        mWorkQueue = new LinkedBlockingQueue<Runnable>();
                        mThreadPool = new ThreadPoolExecutor(
                                NUMBER_OF_CORES,       // Initial pool size
                                NUMBER_OF_CORES,       // Max pool size
                                KEEP_ALIVE_TIME,
                                KEEP_ALIVE_TIME_UNIT,
                                mWorkQueue);
                        /*Prevent task is pending in queue. Here make will let threadPool create core thread first.
                                            The first task received while threadPool is initialing threads and no thread could operate task.
                                            This first task will be operated util threadPool is ready and there's second task received*/
                        //mThreadPool.prestartAllCoreThreads();
                        for (Iterator it = image_list.iterator(); it.hasNext();) {
                            GalleryImage item = (GalleryImage) it.next();
                            if (item.getImgBitmap()==null) {
                                mThreadPool.execute(new LoadBitmapTask(item));
                                MyLog.i("mThreadPool.task="+ mThreadPool.getTaskCount());
                            }
                        }
                        try{
                            mThreadPool.awaitTermination(10, TimeUnit.SECONDS);
                            mThreadPool.shutdown();
                        }catch (InterruptedException ie){
                            MyLog.e("InterruptedException: "+ ie.getMessage());
                        }
                        break;
                    case Request_loadImage_forDetAct:
                        if(mWorkQueue!=null) mWorkQueue.clear();
                        if(mThreadPool!=null) mThreadPool.shutdownNow();
                        if(msg.obj==null||uiHandlers.get(1)==null) break;
                        Bitmap b = loadImgBitmap(((String[])msg.obj)[0],((String[])msg.obj)[1]);
                        if(b==null) break;
                        if(uiHandlers.get(1)!=null)uiHandlers.get(1).obtainMessage(Response_loadImage_forDetAct,b).sendToTarget();
                        break;
                    case Request_cleanCache:
                        cleanDiskCache();
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        };
    }

    //Callable thread
    class LoadBitmapTask implements Runnable {
        GalleryImage item;
        public LoadBitmapTask(GalleryImage item){
            this.item = item;
        }
        @Override
        public void run() {
            loadImgBitmap(item);
            if(uiHandlers.get(0)!=null)
                uiHandlers.get(0).obtainMessage(Response_loadImage_forManAct, image_list).sendToTarget();
        }
    }

    private void initalDiskCacheDir(){
        try {
            File cacheDir = getDiskCacheDir();
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            mDiskLruCache = DiskLruCache.open(cacheDir, getAppVersion(), 1, 10*1024*1024);//Max size:10MB
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getDiskCacheDir() {
        String cachePath;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            cachePath = mContext.getExternalCacheDir().getPath();
        } else {
            cachePath = mContext.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + cache_fileName);
    }

    private int getAppVersion() {
        try {
            PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    private boolean getGallyInfo(String section, String window, String sort, int page){
        MyLog.d("getGallyInfo()");
        if(section==null || window==null || sort==null) return false;
        if(isNetworkAvailable()) {
            HttpClient httpClient = new DefaultHttpClient();
            JSONArray jsonArr = null;
            JSONObject jsonObj = null;
            try {
                HttpGet httpGet = new HttpGet(getUrlStrForGalleryInfo(section, window, sort, page));
                httpGet.addHeader(imgur_app_auth_flag, imgur_app_clientId_content);
                httpGet.addHeader(imgur_app_userAgent_flag, imgur_app_userAgent_content);
                HttpResponse httpResponse = httpClient.execute(httpGet);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode == HTTP_StatusCodes_OK) {
                    InputStream inputStream = httpResponse.getEntity().getContent();
                    String result = convertInputStreamToString(inputStream);
                    if (result != null && result.length() > 0) {
                        try {
                            jsonObj = new JSONObject(result);
                            jsonArr = jsonObj.getJSONArray(JSON_key_data);
                            MyLog.d("GetHttp Data="+result);
                            for (int i = 0; i < jsonArr.length(); i++) {
                                jsonObj = jsonArr.getJSONObject(i);
                                // Check if it's gallery image
                                if (!jsonObj.has(JSON_key_type)) continue;
                                GalleryImage item = new GalleryImage(jsonObj.getString(JSON_key_id),
                                        jsonObj.getString(JSON_key_title),
                                        jsonObj.getString(JSON_key_description),
                                        jsonObj.getString(JSON_key_link), null,
                                        jsonObj.getInt(JSON_key_score),
                                        jsonObj.getInt(JSON_key_ups),
                                        jsonObj.getInt(JSON_key_downs));
                                item.setCacheKey(convertHashKeyForCache(item.getLink()));
                                image_list.add(item);
                            }
                            MyLog.d("image_list size=" + image_list.size());
                            return true;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                } else MyLog.e("Get error statusCode:" + statusCode);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                httpClient.getConnectionManager().shutdown();
            }
        }
        return false;
    }

    private String getUrlStrForGalleryInfo(String section, String window, String sort, int page){
        String sectionFilter=null;
        if(section.startsWith("user")) sectionFilter = "user";
        StringBuilder sb = new StringBuilder();
        sb.append(base_url).append(sectionFilter!=null?sectionFilter:section).append("/").append(sort);
        if(section.equals(MainActivity.Section.top.name())) sb.append("/").append(window);
        sb.append("/").append(url_page+page);
        if(section.equals(MainActivity.Section.user_include_viral.name())) sb.append("?showViral=true");
        else if(section.equals(MainActivity.Section.user_exclude_viral.name())) sb.append("?showViral=false");
        MyLog.i("getUrlStrForGalleryInfo() urlStr:"+sb.toString());
        return sb.toString();
    }

    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        if(inputStream == null) return null;
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "", result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;
        inputStream.close();
        bufferedReader.close();
        return result;
    }

    private String convertHashKeyForCache(String urlLink) { //use link as key
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(urlLink.getBytes());
            // Transfer bytes to Hex String
            StringBuilder sb = new StringBuilder();
            byte[] bytes = mDigest.digest();
            for (int i = 0; i < bytes.length; i++) {
                String hex = Integer.toHexString(0xFF & bytes[i]);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            cacheKey = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(urlLink.hashCode());
        }
        return cacheKey;
    }

    // Allow DetailedImageActivity to load image from cache in UI Thread.
    public Bitmap loadImgBitmapFromCache(String link, String key){
        if(new File(getDiskCacheDir(),key+".0").exists()){//Todo: there include extension name .0
            return readImgFromCache(key);
        }
        return null;
    }

    private boolean loadImgBitmap(GalleryImage item){
        if(item==null) return false;
        String link = item.getLink();
        String key = item.getCacheKey();
        Bitmap bitmap = loadImgBitmap(link,key);
        if(bitmap!=null) {
            item.setImgBitmap(bitmap);
            return true;
        }
        return false;
    }

    private Bitmap loadImgBitmap(String link, String key){
        if(key==null) return null;
        Bitmap bitmap;
        if(new File(getDiskCacheDir(),key+".0").exists()){//Todo: there include extension name .0
            bitmap = readImgFromCache(key);
        } else{
            bitmap = getBitmapFromURL(link);
            if(bitmap!=null) wirteBitmapToDistCache(key, bitmap);
        }
        return bitmap;
    }

    private Bitmap readImgFromCache(String key){
        if(null==key || key.length()==0) return null;
        MyLog.i("readImgFromCache="+key);
        Bitmap bitmap = null;
        try {
            DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
            if (snapShot != null) {
                InputStream is = snapShot.getInputStream(0);
                bitmap = BitmapFactory.decodeStream(is);
            }
        }catch(IOException ioe){
            ioe.printStackTrace();

        }
        return bitmap;
    }

    private Bitmap getBitmapFromURL(String src) {
        if(null==src || src.length()==0) return null;
        MyLog.i("getBitmapFromURL="+src);
        HttpURLConnection connection=null;
        try {
            URL url = new URL(src);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            MyLog.e(e.getMessage());
            return null;
        } finally {
            if(connection!=null) connection.disconnect();
        }
    }

    private void wirteBitmapToDistCache(String key, Bitmap bitmap){
        try {
            DiskLruCache.Editor editor = mDiskLruCache.edit(key);
            if (editor != null) {
                OutputStream outputStream = editor.newOutputStream(0);
                if (bitmap.compress(Bitmap.CompressFormat.JPEG,50, outputStream)) {
                    editor.commit();
                } else {
                    editor.abort();
                }
            }
            mDiskLruCache.flush();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void cleanDiskCache(){
        try {
            mDiskLruCache.delete();//delete() will close DiskLruChe
            initalDiskCacheDir();
        }catch (IOException e){
            e.printStackTrace();
        }
        Toast.makeText(mContext, mContext.getResources().getText(R.string.notification_cache_cleaned)
                , Toast.LENGTH_SHORT).show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if(activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            try {
                if (mContext != null) mContext.unregisterReceiver(networkChangedReceiver);
            }catch (IllegalArgumentException e){}
            return true;
        }
        else {
            if(mContext!=null) mContext.registerReceiver(networkChangedReceiver,networkFilter);
            Toast.makeText(mContext, mContext.getResources().getText(R.string.notify_network_unavailable)
                    , Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public class NetworkChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            MyLog.i("onReceive: "+intent.getAction());
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
                if(isNetworkAvailable()) {
                    String[] obj = {filter_section, filter_window, filter_sort};
                    Message.obtain(bgdHander, Request_getGalleryInfo, obj).sendToTarget();
                }
            }
        }
    }

}
