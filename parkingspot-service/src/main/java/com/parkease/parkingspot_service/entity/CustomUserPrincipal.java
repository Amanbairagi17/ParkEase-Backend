package com.parkease.parkingspot_service.entity;

public class CustomUserPrincipal {

    private Long userId;
    private String email;
    private String role;

    public CustomUserPrincipal(Long userId, String email, String role) {
        this.userId = userId;
        this.email = email;
        this.role = role;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}