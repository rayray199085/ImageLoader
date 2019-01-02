package com.project.stephencao.imageloader.activity;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.project.stephencao.imageloader.R;
import com.project.stephencao.imageloader.adapter.ImageAdapter;
import com.project.stephencao.imageloader.bean.FolderBean;
import com.project.stephencao.imageloader.util.MyImageLoader;
import com.project.stephencao.imageloader.view.ListImagePopUpWindow;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private GridView mGridView;
    private ImageAdapter mImageAdapter;
    private List<String> mImages;
    private ListImagePopUpWindow mListImagePopUpWindow;
    private RelativeLayout mBottomLayout;
    private TextView mDirectoryName, mDirectoryCount;
    private File mCurrentDirectory;
    private int mMaxCount;
    private MyImageLoader mMyImageLoader;
    private static final int LOAD_DATA = 0x110;
    private List<FolderBean> mFolderBeans = new ArrayList<>();
    private ProgressDialog mProgressDialog;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LOAD_DATA: {
                    mProgressDialog.dismiss();
                    //绑定数据
                    mMyImageLoader = MyImageLoader.getInstance(3, MyImageLoader.Type.LIFO);
                    data2View();
                    initPopupWindow();
                    break;
                }
            }
        }
    };

    private void initPopupWindow() {
        mListImagePopUpWindow = new ListImagePopUpWindow(this, mFolderBeans, mMyImageLoader);
        mListImagePopUpWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                lightOn();
            }
        });
        mListImagePopUpWindow.setOnDirectorySelectedListener(new ListImagePopUpWindow.OnDirectorySelectedListener() {
            @Override
            public void onSelected(FolderBean folderBean) {
                mCurrentDirectory = new File(folderBean.getDirectoryPath());
                mImages = Arrays.asList(mCurrentDirectory.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg")) {
                            return true;
                        }
                        return false;
                    }
                }));
                mImageAdapter = new ImageAdapter(MainActivity.this, mImages, mCurrentDirectory.getAbsolutePath(), mMyImageLoader);
                mGridView.setAdapter(mImageAdapter);
                mDirectoryCount.setText(mImages.size() + "");
                mDirectoryName.setText(folderBean.getDirectoryName());
                mListImagePopUpWindow.dismiss();
            }
        });
    }

    /**
     * popup window 消失后， 背景变亮
     */
    private void lightOn() {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.alpha = 1.0f;
        getWindow().setAttributes(layoutParams);
    }

    // 内容区域变暗
    private void lightOff() {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.alpha = 0.3f;
        getWindow().setAttributes(layoutParams);
    }

    private void data2View() {
        if (mCurrentDirectory == null) {
            Toast.makeText(this, "No image has been found.", Toast.LENGTH_SHORT).show();
            return;
        }
        mImages = Arrays.asList(mCurrentDirectory.list());
        mImageAdapter = new ImageAdapter(this, mImages, mCurrentDirectory.getAbsolutePath(), mMyImageLoader);
        mGridView.setAdapter(mImageAdapter);
        mDirectoryCount.setText(mMaxCount + "");
        mDirectoryName.setText(mCurrentDirectory.getName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initData();
        initEvent();
    }

    private void initEvent() {
        mBottomLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListImagePopUpWindow.setAnimationStyle(R.style.directory_popup_window_anim);
                mListImagePopUpWindow.showAsDropDown(mBottomLayout, 0, 0);
                lightOff();
            }
        });
    }


    /**
     * 利用content provider 扫描图片
     */
    private void initData() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "The SD card is not available at this moment.", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(this, null, "Loading Images...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver contentResolver = MainActivity.this.getContentResolver();
                Cursor cursor = contentResolver.query(mImageUri, null,
                        MediaStore.Images.Media.MIME_TYPE + " =? or " + MediaStore.Images.Media.MIME_TYPE + " =?",
                        new String[]{"image/jpeg", "image/png"}, MediaStore.Images.Media.DATE_MODIFIED);
                Set<String> mDirPaths = new HashSet<>();
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null) {
                        continue;
                    }
                    String dirPath = parentFile.getAbsolutePath();
                    FolderBean folderBean = null;
                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDirectoryPath(dirPath);
                        folderBean.setFirstImagePath(path);
                    }
                    if (parentFile.list() == null) {
                        continue;
                    }
                    int picFileSize = parentFile.list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            if (name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg")) {
                                return true;
                            }
                            return false;
                        }
                    }).length;
                    folderBean.setImageCount(picFileSize);
                    mFolderBeans.add(folderBean);
                    if (picFileSize > mMaxCount) {
                        mMaxCount = picFileSize;
                        mCurrentDirectory = parentFile;
                    }
                }
                cursor.close();
                //通知handler 扫描图片完成
                mHandler.sendEmptyMessage(LOAD_DATA);
            }
        }).start();
    }

    private void initView() {
        mGridView = findViewById(R.id.id_gridView);
        mBottomLayout = findViewById(R.id.id_bottom_ly);
        mDirectoryName = findViewById(R.id.id_dir_name);
        mDirectoryCount = findViewById(R.id.id_dir_count);
    }
}
