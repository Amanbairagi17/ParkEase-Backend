package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.client.NotificationServiceClient;
import com.parkease.auth_service.dtos.AdminStatsDto;
import com.parkease.auth_service.dtos.AdminUserResponseDto;
import com.parkease.auth_service.dtos.BroadcastRequestDto;
import com.parkease.auth_service.dtos.WarnUserRequestDto;
import com.parkease.auth_service.entity.Role;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.exception.UserNotFoundException;
import com.parkease.auth_service.mapper.Impl.AdminUserResponseMapper;
import com.parkease.auth_service.repository.AdminUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private AdminUserRepository adminUserRepository;
    @Mock
    private NotificationServiceClient notificationClient;
    @Mock
    private AdminUserResponseMapper responseMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void getAllUsers_ShouldMapNonAdmins() {
        User user = buildUser(1L, Role.DRIVER);
        when(adminUserRepository.findAllNonAdminUsers()).thenReturn(List.of(user));
        when(responseMapper.mapTo(user)).thenReturn(new AdminUserResponseDto());

        List<AdminUserResponseDto> result = adminService.getAllUsers();

        assertEquals(1, result.size());
    }

    @Test
    void getUserById_ShouldThrow_WhenMissing() {
        when(adminUserRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> adminService.getUserById(1L));
    }

    @Test
    void suspendUser_ShouldSkip_WhenAlreadySuspended() {
        User user = buildUser(1L, Role.DRIVER);
        user.setIsActive(false);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(user));

        adminService.suspendUser(1L);

        verify(adminUserRepository, never()).save(any(User.class));
    }

    @Test
    void suspendUser_ShouldSave_WhenActive() {
        User user = buildUser(1L, Role.DRIVER);
        user.setIsActive(true);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(user));

        adminService.suspendUser(1L);

        verify(adminUserRepository).save(user);
        assertFalse(user.getIsActive());
    }

    @Test
    void reactivateUser_ShouldSave_WhenSuspended() {
        User user = buildUser(1L, Role.DRIVER);
        user.setIsActive(false);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(user));

        adminService.reactivateUser(1L);

        verify(adminUserRepository).save(user);
        assertTrue(user.getIsActive());
    }

    @Test
    void reactivateUser_ShouldSkip_WhenAlreadyActive() {
        User user = buildUser(1L, Role.DRIVER);
        user.setIsActive(true);
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(user));

        adminService.reactivateUser(1L);

        verify(adminUserRepository, never()).save(any(User.class));
    }

    @Test
    void getUsersByRole_ShouldThrow_WhenRoleInvalid() {
        assertThrows(IllegalArgumentException.class, () -> adminService.getUsersByRole("bad"));
    }

    @Test
    void getUsersByRole_ShouldMapUsers_WhenRoleValid() {
        User user = buildUser(2L, Role.DRIVER);
        when(adminUserRepository.findByRole(Role.DRIVER)).thenReturn(List.of(user));
        when(responseMapper.mapTo(user)).thenReturn(new AdminUserResponseDto());

        List<AdminUserResponseDto> result = adminService.getUsersByRole("driver");

        assertEquals(1, result.size());
    }

    @Test
    void broadcastToAll_ShouldSendBulk() {
        BroadcastRequestDto request = new BroadcastRequestDto();
        request.setTitle("t");
        request.setMessage("m");
        when(adminUserRepository.findAllNonAdminUsers()).thenReturn(List.of(buildUser(1L, Role.DRIVER)));

        adminService.broadcastToAll(request);

        verify(notificationClient).sendBulk(List.of(1L), "t", "m");
    }

    @Test
    void broadcastToDrivers_ShouldSendBulk() {
        BroadcastRequestDto request = new BroadcastRequestDto();
        request.setTitle("t");
        request.setMessage("m");
        when(adminUserRepository.findByRole(Role.DRIVER)).thenReturn(List.of(buildUser(3L, Role.DRIVER)));

        adminService.broadcastToDrivers(request);

        verify(notificationClient).sendBulk(List.of(3L), "t", "m");
    }

    @Test
    void broadcastToManagers_ShouldSendBulk() {
        BroadcastRequestDto request = new BroadcastRequestDto();
        request.setTitle("t");
        request.setMessage("m");
        when(adminUserRepository.findByRole(Role.LOT_MANAGER)).thenReturn(List.of(buildUser(4L, Role.LOT_MANAGER)));

        adminService.broadcastToManagers(request);

        verify(notificationClient).sendBulk(List.of(4L), "t", "m");
    }

    @Test
    void warnUser_ShouldSendWarning() {
        WarnUserRequestDto request = new WarnUserRequestDto();
        request.setTitle("t");
        request.setMessage("m");
        when(adminUserRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L, Role.DRIVER)));

        adminService.warnUser(1L, request);

        verify(notificationClient).sendWarning(1L, "user@demo.com", "t", "m");
    }

    @Test
    void deleteUser_ShouldRemoveUser() {
        User user = buildUser(9L, Role.DRIVER);
        when(adminUserRepository.findById(9L)).thenReturn(Optional.of(user));

        adminService.deleteUser(9L);

        verify(adminUserRepository).delete(user);
    }

    @Test
    void getPlatformStats_ShouldReturnCounts() {
        when(adminUserRepository.count()).thenReturn(10L);
        when(adminUserRepository.countByIsActive(true)).thenReturn(6L);
        when(adminUserRepository.countByIsActive(false)).thenReturn(4L);
        when(adminUserRepository.countByRole(Role.DRIVER)).thenReturn(3L);
        when(adminUserRepository.countByRole(Role.LOT_MANAGER)).thenReturn(2L);
        when(adminUserRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        AdminStatsDto stats = adminService.getPlatformStats();

        assertEquals(10L, stats.getTotalUsers());
        assertEquals(6L, stats.getActiveUsers());
        assertEquals(4L, stats.getSuspendedUsers());
    }

    private User buildUser(Long id, Role role) {
        User user = new User();
        user.setUserId(id);
        user.setEmail("user@demo.com");
        user.setRole(role);
        user.setIsActive(true);
        return user;
    }
}
