package com.soullan.nettransform.Item;

import java.util.Objects;

public class SolvedTaskItem {
    private String FilePath;
    private String FileName;
    public SolvedTaskItem(String FileName, String FilePath) {
        this.FilePath = FilePath;
        this.FileName = FileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SolvedTaskItem that = (SolvedTaskItem) o;
        return Objects.equals(FilePath, that.FilePath) &&
                Objects.equals(FileName, that.FileName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(FilePath, FileName);
    }

    public String getFileName() {
        return FileName;
    }

    public String getFilePath() {
        return FilePath;
    }

    public void setFileName(String fileName) {
        FileName = fileName;
    }

    public void setFilePath(String filePath) {
        FilePath = filePath;
    }
}
