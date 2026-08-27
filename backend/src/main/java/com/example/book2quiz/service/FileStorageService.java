package com.example.book2quiz.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String directory);
    void deleteFile(String fileKey);
    public String getPreSignedUrl(String fileKey);
}
