package com.vectoredu.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class BackupService {

    private final S3Service s3Service;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${backup.directory:/tmp}")
    private String backupDirectory;

    public BackupService(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    public void createAndUploadBackup() throws IOException {
        String dbName = getDatabaseNameFromUrl(dbUrl);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupFileName = String.format("backup_%s_%s.sql", dbName, timestamp);
        String backupFilePath = Paths.get(backupDirectory, backupFileName).toString();

        createDatabaseBackup(dbName, backupFilePath);
        uploadBackupToS3(backupFilePath, backupFileName);
    }

    private void createDatabaseBackup(String dbName, String backupFilePath) throws IOException {
        String command = String.format("PGPASSWORD=%s pg_dump -U %s -h db -d %s --no-owner --no-comments -F p -f %s",
                dbPassword, dbUser, dbName, backupFilePath);

        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        try {
            if (process.waitFor() != 0) {
                throw new IOException("Ошибка при создании резервной копии БД");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Процесс резервного копирования был прерван", e);
        }
    }

    private void uploadBackupToS3(String backupFilePath, String backupFileName) throws IOException {
        File backupFile = new File(backupFilePath);

        // Проверяем, существует ли файл перед загрузкой
        if (!backupFile.exists()) {
            throw new IOException("Файл резервной копии не найден: " + backupFilePath);
        }

        // Логируем путь к файлу перед загрузкой
        System.out.println("Загружаем файл резервной копии на S3: " + backupFilePath);

        s3Service.uploadBackupFile(backupFileName, backupFile);
        Files.deleteIfExists(backupFile.toPath());
    }

    private String getDatabaseNameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}

