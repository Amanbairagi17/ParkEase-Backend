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
    private final ParkingSpotRepository parkingSpotRepository;

    public boolean isOwner(Long spotId) {

        Long currentUserId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentUserRole();

        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        }

        Long lotId = parkingSpotRepository.findById(spotId)
                .map(spot -> spot.getLotId())
                .orElse(null);
        if (lotId == null) {
            return false;
        }

        ParkingLotLookupResponseDto responseDto = parkingLotServiceClient.getLotById(lotId);

        return currentUserId != null && responseDto != null && currentUserId.equals(responseDto.getManagerId());
    }
}
