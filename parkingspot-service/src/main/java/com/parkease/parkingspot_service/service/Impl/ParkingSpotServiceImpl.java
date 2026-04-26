package com.parkease.parkingspot_service.service.Impl;

import com.parkease.parkingspot_service.client.ParkingLotServiceClient;
import com.parkease.parkingspot_service.dtos.ParkingSpotBulkRequestDto;
import com.parkease.parkingspot_service.dtos.ParkingSpotRequestDto;
import com.parkease.parkingspot_service.dtos.ParkingSpotResponseDto;
import com.parkease.parkingspot_service.entity.ParkingSpot;
import com.parkease.parkingspot_service.entity.SpotStatus;
import com.parkease.parkingspot_service.entity.SpotType;
import com.parkease.parkingspot_service.entity.VehicleType;
import com.parkease.parkingspot_service.exception.DuplicateSpotException;
import com.parkease.parkingspot_service.exception.ParkingSpotNotFoundException;
import com.parkease.parkingspot_service.mapper.Impl.ParkingSpotRequestMapper;
import com.parkease.parkingspot_service.mapper.Impl.ParkingSpotResponseMapper;
import com.parkease.parkingspot_service.repository.ParkingSpotRepository;
import com.parkease.parkingspot_service.service.ParkingSpotService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingSpotServiceImpl implements ParkingSpotService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingSpotRequestMapper requestMapper;
    private final ParkingSpotResponseMapper responseMapper;
    private final ParkingLotServiceClient parkingLotServiceClient;

    @Override
    public ParkingSpotResponseDto addSpot(ParkingSpotRequestDto requestDto) {

        log.info("Adding spot | lotId={} | spotNumber={}", requestDto.getLotId(), requestDto.getSpotNumber());

        validateLotExists(requestDto.getLotId());
        validateUniqueSpotNumber(requestDto.getLotId(), requestDto.getSpotNumber());

        ParkingSpot spot = requestMapper.mapFrom(requestDto);
        if (spot.getStatus() == null) {
            spot.setStatus(SpotStatus.AVAILABLE);
        }

        ParkingSpot saved = parkingSpotRepository.save(spot);

        log.info("Spot created | spotId={}", saved.getSpotId());

        return responseMapper.mapTo(saved);
    }

    @Override
    public List<ParkingSpotResponseDto> addBulkSpots(ParkingSpotBulkRequestDto requestDto) {
        Set<String> inRequestUniqueKeys = new HashSet<>();

        for (ParkingSpotRequestDto spotRequest : requestDto.getSpots()) {
            validateLotExists(spotRequest.getLotId());
            String key = spotRequest.getLotId() + "::" + spotRequest.getSpotNumber().toLowerCase();
            if (!inRequestUniqueKeys.add(key)) {
                throw new DuplicateSpotException(
                        "Duplicate spot number in request for lot " + spotRequest.getLotId() + ": " + spotRequest.getSpotNumber()
                );
            }
            validateUniqueSpotNumber(spotRequest.getLotId(), spotRequest.getSpotNumber());
        }

        List<ParkingSpot> spots = requestDto.getSpots().stream().map(requestMapper::mapFrom).toList();
        spots.forEach(spot -> {
            if (spot.getStatus() == null) {
                spot.setStatus(SpotStatus.AVAILABLE);
            }
        });

        return parkingSpotRepository.saveAll(spots).stream().map(responseMapper::mapTo).toList();
    }

    @Override
    public ParkingSpotResponseDto getSpotById(Long spotId) {
        return responseMapper.mapTo(findSpotOrThrow(spotId));
    }

    @Override
    public List<ParkingSpotResponseDto> getSpotsByLot(Long lotId) {
        validateLotExists(lotId);
        return parkingSpotRepository.findByLotId(lotId).stream().map(responseMapper::mapTo).toList();
    }

    @Override
    public List<ParkingSpotResponseDto> getAvailableSpots(Long lotId) {
        validateLotExists(lotId);
        return parkingSpotRepository.findByLotIdAndStatus(lotId, SpotStatus.AVAILABLE)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    public List<ParkingSpotResponseDto> getByTypeAndLot(Long lotId, SpotType spotType) {
        validateLotExists(lotId);
        return parkingSpotRepository.findByLotIdAndSpotType(lotId, spotType)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    public List<ParkingSpotResponseDto> getByVehicleTypeAndLot(Long lotId, VehicleType vehicleType) {
        validateLotExists(lotId);
        return parkingSpotRepository.findByLotIdAndVehicleType(lotId, vehicleType)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    public List<ParkingSpotResponseDto> getEvChargingSpots(boolean enabled) {
        return parkingSpotRepository.findByIsEVCharging(enabled)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    public ParkingSpotResponseDto occupySpot(Long spotId) {

        log.info("Occupying spot | spotId={}", spotId);

        ParkingSpot spot = findSpotOrThrow(spotId);

        if (spot.getStatus() != SpotStatus.RESERVED) {
            log.warn("Occupy failed | spotId={} | status={}", spotId, spot.getStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Spot must be RESERVED");
        }

        spot.setStatus(SpotStatus.OCCUPIED);

        return responseMapper.mapTo(parkingSpotRepository.save(spot));
    }

    @Override
    public ParkingSpotResponseDto releaseSpot(Long spotId) {
        log.info("Starting release process for spotId={}", spotId);
        ParkingSpot spot = findSpotOrThrow(spotId);
        if (spot.getStatus() != SpotStatus.OCCUPIED && spot.getStatus() != SpotStatus.RESERVED) {
            log.warn("Cannot release spot. Spot is not OCCUPIED or RESERVED. spotId={}, status={}", spotId, spot.getStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Spot cannot be released: current status is " + spot.getStatus());
        }
        spot.setStatus(SpotStatus.AVAILABLE);
        ParkingSpot updatedSpot = parkingSpotRepository.save(spot);
        log.info("Successfully released parking spot. spotId={}", spotId);
        return responseMapper.mapTo(updatedSpot);
    }

    @Override
    public ParkingSpotResponseDto updateSpot(Long spotId, ParkingSpotRequestDto requestDto) {
        ParkingSpot existingSpot = findSpotOrThrow(spotId);
        validateLotExists(requestDto.getLotId());

        boolean changedIdentity = !existingSpot.getLotId().equals(requestDto.getLotId())
                || !existingSpot.getSpotNumber().equalsIgnoreCase(requestDto.getSpotNumber());

        if (changedIdentity) {
            validateUniqueSpotNumber(requestDto.getLotId(), requestDto.getSpotNumber());
        }

        existingSpot.setLotId(requestDto.getLotId());
        existingSpot.setSpotNumber(requestDto.getSpotNumber());
        existingSpot.setFloor(requestDto.getFloor());
        existingSpot.setSpotType(requestDto.getSpotType());
        existingSpot.setVehicleType(requestDto.getVehicleType());
        existingSpot.setHandicapped(requestDto.getHandicapped());
        existingSpot.setEVCharging(requestDto.getEVCharging());
        existingSpot.setPricePerHour(requestDto.getPricePerHour());

        if (requestDto.getStatus() != null) {
            existingSpot.setStatus(requestDto.getStatus());
        }

        return responseMapper.mapTo(parkingSpotRepository.save(existingSpot));
    }

    @Override
    public long countAvailable(Long lotId) {
        validateLotExists(lotId);
        return parkingSpotRepository.countByLotIdAndStatus(lotId, SpotStatus.AVAILABLE);
    }

    private void validateLotExists(Long lotId) {
        try {
            parkingLotServiceClient.getLotById(lotId);
        } catch (FeignException exception) {
            log.error("Error communicating with parkinglot-service to validate lotId={}", lotId, exception);
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parking lot not found with id: " + lotId);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to validate lot id with parkinglot-service");
        }
    }

    @Override
    public ParkingSpotResponseDto reserveSpot(Long spotId) {

        log.info("Reserving spot | spotId={}", spotId);

        ParkingSpot spot = findSpotOrThrow(spotId);

        if (spot.getStatus() != SpotStatus.AVAILABLE) {
            log.warn("Reserve failed | spotId={} | status={}", spotId, spot.getStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Spot not available");
        }

        spot.setStatus(SpotStatus.RESERVED);

        return responseMapper.mapTo(parkingSpotRepository.save(spot));
    }

    @Override
    public void deleteSpot(Long spotId) {

        log.info("Deleting spot | spotId={}", spotId);

        findSpotOrThrow(spotId);

        parkingSpotRepository.deleteBySpotId(spotId);

        log.info("Spot deleted | spotId={}", spotId);
    }

    private ParkingSpot findSpotOrThrow(Long spotId) {
        return parkingSpotRepository.findById(spotId)
                .orElseThrow(() -> {
                    log.error("Spot not found | spotId={}", spotId);
                    return new ParkingSpotNotFoundException("Spot not found: " + spotId);
                });
    }

    private void validateUniqueSpotNumber(Long lotId, String spotNumber) {
        if (parkingSpotRepository.existsByLotIdAndSpotNumberIgnoreCase(lotId, spotNumber)) {
            log.warn("Duplicate spot | lotId={} | spotNumber={}", lotId, spotNumber);
            throw new DuplicateSpotException("Duplicate spot number");
        }
    }
}
