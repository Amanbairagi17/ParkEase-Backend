package com.parkease.parkingspot_service.repository;

import com.parkease.parkingspot_service.entity.ParkingSpot;
import com.parkease.parkingspot_service.entity.SpotStatus;
import com.parkease.parkingspot_service.entity.SpotType;
import com.parkease.parkingspot_service.entity.VehicleType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    List<ParkingSpot> findByLotId(Long lotId);

    List<ParkingSpot> findByLotIdAndStatus(Long lotId, SpotStatus status);

    List<ParkingSpot> findByLotIdAndSpotType(Long lotId, SpotType spotType);

    List<ParkingSpot> findByLotIdAndVehicleType(Long lotId, VehicleType vehicleType);

    long countByLotIdAndStatus(Long lotId, SpotStatus status);

    List<ParkingSpot> findByIsEVCharging(boolean isEVCharging);

    boolean existsByLotIdAndSpotNumberIgnoreCase(Long lotId, String spotNumber);

    void deleteBySpotId(Long spotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ParkingSpot s WHERE s.spotId = :spotId")
    Optional<ParkingSpot> findByIdForUpdate(@Param("spotId") Long spotId);

}
