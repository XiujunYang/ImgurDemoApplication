package com.example.mobilabassignment;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import static com.example.mobilabassignment.AppConstant.Request_checkWholeList_bitmapExisted;
import static com.example.mobilabassignment.AppConstant.Request_getGalleryInfo;
import static com.example.mobilabassignment.AppConstant.Request_loadImage_forDetAct;
import static com.example.mobilabassignment.AppConstant.Request_loadImage_forManAct;
import static com.example.mobilabassignment.AppConstant.Response_loadImage_forDetAct;

public class DetailedImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_image);
        Intent intent = getIntent();
        Resources rcs = getResources();
        if(intent.getAction().equals(AppConstant.Intent_action_displayDetail)){
            ImageLoaderHandler loadHandler = ImageLoaderHandler.getInstance();
            UIHandler uihandler = new UIHandler();
            loadHandler.addUIHander(uihandler);
            GalleryImage item = intent.getParcelableExtra(AppConstant.flag_galleryImage);
            String[] obj = {item.getLink(),item.getCacheKey()};
            // Make loading bitmap run on first priority, after that, check if other image wasn't loaded on MainActivity.
            loadHandler.getHandler().removeMessages(Request_loadImage_forManAct);
            loadHandler.getHandler().removeMessages(Request_getGalleryInfo);
            loadHandler.getHandler().obtainMessage(Request_loadImage_forDetAct,obj).sendToTarget();
            loadHandler.getHandler().obtainMessage(Request_checkWholeList_bitmapExisted).sendToTarget();
            TextView title = (TextView) findViewById(R.id.detail_title_id);
            TextView description = (TextView) findViewById(R.id.detail_description_id);
            TextView score = (TextView) findViewById(R.id.detail_score_id);
            TextView ups = (TextView) findViewById(R.id.detail_upvotes_id);
            TextView downs = (TextView) findViewById(R.id.detail_downvotes_id);
            try {
                // Todo: bitmap could not trasfer by GalleryImage together? App will auto exit..
                title.setText(rcs.getString(R.string.field_title)+item.getTitle());
                description.setText(rcs.getString(R.string.field_description)+item.getDescription());
                score.setText(rcs.getString(R.string.field_score)+item.getScore());
                ups.setText(rcs.getString(R.string.field_upvotes)+item.getUpvotes());
                downs.setText(rcs.getString(R.string.field_downvotes)+item.getDownvotes());
            }catch(NullPointerException e){
                MyLog.d(e.getMessage());
            }
        }
    }

    class UIHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            MyLog.i("msg.what="+msg.what);
            switch (msg.what){
                case Response_loadImage_forDetAct:
                    if(msg.obj==null) break;
                    ImageView image = (ImageView) findViewById(R.id.detail_image_id);
                    image.setImageBitmap((Bitmap) msg.obj);
                    break;
                default: super.handleMessage(msg);
            }
        }
    }
}
