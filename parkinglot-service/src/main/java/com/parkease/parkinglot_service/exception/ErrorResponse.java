package com.parkease.parkinglot_service.exception;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {

    private LocalDateTime timeStamp;
    private int status;
    private String error;
    private String message;
}
