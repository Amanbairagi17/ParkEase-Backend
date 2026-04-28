package com.parkease.parkingspot_service.repository;

import com.parkease.parkingspot_service.entity.ParkingSpot;
import com.parkease.parkingspot_service.entity.SpotStatus;
import com.parkease.parkingspot_service.entity.SpotType;
import com.parkease.parkingspot_service.entity.VehicleType;
import org.hibernate.query.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    List<ParkingSpot> findByLotId(Long lotId);

    List<ParkingSpot> findByLotIdAndStatus(Long lotId, SpotStatus status);

    List<ParkingSpot> findByLotIdAndSpotType(Long lotId, SpotType spotType);

    List<ParkingSpot> findByLotIdAndVehicleType(Long lotId, VehicleType vehicleType);

    long countByLotIdAndStatus(Long lotId, SpotStatus status);

    List<ParkingSpot> findByIsEVCharging(boolean isEVCharging);

    boolean existsByLotIdAndSpotNumberIgnoreCase(Long lotId, String spotNumber);

    void deleteBySpotId(Long spotId);

}
