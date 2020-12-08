package com.soullan.nettransform.UI;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.soullan.nettransform.Manager.ServersManager;
import com.soullan.nettransform.R;
import com.soullan.nettransform.Utils.NetworkUtils;
import com.soullan.nettransform.Utils.ZxingUtils;
import com.soullan.nettransform.constant.TransmissionConstant;

import static com.soullan.nettransform.constant.RequestConstant.GetFileRequest;

public class ServerTaskDialog extends Dialog {
    private static Uri ServerUri;
    private static String ServerName;
    private static String ServerPort;
    private static final String TAG = "ServerTaskDialog";

    public ServerTaskDialog(@NonNull Activity context, int themeResId) {
        super(context, R.style.AppTheme_create_dialog);
        setContentView(themeResId);
        if (ServerName != null) {
            TextView tv = findViewById(R.id.FilePath);
            tv.setText(ServerName);
        }
        if (ServerPort != null) {
            EditText et = findViewById(R.id.port);
            et.setText(ServerPort);
        }
        Button cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(v -> {
            cancel();
        });

        Button serverFile = findViewById(R.id.serverFile);
        serverFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            context.startActivityForResult(intent, GetFileRequest);
        });

        Button active = findViewById(R.id.yes);
        active.setOnClickListener(v -> {
            Intent res = new Intent();
            if (!TextUtils.isEmpty(ServerName)) res.putExtra("FileName", ServerName);
            context.onActivityReenter(TransmissionConstant.createServerResultCode, res);
            cancel();
        });

        Button gen = findViewById(R.id.gen_code);
        gen.setOnClickListener(v -> {
            if (ServerUri == null) {
                cancel();
                return;
            }
            Bitmap res = ZxingUtils.createQRCode(NetworkUtils.getIpAddressString() + ":" + ServersManager.getPort());
            Intent resIntent = new Intent();
            resIntent.putExtra("code", res);
            context.onActivityReenter(TransmissionConstant.generateServerCode, resIntent);
            cancel();
        });
    }

    public void setServerUri(Uri serverUri, Context context) {
        ServerUri = serverUri;
        if (ServerUri == null) {
            return;
        }
        DocumentFile documentFile = DocumentFile.fromSingleUri(context, ServerUri);
        if (documentFile == null) {
            return;
        }
        ServerName = documentFile.getName();

        TextView tv = findViewById(R.id.FilePath);
        tv.setText(ServerName);
    }
}
