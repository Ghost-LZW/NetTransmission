package com.soullan.nettransform.Manager;

import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FileManager {
    private static String DownloadDir;
    private static final String TAG = "FileManger";

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static void setDownloadDir(String dir) {
        DownloadDir = dir;
    }

    public static String getDownloadDir() {
        return DownloadDir;
    }

    public static void createFile(String filename, long size) throws IOException {
        Log.i(TAG, "Create File: " + filename);
        File file = new File(getDownloadDir() + File.separator + filename + ".ltf");
        if (file.exists() && file.delete()) Log.i(TAG, "createInfo: rm success");

        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        randomAccessFile.setLength(size);
        randomAccessFile.close();
    }

    public static void createInfo(String filename, long size, String host, Integer port, boolean ck) throws JSONException, IOException {
        JSONObject info = new JSONObject();
        info.put("FileName", filename);
        info.put("FileSize", size);
        Map<String, JSONArray> address = new HashMap<>();
        address.put(host, new JSONArray().put(port));
        info.put("address", new JSONObject(address));

        Map<String, Long> res = new HashMap<>();
        if (ck) res.put("0", size);
        info.putOpt("tasks", new JSONObject(res));

        File file = new File(getDownloadDir() + File.separator + filename + ".json");
        if (file.exists() && file.delete()) Log.i(TAG, "createInfo: rm success");
        if (file.createNewFile()) {
            OutputStream output = new FileOutputStream(file);
            output.write(info.toString(4).getBytes(StandardCharsets.UTF_8));
            output.close();
        } else Log.e(TAG, "createInfo: createFile error");
    }

}
