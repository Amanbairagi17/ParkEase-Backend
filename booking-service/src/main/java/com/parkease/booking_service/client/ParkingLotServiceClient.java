package com.parkease.booking_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PARKINGLOT-SERVICE", path = "/api/parking-lots")
public interface ParkingLotServiceClient {

    @PatchMapping("/{lotId}/decrement-available")
    void decrementAvailable(@PathVariable("lotId") Long lotId);

    @PatchMapping("/{lotId}/increment-available")
    void incrementAvailable(@PathVariable("lotId") Long lotId);
}