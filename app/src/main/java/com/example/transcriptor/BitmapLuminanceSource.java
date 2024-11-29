package com.example.transcriptor;

import android.graphics.Bitmap;

import com.google.zxing.LuminanceSource;

public class BitmapLuminanceSource extends LuminanceSource {
    private final byte[] bitmapPixels;

    public BitmapLuminanceSource(Bitmap bitmap) {
        super(bitmap.getWidth(), bitmap.getHeight());

        int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(),
                bitmap.getHeight());

        bitmapPixels = new byte[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            int pixel = intArray[i];
            int red = (pixel >> 16) & 0xFF;
            int green = (pixel >> 8) & 0xFF;
            int blue = pixel & 0xFF;
            bitmapPixels[i] = (byte) ((red + green + blue) / 3);
        }
    }

    @Override
    public byte[] getMatrix() {
        return bitmapPixels;
    }

    @Override
    public byte[] getRow(int y, byte[] row) {
        System.arraycopy(bitmapPixels, y * getWidth(), row, 0, getWidth());
        return row;
    }
}

