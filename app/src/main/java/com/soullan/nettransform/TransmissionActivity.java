package com.soullan.nettransform;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.io.ByteStreams;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.soullan.nettransform.Item.RunningTaskItem;
import com.soullan.nettransform.Item.SolvedTaskItem;
import com.soullan.nettransform.Item.TaskItem;
import com.soullan.nettransform.Manager.DownloadManager;
import com.soullan.nettransform.Manager.FileManager;
import com.soullan.nettransform.Manager.PermissionManager;
import com.soullan.nettransform.Manager.ServersManager;
import com.soullan.nettransform.Manager.TransmissionManager;
import com.soullan.nettransform.UI.BitMapDialog;
import com.soullan.nettransform.UI.CreateTaskDialog;
import com.soullan.nettransform.UI.ListView.RunningTaskAdapter;
import com.soullan.nettransform.UI.ListView.SolvedTaskAdapter;
import com.soullan.nettransform.UI.ServerTaskDialog;
import com.soullan.nettransform.Utils.NetworkUtils;
import com.soullan.nettransform.constant.PermissionConstant;
import com.soullan.nettransform.exception.DownLoadException;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static android.os.Environment.DIRECTORY_DOWNLOADS;
import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;
import static com.soullan.nettransform.constant.RequestConstant.GetFileRequest;
import static com.soullan.nettransform.constant.RequestConstant.RequestResource;
import static com.soullan.nettransform.constant.RequestConstant.ServersRequest;
import static com.soullan.nettransform.constant.TransmissionConstant.createServerResultCode;
import static com.soullan.nettransform.constant.TransmissionConstant.createTaskResultCode;
import static com.soullan.nettransform.constant.TransmissionConstant.downloadPartFinish;
import static com.soullan.nettransform.constant.TransmissionConstant.downloadTaskFinish;
import static com.soullan.nettransform.constant.TransmissionConstant.generateServerCode;
import static com.soullan.nettransform.constant.TransmissionConstant.tryToGetCode;

public class TransmissionActivity extends AppCompatActivity {
    private static final String TAG = "TestActivity";
    private static PermissionManager permissionManager;
    private static ServerTaskDialog serverTaskDialogHandle;
    private final static int FlushListRequest = 111;
    private ListView listView;
    private RunningTaskAdapter adapter;
    private SolvedTaskAdapter solvedAdapter;

    private static class MainThreadHandle extends Handler {
        private TransmissionActivity context;

        public MainThreadHandle(TransmissionActivity context) {
            this.context = context;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                RunningTaskItem runningTaskItem = (RunningTaskItem) msg.obj;
                context.adapter.add(runningTaskItem);
                context.adapter.notifyDataSetChanged();
            } else if (msg.what == 1) {
                Pair<String, String> res = (Pair<String, String>) msg.obj;
                context.solvedItem(res.first, res.second);
            } else if (msg.what == 2) {
                Pair<String, String> res = (Pair<String, String>) msg.obj;
                context.solvedPart(res.first, res.second, msg.arg1);
            }
        }
    }

    static MainThreadHandle mainThreadHandle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FileManager.setDownloadDir(Objects.requireNonNull(this.getExternalFilesDir(DIRECTORY_DOWNLOADS)).getAbsolutePath());
        DownloadManager.init();
        permissionManager = new PermissionManager(this);
        mainThreadHandle = new MainThreadHandle(this);

        setContentView(R.layout.transmission_main_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = findViewById(R.id.show_content);

        FloatingActionButton fab = findViewById(R.id.create_task);
        fab.setOnClickListener(view -> {
            new CreateTaskDialog(TransmissionActivity.this, R.layout.create_task_dialog_layout).show();
        });

        FloatingActionButton ser = findViewById(R.id.create_server);
        ser.setOnClickListener(v -> {
            permissionManager.askForPermissions(ServersRequest,
                                                Manifest.permission.INTERNET,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE);
        });

        permissionManager.askForPermissions(FlushListRequest,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.READ_EXTERNAL_STORAGE);

        Button running_task = findViewById(R.id.running_tasks);
        running_task.setOnClickListener(v -> {
            setAdapter(1);
        });

        Button over_task = findViewById(R.id.over_tasks);
        over_task.setOnClickListener(v -> {
            setAdapter(2);
        });

        Button exit = findViewById(R.id.footer_item_out);
        exit.setOnClickListener(v -> {
            finish();
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(item -> {
            Toast.makeText(getApplicationContext(),"Hello",Toast.LENGTH_SHORT).show();
            switch (item.getItemId()) {
                case R.id.NavSubOne :
                    Toast.makeText(this, "goto dev", Toast.LENGTH_SHORT).show();
                    Intent testUI = new Intent(this, MainActivity.class);
                    startActivity(testUI);
                    break;
                default:
            }

            drawer.close();
            return true;
        });
    }

    private void FlushList() {
        File dirs = new File(FileManager.getDownloadDir());
        if (!dirs.exists()) {
            return;
        }
        File[] files = dirs.listFiles();
        if (files == null) return;
        List<RunningTaskItem> data = new ArrayList<>();
        List<SolvedTaskItem> solvedData = new ArrayList<>();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                String name = file.getName();
                String path = file.getAbsolutePath();
                name = name.substring(0, name.length() - 5);
                path = path.substring(0, path.length() - 5);
                TaskItem task = new TaskItem(name, path);
                try {
                    if (!task.isFinish())
                        data.add(new RunningTaskItem(name, path, task.getFileSize(), task.getUnSolvedSize()));
                    else solvedData.add(new SolvedTaskItem(name, path));
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        solvedAdapter = new SolvedTaskAdapter(this, R.layout.solved_task_item_layout, solvedData);

        adapter = new RunningTaskAdapter(this, R.layout.running_task_item_layout, data);

        setAdapter(1);
    }

    private void setAdapter(int opt) {
        switch (opt) {
            case 1 :
                listView.setAdapter(adapter);
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    RunningTaskItem item = adapter.getItem(position);
                    if (item == null) return;
                    ImageView statue = ((RunningTaskAdapter.ViewHolder) view.getTag()).statue;
                    if (item.getStatue()) {
                        item.setStatue(false);
                        item.shutdown();
                        statue.setImageResource(R.drawable.ic_run);
                    } else {
                        Log.e(TAG, "setAdapter: begin ");
                        item.setStatue(true);
                        try {
                            item.download(this);
                        } catch (JSONException | DownLoadException | IOException e) {
                            e.printStackTrace();
                        }
                        statue.setImageResource(R.drawable.ic_stop);
                    }
                });
                break;
            case 2 :
                listView.setAdapter(solvedAdapter);
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VIEW);
                    SolvedTaskItem item = solvedAdapter.getItem(position);
                    if (item == null) return;
                    File file = new File(item.getFilePath());
                    Uri uri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider",
                                                         file);
                    Log.i(TAG, "setAdapter: " + uri);
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setData(uri);

                    startActivity(intent);
                });
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ServersRequest :
                if (grantResults.length > 1 && grantResults[0] == PERMISSION_GRANTED &&
                        grantResults[1] == PERMISSION_GRANTED &&
                        grantResults[2] == PERMISSION_GRANTED) {
                    serverTaskDialogHandle = new ServerTaskDialog(TransmissionActivity.this, R.layout.create_server_dialog_layout);
                    serverTaskDialogHandle.show();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET))
                        new AlertDialog.Builder(this)
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
                break;
            case RequestResource :
                if (grantResults.length > 1 && grantResults[0] == PERMISSION_GRANTED &&
                        grantResults[1] == PERMISSION_GRANTED) new Thread(()-> {
                    try {
                        TaskItem item = TransmissionManager.ReceiveFiles(this, tmpHost, tmpPort);
                        RunningTaskItem runningTaskItem = new RunningTaskItem(item.getFileName(), item.getFilePath(), item.getFileSize(), item.getUnSolvedSize());
                        Log.i(TAG, "onRequestPermissionsResult: " + runningTaskItem.getFileName() + ' ' +
                                                                          runningTaskItem.getFilePath() + ' ' + runningTaskItem.getFileSize());
                        Message msg = new Message();
                        msg.what = 0;
                        msg.obj = runningTaskItem;
                        mainThreadHandle.sendMessage(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "onRequestPermissionsResult: host error");
                    } catch (DownLoadException | JSONException e) {
                        e.printStackTrace();
                    }
                }).start();
                else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.INTERNET))
                        new AlertDialog.Builder(this)
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
            case FlushListRequest :
                if (grantResults.length >= 1 && grantResults[0] == PERMISSION_GRANTED) {
                    FlushList();
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE))
                        new AlertDialog.Builder(this)
                                .setMessage("需要文件权限以进行文件读取")
                                .setPositiveButton("OK", (d, w) ->
                                        permissionManager.askForPermissions(FlushListRequest,
                                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE))
                                .setNegativeButton("Cancel", null)
                                .create()
                                .show();
                }
                break;
            case PermissionConstant.RequestGetCodePermission :
                new IntentIntegrator(this).initiateScan();
                break;
            default:
        }
    }

    static Uri ServerUri;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GetFileRequest) {
            if (data == null) {
                return;
            }
            ServerUri = data.getData();
            serverTaskDialogHandle.setServerUri(ServerUri, this);
        } else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    String scanResult = result.getContents();
                    Log.i(TAG, "onActivityResult: " + scanResult);
                    String[] res = scanResult.split(":");
                    Intent intent = new Intent();
                    intent.putExtra("host", res[0]);
                    intent.putExtra("port", Integer.parseInt(res[1]));
                    this.onActivityReenter(createTaskResultCode, intent);
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private static String tmpHost;
    private static int tmpPort;

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        switch (resultCode) {
            case createTaskResultCode :
                String host = data.getStringExtra("host");
                int port = data.getIntExtra("port", -1);
                if (!NetworkUtils.isIPv4Address(host)) {
                    Snackbar.make(findViewById(R.id.create_task), "ip输入错误", Snackbar.LENGTH_SHORT)
                            .show();
                    break;
                }
                if (!NetworkUtils.isIPv4Port(port)) {
                    Snackbar.make(findViewById(R.id.create_task), "端口输入错误", Snackbar.LENGTH_SHORT)
                            .show();
                    break;
                }

                tmpHost = host;
                tmpPort = port;

                Snackbar.make(findViewById(R.id.create_task), "connect " + host + ':' + port, Snackbar.LENGTH_LONG)
                        .show();

                permissionManager.askForPermissions(RequestResource,
                        Manifest.permission.INTERNET,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE);
                break;
            case createServerResultCode :
                String fileName = data.getStringExtra("FileName");
                if (TextUtils.isEmpty(fileName)) {
                    Snackbar.make(findViewById(R.id.create_server), "文件选择错误", Snackbar.LENGTH_SHORT)
                            .show();
                    break;
                }

                Log.i(TAG, "onActivityReenter: " + fileName);

                InputStream ServerInput = null;
                try {
                    ServerInput = this.getContentResolver().openInputStream(ServerUri);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                Log.i(TAG, "onRequestPermissionsResult: " + fileName);
                File file = new File(FileManager.getDownloadDir() + File.separator + fileName);
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

                ServersManager.setFileName(FileManager.getDownloadDir() + File.separator + fileName);

                new Thread(() -> {
                    try {
                        ServersManager.startServers();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();

                Snackbar.make(findViewById(R.id.create_task), "server on " + NetworkUtils.getIpAddressString() + ':'
                                                                   + ServersManager.getPort(), Snackbar.LENGTH_LONG)
                        .show();

                break;
            case downloadTaskFinish :
                {
                    String Name = data.getStringExtra("FileName");
                    String Path = data.getStringExtra("FilePath");
                    Message msg = new Message();
                    msg.what = 1;
                    msg.obj = new Pair<>(Name, Path);
                    mainThreadHandle.sendMessage(msg);
                }

                break;
            case generateServerCode :
                Bitmap bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("code");
                new BitMapDialog(this, R.layout.bitmap_show_layout).setBitMap(bitmap).show();
                break;
            case tryToGetCode :
                startQrCode();
                break;
            case downloadPartFinish :
                {
                    String Name = data.getStringExtra("FileName");
                    String Path = data.getStringExtra("FilePath");
                    int size = data.getIntExtra("solvedSize", -1);
                    if (size == -1) return;
                    Message msg = new Message();
                    msg.what = 2;
                    msg.obj = new Pair<>(Name, Path);
                    msg.arg1 = size;
                    mainThreadHandle.sendMessage(msg);
                }
                break;
            default:

        }
    }

    private void startQrCode() {
        // 申请相机权限
        permissionManager.askForPermissions(PermissionConstant.RequestGetCodePermission, Manifest.permission.CAMERA,
                                            Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    private void solvedItem(String Name, String Path) {
        Log.i(TAG, "solvedItem: " + Name + ' ' + Path);
        adapter.remove(new RunningTaskItem(Name, Path, true));
        solvedAdapter.add(new SolvedTaskItem(Name, Path));
        solvedAdapter.notifyDataSetChanged();
        adapter.notifyDataSetChanged();
    }

    private void solvedPart(String Name, String Path, int partSize) {
        Log.i(TAG, "solvedPart: " + Name + ' ' + Path);
        int position = adapter.getPosition(new RunningTaskItem(Name, Path, true));
        if (position == -1) return;
        Objects.requireNonNull(adapter.getItem(position)).addProSize(partSize);
        adapter.notifyDataSetChanged();
        updateView(position);
    }

    @SuppressLint("SetTextI18n")
    public void updateView(int itemIndex) {
        int visiblePosition = listView.getFirstVisiblePosition();
        if (itemIndex - visiblePosition >= 0) {

            View view = listView.getChildAt(itemIndex - visiblePosition);

            RunningTaskAdapter.ViewHolder holder = (RunningTaskAdapter.ViewHolder) view.getTag();
            RunningTaskItem item = adapter.getItem(itemIndex);
            holder.progressBar.setProgress((int) Objects.requireNonNull(item).getProSize());
            holder.progress.setText(item.getProSize() + "/" + item.getFileSize());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        setContentView(R.layout.transmission_main_layout);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.e(TAG, "onCreateOptionsMenu: get in");
        getMenuInflater().inflate(R.menu.transmission_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ntmStart :
                Toast.makeText(this, "all start", Toast.LENGTH_SHORT).show();
                break;
            case R.id.ntmDel :
                Toast.makeText(this, "all delete", Toast.LENGTH_SHORT).show();
                break;
            default:
        }
        return true;
    }
}

//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
