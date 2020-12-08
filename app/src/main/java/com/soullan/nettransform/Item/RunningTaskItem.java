package com.soullan.nettransform.Item;

import android.app.Activity;

import com.soullan.nettransform.Manager.DownloadManager;
import com.soullan.nettransform.exception.DownLoadException;

import org.json.JSONException;

import java.io.IOException;
import java.util.Objects;

public class RunningTaskItem {
    private String FilePath;
    private String FileName;
    private boolean Statue;
    private DownloadManager downloadManager;
    private long FileSize;
    private long proSize;
    public RunningTaskItem(String FileName, String FilePath, long fileSize, long Unsolved) {
        this.FilePath = FilePath;
        this.FileName = FileName;
        Statue = false;
        downloadManager = null;
        FileSize = fileSize;
        proSize = fileSize - Unsolved;
    }

    public RunningTaskItem(String FileName, String FilePath, boolean statue) {
        this.FilePath = FilePath;
        this.FileName = FileName;
        Statue = statue;
        downloadManager = null;
        FileSize = 0;
        proSize = 0;
    }

    public void download(Activity context) throws JSONException, DownLoadException, IOException {
        if (downloadManager == null) downloadManager = new DownloadManager(getFileName());
        downloadManager.download(context);
    }

    public void shutdown() {
        if (downloadManager != null) downloadManager.shutdown();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RunningTaskItem that = (RunningTaskItem) o;
        return  FilePath.equals(that.FilePath) &&
                FileName.equals(that.FileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(FilePath, FileName, Statue);
    }

    public void setProSize(long proSize) {
        this.proSize = proSize;
    }

    public void addProSize(int size) {
        proSize += size;
        if (proSize >= FileSize) proSize = FileSize;
    }

    public long getFileSize() {
        return FileSize;
    }

    public long getProSize() {
        return proSize;
    }

    public String getFileName() {
        return FileName;
    }

    public String getFilePath() {
        return FilePath;
    }

    public boolean getStatue() {
        return Statue;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public void setFilePath(String filePath) {
        FilePath = filePath;
    }

    public void setStatue(boolean statue) {
        Statue = statue;
    }
}
