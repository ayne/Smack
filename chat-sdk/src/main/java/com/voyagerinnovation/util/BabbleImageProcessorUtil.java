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

    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_IMAGE = "image";
    public static final String THUMBNAIL_DESTINATION_URL = Environment
            .getExternalStorageDirectory() + "/download/thumb/";

    private static final int THUMBNAIL_MAX_DIMENSION = 600;

    public static String generateThumbnail(String id, String url, String mimeType) {
        String result = null;
        String type = null;

        if (mimeType != null) {
            type = mimeType.split("/")[0];
        }

        if (type != null) {
            if (TYPE_VIDEO.equals(type)) {
                Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(url,
                        Thumbnails.MICRO_KIND);
                persistThumbnail(thumbnail, id);
                result = convertToBase64(thumbnail);
                thumbnail.recycle();
            } else if (TYPE_IMAGE.equals(type)) {
                Bitmap thumbnail = getResizeBitmap(url);
                if (thumbnail != null) {
                    persistThumbnail(thumbnail, id);
                    result = convertToBase64(thumbnail);
                    thumbnail.recycle();
                } else {
                    return null;
                }
            }
        }

        return result;
    }

    public static void persistThumbnail(Bitmap bitmap, String id) {
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

    private static String convertToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(CompressFormat.JPEG, 50, baos);
        byte[] b = baos.toByteArray();
        Log.d(TAG, "Base 64 byte length " + b.length);
        return Base64.encodeToString(b, 0);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            while ((height / inSampleSize) > reqHeight &&
                    (width / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        Log.d(TAG, "In Sample Size: " + inSampleSize);
        return inSampleSize;
    }

    public static Bitmap getResizeBitmap(String existingFileName) {
        existingFileName = existingFileName.replace("file://", "");

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(existingFileName, options);

        int width = options.outWidth;
        int height = options.outHeight;

        Log.d(TAG, "Original bitmap width: " + width + " height: " + height);
        if (width > THUMBNAIL_MAX_DIMENSION || height > THUMBNAIL_MAX_DIMENSION) {
            if (width > height) {
                double ratio = width * 1.0 / THUMBNAIL_MAX_DIMENSION;

                width = THUMBNAIL_MAX_DIMENSION;
                height = (int)(height * 1.0 / ratio);
            } else if (width < height) {
                double ratio = height / THUMBNAIL_MAX_DIMENSION;

                width = (int)(width * 1.0 / ratio);
                height = THUMBNAIL_MAX_DIMENSION;
            } else {
                width = THUMBNAIL_MAX_DIMENSION;
                height = THUMBNAIL_MAX_DIMENSION;
            }
        }

        options.inSampleSize = calculateInSampleSize(options, width, height);
        options.inJustDecodeBounds = false;

        Bitmap bitmap = BitmapFactory.decodeFile(existingFileName, options);
        Log.d(TAG, "Resized bitmap width: " + bitmap.getWidth() + " height: " + bitmap.getHeight());
        if (bitmap == null) {
            return null;
        }

        try {
            ExifInterface exif = new ExifInterface(existingFileName);
            Matrix matrix = new Matrix();

            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);

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
