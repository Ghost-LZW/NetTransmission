package com.soullan.nettransform.Manager;

import android.content.Context;
import android.util.Log;

import com.soullan.nettransform.Item.TaskItem;
import com.soullan.nettransform.Utils.ArrayUtils;
import com.soullan.nettransform.Utils.ByteUtils;
import com.soullan.nettransform.exception.DownLoadException;
import com.soullan.nettransform.exception.GetInfoException;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class TransmissionManager {
    private static final String TAG = "TransmissionManager";

    public static TaskItem ReceiveFiles(Context context, String address, Integer port) throws IOException, JSONException, DownLoadException {
        Socket remote = new Socket(address, port);
        remote.setTcpNoDelay(true);
        OutputStream outputStream = remote.getOutputStream();
        byte[] SizeOfMsg = Arrays.copyOfRange(ByteBuffer.allocate(4)
                          .order(ByteOrder.BIG_ENDIAN).putInt("LP2P 0.1\n0".length() + 3).array(), 1, 4);
        outputStream.write(ArrayUtils.addArray(SizeOfMsg, "LP2P 0.1\n0".getBytes(StandardCharsets.UTF_8)));

        InputStream inputStream = remote.getInputStream();

        byte[] bytes = new byte[1024];
        int len;
        Log.i(TAG, "ReceiveFiles: remote " + remote.isClosed());
        ArrayList<Byte> res = new ArrayList<>();
        int resourceSize = Integer.MAX_VALUE;
        int pos = 0;
        while (res.size() < resourceSize && (len = inputStream.read(bytes)) != -1) {
            Log.i(TAG, "ReceiveFiles: " + res.size() + ' ' + len);
            res.addAll(Arrays.asList(ArrayUtils.unPrimitive(Arrays.copyOfRange(bytes, 0, len))));
            if (res.size() >= 3 && resourceSize == Integer.MAX_VALUE) {
                resourceSize = ByteBuffer.wrap(ArrayUtils.addArray(new byte[]{0},
                        ArrayUtils.toPrimitive(res.subList(pos, pos + 3).toArray(new Byte[0]))))
                        .order(ByteOrder.BIG_ENDIAN).getInt();
                pos += 3;
            }
            Log.i(TAG, "ReceiveFiles: out " + resourceSize);
        }
        Log.i(TAG, "ReceiveFiles: " + remote.isClosed() + ' ' + res.size());
        Log.i(TAG, "ReceiveFiles: " + Arrays.toString(res.toArray()));
        remote.close();
        while (pos < res.size() && res.get(pos) != '\n') ++pos;
        Log.i(TAG, "ReceiveFiles: pos = " + pos);
        if (pos == res.size()) throw new GetInfoException("first connect error");
        if (res.get(++pos) != '1') {
            Log.e(TAG, "ReceiveFiles: error receive url");
            throw new GetInfoException("first connect tag error");
        }
        int nameSize = ByteUtils.toUnsigned(res.get(++pos));
        pos += 1;
        Log.i(TAG, "ReceiveFiles: " + nameSize + ' ' + pos);
        String fileName = new String(ArrayUtils.toPrimitive(res.toArray(new Byte[0])),
                                     pos, nameSize, StandardCharsets.UTF_8);
        pos += nameSize;
        ByteOrder order = ByteOrder.BIG_ENDIAN;
        long fileSize = ByteBuffer.wrap(ArrayUtils.addArray(new byte[]{0, 0},
                                                   ArrayUtils.toPrimitive(res.subList(pos, pos + 6).toArray(new Byte[0]))))
                                        .order(order).getLong();
        FileManager.createFile(fileName, fileSize);
        FileManager.createInfo(fileName, fileSize, address, port, true);

        return new TaskItem(fileName, FileManager.getDownloadDir() + File.separator + fileName);

//        DownloadManager downloadManager = new DownloadManager(fileName);
//        downloadManager.download(context);
    }


}
