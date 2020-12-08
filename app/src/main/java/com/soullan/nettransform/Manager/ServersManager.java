package com.soullan.nettransform.Manager;

import android.util.Log;

import androidx.core.util.Pair;

import com.soullan.nettransform.Utils.ArrayUtils;
import com.soullan.nettransform.Utils.ByteUtils;
import com.soullan.nettransform.constant.FileConstant;
import com.soullan.nettransform.constant.NetworkConstant;
import com.soullan.nettransform.exception.GetInfoException;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServersManager {
    private static final String TAG = "ServersManager";
    private static ServerSocket server;
    private volatile static ExecutorService exec;
    private static String fileName = FileManager.getDownloadDir() + File.separator + "76766589_p0_master1200.jpg";
    private static File file;
    private static Random rand = new Random();

    static {
        try {
            server = new ServerSocket(NetworkConstant.PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getPort() {
        return server.getLocalPort();
    }

    public static void setFileName(String res) {
        fileName = res;
    }

    synchronized void shutDown() {
        exec.shutdownNow();
    }

    public static void serverClient(Socket client) throws IOException, JSONException {
        ByteArrayOutputStream request = new ByteArrayOutputStream();
        byte[] bytes = new byte[1024];
        int len = 0;
        int pos = 0;
        int requestSize = Integer.MAX_VALUE;
        InputStream inputStream = client.getInputStream();
        while (request.size() < requestSize && (len = inputStream.read(bytes)) != -1) {
            request.write(bytes, 0, len);
            if (request.size() >= 3 && requestSize == Integer.MAX_VALUE) {
                requestSize = ByteBuffer.wrap(ArrayUtils.addArray(new byte[]{0}, Arrays.copyOfRange(request.toByteArray(), 0, 3)),
                                       0, 4).order(ByteOrder.BIG_ENDIAN).getInt();
                pos += 3;
                Log.i(TAG, "serverClient: requestSize = " + requestSize);
            }
        }
        byte[] byteRequest = request.toByteArray();
        while (pos < byteRequest.length && byteRequest[pos] != '\n') ++pos;
        if (pos == byteRequest.length) throw new GetInfoException("get info error");
        switch (byteRequest[++pos]) {
            case '0' :
                ByteArrayOutputStream info = new ByteArrayOutputStream();
                info.write("LP2P 0.1\n1".getBytes(StandardCharsets.UTF_8));
                Log.i(TAG, "serverClient: " + file.getName());
                byte[] name = file.getName().getBytes(StandardCharsets.UTF_8);
                info.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(name.length).array(), 3, 1);
                info.write(name);
                info.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(file.length()).array(), 2, 6);

                byte[] infoSize = Arrays.copyOfRange(ByteBuffer.allocate(4)
                                  .order(ByteOrder.BIG_ENDIAN).putInt(info.size() + 3).array(), 1, 4);

                client.getOutputStream().write(ArrayUtils.addArray(infoSize, info.toByteArray()));
                break;
            case '2' :
                int nameSize = ByteUtils.toUnsigned(byteRequest[++pos]);
                ++pos;
                String fileName = new String(Arrays.copyOfRange(byteRequest, pos, pos + nameSize), StandardCharsets.UTF_8);
                pos += nameSize;
                long fileSize = ByteBuffer.wrap(ArrayUtils.addArray(new byte[]{0, 0},
                                                                    Arrays.copyOfRange(byteRequest, pos, pos + 6)))
                                          .order(ByteOrder.BIG_ENDIAN).getLong();
                pos += 6;
                long aimPos = ByteBuffer.wrap(ArrayUtils.addArray(new byte[]{0, 0},
                                                                  Arrays.copyOfRange(byteRequest, pos, pos + 6)))
                                          .order(ByteOrder.BIG_ENDIAN).getLong();
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                if (!checkTask(aimPos)) {
                    data.write("LP2P 0.1\n4".getBytes(StandardCharsets.UTF_8));
                    byte[] dataSize = Arrays.copyOfRange(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                            .putInt(data.size() + 3).array(), 1, 4);

                    client.getOutputStream().write(ArrayUtils.addArray(dataSize, data.toByteArray()));
                    break;
                }
                data.write("LP2P 0.1\n3".getBytes(StandardCharsets.UTF_8));
                byte[] nameF = file.getName().getBytes(StandardCharsets.UTF_8);
                data.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(nameF.length).array(), 3, 1);
                data.write(nameF);
                data.write(ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(file.length()).array(), 2, 6);

                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                byte[] filePart = new byte[FileConstant.PART];
                randomAccessFile.seek(aimPos);
                Log.i(TAG, "serverClient: random " + file.getPath() + " " + file.canRead() + " " + file.length());
                int size = randomAccessFile.read(filePart);
                randomAccessFile.close();
                data.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(size).array(), 1, 3);
                Log.i(TAG, "serverClient: " + size + ' ' + filePart.length + " " + aimPos + " " + fileSize + " " + fileName);
                data.write(filePart, 0, size);

                if (rand.nextInt(10) == 4) {
                    String remote = client.getInetAddress().getHostAddress();
                    int remotePort = client.getPort();
                    File infoFile = new File(fileName + ".json");
                    if (!infoFile.exists()) FileManager.createInfo(fileName, fileSize, remote, remotePort, false);
                    else {
                        JSONObject infoF = new JSONObject(FileUtils.readFileToString(infoFile));
                        if (!infoF.getJSONObject("address").has(remote)) {
                            int addressSize = infoF.getJSONObject("address").length();
                            List<Byte> res = new ArrayList<>(); int cnt = 0;
                            for (Iterator<String> it = infoF.getJSONObject("address").keys(); it.hasNext(); ) {
                                String key = it.next();
                                byte[] ip = new byte[6];
                                int posCount = 0;
                                for (String p : key.split("."))
                                    ip[posCount++] = ByteUtils.getLastByte(Integer.parseInt(p));
                                for (int i = 0; i < len; i++) {
                                    String port = infoF.getJSONObject("address").getJSONArray(key).getString(i);
                                    Pair<Byte, Byte> tm = ByteUtils.getLastTwoBytes(Integer.parseInt(port));
                                    ip[4] = tm.first;
                                    ip[5] = tm.second;
                                    for (int j = 0; j < 6; j++) res.add(ip[i]);
                                    cnt += 1;
                                }
                            }
                            data.write(ByteUtils.getLastByte(cnt));
                            data.write(ArrayUtils.toPrimitive(res.toArray(new Byte[0])));
                        }
                    }
                }

                byte[] dataSize = Arrays.copyOfRange(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
                                                     .putInt(data.size() + 3).array(), 1, 4);

                client.getOutputStream().write(ArrayUtils.addArray(dataSize, data.toByteArray()));
                break;
        }
    }

    private static boolean checkTask(long aimPos) throws IOException, JSONException {
        File infoFile = new File(fileName + ".json");
        if (!infoFile.exists()) return true;
        JSONObject infoF = new JSONObject(FileUtils.readFileToString(infoFile));
        for (Iterator<String> it = infoF.getJSONObject("tasks").keys(); it.hasNext(); ) {
            String key = it.next();
            long l = Integer.parseInt(key);
            long r = l + infoF.getJSONObject("tasks").getLong(key);
            if (aimPos >= l && aimPos < r) {
                return false;
            }
        }
        return true;
    }

    public static void startServers() throws IOException {
        Socket client = null;
        file = new File(fileName);
        Log.i(TAG, "startServers: " + fileName);
        exec = Executors.newCachedThreadPool();
        Log.i(TAG, "startServers: start server " + server.getLocalPort());
        while ((client = server.accept()) != null && !exec.isShutdown()) {
            Socket finalClient = client;
            Log.i(TAG, "startServers: get in ");
            exec.execute(() -> {
                try {
                    serverClient(finalClient);
                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            });
        }
        exec.shutdown();
    }
}
