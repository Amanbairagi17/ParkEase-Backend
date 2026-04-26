package com.parkease.parkingspot_service.exception;

public class DuplicateSpotException extends RuntimeException {

    public DuplicateSpotException(String message) {
        super(message);
    }
}
