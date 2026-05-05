package com.parkease.auth_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImageUploadResponse{
        private String imageUrl;
        private String publicId;


} 