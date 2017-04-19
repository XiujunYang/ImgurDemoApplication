package com.example.mobilabassignment;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

import static com.example.mobilabassignment.AppConstant.Intent_action_displayDetail;
import static com.example.mobilabassignment.AppConstant.flag_galleryImage;

/**
 * Created by Jean on 2017/4/14.
 */

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder>{
    ArrayList<GalleryImage> gallery_list;
    int itemLayout;
    Context context;
    ImageAdapter(ArrayList<GalleryImage> gallery_list,int item_layout){
        this.gallery_list = gallery_list;
        this.itemLayout = item_layout;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder holder;
        context = parent.getContext();
        if(itemLayout==1)
            holder = new ViewHolder(
                    LayoutInflater.from(context).inflate(R.layout.item_horizontal_layout, null));
        else holder = new ViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_vertical_layout, null));
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.imageView.setImageBitmap(gallery_list.get(position).getImgBitmap());
        holder.imageText.setText(gallery_list.get(position).getTitle());
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(context, DetailedImageActivity.class);
                intent.setAction(Intent_action_displayDetail);
                intent.putExtra(flag_galleryImage, gallery_list.get(position));
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return gallery_list.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder{
        ImageButton imageView;
        TextView imageText;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageButton) itemView.findViewById(R.id.gridViewImgId);
            imageText = (TextView) itemView.findViewById(R.id.gridViewTextId);
        }
    }
}
