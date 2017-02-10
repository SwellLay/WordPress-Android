package org.wordpress.android.ui.posts.photochooser;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.ImageView;

import org.wordpress.android.R;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

class ThumbnailLoader {

    private final Context mContext;
    private final ThreadPoolExecutor mExecutor;
    private final Handler mHandler;

    ThumbnailLoader(Context context) {
        mContext = context;
        int numCores = Runtime.getRuntime().availableProcessors();
        mExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numCores);
        mExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        mHandler = new Handler(Looper.getMainLooper());
    }

    void loadThumbnail(ImageView imageView, long imageId) {
        Runnable task = new PhotoLoaderRunnable(imageView, imageId);
        mExecutor.submit(task);
    }

    /*
     * task to load a device thumbnail and display it in an ImageView
     */
    class PhotoLoaderRunnable implements Runnable {
        private final WeakReference<ImageView> mWeakImageView;
        private final long mImageId;
        private final String mTag;
        private Bitmap mBitmap;

        PhotoLoaderRunnable(ImageView imageView, long imageId) {
            imageView.setImageResource(R.drawable.photo_chooser_item_background);
            mWeakImageView = new WeakReference<>(imageView);
            mImageId = imageId;
            mTag = Long.toString(mImageId);
            imageView.setTag(mTag);
        }

        private boolean isImageViewValid() {
            ImageView imageView = mWeakImageView.get();
            if (imageView != null && imageView.getTag() instanceof String) {
                // make sure this imageView has the original tag - it may
                // be different if the view was recycled
                String currentTag = (String) imageView.getTag();
                return mTag.equals(currentTag);
            } else {
                return false;
            }
        }

        @Override
        public void run() {
            mBitmap = MediaStore.Images.Thumbnails.getThumbnail(
                    mContext.getContentResolver(),
                    mImageId,
                    MediaStore.Images.Thumbnails.MINI_KIND,
                    null);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mBitmap != null && isImageViewValid()) {
                        mWeakImageView.get().setImageBitmap(mBitmap);
                    }
                }
            });
        }
    }

}
