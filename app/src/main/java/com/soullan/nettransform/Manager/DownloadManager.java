package com.soullan.nettransform.Manager;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.soullan.nettransform.Utils.ArrayUtils;
import com.soullan.nettransform.Utils.ByteUtils;
import com.soullan.nettransform.constant.FileConstant;
import com.soullan.nettransform.constant.TransmissionConstant;
import com.soullan.nettransform.exception.DownLoadException;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DownloadManager {
    private static final String TAG = "DownloadManager";
    private volatile File file;
    private volatile File newFile;
    private volatile File infoFile;
    private volatile JSONObject info;
    private volatile JSONObject tasks;
    private volatile JSONObject address;
    private volatile ExecutorService exec;
    private volatile Map<String, Long> runTaskInfo;
    private volatile boolean finish;
    private static int MxThread = 16;
    private int ThreadNum = 0;

    private static class CallHandle extends Handler {
        int cnt = 0;
        @Override
        public synchronized void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                Pair<Activity, Intent> res = (Pair<Activity, Intent>) msg.obj;
                assert res.first != null;
                Log.i(TAG, "handleMessage: cnt = " + cnt);
                ++cnt;
                res.first.onActivityReenter(TransmissionConstant.downloadTaskFinish, res.second);
            } else if (msg.what == 1) {
                Pair<Activity, Intent> res = (Pair<Activity, Intent>) msg.obj;
                assert res.first != null;
                Log.i(TAG, "handleMessage: cnt = " + cnt);
                ++cnt;
                res.first.onActivityReenter(TransmissionConstant.downloadPartFinish, res.second);
            }
        }
    }

    static CallHandle msgHandle;
    public static void init() {
        msgHandle = new CallHandle();
    }

    public DownloadManager(String filename) throws DownLoadException, IOException, JSONException {
        Log.i(TAG, "DownloadManager: " + filename);
        finish = false;
        newFile = new File(FileManager.getDownloadDir() + File.separator + filename);
        file = new File(FileManager.getDownloadDir() + File.separator + filename + ".ltf");
        infoFile = new File(FileManager.getDownloadDir() + File.separator + filename + ".json");
        Log.i(TAG, "DownloadManager: " + file.exists() + ' ' + infoFile.exists());
        if (!file.exists() || !infoFile.exists()) throw new DownLoadException("File not exist " + filename);
        info = new JSONObject(FileUtils.readFileToString(infoFile));
        tasks = info.getJSONObject("tasks");

        runTaskInfo = new HashMap<>();

        address = info.getJSONObject("address");
    }

    public void setMxThread(int val) {
        MxThread = val;
    }

    synchronized private void saveInfo() throws IOException, JSONException {
        OutputStream output = new FileOutputStream(infoFile);
        output.write(info.toString(4).getBytes(StandardCharsets.UTF_8));
        output.close();
    }

    synchronized private long findTask(int opt, long oldTask, long updateTask, int solvedSize) throws JSONException {
        switch (opt) {
            case 0 :
                for (Iterator<String> it = tasks.keys(); it.hasNext(); ) {
                    String key = it.next();
                    if (!runTaskInfo.containsKey(key)) {
                        runTaskInfo.put(key, tasks.getLong(key));
                        return Long.parseLong(key);
                    }
                }
                String MaxTask = "";
                long size = 0;
                for (String task : runTaskInfo.keySet()) {
                    Long res = runTaskInfo.get(task);
                    if (res != null && res > size) {
                        MaxTask = task;
                        size = res;
                    }
                }
                if (size == 0) return -1;
                try {
                    long cnt = (size + FileConstant.PART - 1) / FileConstant.PART;
                    Log.i(TAG, "findTask: cnt = " + cnt + ' ' + MaxTask);
                    long stay = cnt / 2;
                    if (stay == 0) return -1;
                    long newTask = Integer.parseInt(MaxTask) + stay * FileConstant.PART + tasks.getLong(MaxTask) - size;
                    runTaskInfo.put(MaxTask, stay * FileConstant.PART);
                    tasks.put(MaxTask, tasks.getLong(MaxTask) - size + stay * FileConstant.PART);
                    runTaskInfo.put(String.valueOf(newTask), size - stay * FileConstant.PART);
                    tasks.put(String.valueOf(newTask), size - stay * FileConstant.PART);
                    saveInfo();
                    return newTask;
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                break;
            case 1 :
                Long oldSize = runTaskInfo.get(String.valueOf(oldTask));
                if (oldSize == null) {
                    Log.e(TAG, "findTask: run task not found");
                } else {
                    if (oldSize > solvedSize) {
                        runTaskInfo.put(String.valueOf(updateTask), oldSize - solvedSize);
                        tasks.put(String.valueOf(updateTask), oldSize - solvedSize);
                    }
                    runTaskInfo.remove(String.valueOf(oldTask));
                    tasks.remove(String.valueOf(oldTask));
                    try {
                        saveInfo();
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "findTask: not save");
                    }
                }
                break;
            case 2 :
                runTaskInfo.remove(Long.toString(oldTask));
                break;
            default:

        }
        return -1;
    }

    synchronized private void newThread(Activity context, String host, int port) {
        newThread(context, host, port, null);
    }

    synchronized private void newThread(Activity context, String host, int port, Long AimTask) {
        if (ThreadNum >= MxThread) return ;
        for (int i = 0; i < (ThreadNum + 1 < MxThread ? 2 : 1); ++i) {
            ++ThreadNum;
            try {
                exec.execute(() -> {
                    long task = 0;
                    try {
                        if (AimTask == null)
                            download(context, host, port, task = findTask(0, -1, -1, -1));
                        else download(context, host, port, task = AimTask);
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    --ThreadNum;
                    Log.i(TAG, "newThread: task = " + task);
                    try {
                        findTask(2, task, -1, -1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (ThreadNum < MxThread / 2 && !finish) newThread(context, host, port);
                });
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }

    private void download(Activity context, String host, int port, long task) throws IOException, JSONException {
        if (task == -1) {
            Log.i(TAG, "download: " + tasks.length() + " task = " + task);
            if (tasks.length() == 0 && file.exists() && !finish) {
                synchronized (this) {
                    Log.i(TAG, "download: info = " + tasks.toString(4));
                    if (file.renameTo(newFile)) {
                        Log.i(TAG, "download: rename success");
                        Message msg = new Message();
                        finish = true;
                        msg.what = 0;

                        Intent intent = new Intent();
                        intent.putExtra("FileName", info.getString("FileName"));
                        intent.putExtra("FilePath", FileManager.getDownloadDir() + File.separator
                                + info.getString("FileName"));

                        msg.obj = new Pair<>(context, intent);

                        msgHandle.sendMessage(msg);
                    }
                    else Log.i(TAG, "download: rename error");
                }
            }
            return;
        }
        Socket remote = new Socket(host, port);
        remote.setTcpNoDelay(true);
        OutputStream outputStream = remote.getOutputStream();

        ByteArrayOutputStream bufStream = new ByteArrayOutputStream();
        
        bufStream.write("LP2P 0.1\n2".getBytes(StandardCharsets.UTF_8));
        bufStream.write((byte) info.getString("FileName").getBytes(StandardCharsets.UTF_8).length);
        bufStream.write(info.getString("FileName").getBytes(StandardCharsets.UTF_8));
        bufStream.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(info.getLong("FileSize")).array(),
                       2 ,6);
        Log.i(TAG, "download: " + Arrays.toString(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(info.getLong("FileSize")).array()));
        bufStream.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(task).array(), 2, 6);

        Log.i(TAG, "download: " + ArrayUtils.toHexString(bufStream.toByteArray()));
        Log.i(TAG, "download: info : FileSize = " + info.getLong("FileSize") +
                                          " TaskPos = " + task);

        byte[] SizeOfMsg = Arrays.copyOfRange(ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN).putInt(bufStream.size() + 3).array(), 1, 4);
        outputStream.write(ArrayUtils.addArray(SizeOfMsg, bufStream.toByteArray()));

        InputStream inputStream = remote.getInputStream();
        byte[] bytes = new byte[10240];
        int len = -1;
        ArrayList<Byte> res = new ArrayList<>();
        int size = 0;
        int pos = 0;
        int resourceSize = Integer.MAX_VALUE;
        Log.i(TAG, "download: available = " + inputStream.available());
        remote.setSoTimeout(1000);
        while (res.size() < resourceSize && (len = inputStream.read(bytes)) != -1 && (exec != null && !exec.isShutdown())) {
            if (len > 0) {
                res.addAll(Arrays.asList(ArrayUtils.unPrimitive(Arrays.copyOfRange(bytes, 0, len))));
                size += len;
            }
            if (res.size() >= 3 && resourceSize == Integer.MAX_VALUE) {
                resourceSize = ByteBuffer.wrap(ArrayUtils.addArray(new byte[]{0},
                        ArrayUtils.toPrimitive(res.subList(pos, pos + 3).toArray(new Byte[0]))))
                        .order(ByteOrder.BIG_ENDIAN).getInt();
                pos += 3;
            }
        }
        remote.close();
        Log.i(TAG, "download: " + res.size() + ' ' + size);
        byte[] byteRes = ArrayUtils.toPrimitive(res.toArray(new Byte[0]));

        while (pos < res.size() && res.get(pos) != '\n') ++pos;
        if (pos == res.size()) {
            Log.e(TAG, "download: error msg length");
            newThread(context, host, port);
            return;
        } else if (res.get(++pos) != '3') {
            newThread(context, host, port);
            Log.e(TAG, "download: url error");
            return;
        }
        int nameSize = ((byte) res.get(++pos)) & 0xFF;
        ++pos;
        String filename = new String(Arrays.copyOfRange(byteRes, pos, pos + nameSize));
        if (!filename.equals(info.getString("FileName"))) {
            Log.e(TAG, "download: error filename");
            newThread(context, host, port);
            return ;
        }
        pos += nameSize;
        long fileSize = ByteBuffer.wrap(ArrayUtils.addArray(new byte[]{0, 0}, Arrays.copyOfRange(byteRes, pos, pos + 6)))
                        .order(ByteOrder.BIG_ENDIAN).getLong();
        if (fileSize != info.getLong("FileSize")) {
            Log.e(TAG, "download: error file size");
            newThread(context, host, port);
            return ;
        }
        pos += 6;
        int partSize = ByteBuffer.wrap(ArrayUtils.addArray(new byte[]{0}, Arrays.copyOfRange(byteRes, pos, pos + 3)))
                                 .order(ByteOrder.BIG_ENDIAN).getInt();
        pos += 3;
        Log.i(TAG, "download: partSize = " + partSize);
        synchronized (this) {
            if (!tasks.has(Long.toString(task))) {
                return;
            }
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            randomAccessFile.seek(task);
            Log.i(TAG, "download: task = " + task + " size = " + partSize);
            randomAccessFile.write(Arrays.copyOfRange(byteRes, pos, pos + partSize));
            randomAccessFile.close();
            long newTask = task + partSize;
            findTask(1, task, newTask, partSize);
        }
        pos += partSize;
        if (pos < resourceSize) {
            int addressCount = byteRes[pos++] & 0xFF;
            for (int i = 0; i < addressCount; i++) {
                byte[] ip = Arrays.copyOfRange(byteRes, pos, pos + 6);
                pos += 6;
                StringBuilder hostRes = new StringBuilder();
                for (int j = 0; j < 4; j++) {
                    hostRes.append(ByteUtils.toUnsigned(ip[j]));
                    if (j != 3) hostRes.append('.');
                }
                int portRes = ByteUtils.toUnsigned(ip[4]) * (1 << 8) + ByteUtils.toUnsigned(ip[5]);
                synchronized (this) {
                    address.put(hostRes.toString(), portRes);
                    saveInfo();
                }
                newThread(context, hostRes.toString(), portRes);
            }
        }

        {
            Message msg = new Message();

            msg.what = 1;

            Intent intent = new Intent();
            intent.putExtra("FileName", info.getString("FileName"));
            intent.putExtra("FilePath", FileManager.getDownloadDir() + File.separator
                    + info.getString("FileName"));
            intent.putExtra("solvedSize", partSize);

            msg.obj = new Pair<>(context, intent);

            msgHandle.sendMessage(msg);
        }

        if (runTaskInfo.get(String.valueOf(task + partSize)) != null) {
            newThread(context, host, port, task + partSize);
        }
    }

    public void download(Activity context) {
        try {
            exec = Executors.newCachedThreadPool();
            for (Iterator<String> it = address.keys(); it.hasNext(); ) {
                String host = it.next();
                JSONArray ports = address.getJSONArray(host);
                for (int i = 0; i < ports.length(); i++) {
                    newThread(context, host, ports.getInt(i));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (exec != null) {
            exec.shutdown();
            try {
                if (!exec.awaitTermination(5, TimeUnit.SECONDS))
                    exec.shutdownNow();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        ThreadNum = 0;
        runTaskInfo.clear();
    }
}
