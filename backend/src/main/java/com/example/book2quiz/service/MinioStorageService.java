package com.example.book2quiz.service;

import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioStorageService implements FileStorageService {

    private final MinioClient internalClient;
    private final MinioClient externalClient;
    private final String bucketName = "books-bucket";

    public MinioStorageService(
            @Value("${minio.internal-url}") String internalUrl,
            @Value("${minio.external-url}") String externalUrl,
            @Value("${minio.access.key}") String accessKey,
            @Value("${minio.secret.key}") String secretKey) {

        this.internalClient = MinioClient.builder()
                .endpoint(internalUrl)
                .credentials(accessKey, secretKey)
                .build();

        this.externalClient = MinioClient.builder()
                .endpoint(externalUrl)
                .credentials(accessKey, secretKey)
                .region("us-east-1")//TODO make configurable
                .build();

        initBucket();
    }

    private void initBucket() {
        try {
            boolean found = internalClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                internalClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during MinIO bucket init", e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String directory) {
        try {
            String fileKey = directory + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

            internalClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return fileKey;
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file", e);
        }
    }

    @Override
    public String getPreSignedUrl(String fileKey) {
        try {
            return externalClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(fileKey)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error generating pre-signed URL-a for file: " + fileKey, e);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        // TODO: implement delete
    }
}