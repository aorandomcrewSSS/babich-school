package com.vectoredu.backend.config;

import com.vectoredu.backend.service.BackupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BackupScheduler {

    private final BackupService backupService;

    public BackupScheduler(BackupService backupService) {
        this.backupService = backupService;
    }

//    @Scheduled(fixedRate = 10800000) // Каждые 3 часа
    public void scheduleDatabaseBackup() {
        try {
            backupService.createAndUploadBackup();
            System.out.println("Резервное копирование завершено");
        } catch (IOException e) {
            System.err.println("Ошибка при резервном копировании: " + e.getMessage());
        }
    }
}