package me.tom.mosaicimage.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;

public class ImageUtils {

    public static Bitmap mosaic(Bitmap source, int mosaicAreaSize) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        Bitmap destBitmap = source.copy(Bitmap.Config.ARGB_8888, true);
        for (int x = 0; x < sourceWidth; x++) {
            for (int y = 0; y < sourceHeight; y++) {
                int pixel = source.getPixel(x / mosaicAreaSize * mosaicAreaSize, y / mosaicAreaSize * mosaicAreaSize);
                destBitmap.setPixel(x, y, pixel);
            }
        }
        return destBitmap;
    }

    public static Boolean saveToFile(Bitmap bitmap, String filePath) {
        try {
            OutputStream outputStream = new FileOutputStream(new File(filePath));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static Observable<String> download(final Context context, final String url) {
        return Observable.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String imagePath = FileUtils.getTempFilePath(context, System.currentTimeMillis() + ".jpg");
                    InputStream inputStream = response.body().byteStream();
                    OutputStream outputStream = new FileOutputStream(new File(imagePath));
                    int count;
                    byte[] buffer = new byte[1024];
                    while ((count = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, count);
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                    return imagePath;
                }
                throw  new Exception("Unknown Error");
            }
        });
    }

    public static Observable<Bitmap> compress(String path) {
        final String imagePath = path.replace("file://", "");
        return Observable.fromCallable(new Callable<Bitmap>() {
            @Override
            public Bitmap call() throws Exception {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                options.inSampleSize = 1;
                BitmapFactory.decodeFile(imagePath, options);
                int width = options.outWidth;
                int height = options.outHeight;
                int thumbWidth = width % 2 == 1 ? width + 1 : width;
                int thumbHeight = height % 2 == 1 ? height + 1: height;

                if (thumbWidth > thumbHeight) {
                    width = thumbHeight;
                    height = thumbWidth;
                } else {
                    width = thumbWidth;
                    height = thumbHeight;
                }
                double scale = ((double) width / height);
                long originalSize = (new File(imagePath)).length() / 1024;
                if (scale <= 1 && scale > 0.5625) {
                    if (height < 1664) {
                        if (originalSize < 150) {
                            return BitmapFactory.decodeFile(imagePath);
                        }
                    } else if (height >= 1664 && height < 4990) {
                        thumbWidth = width / 2;
                        thumbHeight = height / 2;
                    } else if (height >= 4990 && height < 10240) {
                        thumbWidth = width / 4;
                        thumbHeight = height / 4;
                    } else {
                        int multiple = height / 1280 == 0 ? 1 : height / 1280;
                        thumbWidth = width / multiple;
                        thumbHeight = height / multiple;
                    }
                } else if (scale <= 0.5625 && scale > 0.5) {
                    if (height < 1280 && originalSize < 200) {
                        return BitmapFactory.decodeFile(imagePath);
                    }
                    int multiple = height / 1280 == 0 ? 1 : height / 1280;
                    thumbWidth = width / multiple;
                    thumbHeight = height / multiple;
                } else {
                    int multiple = (int) Math.ceil(height / (1280.0 / scale));
                    thumbWidth = width / multiple;
                    thumbHeight = height / multiple;
                }
                return compress(imagePath, thumbWidth, thumbHeight);
            }
        });
    }

    public static Bitmap rotating(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static Bitmap compress(String imagePath, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        int sourceWidth = options.outWidth;
        int sourceHeight = options.outHeight;

        int inSampleSize = 1;

        if (sourceWidth > width || sourceHeight > height) {
            int halfWidth = sourceWidth / 2;
            int halfHeight = sourceHeight / 2;
            while ((halfHeight / inSampleSize) > height && (halfWidth / inSampleSize) > width) {
                inSampleSize *= 2;
            }
        }
        options.inSampleSize = inSampleSize;
        options.inJustDecodeBounds = false;
        int heightRatio = (int) Math.ceil(options.outHeight / (float) height);
        int widthRatio = (int) Math.ceil(options.outWidth / (float) width);
        if (heightRatio > 1 || widthRatio > 1) {
            if (heightRatio > widthRatio) {
                options.inSampleSize = heightRatio;
            } else {
                options.inSampleSize = widthRatio;
            }
        }
        options.inJustDecodeBounds = false;
        return rotating(BitmapFactory.decodeFile(imagePath, options), getSpinAngle(imagePath));
    }

    protected static int getSpinAngle(String imagePath) {
        int angle = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            switch (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    angle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    angle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    angle = 270;
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
        }
        return angle;
    }
}
