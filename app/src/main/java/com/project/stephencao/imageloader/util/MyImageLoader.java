package com.project.stephencao.imageloader.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


public class MyImageLoader {
    private static MyImageLoader mInstance;
    // image cache 缓存
    private LruCache<String, Bitmap> mLruCache;
    // thread pool 线程池
    private ExecutorService mThreadPool;
    // default thread count
    private static final int DEFAULT_THREAD_COUNT = 1;

    public enum Type {
        FIFO, LIFO
    }

    //同步信号量，轮询线程与其他并发执行。
    private Semaphore mSemaphorePoolThreadHandler = new Semaphore(0); // default value

    private Semaphore mSemaphoreThreadPool; //信号量， 空闲时再去执行下一个

    private Type mType = Type.LIFO;
    //队列
    private LinkedList<Runnable> mTaskQueue;

    private Thread mPoolThread;

    private Handler mPoolThreadHandler;

    private Handler mUIHandler;

    private MyImageLoader(int threadCount, Type type) {
        init(threadCount, type);
    }

    private void init(int threadCount, Type type) {
        //后台轮询线程
        mPoolThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHandler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池 取出一个任务执行
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                //释放一个信号量
                mSemaphorePoolThreadHandler.release();
                Looper.loop();
            }
        });
        mPoolThread.start();
        //获取最大可用内存
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheMemory = maxMemory / 8;
        mLruCache = new LruCache<String, Bitmap>(cacheMemory) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
        //创建线程池
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        mTaskQueue = new LinkedList<>();
        mType = type;
        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    //从任务队列取出方法
    private Runnable getTask() {
        if (mType == Type.FIFO) {
            return mTaskQueue.removeFirst();
        } else if (mType == Type.LIFO) {
            return mTaskQueue.removeLast();
        }
        return null;
    }

    public static MyImageLoader getInstance(int threadCount, Type type) {
        if (mInstance == null) {
            synchronized (MyImageLoader.class) {
                if (mInstance == null) {
                    mInstance = new MyImageLoader(threadCount, type);
                }
            }
        }
        return mInstance;
    }

    /**
     * 根据路径为image view 设置图片
     *
     * @param path
     * @param imageView
     */
    public void loadImage(final String path, final ImageView imageView) {
        //防止调用多次 造成混乱
        imageView.setTag(path);
        if (mUIHandler == null) {
            mUIHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    ImageBean imageBean = (ImageBean) msg.obj;
                    Bitmap bm = imageBean.bitmap;
                    ImageView iv = imageBean.imageView;
                    String path = imageBean.path;
                    if (iv.getTag().toString().equals(path)) {
                        iv.setImageBitmap(bm);
                    }
                }
            };
        }
        Bitmap bitmap = getBitmapFromLruCache(path);
        if (bitmap != null) {
            refreshBitmap(bitmap, path, imageView);
        } else {
            addTasks(new Runnable() {
                @Override
                public void run() {
                    //加载图片
                    ImageSize imagesize = getImageViewSize(imageView);
                    //压缩图片
                    Bitmap bm = decodeSampleBitmapFromPath(imagesize.width, imagesize.height, path);
                    //把图片加入缓存
                    addBitmapToLruCache(path, bm);
                    refreshBitmap(bm, path, imageView);

                    mSemaphoreThreadPool.release();
                }
            });
        }
    }

    private void refreshBitmap(Bitmap bm, String path, ImageView imageView) {
        ImageBean imageBean = new ImageBean();
        imageBean.bitmap = bm;
        imageBean.path = path;
        imageBean.imageView = imageView;
        Message message = Message.obtain();
        message.obj = imageBean;
        mUIHandler.sendMessage(message);
    }

    /**
     * 将图片加入缓存中
     *
     * @param path
     * @param bm
     */
    private void addBitmapToLruCache(String path, Bitmap bm) {
        if (getBitmapFromLruCache(path) == bm) {
            if (bm != null) {
                mLruCache.put(path, bm);
            }
        }
    }

    //根据图片需要显示的宽和高对图片进行压缩
    private Bitmap decodeSampleBitmapFromPath(int width, int height, String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, width, height);
        //使用获取导的in sample size
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        return bitmap;
    }

    //根据需求的宽和高，和图片实际的宽和高计算sample size
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;
        if (width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);
            inSampleSize = Math.max(widthRadio, heightRadio);
        }
        return inSampleSize;
    }

    private class ImageSize {
        int width;
        int height;
    }

    // 根据image view 获得适当的宽高
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize imageSize = new ImageSize();
        DisplayMetrics displayMetrics = imageView.getContext().getResources().getDisplayMetrics();

        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        int width = imageView.getWidth();
        if (width <= 0) {
            width = layoutParams.width;
        }
        if (width <= 0) {
            width = getImageViewFieldValue(imageView, "mMaxWidth");
        }
        if (width <= 0) {
            width = displayMetrics.widthPixels;
        }

        int height = imageView.getHeight();
        if (height <= 0) {
            height = layoutParams.height;
        }
        if (height <= 0) {
            height = getImageViewFieldValue(imageView, "mMaxHeight");
        }
        if (height <= 0) {
            height = displayMetrics.heightPixels;
        }
        imageSize.width = width;
        imageSize.height = height;
        return imageSize;
    }

    /**
     * 通过反射获得image view的宽和高
     *
     * @param object
     * @param fieldName
     * @return
     */
    private static int getImageViewFieldValue(Object object, String fieldName) {
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            int fieldValue = field.getInt(object);
            if (fieldValue > 0 && fieldValue < Integer.MAX_VALUE) {
                value = fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    private synchronized void addTasks(Runnable runnable) {
        mTaskQueue.add(runnable);
        try {
            if (mSemaphorePoolThreadHandler == null) {
                mSemaphorePoolThreadHandler.acquire();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (mPoolThreadHandler != null) {
            mPoolThreadHandler.sendEmptyMessage(0x110);
        }
    }

    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    // 防止错乱
    private class ImageBean {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }
}
