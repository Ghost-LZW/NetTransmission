package com.soullan.nettransform.UI;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.soullan.nettransform.R;

public class BitMapDialog extends Dialog {

    public BitMapDialog(@NonNull Context context, int themeResId) {
        super(context, R.style.AppTheme_create_dialog);
        setContentView(themeResId);
    }

    public BitMapDialog setBitMap(Bitmap res) {
        ImageView iv = findViewById(R.id.resource);
        iv.setImageBitmap(res);
        return this;
    }
}
