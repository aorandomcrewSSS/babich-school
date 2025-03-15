package com.vectoredu.backend.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.vectoredu.backend.util.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

@Service
public class S3Service {

    private final AmazonS3 s3client;

    @Value("${selectel.s3.bucket-name}")
    private String bucketName;

    @Value("${selectel.s3.domain-name}")
    private String domain;

    // Максимальный размер изображения (25 MB)
    private static final long MAX_IMAGE_SIZE = 25 * 1024 * 1024;

    // Максимальный размер видео (200 MB)
    private static final long MAX_VIDEO_SIZE = 2000 * 1024 * 1024;

    // Разрешенные типы файлов для изображений
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/svg+xml");

    // Разрешенные типы файлов для видео
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList("video/mp4", "video/avi", "video/mkv");


    public S3Service(AmazonS3 s3client) {
        this.s3client = s3client;
    }

    public void uploadImage(String objectName, MultipartFile file) throws IOException {
        // Проверка размера изображения
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new ValidationException("Размер изображения слишком большой. Максимальный размер: " + MAX_IMAGE_SIZE / (1024 * 1024) + " MB");
        }

        // Проверка типа изображения
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new ValidationException("Неверный формат изображения. Разрешены только: " + String.join(", ", ALLOWED_IMAGE_TYPES));
        }

        uploadFile(objectName, file);
    }

    public void uploadVideo(String objectName, MultipartFile file) throws IOException {
        // Проверка размера видео
        if (file.getSize() > MAX_VIDEO_SIZE) {
            throw new ValidationException("Размер видео слишком большой. Максимальный размер: " + MAX_VIDEO_SIZE / (1024 * 1024) + " MB");
        }

        // Проверка типа видео
        if (!ALLOWED_VIDEO_TYPES.contains(file.getContentType())) {
            throw new ValidationException("Неверный формат видео. Разрешены только: " + String.join(", ", ALLOWED_VIDEO_TYPES));
        }

        uploadFile(objectName, file);
    }

    private void uploadFile(String objectName, MultipartFile file) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            s3client.putObject(new PutObjectRequest(bucketName, objectName, inputStream, metadata));
        }
    }

    public void uploadBackupFile(String objectName, File file) throws IOException {
        // Логирование ключей перед загрузкой
        System.out.println("Используем ключи доступа: " + System.getenv("SELECTEL_S3_ACCESS_KEY"));

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.length());

        try (InputStream inputStream = new FileInputStream(file)) {
            s3client.putObject(new PutObjectRequest(bucketName, objectName, inputStream, metadata));
            System.out.println("Файл успешно загружен на S3");
        }
    }

    public S3Object downloadFile(String objectName) {
        return s3client.getObject(bucketName, objectName);
    }

    public String getFileUrl(String objectName) {
        return String.format("https://%s/%s", domain, objectName);
    }

    public void deleteFile(String objectName) {
        s3client.deleteObject(bucketName, objectName);
    }

    public void createBucket() {
        if (!s3client.doesBucketExistV2(bucketName)) {
            s3client.createBucket(bucketName);
        }
    }
}