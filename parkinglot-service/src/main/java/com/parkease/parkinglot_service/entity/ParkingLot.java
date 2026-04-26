package com.parkease.parkinglot_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_lots")
@Data
public class ParkingLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int lotId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    private double latitude;
    private double longitude;

    @Column(nullable = false)
    private int totalSpots;

    private int availableSpots;

    @Column(nullable = false)
    private int managerId;

    @Column(nullable = false)
    private boolean isOpen;

    @Column(nullable = false)
    private boolean isApproved;

    private LocalTime openTime;
    private LocalTime closeTime;

    private String imageUrl;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.availableSpots == 0 && this.totalSpots > 0) {
            this.availableSpots = this.totalSpots;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}