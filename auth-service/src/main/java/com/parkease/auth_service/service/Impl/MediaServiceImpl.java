package com.parkease.auth_service.service.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.parkease.auth_service.dtos.ImageUploadResponse;
import com.parkease.auth_service.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final Cloudinary cloudinary;

    @Override
    public ImageUploadResponse uploadImage(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getContentType() == null ||
                !file.getContentType().startsWith("image/")) {
            throw new RuntimeException("Only image files are allowed");
        }

        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "profile_images",
                            "public_id", UUID.randomUUID().toString(),
                            "resource_type", "image"
                    )
            );
            log.info("Uploading image to Cloudinary...");
            return new ImageUploadResponse(
                    uploadResult.get("secure_url").toString(),
                    uploadResult.get("public_id").toString()
            );

        } catch (IOException e) {
            throw new RuntimeException("Failed to upload image to Cloudinary", e);
        }
    }

    @Override
    public void deleteImage(String publicId) {

        if (publicId == null || publicId.isBlank()) {
            return; // nothing to delete
        }

        try {
            Map<?, ?> result = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "image")
            );

            String status = result.get("result").toString();

            if (!status.equals("ok") && !status.equals("not found")) {
                throw new RuntimeException("Failed to delete image from Cloudinary");
            }

        } catch (IOException e) {
            throw new RuntimeException("Error deleting image from Cloudinary", e);
        }
    }

}