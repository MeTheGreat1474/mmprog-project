package com.example.project.models;

public class ImageRecord {
    private int id;
    private String filePath;
    private String filename;
    private long importDate;

    public ImageRecord() {}

    public ImageRecord(int id, String filePath, String filename, long importDate) {
        this.id = id;
        this.filePath = filePath;
        this.filename = filename;
        this.importDate = importDate;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public long getImportDate() { return importDate; }
    public void setImportDate(long importDate) { this.importDate = importDate; }
}
