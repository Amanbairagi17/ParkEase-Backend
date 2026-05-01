package com.parkease.auth_service.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminStatsDto {
    private long totalUsers;
    private long activeUsers;
    private long suspendedUsers;
    private long totalDrivers;
    private long totalLotManagers;
    private long totalAdmins;
}
