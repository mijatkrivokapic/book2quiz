package com.example.book2quiz.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {
    String uploadFile(MultipartFile file, String directory);
    void deleteFile(String fileKey);
    public String getPreSignedUrl(String fileKey);

    /**
     * Uploads raw bytes under an explicit object key (no random UUID), overwriting
     * any existing object. Used for derived artifacts such as chapter PDFs and
     * Markdown files whose keys are computed deterministically.
     */
    void uploadBytes(byte[] content, String fileKey, String contentType);

    /**
     * Downloads the full object into memory. Used by the chapter extraction pipeline
     * which needs the book PDF as a byte array for PDFBox.
     */
    byte[] downloadFile(String fileKey);

    /**
     * Opens a stream to the object. Caller is responsible for closing it. Used for
     * streaming Markdown content back to clients.
     */
    InputStream openStream(String fileKey);

    /**
     * The bucket all objects live in.
     */
    String getBucketName();
}
