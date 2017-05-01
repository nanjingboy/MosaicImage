package me.tom.mosaicimage.utils;

import android.graphics.Bitmap;

public class ImageUtils {

    public static Bitmap mosaic(Bitmap source, int size) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        Bitmap destBitmap = source.copy(Bitmap.Config.ARGB_8888, true);
        for (int x = 0; x < sourceWidth; x++) {
            for (int y = 0; y < sourceHeight; y++) {
                int pixel = source.getPixel(x / size * size, y / size * size);
                destBitmap.setPixel(x, y, pixel);
            }
        }
        return destBitmap;
    }
}
