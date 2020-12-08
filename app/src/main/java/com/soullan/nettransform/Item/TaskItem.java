package com.soullan.nettransform.Item;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class TaskItem {
    private String FileName;
    private String FilePath;
    private File infoFile;

    public TaskItem(String FileName, String FilePath) {
        this.FileName = FileName;
        this.FilePath = FilePath;
        infoFile = new File(FilePath + ".json");
    }

    public boolean isFinish() throws IOException, JSONException {
        JSONObject info = new JSONObject(FileUtils.readFileToString(infoFile));
        return info.getJSONObject("tasks").length() == 0;
    }

    public String getFilePath() {
        return FilePath;
    }

    public String getFileName() {
        return FileName;
    }

    public File getInfoFile() {
        return infoFile;
    }

    public void setFilePath(String filePath) {
        FilePath = filePath;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public void setInfoFile(File infoFile) {
        this.infoFile = infoFile;
    }
}
