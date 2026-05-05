package com.parkease.auth_service.service;

import com.parkease.auth_service.dtos.ImageUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface MediaService {
    ImageUploadResponse uploadImage(MultipartFile file);

    void deleteImage(String publicId);
}
