package com.soullan.nettransform;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.common.io.ByteStreams;
import com.soullan.nettransform.Manager.FileManager;
import com.soullan.nettransform.Manager.PermissionManager;
import com.soullan.nettransform.Manager.ServersManager;
import com.soullan.nettransform.Manager.TransmissionManager;
import com.soullan.nettransform.exception.DownLoadException;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static com.soullan.nettransform.constant.PermissionConstant.RequestInterNetPermission;
import static com.soullan.nettransform.constant.RequestConstant.GetFileRequest;
import static com.soullan.nettransform.constant.RequestConstant.RequestCreateFile;
import static com.soullan.nettransform.constant.RequestConstant.RequestResource;
import static com.soullan.nettransform.constant.RequestConstant.ServersRequest;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private final PermissionManager permissionManager = new PermissionManager(this);
    private final FileManager fileManager = new FileManager();
    private String resourceAddress;
    private Integer resourcePort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FileManager.setDownloadDir(Objects.requireNonNull(this.getExternalFilesDir(DIRECTORY_DOWNLOADS)).getAbsolutePath());

        setContentView(R.layout.main_layout);

        Button getFile = findViewById(R.id.serverFile);

        getFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            this.startActivityForResult(intent, GetFileRequest);
        });

        Button bt1 = findViewById(R.id.Button1);
        bt1.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "You click", Toast.LENGTH_SHORT).show();
            Intent testUI = new Intent(MainActivity.this, TransmissionActivity.class);
            startActivity(testUI);
        });

        Button createFile = findViewById(R.id.CreateFile);
        createFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "create file", Toast.LENGTH_SHORT).show();
                permissionManager.askForPermissions(RequestCreateFile,
                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                    Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });

        Button receive = findViewById(R.id.getUrl);
        receive.setOnClickListener(v -> {
            EditText res = findViewById(R.id.resUrl);
            String[] aim = res.getText().toString().split(":");
            Log.i(TAG, "onClick: " + aim[0] + " : " + aim[1]);
            resourceAddress = aim[0];
            resourcePort = Integer.parseInt(aim[1]);
            permissionManager.askForPermissions(RequestResource,
                                                Manifest.permission.INTERNET,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE);
        });
    }

    static Uri ServerUri;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GetFileRequest) {
            if (data == null) return;
            ServerUri = data.getData();
            if (ServerUri == null) return;

            permissionManager.askForPermissions(ServersRequest,
                    Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE);
        }

    }

    public static void testTCPConnect() {
        try {
            Socket socket = new Socket("10.0.2.2", 9999);

            Log.i(TAG, "getIpAddressString: out put success");

            OutputStream outputStream = socket.getOutputStream();
            String val = "Hello Outside\n";
            outputStream.write(val.getBytes(StandardCharsets.UTF_8));

            socket.shutdownOutput();

            InputStream inputStream = socket.getInputStream();
            byte[] bytes = new byte[1024];
            int len;
            StringBuilder stringBuilder = new StringBuilder();
            while ((len = inputStream.read(bytes)) != -1) {
                stringBuilder.append(new String(bytes, 0, len, StandardCharsets.UTF_8));
            }
            Log.i(TAG, "getIpAddressString: " + stringBuilder);

            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "getIpAddressString: socket error");
            e.printStackTrace();
        }
    }

    public void testCreateFile() {
        if (FileManager.isExternalStorageWritable()) Log.i(TAG, "testCreateFile: can write");
        if (FileManager.isExternalStorageReadable()) Log.i(TAG, "testCreateFile: can read");

        try {
            String path = FileManager.getDownloadDir();
            Log.i(TAG, "testCreateFile: " + path);
            File file = new File(path + File.separator + "testFile.ltf");

            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.setLength(1024 * 1024 * 1024);
            randomAccessFile.close();
        } catch (IOException e) {
            Log.e(TAG, "testCreateFile: io error " + e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestInterNetPermission :
                if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) new Thread(MainActivity::testTCPConnect).start();
                else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET))
                        new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.net_permission_need)
                            .setPositiveButton("OK", (d, w) ->
                                    permissionManager.askForPermission(Manifest.permission.INTERNET, MainActivity::testTCPConnect))
                            .setNegativeButton("Cancel", null)
                            .create()
                            .show();
                }
                break;
            case RequestCreateFile :
                if (grantResults.length > 1 && grantResults[0] == PERMISSION_GRANTED
                    && grantResults[1] == PERMISSION_GRANTED) testCreateFile();
                else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        new AlertDialog.Builder(this)
                                .setMessage(R.string.need_write_permission)
                                .setPositiveButton("OK", (d, w) ->
                                        permissionManager.askForPermissions(RequestCreateFile,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE))
                                .setNegativeButton("Cancel", null)
                                .create()
                                .show();
                }
                break;
            case RequestResource :
                if (grantResults.length > 1 && grantResults[0] == PERMISSION_GRANTED &&
                                               grantResults[1] == PERMISSION_GRANTED) new Thread(()-> {
                    try {
                        TransmissionManager.ReceiveFiles(this, resourceAddress, resourcePort);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "onRequestPermissionsResult: host error");
                        Toast.makeText(MainActivity.this, "please confirm your host and port", Toast.LENGTH_SHORT).show();
                    } catch (DownLoadException | JSONException e) {
                        e.printStackTrace();
                    }
                }).start();
                else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET))
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage(R.string.net_permission_need)
                                .setPositiveButton("OK", (d, w) ->
                                        permissionManager.askForPermissions(RequestResource,
                                                Manifest.permission.INTERNET,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE))
                                .setNegativeButton("Cancel", null)
                                .create()
                                .show();
                }
                break;
            case ServersRequest :
                if (grantResults.length > 1 && grantResults[0] == PERMISSION_GRANTED &&
                        grantResults[1] == PERMISSION_GRANTED &&
                        grantResults[2] == PERMISSION_GRANTED) {
                    InputStream ServerInput = null;
                    try {
                        ServerInput = this.getContentResolver().openInputStream(ServerUri);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    DocumentFile documentFile = DocumentFile.fromSingleUri(this, ServerUri);
                    assert documentFile != null;
                    String ServerName = documentFile.getName();
                    Log.i(TAG, "onRequestPermissionsResult: " + ServerName);
                    File file = new File(FileManager.getDownloadDir() + File.separator + ServerName);
                    try {
                        FileOutputStream out = new FileOutputStream(file);
                        assert ServerInput != null;
                        ByteStreams.copy(ServerInput, out);
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            assert ServerInput != null;
                            ServerInput.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    ServersManager.setFileName(FileManager.getDownloadDir() + File.separator + ServerName);

                    new Thread(() -> {
                        try {
                            ServersManager.startServers();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET))
                        new AlertDialog.Builder(MainActivity.this)
                                .setMessage(R.string.net_permission_need)
                                .setPositiveButton("OK", (d, w) ->
                                        permissionManager.askForPermissions(ServersRequest,
                                                Manifest.permission.INTERNET,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE))
                                .setNegativeButton("Cancel", null)
                                .create()
                                .show();
                }
            default:
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect :
                Toast.makeText(MainActivity.this, "You connect success", Toast.LENGTH_SHORT).show();
                break;
            case R.id.dis_connect :
                Toast.makeText(MainActivity.this, "you disconnect error", Toast.LENGTH_SHORT).show();
                break;
            default:
        }
        return true;
    }
}