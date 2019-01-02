package com.project.stephencao.imageloader.view;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import com.project.stephencao.imageloader.R;
import com.project.stephencao.imageloader.bean.FolderBean;
import com.project.stephencao.imageloader.util.MyImageLoader;

import java.util.List;

public class ListImagePopUpWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private OnDirectorySelectedListener mOnDirectorySelectedListener;
    private View mConvertView;
    private ListView mListView;
    private List<FolderBean> mData;
    private MyImageLoader mMyImageLoader;

    public ListImagePopUpWindow(Context context, List<FolderBean> mData, MyImageLoader myImageLoader) {
        this.mData = mData;
        this.mMyImageLoader = myImageLoader;
        getWidthAndHeight(context);
        mConvertView = LayoutInflater.from(context).inflate(R.layout.pop_up_main, null);
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        //点击外部可以消失
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initView(context);
        initEvent();
    }

    private void initEvent() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               if(mOnDirectorySelectedListener!=null){
                   mOnDirectorySelectedListener.onSelected(mData.get(position));
               }
            }
        });
    }
    public interface OnDirectorySelectedListener{
        void onSelected(FolderBean folderBean);
    }
    public void setOnDirectorySelectedListener(OnDirectorySelectedListener onDirectorySelectedListener){
        mOnDirectorySelectedListener = onDirectorySelectedListener;
    }

    private void initView(Context context) {
        mListView = mConvertView.findViewById(R.id.id_list_dir);
        DirectoryListAdapter directoryListAdapter = new DirectoryListAdapter(context,mData);
        mListView.setAdapter(directoryListAdapter);
    }

    //计算popup window 的宽高
    private void getWidthAndHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        mWidth = displayMetrics.widthPixels;
        mHeight = (int) (displayMetrics.heightPixels * 0.7);
    }

    private class DirectoryListAdapter extends ArrayAdapter<FolderBean> {
        private LayoutInflater mLayoutInflater;
        private List<FolderBean> mData;

        public DirectoryListAdapter(@NonNull Context context, @NonNull List<FolderBean> objects) {
            super(context, 0, objects);
            mLayoutInflater = LayoutInflater.from(context);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.item_pop_up_main, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.mImage = convertView.findViewById(R.id.id_dir_item_image);
                viewHolder.mDirName = convertView.findViewById(R.id.id_dir_item_name);
                viewHolder.mDirCount = convertView.findViewById(R.id.id_dir_item_count);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            FolderBean folderBean = getItem(position);
            viewHolder.mImage.setImageResource(R.drawable.picture_no);
            mMyImageLoader.loadImage(folderBean.getFirstImagePath(), viewHolder.mImage);
            viewHolder.mDirName.setText(folderBean.getDirectoryName());
            viewHolder.mDirCount.setText(folderBean.getImageCount() + "");

            return convertView;
        }

        private class ViewHolder {
            ImageView mImage;
            TextView mDirName, mDirCount;
        }
    }
}
