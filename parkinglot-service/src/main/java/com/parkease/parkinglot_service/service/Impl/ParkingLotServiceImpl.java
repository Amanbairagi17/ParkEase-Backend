package com.parkease.parkinglot_service.service.Impl;

import com.parkease.parkinglot_service.client.UserServiceClient;
import com.parkease.parkinglot_service.dtos.ParkingLotRequestDto;
import com.parkease.parkinglot_service.dtos.ParkingLotResponseDto;
import com.parkease.parkinglot_service.dtos.UserLookupResponseDto;
import com.parkease.parkinglot_service.entity.ParkingLot;
import com.parkease.parkinglot_service.exception.ParkingLotNotFoundException;
import com.parkease.parkinglot_service.mapper.Impl.ParkingLotRequestMapper;
import com.parkease.parkinglot_service.mapper.Impl.ParkingLotResponseMapper;
import com.parkease.parkinglot_service.repository.ParkingLotRepository;
import com.parkease.parkinglot_service.service.ParkingLotService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;
    private final ParkingLotRequestMapper requestMapper;
    private final ParkingLotResponseMapper responseMapper;
    private final UserServiceClient userServiceClient;

    @Override
    public ParkingLotResponseDto createLot(ParkingLotRequestDto requestDto) {

        log.info("Starting parking lot creation for managerId={}", requestDto.getManagerId());

        validateManagerOrAdmin(requestDto.getManagerId());

        ParkingLot parkingLot = requestMapper.mapFrom(requestDto);
        parkingLot.setAvailableSpots(requestDto.getTotalSpots());

        ParkingLot createdLot = parkingLotRepository.save(parkingLot);

        log.info("Parking lot created successfully. lotId={}", createdLot.getLotId());

        return responseMapper.mapTo(createdLot);
    }

    @Override
    public ParkingLotResponseDto getLotById(Long lotId) {
        ParkingLot parkingLot = findLotOrThrow(lotId);
        return responseMapper.mapTo(parkingLot);
    }

    @Override
    public List<ParkingLotResponseDto> getLotsByCity(String city) {
        return parkingLotRepository.findByCityIgnoreCase(city)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    public List<ParkingLotResponseDto> getNearbyLots(double latitude, double longitude, double radiusKm) {
        return parkingLotRepository.findNearby(latitude, longitude, radiusKm)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    public List<ParkingLotResponseDto> getLotsByManager(Long managerId) {
        return parkingLotRepository.findByManagerId(managerId)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    public ParkingLotResponseDto updateLot(Long lotId, ParkingLotRequestDto requestDto) {

        ParkingLot existingLot = findLotOrThrow(lotId);

        validateManagerOrAdmin(requestDto.getManagerId());

        // ✅ Optional ownership check (recommended)
        // If manager, must own the lot
        if (!existingLot.getManagerId().equals(requestDto.getManagerId())) {
            log.warn("Manager trying to update another manager's lot");
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only update your own parking lot"
            );
        }

        // update fields (same as before)
        existingLot.setName(requestDto.getName());
        existingLot.setAddress(requestDto.getAddress());
        existingLot.setCity(requestDto.getCity());
        existingLot.setLatitude(requestDto.getLatitude());
        existingLot.setLongitude(requestDto.getLongitude());
        existingLot.setManagerId(requestDto.getManagerId());
        existingLot.setOpen(requestDto.getOpen());
        existingLot.setApproved(requestDto.getApproved());
        existingLot.setOpenTime(requestDto.getOpenTime());
        existingLot.setCloseTime(requestDto.getCloseTime());
        existingLot.setImageUrl(requestDto.getImageUrl());

        Long previousTotalSpots = existingLot.getTotalSpots();
        Long previousAvailableSpots = existingLot.getAvailableSpots();
        Long requestedTotalSpots = requestDto.getTotalSpots();

        if (requestedTotalSpots < (previousTotalSpots - previousAvailableSpots)) {
            throw new IllegalStateException("Total spots cannot be less than occupied spots");
        }

        existingLot.setTotalSpots(requestedTotalSpots);
        Long delta = requestedTotalSpots - previousTotalSpots;
        existingLot.setAvailableSpots(previousAvailableSpots + delta);

        return responseMapper.mapTo(parkingLotRepository.save(existingLot));
    }

    @Override
    public void toggleOpen(Long lotId) {
        log.info("Starting toggle open status for lotId={}", lotId);
        ParkingLot existingLot = findLotOrThrow(lotId);
        existingLot.setOpen(!existingLot.isOpen());
        parkingLotRepository.save(existingLot);
        log.info("Successfully toggled open status for lotId={}. New status={}", lotId, existingLot.isOpen());
    }

    @Override
    public void deleteLot(Long lotId) {
        ParkingLot existingLot = findLotOrThrow(lotId);
        parkingLotRepository.delete(existingLot);
    }

    @Override
    @Transactional
    public void decrementAvailable(Long lotId) {
        log.info("Decrementing available spots for lotId={}", lotId);
        ParkingLot existingLot = findLotForUpdateOrThrow(lotId);

        if (existingLot.getAvailableSpots() <= 0) {
            log.warn("Cannot decrement available spots. No spots left. lotId={}", lotId);
            throw new IllegalStateException("No available spots left to decrement");
        }

        existingLot.setAvailableSpots(existingLot.getAvailableSpots() - 1);
        parkingLotRepository.save(existingLot);
        log.info("Decremented available spots for lotId={}. New availableSpots={}", lotId, existingLot.getAvailableSpots());
    }

    @Override
    @Transactional
    public void incrementAvailable(Long lotId) {
        ParkingLot existingLot = findLotForUpdateOrThrow(lotId);

        if (existingLot.getAvailableSpots() >= existingLot.getTotalSpots()) {
            throw new IllegalStateException("Available spots cannot exceed total spots");
        }

        existingLot.setAvailableSpots(existingLot.getAvailableSpots() + 1);
        parkingLotRepository.save(existingLot);
    }

    @Override
    public List<ParkingLotResponseDto> searchLots(String keyword) {
        return parkingLotRepository.searchByKeyword(keyword)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    private ParkingLot findLotOrThrow(Long lotId) {
        return (ParkingLot) parkingLotRepository.findById(lotId)
                .orElseThrow(() -> new ParkingLotNotFoundException(
                        "Parking lot not found with id: " + lotId
                ));
    }

    private ParkingLot findLotForUpdateOrThrow(Long lotId) {
        return parkingLotRepository.findByIdForUpdate(lotId)
                .orElseThrow(() -> new ParkingLotNotFoundException(
                        "Parking lot not found with id: " + lotId
                ));
    }

    private void validateManagerOrAdmin(Long managerId) {
        try {
            UserLookupResponseDto user = userServiceClient.getUserById(managerId);
            String role = user.getRole();

            if (role == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Role not found for user id: " + managerId
                );
            }

            String normalizedRole = role.toUpperCase(Locale.ROOT);

            // ✅ FIX: allow ADMIN also
            if (!normalizedRole.equals("MANAGER") &&
                    !normalizedRole.equals("ADMIN")) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "User must be MANAGER or ADMIN for id: " + managerId
                );
            }

        } catch (FeignException ex) {
            if (ex.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + managerId);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "User service error");
        }
    }
}
