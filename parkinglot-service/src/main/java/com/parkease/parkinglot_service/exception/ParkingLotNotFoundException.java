package com.parkease.parkinglot_service.exception;

public class ParkingLotNotFoundException extends RuntimeException {

    public ParkingLotNotFoundException(String message) {
        super(message);
    }
}
