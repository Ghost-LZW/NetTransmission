package com.soullan.nettransform.Utils;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class ZxingUtils {
    private static BarcodeEncoder barcodeEncoder;
    static { barcodeEncoder = new BarcodeEncoder(); }

    public static Bitmap createQRCode(String contents) {
        try {
            return barcodeEncoder.encodeBitmap(contents, BarcodeFormat.QR_CODE, 500, 500);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }
}
