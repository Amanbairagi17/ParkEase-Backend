package com.parkease.parkinglot_service.repository;

import com.parkease.parkinglot_service.entity.ParkingLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParkingLotRepository extends JpaRepository<ParkingLot, Integer> {

    List<ParkingLot> findByCityIgnoreCase(String city);

    List<ParkingLot> findByManagerId(Integer managerId);

    List<ParkingLot> findByIsOpen(boolean open);

    List<ParkingLot> findByAvailableSpotsGreaterThan(int availableSpots);

    int countByCityIgnoreCase(String city);

    void deleteByLotId(int lotId);

    @Query(value = """
            SELECT * FROM parking_lots p
            WHERE (6371 * acos(
                cos(radians(:latitude)) * cos(radians(p.latitude))
                * cos(radians(p.longitude) - radians(:longitude))
                + sin(radians(:latitude)) * sin(radians(p.latitude))
            )) <= :radiusKm
            """, nativeQuery = true)
    List<ParkingLot> findNearby(@Param("latitude") double latitude,
                                @Param("longitude") double longitude,
                                @Param("radiusKm") double radiusKm);

    @Query("""
            SELECT p FROM ParkingLot p
            WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.city) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    List<ParkingLot> searchByKeyword(@Param("keyword") String keyword);

    Integer findManagerIdByLotId(Integer lotId);
}
