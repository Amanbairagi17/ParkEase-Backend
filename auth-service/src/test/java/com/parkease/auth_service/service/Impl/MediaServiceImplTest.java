package com.parkease.auth_service.service.Impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.parkease.auth_service.dtos.ImageUploadResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    @InjectMocks
    private MediaServiceImpl mediaService;

    @Test
    void uploadImage_ShouldThrow_WhenFileNull() {
        assertThrows(RuntimeException.class, () -> mediaService.uploadImage(null));
    }

    @Test
    void uploadImage_ShouldThrow_WhenFileEmpty() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThrows(RuntimeException.class, () -> mediaService.uploadImage(file));
    }

    @Test
    void uploadImage_ShouldThrow_WhenContentTypeInvalid() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("application/pdf");

        assertThrows(RuntimeException.class, () -> mediaService.uploadImage(file));
    }

    @Test
    void uploadImage_ShouldReturnResponse_WhenUploadSucceeds() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(new byte[] {1, 2});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "url", "public_id", "pid"));

        ImageUploadResponse response = mediaService.uploadImage(file);

        assertEquals("url", response.getImageUrl());
        assertEquals("pid", response.getPublicId());
    }

    @Test
    void uploadImage_ShouldThrow_WhenUploadFails() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getBytes()).thenReturn(new byte[] {1, 2});
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new IOException("fail"));

        assertThrows(RuntimeException.class, () -> mediaService.uploadImage(file));
    }

    @Test
    void deleteImage_ShouldReturn_WhenPublicIdBlank() {
        assertDoesNotThrow(() -> mediaService.deleteImage(""));
        assertDoesNotThrow(() -> mediaService.deleteImage(null));
    }

    @Test
    void deleteImage_ShouldThrow_WhenDeleteStatusInvalid() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("pid"), any(Map.class))).thenReturn(Map.of("result", "error"));

        assertThrows(RuntimeException.class, () -> mediaService.deleteImage("pid"));
    }

    @Test
    void deleteImage_ShouldNotThrow_WhenNotFound() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("pid"), any(Map.class))).thenReturn(Map.of("result", "not found"));

        assertDoesNotThrow(() -> mediaService.deleteImage("pid"));
    }
}
