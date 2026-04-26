package com.parkease.vehicle_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "vehicle")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long vehicleId;

    private Long ownerId;   // userId from auth service

    @Column(unique = true, nullable = false)
    private String licensePlate;

    private String make;

    private String model;

    private String color;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType; // 2W, 4W, HEAVY

    @Column(nullable = false)
    private Boolean isEV;

    private Boolean isActive = true;

    private LocalDateTime registeredAt;
}
