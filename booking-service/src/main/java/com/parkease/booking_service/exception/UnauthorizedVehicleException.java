package com.parkease.booking_service.exception;

public class UnauthorizedVehicleException extends RuntimeException {
    public UnauthorizedVehicleException(String message) {
        super(message);
    }
}
