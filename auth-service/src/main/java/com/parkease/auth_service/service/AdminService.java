package com.parkease.auth_service.service;

import com.parkease.auth_service.dtos.AdminStatsDto;
import com.parkease.auth_service.dtos.AdminUserResponseDto;
import com.parkease.auth_service.dtos.BroadcastRequestDto;
import com.parkease.auth_service.dtos.WarnUserRequestDto;

import java.util.List;

public interface AdminService {

    List<AdminUserResponseDto> getAllUsers();

    AdminUserResponseDto getUserById(Long userId);

    void suspendUser(Long userId);

    void reactivateUser(Long userId);

    void deleteUser(Long userId);

    List<AdminUserResponseDto> getUsersByRole(String role);

    void broadcastToAll(BroadcastRequestDto request);

    void broadcastToDrivers(BroadcastRequestDto request);

    void broadcastToManagers(BroadcastRequestDto request);

    void warnUser(Long userId, WarnUserRequestDto request);

    AdminStatsDto getPlatformStats();
}
