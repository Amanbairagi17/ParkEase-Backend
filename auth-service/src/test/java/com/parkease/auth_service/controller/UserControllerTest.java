package com.parkease.auth_service.controller;

import com.parkease.auth_service.dtos.AuthResponseDto;
import com.parkease.auth_service.dtos.ProfileUpdateDto;
import com.parkease.auth_service.service.UserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    @Test
    void getUserById_ShouldReturnOk() {

        AuthResponseDto responseDto = new AuthResponseDto();
        responseDto.setUserId(1L);
        responseDto.setEmail("user@demo.com");

        when(userService.findUserById(1L))
                .thenReturn(responseDto);

        ResponseEntity<AuthResponseDto> response =
                userController.getUserById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getUserId());
        assertEquals(
                "user@demo.com",
                response.getBody().getEmail()
        );

        verify(userService).findUserById(1L);
    }

    @Test
    void getUserByEmail_ShouldReturnOk() {

        AuthResponseDto responseDto = new AuthResponseDto();
        responseDto.setEmail("user@demo.com");

        when(userService.findUserByEmail("user@demo.com"))
                .thenReturn(responseDto);

        ResponseEntity<AuthResponseDto> response =
                userController.getUserByEmail("user@demo.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(
                "user@demo.com",
                response.getBody().getEmail()
        );

        verify(userService)
                .findUserByEmail("user@demo.com");
    }

    @Test
    void updateProfile_ShouldReturnOk() {

        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFullName("Test User");
        dto.setPhone("9876543210");

        AuthResponseDto responseDto = new AuthResponseDto();
        responseDto.setUserId(1L);

        when(userService.updateProfile(anyLong(), any(ProfileUpdateDto.class)))
                .thenReturn(responseDto);

        ResponseEntity<AuthResponseDto> response =
                userController.updateProfile(1L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getUserId());

        verify(userService)
                .updateProfile(anyLong(), any(ProfileUpdateDto.class));
    }

    @Test
    void deleteProfileImage_ShouldReturnOk() {

        doNothing().when(userService)
                .deleteProfileImage(1L);

        ResponseEntity<String> response =
                userController.deleteProfileImage(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "Profile image deleted successfully",
                response.getBody()
        );

        verify(userService).deleteProfileImage(1L);
    }

    @Test
    void updateProfilePicture_ShouldReturnOk() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "profile.jpg",
                        "image/jpeg",
                        "dummy-image".getBytes()
                );

        AuthResponseDto responseDto = new AuthResponseDto();
        responseDto.setUserId(1L);

        when(userService.updateProfilePicture(anyLong(), any()))
                .thenReturn(responseDto);

        ResponseEntity<AuthResponseDto> response =
                userController.updateProfilePicture(1L, file);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getUserId());

        verify(userService)
                .updateProfilePicture(anyLong(), any());
    }
}