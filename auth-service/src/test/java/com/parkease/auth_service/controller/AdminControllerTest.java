package com.parkease.auth_service.controller;

import com.parkease.auth_service.dtos.AdminStatsDto;
import com.parkease.auth_service.dtos.AdminUserResponseDto;
import com.parkease.auth_service.dtos.BroadcastRequestDto;
import com.parkease.auth_service.dtos.WarnUserRequestDto;
import com.parkease.auth_service.service.AdminService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @InjectMocks
    private AdminController adminController;

    @Mock
    private AdminService adminService;

    @Test
    void getAllUsers_ShouldReturnOk() {

        AdminUserResponseDto dto = new AdminUserResponseDto();
        dto.setUserId(1L);

        when(adminService.getAllUsers())
                .thenReturn(List.of(dto));

        ResponseEntity<List<AdminUserResponseDto>> response =
                adminController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(adminService).getAllUsers();
    }

    @Test
    void getUserById_ShouldReturnOk() {

        AdminUserResponseDto dto = new AdminUserResponseDto();
        dto.setUserId(1L);

        when(adminService.getUserById(1L))
                .thenReturn(dto);

        ResponseEntity<AdminUserResponseDto> response =
                adminController.getUserById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getUserId());

        verify(adminService).getUserById(1L);
    }

    @Test
    void suspendUser_ShouldReturnOk() {

        doNothing().when(adminService).suspendUser(1L);

        ResponseEntity<String> response =
                adminController.suspendUser(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "User 1 suspended successfully",
                response.getBody()
        );

        verify(adminService).suspendUser(1L);
    }

    @Test
    void reactivateUser_ShouldReturnOk() {

        doNothing().when(adminService).reactivateUser(1L);

        ResponseEntity<String> response =
                adminController.reactivateUser(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "User 1 reactivated successfully",
                response.getBody()
        );

        verify(adminService).reactivateUser(1L);
    }

    @Test
    void deleteUser_ShouldReturnNoContent() {

        doNothing().when(adminService).deleteUser(1L);

        ResponseEntity<String> response =
                adminController.deleteUser(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(adminService).deleteUser(1L);
    }

    @Test
    void getUsersByRole_ShouldReturnOk() {

        AdminUserResponseDto dto = new AdminUserResponseDto();
        dto.setUserId(1L);

        when(adminService.getUsersByRole("DRIVER"))
                .thenReturn(List.of(dto));

        ResponseEntity<List<AdminUserResponseDto>> response =
                adminController.getUsersByRole("DRIVER");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());

        verify(adminService).getUsersByRole("DRIVER");
    }

    @Test
    void broadcastToAll_ShouldReturnOk() {

        BroadcastRequestDto request = new BroadcastRequestDto();
        request.setTitle("Test");
        request.setMessage("Message");

        doNothing().when(adminService)
                .broadcastToAll(any(BroadcastRequestDto.class));

        ResponseEntity<String> response =
                adminController.broadcastToAll(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "Broadcast sent to all users",
                response.getBody()
        );

        verify(adminService)
                .broadcastToAll(any(BroadcastRequestDto.class));
    }

    @Test
    void broadcastToDrivers_ShouldReturnOk() {

        BroadcastRequestDto request = new BroadcastRequestDto();
        request.setTitle("Test");
        request.setMessage("Message");

        doNothing().when(adminService)
                .broadcastToDrivers(any(BroadcastRequestDto.class));

        ResponseEntity<String> response =
                adminController.broadcastToDrivers(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "Broadcast sent to all drivers",
                response.getBody()
        );

        verify(adminService)
                .broadcastToDrivers(any(BroadcastRequestDto.class));
    }

    @Test
    void broadcastToManagers_ShouldReturnOk() {

        BroadcastRequestDto request = new BroadcastRequestDto();
        request.setTitle("Test");
        request.setMessage("Message");

        doNothing().when(adminService)
                .broadcastToManagers(any(BroadcastRequestDto.class));

        ResponseEntity<String> response =
                adminController.broadcastToManagers(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "Broadcast sent to all lot managers",
                response.getBody()
        );

        verify(adminService)
                .broadcastToManagers(any(BroadcastRequestDto.class));
    }

    @Test
    void warnUser_ShouldReturnOk() {

        WarnUserRequestDto request = new WarnUserRequestDto();
        request.setTitle("Warning");
        request.setMessage("Test warning");

        doNothing().when(adminService)
                .warnUser(anyLong(), any(WarnUserRequestDto.class));

        ResponseEntity<String> response =
                adminController.warnUser(1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "Warning sent to user 1",
                response.getBody()
        );

        verify(adminService)
                .warnUser(anyLong(), any(WarnUserRequestDto.class));
    }

    @Test
    void getPlatformStats_ShouldReturnOk() {

        AdminStatsDto statsDto = new AdminStatsDto();

        when(adminService.getPlatformStats())
                .thenReturn(statsDto);

        ResponseEntity<AdminStatsDto> response =
                adminController.getPlatformStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        verify(adminService).getPlatformStats();
    }
}