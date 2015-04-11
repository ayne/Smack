package com.voyagerinnovation.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Utility class for generating scaled Bitmaps and Thumbnails.
 * @author charmanesantiago
 */
public class BabbleImageProcessorUtil {

    private static final String TAG = BabbleImageProcessorUtil.class.getSimpleName();
    static public final String THUMBNAIL_DESTINATION_URL = Environment
            .getExternalStorageDirectory() + "/download/thumb/";

    static public String generateThumbnail(String id, String url,
                                                 String mimeType) {
        String result = null;
        String type = null;

        if (mimeType != null) {
            type = mimeType.split("/")[0];
        }

        if (type != null) {
            if ("video".equals(type)) {
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(url,
                        Thumbnails.MICRO_KIND);
                persistThumbnail(thumbnail, id);
                result = convertToBase64(thumbnail);
                thumbnail.recycle();
            } else if ("image".equals(type)) {
                Bitmap bitmap = getResizeBitmap(url);
                if (bitmap == null) {
                    return null;
                } else {
                    Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap,
                            bitmap.getWidth() / 2,
                            bitmap.getHeight() / 2, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                    persistThumbnail(thumbnail, id);
                    result = convertToBase64(thumbnail);
                    thumbnail.recycle();
                    bitmap.recycle();
                }
            }
        }

        return result;
    }

    static public void persistThumbnail(Bitmap bitmap, String id) {

        // String result = null;

        File fileDir = new File(THUMBNAIL_DESTINATION_URL);
        if (!fileDir.exists()) {
            if(!fileDir.mkdirs()){
                return;
            }
        }

        File file = new File(THUMBNAIL_DESTINATION_URL + id + ".jpeg");
        try {
            if(!file.createNewFile()){
                return;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream output = null;

        try {
            output = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        if (output != null) {
            bitmap.compress(CompressFormat.JPEG, 50, output);
        }
    }

    static private String convertToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, 50, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, 0);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap getResizeBitmap(String existingFileName) {

        BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        existingFileName = existingFileName.replace("file://", "");
        Log.d(TAG, "existingFileName: " + existingFileName);

        BitmapFactory.decodeFile(existingFileName, options);
        int width = options.outWidth;
        int height = options.outHeight;

        int downscaleSize = width > height ? width : height;
        float downscaleFactor = downscaleSize / 1000.00f;
        downscaleFactor = (downscaleFactor < 1.00f) ? 1 : downscaleFactor;

        int newWidth = (int) (width / downscaleFactor);
        int newHeight = (int) (height / downscaleFactor);

        options.inSampleSize = BabbleImageProcessorUtil.calculateInSampleSize(options,
                newWidth, newHeight);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(existingFileName, options);
        if (bitmap == null) {
            return null;
        }

        try {
            ExifInterface exif = new ExifInterface(existingFileName);
            Matrix matrix = new Matrix();

            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);

//            if (orientation == ExifInterface.ORIENTATION_NORMAL) {
//
//                // Do nothing. The original image is fine.
//            } else
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {

                matrix.postRotate(90);

            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {

                matrix.postRotate(180);

            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {

                matrix.postRotate(270);

            }
            return Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(),
                    bitmap.getHeight(), matrix, false);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }

    }

}
