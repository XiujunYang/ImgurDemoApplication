package com.example.mobilabassignment;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Jean on 2017/4/12.
 */

public class GalleryImage implements Parcelable{
    private String id, title, description, link, cacheKey;
    private int score, upvotes, downvotes;
    private Bitmap imgBitmap=null;

    public GalleryImage(String id, String title, String descript, String link, String cacheKey,
                        int score, int ups, int downs){
        this.id = id;
        this.title = title;
        this.description = descript;
        this.link = link;
        this.cacheKey = cacheKey;
        this.score = score;
        this.upvotes = ups;
        this.downvotes = downs;
    }

    public String getDescription(){return this.description;}
    public String getLink(){return this.link;}
    public void setCacheKey(String value){this.cacheKey=value;}
    public String getCacheKey(){return this.cacheKey;}
    public String getTitle(){return this.title;}
    public int getScore(){return this.score;}
    public int getUpvotes(){return this.upvotes;}
    public int getDownvotes(){return this.downvotes;}
    public void setImgBitmap(Bitmap bitmap){this.imgBitmap=bitmap;}
    public Bitmap getImgBitmap(){return this.imgBitmap;}


    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.title);
        dest.writeString(this.description);
        dest.writeString(this.link);
        dest.writeString(this.cacheKey);
        dest.writeInt(this.score);
        dest.writeInt(this.upvotes);
        dest.writeInt(this.downvotes);
        //dest.writeParcelable(this.imgBitmap,flags);
    }

    public static final Parcelable.Creator<GalleryImage> CREATOR = new Parcelable.Creator<GalleryImage>() {
        @Override
        public GalleryImage createFromParcel(Parcel source) {
            String id= source.readString();
            String title = source.readString();
            String descript = source.readString();
            String link = source.readString();
            String key = source.readString();
            int score = source.readInt();
            int up = source.readInt();
            int down = source.readInt();
            //Bitmap bitmap = (Bitmap) source.readParcelable(Bitmap.class.getClassLoader());
            return new GalleryImage(id,title,descript,link,key,score,up,down);
        }

        @Override
        public GalleryImage[] newArray(int size) {
            return new GalleryImage[size];
        }
    };
}
