package com.parkease.notification_service.exception;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponse {
    public LocalDateTime timeStamp;
    public int status;
    public String error;
    public String message;
}
