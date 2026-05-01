package com.parkease.parkingspot_service.utils;

import com.parkease.parkingspot_service.client.ParkingLotServiceClient;
import com.parkease.parkingspot_service.dtos.ParkingLotLookupResponseDto;
import com.parkease.parkingspot_service.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("parkingSpotSecurity")
@RequiredArgsConstructor
public class ParkingLotSecurity {

    private final ParkingLotServiceClient parkingLotServiceClient;

    public boolean isOwner(Long spotId) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }

//        ParkingSpot spot = parkingSpotRepository.findById(spotId)
//                .orElse(null);
//        if (spot == null) return false;

        // 🔥 Call another service
        ParkingLotLookupResponseDto responseDto = parkingLotServiceClient.getLotById(spotId);

        return currentUserId != null && currentUserId.equals(responseDto.getManagerId());
    }
}