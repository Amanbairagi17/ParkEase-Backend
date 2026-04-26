package com.parkease.parkingspot_service.exception;

public class ParkingSpotNotFoundException extends RuntimeException {

    public ParkingSpotNotFoundException(String message) {
        super(message);
    }
}
