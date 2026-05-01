package com.parkease.auth_service.controller;

import com.parkease.auth_service.dtos.*;
import com.parkease.auth_service.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponseDto>> getAllUsers() {
        log.info("[Admin] GET /users");
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserResponseDto> getUserById(@PathVariable Long userId) {
        log.info("[Admin] GET /users/{}", userId);
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    @PatchMapping("/users/{userId}/suspend")
    public ResponseEntity<String> suspendUser(@PathVariable Long userId) {
        log.info("[Admin] PATCH /users/{}/suspend", userId);
        adminService.suspendUser(userId);
        return ResponseEntity.ok("User " + userId + " suspended successfully");
    }

    @PatchMapping("/users/{userId}/reactivate")
    public ResponseEntity<String> reactivateUser(@PathVariable Long userId) {
        log.info("[Admin] PATCH /users/{}/reactivate", userId);
        adminService.reactivateUser(userId);
        return ResponseEntity.ok("User " + userId + " reactivated successfully");
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
        log.info("[Admin] DELETE /users/{}", userId);
        adminService.deleteUser(userId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<AdminUserResponseDto>> getUsersByRole(@PathVariable String role) {
        log.info("[Admin] GET /users/role/{}", role);
        return ResponseEntity.ok(adminService.getUsersByRole(role));
    }


    // POST /api/admin/notifications/broadcast — send to ALL users 
    @PostMapping("/notifications/broadcast")
    public ResponseEntity<String> broadcastToAll(@RequestBody BroadcastRequestDto request) {
        log.info("[Admin] POST /notifications/broadcast — title='{}'", request.getTitle());
        adminService.broadcastToAll(request);
        return ResponseEntity.ok("Broadcast sent to all users");
    }

    // POST /api/admin/notifications/broadcast/drivers — send to DRIVER users only 
    @PostMapping("/notifications/broadcast/drivers")
    public ResponseEntity<String> broadcastToDrivers(@RequestBody BroadcastRequestDto request) {
        log.info("[Admin] POST /notifications/broadcast/drivers — title='{}'", request.getTitle());
        adminService.broadcastToDrivers(request);
        return ResponseEntity.ok("Broadcast sent to all drivers");
    }

    //POST /api/admin/notifications/broadcast/managers — send to LOT_MANAGER users only 
    @PostMapping("/notifications/broadcast/managers")
    public ResponseEntity<String> broadcastToManagers(@RequestBody BroadcastRequestDto request) {
        log.info("[Admin] POST /notifications/broadcast/managers — title='{}'", request.getTitle());
        adminService.broadcastToManagers(request);
        return ResponseEntity.ok("Broadcast sent to all lot managers");
    }

    // POST /api/admin/notifications/warn/{userId} — warning to specific user 
    @PostMapping("/notifications/warn/{userId}")
    public ResponseEntity<String> warnUser(
            @PathVariable Long userId,
            @RequestBody WarnUserRequestDto request) {
        log.info("[Admin] POST /notifications/warn/{} — title='{}'", userId, request.getTitle());
        adminService.warnUser(userId, request);
        return ResponseEntity.ok("Warning sent to user " + userId);
    }

    /** GET /api/admin/stats — total users, active, by role */
    @GetMapping("/stats")
    public ResponseEntity<AdminStatsDto> getPlatformStats() {
        log.info("[Admin] GET /stats");
        return ResponseEntity.ok(adminService.getPlatformStats());
    }
}
