package com.parkease.parkingspot_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "parking_spots",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lot_id", "spot_number"})
)
@Data
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long spotId;

    @Column(name = "lot_id", nullable = false)
    private Long lotId;

    @Column(name = "spot_number", nullable = false)
    private String spotNumber;

    @Column(nullable = false)
    private int floor;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SpotType spotType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SpotStatus status;

    @Column(nullable = false)
    private boolean isHandicapped;

    @Column(nullable = false)
    private boolean isEVCharging;

    @Column(nullable = false)
    private double pricePerHour;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null) {
            this.status = SpotStatus.AVAILABLE;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}