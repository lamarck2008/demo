package com.md4u.demo;

import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.md4u.demo.MDModel.Image;

public class MDImageLoader {
    // handler of image loader
    public interface ImageLoaderHandler {
        public void onLoaded(ImageView imageView, Bitmap bitmap);
    }

    private static abstract class BasicImageLoader extends AsyncTask<String, Void, Bitmap> {
        private final ImageView imageView;
        private final ImageLoaderHandler handler;
        protected int reqWidth;
        protected int reqHeight;

        protected BasicImageLoader(
            ImageView imageView, int reqWidth, int reqHeight,
            ImageLoaderHandler handler
        ) {
            this.imageView = imageView;
            this.reqWidth = reqWidth;
            this.reqHeight = reqHeight;
            this.handler = handler;
        }

        // see if ImageView is still around and set bitmap
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
            if (handler != null) {
                handler.onLoaded(imageView, bitmap);
            }
        }

        // calculate in sample size
        protected int calcInSampleSize(Options options) {
            // load raw image width and height
            final int width = options.outWidth;
            final int height = options.outHeight;

            int inSampleSize = 1;
            if (height > reqHeight || width > reqWidth) {
                final int halfWidth = width / 2;
                final int halfHeight = height / 2;
                while ((halfWidth / inSampleSize) > reqWidth &&
                    (halfHeight / inSampleSize) > reqHeight
                ) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }
    }

    private static class LocalImageLoader extends BasicImageLoader {
        protected LocalImageLoader(
            ImageView imageView, int reqWidth, int reqHeight,
            ImageLoaderHandler handler
        ) {
            super(imageView, reqWidth, reqHeight, handler);
        }

        // decode image in background
        @Override
        protected Bitmap doInBackground(String... params) {
            int orientation = 0;
            Bitmap bitmap = null;
            try {
                ExifInterface exif = new ExifInterface(params[0]);
                switch (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    case ExifInterface.ORIENTATION_ROTATE_90: {
                        orientation = 90;
                    } break;
                    case ExifInterface.ORIENTATION_ROTATE_180: {
                        orientation = 180;
                    } break;
                    case ExifInterface.ORIENTATION_ROTATE_270: {
                        orientation = 270;
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (orientation == 90 || orientation == 270) {
                int temp = reqWidth;
                reqWidth = reqHeight;
                reqHeight = temp;
            }
            Options options = new Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(params[0], options);
            options.inSampleSize = calcInSampleSize(options);
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeFile(params[0], options);
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);
            bitmap = Bitmap.createBitmap(
                bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(),
                matrix, true
            );
            return bitmap;
        }
    }

    private static class RemoteImageLoader extends BasicImageLoader {

        protected RemoteImageLoader(
            ImageView imageView, int reqWidth, int reqHeight,
            ImageLoaderHandler handler
        ) {
            super(imageView, reqWidth, reqHeight, handler);
        }

        // decode image in background
        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = MDCacheUtils.getBitmap(params[0]);

            if (bitmap != null) {
                return bitmap;
            }

            // load image from server
            byte[] imageData = MDService.MDServiceInstance.loadImageData(
                new Image(params[0])
            );
            // decode image
            Options options = new Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(
                imageData, 0, imageData.length, options
            );
            options.inSampleSize = calcInSampleSize(options);
            options.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeByteArray(
                imageData, 0, imageData.length, options
            );

            if (bitmap != null) {
                MDCacheUtils.saveBitmap(params[0], bitmap);
            }
            return bitmap;
        }
    }

    // asynchronous operation of loading image file
    public static void loadLocalImage(
        String pathName, ImageView imageView,
        int reqWidth, int reqHeight,
        ImageLoaderHandler handler
    ) {
        LocalImageLoader loader = new LocalImageLoader(
            imageView, reqWidth, reqHeight, handler
        );
        loader.execute(pathName);
    }

    // asynchronous operation of loading remote image
    public static void loadRemoteImage(
        String imageId, ImageView imageView,
        int reqWidth, int reqHeight,
        ImageLoaderHandler handler
    ) {
        RemoteImageLoader loader = new RemoteImageLoader(
            imageView, reqWidth, reqHeight, handler
        );
        loader.execute(imageId);
    }
}
