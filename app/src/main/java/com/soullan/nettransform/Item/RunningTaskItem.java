package com.soullan.nettransform.Item;

import com.soullan.nettransform.Manager.DownloadManager;
import com.soullan.nettransform.exception.DownLoadException;

import org.json.JSONException;

import java.io.IOException;
import java.util.Objects;

public class RunningTaskItem {
    private String FilePath;
    private String FileName;
    private boolean Statue;
    public DownloadManager downloadManager;
    public RunningTaskItem(String FileName, String FilePath) {
        this.FilePath = FilePath;
        this.FileName = FileName;
        Statue = false;
        try {
            downloadManager = new DownloadManager(FileName);
        } catch (DownLoadException | IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public RunningTaskItem(String FileName, String FilePath, boolean statue) {
        this.FilePath = FilePath;
        this.FileName = FileName;
        Statue = statue;
        try {
            downloadManager = new DownloadManager(FileName);
        } catch (DownLoadException | IOException | JSONException e) {
            e.printStackTrace();
        }
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
