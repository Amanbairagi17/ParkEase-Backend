package com.parkease.auth_service.service;

public interface EmailService {
    void sendOtp(String toEmail, String otp);
    void sendVerificationEmail(String toEmail, String verificationLink);
}
