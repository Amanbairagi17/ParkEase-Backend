package com.parkease.vehicle_service.utils;

import com.parkease.vehicle_service.client.UserServiceClient;
import com.parkease.vehicle_service.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("vehicleSecurity")
@RequiredArgsConstructor
public class VehicleSecurity {

    private final UserServiceClient userServiceClient;

    public boolean isOwner(Long vehicleId) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        // ADMIN override
        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }

        //  Fetch only ownerId (optimized)
        Long ownerId = userServiceClient.getUserById(SecurityUtils.getCurrentUserId()).getOwnerId();

        if (ownerId == null) {
            return false;
        }

        return ownerId.equals(currentUserId);
    }
}