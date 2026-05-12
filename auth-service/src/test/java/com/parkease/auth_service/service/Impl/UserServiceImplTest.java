package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.dtos.ImageUploadResponse;
import com.parkease.auth_service.dtos.ProfileUpdateDto;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.mapper.Impl.AuthResponseMapper;
import com.parkease.auth_service.repository.UserRepository;
import com.parkease.auth_service.service.MediaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;
    @Mock
    private AuthResponseMapper authResponseMapper;
    @Mock
    private MediaService mediaService;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void findUserById_ShouldReturnDto_WhenUserExists() {
        User user = buildUser();
        AuthResponseDto response = new AuthResponseDto();
        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(authResponseMapper.mapTo(user)).thenReturn(response);

        AuthResponseDto result = userService.findUserById(1L);

        assertSame(response, result);
    }

    @Test
    void findUserById_ShouldThrow_WhenMissing() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.findUserById(1L));
    }

    @Test
    void findUserByEmail_ShouldReturnDto_WhenUserExists() {
        User user = buildUser();
        AuthResponseDto response = new AuthResponseDto();
        when(repository.findByEmail("user@demo.com")).thenReturn(Optional.of(user));
        when(authResponseMapper.mapTo(user)).thenReturn(response);

        AuthResponseDto result = userService.findUserByEmail("user@demo.com");

        assertSame(response, result);
    }

    @Test
    void updateProfile_ShouldUpdateFieldsAndUploadImage_WhenFileProvided() {
        User user = buildUser();
        user.setProfilePicPublicId("old-public");
        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFullName("Updated");
        dto.setEmail("new@demo.com");
        dto.setPhone("9999999999");
        dto.setAddress("Addr");

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        dto.setProfileImageFile(file);

        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(mediaService.uploadImage(file)).thenReturn(new ImageUploadResponse("url", "publicId"));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authResponseMapper.mapTo(any(User.class))).thenReturn(new AuthResponseDto());

        userService.updateProfile(1L, dto);

        verify(mediaService).deleteImage("old-public");
        verify(mediaService).uploadImage(file);
        verify(repository).save(userCaptor.capture());
        assertEquals("url", userCaptor.getValue().getProfilePicUrl());
        assertEquals("publicId", userCaptor.getValue().getProfilePicPublicId());
    }

    @Test
    void updateProfile_ShouldSkipUpload_WhenFileMissing() {
        User user = buildUser();
        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFullName("Updated");
        dto.setEmail("new@demo.com");
        dto.setPhone("9999999999");
        dto.setAddress("Addr");

        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authResponseMapper.mapTo(any(User.class))).thenReturn(new AuthResponseDto());

        userService.updateProfile(1L, dto);

        verify(mediaService, never()).deleteImage(anyString());
        verify(mediaService, never()).uploadImage(any(MultipartFile.class));
    }

    @Test
    void updateProfile_ShouldSkipUpload_WhenFileEmpty() {
        User user = buildUser();
        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFullName("Updated");
        dto.setEmail("new@demo.com");
        dto.setPhone("9999999999");
        dto.setAddress("Addr");

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        dto.setProfileImageFile(file);

        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(authResponseMapper.mapTo(any(User.class))).thenReturn(new AuthResponseDto());

        userService.updateProfile(1L, dto);

        verify(mediaService, never()).deleteImage(anyString());
        verify(mediaService, never()).uploadImage(any(MultipartFile.class));
    }

    @Test
    void deleteProfileImage_ShouldRemoveImageAndSave_WhenPublicIdPresent() {
        User user = buildUser();
        user.setProfilePicPublicId("public");
        user.setProfilePicUrl("url");
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteProfileImage(1L);

        verify(mediaService).deleteImage("public");
        verify(repository).save(user);
        assertNull(user.getProfilePicPublicId());
        assertNull(user.getProfilePicUrl());
    }

    @Test
    void deleteProfileImage_ShouldSkipDelete_WhenNoPublicId() {
        User user = buildUser();
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteProfileImage(1L);

        verify(mediaService, never()).deleteImage(anyString());
        verify(repository).save(user);
    }

    @Test
    void updateProfilePicture_ShouldThrow_WhenFileMissing() {
        when(repository.findById(1L)).thenReturn(Optional.of(buildUser()));

        assertThrows(RuntimeException.class, () -> userService.updateProfilePicture(1L, null));
    }

    @Test
    void updateProfilePicture_ShouldThrow_WhenFileEmpty() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        when(repository.findById(1L)).thenReturn(Optional.of(buildUser()));

        assertThrows(RuntimeException.class, () -> userService.updateProfilePicture(1L, file));
    }

    @Test
    void updateProfilePicture_ShouldReplaceImage_WhenValid() {
        User user = buildUser();
        user.setProfilePicPublicId("old");
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);

        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(mediaService.uploadImage(file)).thenReturn(new ImageUploadResponse("url", "new"));
        when(authResponseMapper.mapTo(any(User.class))).thenReturn(new AuthResponseDto());

        userService.updateProfilePicture(1L, file);

        verify(mediaService).deleteImage("old");
        verify(mediaService).uploadImage(file);
        verify(repository).save(user);
        assertEquals("url", user.getProfilePicUrl());
        assertEquals("new", user.getProfilePicPublicId());
    }

    private User buildUser() {
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@demo.com");
        return user;
    }
}
