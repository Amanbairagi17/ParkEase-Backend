package com.parkease.vehicle_service.utils;

import com.parkease.vehicle_service.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("vehicleSecurity")
@RequiredArgsConstructor
public class VehicleSecurity {

    private final VehicleRepository vehicleRepository;

    public boolean isOwner(Long vehicleId) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        // ADMIN override
        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }

        Long ownerId = vehicleRepository.findById(vehicleId)
                .map(vehicle -> vehicle.getOwnerId())
                .orElse(null);

        if (ownerId == null) {
            return false;
        }

        return ownerId.equals(currentUserId);
    }

    public boolean isCurrentUser(Long ownerId) {
        return SecurityUtils.isCurrentUser(ownerId);
    }
}
