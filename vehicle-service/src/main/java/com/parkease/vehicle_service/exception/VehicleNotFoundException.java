package com.parkease.vehicle_service.exception;


public class VehicleNotFoundException extends RuntimeException{
    public VehicleNotFoundException(String msg){
        super(msg);
    }
}
