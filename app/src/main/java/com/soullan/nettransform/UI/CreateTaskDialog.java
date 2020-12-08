package com.soullan.nettransform.UI;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import com.soullan.nettransform.R;
import com.soullan.nettransform.Utils.NetworkUtils;
import com.soullan.nettransform.constant.TransmissionConstant;

public class CreateTaskDialog extends Dialog {
    private static final String TAG = "CreateTaskDialog";

    public CreateTaskDialog(Activity context, int layoutId) {
        super(context, R.style.AppTheme_create_dialog);
        setContentView(layoutId);

        Button cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(v -> {
            cancel();
        });

        Button active = findViewById(R.id.yes);
        active.setOnClickListener(v -> {
            EditText ipText = findViewById(R.id.ip);
            String host = ipText.getText().toString();
            EditText portText = findViewById(R.id.port);
            String port = portText.getText().toString();
            Intent res = new Intent();

            res.putExtra("host", host);
            Log.i(TAG, "CreateTaskDialog: " + port + " " + NetworkUtils.isInteger(port));
            if (NetworkUtils.isInteger(port))
                res.putExtra("port", Integer.parseInt(port));
            context.onActivityReenter(TransmissionConstant.createTaskResultCode, res);
            cancel();
        });

        Button get_code = findViewById(R.id.get_code);
        get_code.setOnClickListener(v -> {
            context.onActivityReenter(TransmissionConstant.tryToGetCode, null);
        });
    }
}
