package com.example.project.database;

import com.example.project.models.ImageRecord;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    public static final String APP_FOLDER = System.getProperty("user.home") + File.separator + "DarkroomLibrary";
    public static final String IMAGES_FOLDER = APP_FOLDER + File.separator + "Images";
    private static final String DB_FILE = APP_FOLDER + File.separator + "library_catalog.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    private static DatabaseManager instance;

    private DatabaseManager() {
        initializeDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private Connection connect() {
        Connection conn = null;
        try {
            File folder = new File(APP_FOLDER);
            if (!folder.exists()) folder.mkdirs();
            File imgFolder = new File(IMAGES_FOLDER);
            if (!imgFolder.exists()) imgFolder.mkdirs();

            conn = DriverManager.getConnection(DB_URL);
        } catch (Exception e) {
            System.out.println("SQL connection failed: " + e.getMessage());
        }
        return conn;
    }

    public void initializeDatabase() {
        String sqlImages = "CREATE TABLE IF NOT EXISTS images (\n"
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                + " file_path TEXT NOT NULL UNIQUE,\n"
                + " filename TEXT NOT NULL,\n"
                + " import_date INTEGER NOT NULL\n"
                + ");";

        String sqlAnnotations = "CREATE TABLE IF NOT EXISTS annotations (\n"
                + " file_path TEXT PRIMARY KEY,\n"
                + " notes TEXT NOT NULL\n"
                + ");";

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            if (conn != null) {
                stmt.execute(sqlImages);
                stmt.execute(sqlAnnotations);
            }
        } catch (Exception e) {
            System.out.println("Init failed: " + e.getMessage());
        }
    }

    public void insertImageRecord(ImageRecord record) {
        String sql = "INSERT OR IGNORE INTO images(file_path, filename, import_date) VALUES(?,?,?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, record.getFilePath());
            pstmt.setString(2, record.getFilename());
            pstmt.setLong(3, record.getImportDate());
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                record.setId(rs.getInt(1));
            }
        } catch (Exception e) {
            System.out.println("Insert failed: " + e.getMessage());
        }
    }

    public List<ImageRecord> loadLibrary() {
        List<ImageRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM images ORDER BY import_date ASC";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new ImageRecord(
                        rs.getInt("id"),
                        rs.getString("file_path"),
                        rs.getString("filename"),
                        rs.getLong("import_date")
                ));
            }
        } catch (Exception e) {
            System.out.println("Load failed: " + e.getMessage());
        }
        return list;
    }

    public void saveAnnotation(String filePath, String notes) {
        String sql = "INSERT OR REPLACE INTO annotations(file_path, notes) VALUES(?,?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, filePath);
            pstmt.setString(2, notes);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Save note failed: " + e.getMessage());
        }
    }

    public void deleteAnnotation(String filePath) {
        String sql = "DELETE FROM annotations WHERE file_path = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, filePath);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("Delete note failed: " + e.getMessage());
        }
    }

    public Map<String, String> loadAllAnnotations() {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT file_path, notes FROM annotations";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("file_path"), rs.getString("notes"));
            }
        } catch (Exception e) {
            System.out.println("Load notes failed: " + e.getMessage());
        }
        return map;
    }
}
