package com.project.stephencao.imageloader.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import com.project.stephencao.imageloader.R;
import com.project.stephencao.imageloader.util.MyImageLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImageAdapter extends BaseAdapter {
        private static Set<String> mSelectedImages = new HashSet<>();
        private List<String> mImageNames;
        private String mDirPath;
        private MyImageLoader mMyImageLoader;
        private LayoutInflater mInflater;


        public ImageAdapter(Context context, List<String> mData, String dirpath, MyImageLoader myImageLoader) {
            this.mImageNames = mData;
            this.mMyImageLoader = myImageLoader;
            this.mDirPath = dirpath;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mImageNames.size();
        }

        @Override
        public Object getItem(int position) {
            return mImageNames.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder viewHolder;
            if(convertView == null){
                convertView = mInflater.inflate(R.layout.item_gridview,parent,false);
                viewHolder = new ViewHolder();
                viewHolder.mImageView = convertView.findViewById(R.id.id_item_image);
                viewHolder.mImageButton = convertView.findViewById(R.id.id_item_select);
                convertView.setTag(viewHolder);
            }
            else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            //重置状态
            viewHolder.mImageView.setImageResource(R.drawable.picture_no);
            viewHolder.mImageButton.setImageResource(R.drawable.image_unselected);
            viewHolder.mImageView.setColorFilter(null);
            mMyImageLoader.loadImage(
                    mDirPath+"/"+mImageNames.get(position),viewHolder.mImageView);
            final String filePath = mDirPath + "/" + mImageNames.get(position);
            viewHolder.mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(mSelectedImages.contains(filePath)){
                        mSelectedImages.remove(filePath);
                        viewHolder.mImageButton.setImageResource(R.drawable.image_unselected);
                        viewHolder.mImageView.setColorFilter(null);

                    }else {
                        mSelectedImages.add(filePath);
                        viewHolder.mImageView.setColorFilter(Color.parseColor("#77000000"));
                        viewHolder.mImageButton.setImageResource(R.drawable.image_selected);
                    }
//                    notifyDataSetChanged();
                }
            });
            if(mSelectedImages.contains(filePath)){

            }
            return convertView;
        }

        private class ViewHolder{
            ImageView mImageView;
            ImageButton mImageButton;
        }
    }