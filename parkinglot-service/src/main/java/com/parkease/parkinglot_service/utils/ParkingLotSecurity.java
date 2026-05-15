package com.parkease.parkinglot_service.utils;

import com.parkease.parkinglot_service.repository.ParkingLotRepository;
import org.springframework.stereotype.Component;

@Component
public class ParkingLotSecurity {

    private final ParkingLotRepository parkingLotRepository;

    public ParkingLotSecurity(ParkingLotRepository parkingLotRepository) {
        this.parkingLotRepository = parkingLotRepository;
    }

    public boolean isOwner(Long lotId) {

        // current logged-in user
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // lot owner (managerId)
        Long managerId = parkingLotRepository.findManagerIdByLotId(lotId);

        if (managerId == null) {
            return false;
        }

        return currentUserId.equals(Long.valueOf(managerId));
    }

    public boolean isCurrentUser(Long managerId) {
        return SecurityUtils.isCurrentUser(managerId);
    }
}
