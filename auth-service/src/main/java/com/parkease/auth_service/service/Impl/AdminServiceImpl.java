package com.parkease.auth_service.service.Impl;

import com.parkease.auth_service.client.NotificationServiceClient;
import com.parkease.auth_service.dtos.*;
import com.parkease.auth_service.entity.Role;
import com.parkease.auth_service.entity.User;
import com.parkease.auth_service.exception.UserNotFoundException;
import com.parkease.auth_service.mapper.Impl.AdminUserResponseMapper;
import com.parkease.auth_service.repository.AdminUserRepository;
import com.parkease.auth_service.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminUserRepository adminUserRepository;
    private final NotificationServiceClient notificationClient;
    private final AdminUserResponseMapper responseMapper;

    private User fetchUser(Long userId) {
        return adminUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }

    @Override
    public List<AdminUserResponseDto> getAllUsers() {
        log.info("[Admin] Fetching all non-admin users");
        return adminUserRepository.findAllNonAdminUsers()
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    public AdminUserResponseDto getUserById(Long userId) {
        log.info("[Admin] Fetching user by id={}", userId);
        return responseMapper.mapTo(fetchUser(userId));
    }

    @Override
    @Transactional
    public void suspendUser(Long userId) {
        User user = fetchUser(userId);
        if (Boolean.FALSE.equals(user.getIsActive())) {
            log.warn("[Admin] User {} is already suspended", userId);
            return;
        }
        user.setIsActive(false);
        adminUserRepository.save(user);
        log.info("[Admin] User {} suspended", userId);
    }

    @Override
    @Transactional
    public void reactivateUser(Long userId) {
        User user = fetchUser(userId);
        if (Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("[Admin] User {} is already active", userId);
            return;
        }
        user.setIsActive(true);
        adminUserRepository.save(user);
        log.info("[Admin] User {} reactivated", userId);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = fetchUser(userId);
        adminUserRepository.delete(user);
        log.info("[Admin] User {} permanently deleted", userId);
    }

    @Override
    public List<AdminUserResponseDto> getUsersByRole(String role) {
        log.info("[Admin] Fetching users by role={}", role);
        Role roleEnum;
        try {
            roleEnum = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role + ". Valid values: DRIVER, MANAGER, ADMIN");
        }
        return adminUserRepository.findByRole(roleEnum)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    public void broadcastToAll(BroadcastRequestDto request) {
        List<Long> allIds = adminUserRepository.findAllNonAdminUsers()
                .stream()
                .map(User::getUserId)
                .toList();
        log.info("[Admin] Broadcasting to ALL users. count={}", allIds.size());
        notificationClient.sendBulk(allIds, request.getTitle(), request.getMessage());
    }

    @Override
    public void broadcastToDrivers(BroadcastRequestDto request) {
        List<Long> driverIds = adminUserRepository.findByRole(Role.DRIVER)
                .stream()
                .map(User::getUserId)
                .toList();
        log.info("[Admin] Broadcasting to DRIVERS. count={}", driverIds.size());
        notificationClient.sendBulk(driverIds, request.getTitle(), request.getMessage());
    }

    @Override
    public void broadcastToManagers(BroadcastRequestDto request) {
        List<Long> managerIds = adminUserRepository.findByRole(Role.LOT_MANAGER)
                .stream()
                .map(User::getUserId)
                .toList();
        log.info("[Admin] Broadcasting to LOT_MANAGERs. count={}", managerIds.size());
        notificationClient.sendBulk(managerIds, request.getTitle(), request.getMessage());
    }

    @Override
    public void warnUser(Long userId, WarnUserRequestDto request) {
        User user = fetchUser(userId);
        log.info("[Admin] Sending warning to userId={}", userId);
        notificationClient.sendWarning(userId, user.getEmail(), request.getTitle(), request.getMessage());
    }


    @Override
    public AdminStatsDto getPlatformStats() {
        long total     = adminUserRepository.count();
        long active    = adminUserRepository.countByIsActive(true);
        long suspended = adminUserRepository.countByIsActive(false);
        long drivers   = adminUserRepository.countByRole(Role.DRIVER);
        long managers  = adminUserRepository.countByRole(Role.LOT_MANAGER);
        long admins    = adminUserRepository.countByRole(Role.ADMIN);

        log.info("[Admin] Stats fetched — total={}, active={}, suspended={}", total, active, suspended);
        return new AdminStatsDto(total, active, suspended, drivers, managers, admins);
    }
}
