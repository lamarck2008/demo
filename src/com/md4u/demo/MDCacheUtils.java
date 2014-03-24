package com.md4u.demo;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.io.comparator.LastModifiedFileComparator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.util.LruCache;

public class MDCacheUtils {
    private static Context context = null;
    private static String sdRootPath = Environment.getExternalStorageDirectory().getPath();   // sd card directory
    private static String appRootPath = null;   // application storage directory
    private static LruCache<String, Bitmap> bitmapCache;
    private final static String FOLDER_NAME = "/md4uImages";
    private final static Long MAX_FOLDER_SIZE = (long) (20 * 1024 * 1024 * 8);   // 20M

    // set context and set the appRootPath
    public static void setContext(Context c) {
        context = c;
        appRootPath = context.getCacheDir().getPath();
    }

    // check bitmap cache
    private static void checkBitmapCache() {
        if (bitmapCache == null) {
            bitmapCache = new LruCache<String, Bitmap>(
                (int)Runtime.getRuntime().maxMemory() / 8
            ) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getRowBytes() * bitmap.getHeight();
                }
            };
        }
    }

    // get the directory to store images
    private static String getStorageDirectory() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ?
            sdRootPath + FOLDER_NAME : appRootPath + FOLDER_NAME;
    }

    // get a bitmap from cache
    private static Bitmap getBitmapFromCache(String key) {
        checkBitmapCache();
        return bitmapCache.get(key);
    }

    // add a bitmap to cache
    private static void addBitmapToCache(String key, Bitmap bitmap) {
        checkBitmapCache();
        if (getBitmapFromCache(key) == null) {
            bitmapCache.put(key, bitmap);
        }
    }

    private static File[] listFiles(File dir) {
        File[] fileList = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });

        return fileList;
    }

    // get size of files in the directory
    private static long getStorageSize() {
        File dir = new File(getStorageDirectory());
        if (dir.exists()) {
            long result = 0;
            File[] fileList = listFiles(dir);
            for(int i = 0; i < fileList.length; i++) {
                    result += fileList[i].length();
            }
            return result;
        }
        return 0;
    }

    // return the oldest file in the storage
    public static File getOldestFile() {
        File dir = new File(getStorageDirectory());
        File[] files = listFiles(dir);

        Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
        return files[0];
    }

    // delete file from directory
    public static void deleteFile() {
        // recursively delete the oldest file till the size of folder less than MAX_FOLDER_SIZE
        getOldestFile().delete();
        if (getStorageSize() > MAX_FOLDER_SIZE) {
            deleteFile();
        }
    }

    // get bitmap from cache or file system
    public static Bitmap getBitmap(String fileName) {
        Bitmap bitmap = getBitmapFromCache(fileName);
        if (bitmap != null) {
            return bitmap;
        }

        // modify the file attribute
        String filePath = getStorageDirectory() + File.separator + fileName;
        File file = new File(filePath);
        long time = new Date().getTime();
        file.setLastModified(time);
        return BitmapFactory.decodeFile(filePath);
    }

    // save image to cache memory and sd card, if sd card is not present, save to application storage directory
    public static void saveBitmap(String fileName, Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }

        // save to memory cache
        addBitmapToCache(fileName, bitmap);

        // save to sd card or application storage directory
        if (getStorageSize() > MAX_FOLDER_SIZE) {
            deleteFile();
        }
        String path = getStorageDirectory();
        File fileFolder = new File(path);
        if (!fileFolder.exists()) {
            fileFolder.mkdirs();
        }

        try {
            File imageFile = new File(path + File.separator + fileName);
            imageFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
