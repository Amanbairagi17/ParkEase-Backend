package com.parkease.parkingspot_service.utils;

import com.parkease.parkingspot_service.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParkingLotSecurity {

    private final ParkingSpotRepository parkingSpotRepository;

    public boolean isOwner(Integer spotId) {

        // ✅ Current user
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        // ✅ ADMIN override
        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }

        // ✅ Fetch managerId via JOIN query
        Integer managerId = parkingSpotRepository.findManagerIdBySpotId(spotId);

        if (managerId == null) {
            return false;
        }

        return currentUserId != null && currentUserId.equals(managerId.longValue());
    }
}