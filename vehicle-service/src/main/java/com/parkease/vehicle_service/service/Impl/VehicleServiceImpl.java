package com.parkease.vehicle_service.service.Impl;

import com.parkease.vehicle_service.dtos.UserLookupResponseDto;
import com.parkease.vehicle_service.dtos.VehicleResponseDto;
import com.parkease.vehicle_service.dtos.VehicleRequestDto;
import com.parkease.vehicle_service.client.UserServiceClient;
import com.parkease.vehicle_service.entity.Vehicle;
import com.parkease.vehicle_service.entity.VehicleType;
import com.parkease.vehicle_service.exception.VehicleNotFoundException;
import com.parkease.vehicle_service.mapper.Impl.VehicleRequestMapper;
import com.parkease.vehicle_service.mapper.Impl.VehicleResponseMapper;
import com.parkease.vehicle_service.repository.VehicleRepository;
import com.parkease.vehicle_service.service.VehicleService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {
    private final VehicleRepository repository;
    private final VehicleRequestMapper requestMapper;
    private final VehicleResponseMapper responseMapper;
    private final UserServiceClient userServiceClient;
    private final HttpServletRequest request;

    @Override
    public VehicleResponseDto createVehicle(VehicleRequestDto requestDto) {
         Long ownerId = requestDto.getOwnerId();

         UserLookupResponseDto responseDto = userServiceClient.getUserById(ownerId);
         log.info("Starting vehicle creation for email={}", responseDto.getEmail());

         Vehicle vehicle = requestMapper.mapFrom(requestDto);
         log.info("Converting request to vehicle entity ");
         vehicle.setOwnerId(ownerId);
         log.info("Assigning owner to vehicle ");

         vehicle.setVehicleType(parseVehicleType(requestDto.getVehicleType()));
         vehicle.setRegisteredAt(LocalDateTime.now());

         Vehicle createdVehicle = repository.save(vehicle);
        log.info("Vehicle saved successfully with id={}", createdVehicle.getVehicleId());
         return responseMapper.mapTo(createdVehicle);



//        String email = request.getHeader("X-User-Name");
//        log.info("Starting vehicle creation for email={}", email);
//
//        if (email == null || email.isEmpty()) {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User email not found in request headers");
//        }
//
//        Long ownerId = fetchUserIdByEmail(email);
//        log.info("Successfully fetched userId={} for email={}", ownerId, email);
//
//        Vehicle vehicle = requestMapper.mapFrom(requestDto);
//        vehicle.setOwnerId(ownerId);
//        vehicle.setVehicleType(parseVehicleType(requestDto.getVehicleType()));
//        vehicle.setRegisteredAt(LocalDateTime.now());
//        Vehicle createdVehicle = repository.save(vehicle);
//        log.info("Vehicle saved successfully with id={}", createdVehicle.getVehicleId());
//        return responseMapper.mapTo(createdVehicle);
    }

    @Override
    public VehicleResponseDto findVehicleById(Long vehicleId) {
        log.info("Fetching vehicle details for vehicleId={}", vehicleId);
        Vehicle vehicle = repository.findById(vehicleId).orElseThrow(
                () -> {
                    log.error("Error: Vehicle not found for vehicleId={}", vehicleId);
                    return new VehicleNotFoundException("Vehicle not available with the given vehicle Id : "+vehicleId);
                } );
        VehicleResponseDto response = responseMapper.mapTo(vehicle);

        return response;
    }

    @Override
    public List<VehicleResponseDto> getVehiclesByOwner(Long ownerId) {
        return repository.findByOwnerId(ownerId)
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    @Override
    public VehicleResponseDto getByLicensePlate(String licensePlate) {
        Vehicle vehicle = repository.findByLicensePlate(licensePlate)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not available with license plate : " + licensePlate));

        return responseMapper.mapTo(vehicle);
    }

    @Override
    public VehicleResponseDto updateVehicle(Long vehicleId, VehicleRequestDto requestDto) {
        Vehicle existingVehicle = repository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not available with the given vehicle Id : " + vehicleId));

        validateOwnerExists(requestDto.getOwnerId());
        existingVehicle.setOwnerId(requestDto.getOwnerId());
        existingVehicle.setLicensePlate(requestDto.getLicensePlate());
        existingVehicle.setMake(requestDto.getMake());
        existingVehicle.setModel(requestDto.getModel());
        existingVehicle.setColor(requestDto.getColor());
        existingVehicle.setVehicleType(parseVehicleType(requestDto.getVehicleType()));
        existingVehicle.setIsEV(requestDto.getIsEV());

        Vehicle updatedVehicle = repository.save(existingVehicle);
        return responseMapper.mapTo(updatedVehicle);
    }

    @Override
    public void deleteVehicle(Long vehicleId) {
        log.info("Starting vehicle deletion for vehicleId={}", vehicleId);
        Vehicle existingVehicle = repository.findById(vehicleId)
                .orElseThrow(() -> {
                    log.error("Error deleting vehicle. Vehicle not found for vehicleId={}", vehicleId);
                    return new VehicleNotFoundException("Vehicle not available with the given vehicle Id : " + vehicleId);
                });
        repository.delete(existingVehicle);
        log.info("Vehicle deleted successfully for vehicleId={}", vehicleId);
    }

    @Override
    public String getVehicleType(Long vehicleId) {
        Vehicle vehicle = repository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not available with the given vehicle Id : " + vehicleId));
        return vehicle.getVehicleType().name();
    }

    @Override
    public Boolean isEVVehicle(Long vehicleId) {
        Vehicle vehicle = repository.findById(vehicleId)
                .orElseThrow(() -> new VehicleNotFoundException("Vehicle not available with the given vehicle Id : " + vehicleId));
        return vehicle.getIsEV();
    }

    @Override
    public List<VehicleResponseDto> getAllVehicles() {
        return repository.findAll()
                .stream()
                .map(responseMapper::mapTo)
                .toList();
    }

    private VehicleType parseVehicleType(String type) {
        return switch (type) {
            case "2W" -> VehicleType.TWO_WHEELER;
            case "3W" -> VehicleType.THREE_WHEELER;
            case "4W" -> VehicleType.FOUR_WHEELER;
            case "HEAVY" -> VehicleType.HEAVY;
            default -> throw new IllegalArgumentException("Invalid vehicle type: " + type);
        };
    }

    private void validateOwnerExists(Long ownerId) {
        try {
            userServiceClient.getUserById(ownerId);
        } catch (FeignException exception) {
            log.error("Error communicating with auth-service for ownerId={}", ownerId, exception);
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found with id: " + ownerId);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to validate owner id with auth-service");
        }
    }

    private Long fetchUserIdByEmail(String email) {
        try {
            return userServiceClient.getUserByEmail(email).getOwnerId();
        } catch (FeignException exception) {
            log.error("Error fetching user details from auth-service for email={}", email, exception);
            if (exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to fetch user details from auth-service");
        }
    }
}
