package com.parkease.parkingspot_service.service.Impl;

import com.parkease.parkingspot_service.client.ParkingLotServiceClient;
import com.parkease.parkingspot_service.dtos.ParkingLotLookupResponseDto;
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

import feign.FeignException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParkingSpotServiceImplTest {

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @Mock
    private ParkingSpotRequestMapper requestMapper;

    @Mock
    private ParkingSpotResponseMapper responseMapper;

    @Mock
    private ParkingLotServiceClient parkingLotServiceClient;

    @InjectMocks
    private ParkingSpotServiceImpl service;

    @Test
    void addSpot_ShouldThrow_WhenDuplicateSpot() {

        ParkingSpotRequestDto request =
                buildRequest();

        when(parkingLotServiceClient.getLotById(1L))
                .thenReturn(buildLot());

        when(parkingSpotRepository
                .existsByLotIdAndSpotNumberIgnoreCase(1L, "A1"))
                .thenReturn(true);

        assertThrows(
                DuplicateSpotException.class,
                () -> service.addSpot(request)
        );
    }

    @Test
    void addSpot_ShouldSave_WhenValid() {

        ParkingSpotRequestDto request =
                buildRequest();

        ParkingSpot spot =
                new ParkingSpot();

        when(parkingLotServiceClient.getLotById(1L))
                .thenReturn(buildLot());

        when(parkingSpotRepository
                .existsByLotIdAndSpotNumberIgnoreCase(1L, "A1"))
                .thenReturn(false);

        when(requestMapper.mapFrom(request))
                .thenReturn(spot);

        when(parkingSpotRepository.save(spot))
                .thenReturn(spot);

        when(responseMapper.mapTo(spot))
                .thenReturn(new ParkingSpotResponseDto());

        ParkingSpotResponseDto response =
                service.addSpot(request);

        assertNotNull(response);

        assertEquals(
                SpotStatus.AVAILABLE,
                spot.getStatus()
        );
    }

    @Test
    void addBulkSpots_ShouldThrow_WhenDuplicateInRequest() {

        ParkingSpotRequestDto request =
                buildRequest();

        ParkingSpotBulkRequestDto bulk =
                new ParkingSpotBulkRequestDto();

        bulk.setSpots(List.of(request, request));

        when(parkingLotServiceClient.getLotById(1L))
                .thenReturn(buildLot());

        when(parkingSpotRepository
                .existsByLotIdAndSpotNumberIgnoreCase(1L, "A1"))
                .thenReturn(false);

        assertThrows(
                DuplicateSpotException.class,
                () -> service.addBulkSpots(bulk)
        );
    }

    @Test
    void getSpotById_ShouldReturnSpot_WhenExists() {

        ParkingSpot spot =
                new ParkingSpot();

        spot.setSpotId(1L);

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.of(spot));

        when(responseMapper.mapTo(spot))
                .thenReturn(new ParkingSpotResponseDto());

        ParkingSpotResponseDto result =
                service.getSpotById(1L);

        assertNotNull(result);
    }

    @Test
    void occupySpot_ShouldThrow_WhenSpotMissing() {

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ParkingSpotNotFoundException.class,
                () -> service.occupySpot(1L)
        );
    }

    @Test
    void countAvailable_ShouldReturnZero() {

        when(parkingLotServiceClient.getLotById(1L))
                .thenReturn(buildLot());

        when(parkingSpotRepository.countByLotIdAndStatus(
                1L,
                SpotStatus.AVAILABLE
        )).thenReturn(0L);

        long result =
                service.countAvailable(1L);

        assertEquals(0L, result);
    }

    @Test
    void releaseSpot_ShouldThrow_WhenSpotMissing() {

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ParkingSpotNotFoundException.class,
                () -> service.releaseSpot(1L)
        );
    }

    @Test
    void getSpotById_ShouldThrow_WhenMissing() {

        when(parkingSpotRepository.findById(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                ParkingSpotNotFoundException.class,
                () -> service.getSpotById(10L)
        );
    }

    @Test
    void getSpotsByLot_ShouldReturnList_WhenLotValid() {

        when(parkingLotServiceClient.getLotById(1L))
                .thenReturn(buildLot());

        when(parkingSpotRepository.findByLotId(1L))
                .thenReturn(List.of(new ParkingSpot()));

        when(responseMapper.mapTo(any(ParkingSpot.class)))
                .thenReturn(new ParkingSpotResponseDto());

        List<ParkingSpotResponseDto> result =
                service.getSpotsByLot(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getByTypeAndLot_ShouldReturnList() {

        when(parkingLotServiceClient.getLotById(1L))
                .thenReturn(buildLot());

        when(parkingSpotRepository
                .findByLotIdAndSpotType(1L, SpotType.TRUCK))
                .thenReturn(List.of(new ParkingSpot()));

        when(responseMapper.mapTo(any(ParkingSpot.class)))
                .thenReturn(new ParkingSpotResponseDto());

        List<ParkingSpotResponseDto> result =
                service.getByTypeAndLot(
                        1L,
                        SpotType.TRUCK
                );

        assertEquals(1, result.size());
    }

    @Test
    void getByVehicleTypeAndLot_ShouldReturnList() {

        when(parkingLotServiceClient.getLotById(1L))
                .thenReturn(buildLot());

        when(parkingSpotRepository
                .findByLotIdAndVehicleType(
                        1L,
                        VehicleType.FOUR_WHEELER
                ))
                .thenReturn(List.of(new ParkingSpot()));

        when(responseMapper.mapTo(any(ParkingSpot.class)))
                .thenReturn(new ParkingSpotResponseDto());

        List<ParkingSpotResponseDto> result =
                service.getByVehicleTypeAndLot(
                        1L,
                        VehicleType.FOUR_WHEELER
                );

        assertEquals(1, result.size());
    }

    @Test
    void getEvChargingSpots_ShouldReturnList() {

        when(parkingSpotRepository.findByIsEVCharging(true))
                .thenReturn(List.of(new ParkingSpot()));

        when(responseMapper.mapTo(any(ParkingSpot.class)))
                .thenReturn(new ParkingSpotResponseDto());

        List<ParkingSpotResponseDto> result =
                service.getEvChargingSpots(true);

        assertEquals(1, result.size());
    }

    @Test
    void reserveSpot_ShouldThrow_WhenNotAvailable() {

        ParkingSpot spot =
                new ParkingSpot();

        spot.setSpotId(1L);

        spot.setStatus(SpotStatus.OCCUPIED);

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.of(spot));

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.reserveSpot(1L)
                );

        assertEquals(
                HttpStatus.CONFLICT,
                ex.getStatusCode()
        );
    }

    @Test
    void reserveSpot_ShouldReserve_WhenAvailable() {

        ParkingSpot spot =
                new ParkingSpot();

        spot.setSpotId(1L);

        spot.setStatus(SpotStatus.AVAILABLE);

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.of(spot));

        when(parkingSpotRepository.save(spot))
                .thenReturn(spot);

        when(responseMapper.mapTo(spot))
                .thenReturn(new ParkingSpotResponseDto());

        ParkingSpotResponseDto response =
                service.reserveSpot(1L);

        assertNotNull(response);

        assertEquals(
                SpotStatus.RESERVED,
                spot.getStatus()
        );
    }

    @Test
    void occupySpot_ShouldThrow_WhenNotReserved() {

        ParkingSpot spot =
                new ParkingSpot();

        spot.setSpotId(1L);

        spot.setStatus(SpotStatus.AVAILABLE);

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.of(spot));

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.occupySpot(1L)
                );

        assertEquals(
                HttpStatus.CONFLICT,
                ex.getStatusCode()
        );
    }

    @Test
    void occupySpot_ShouldOccupy_WhenReserved() {

        ParkingSpot spot =
                new ParkingSpot();

        spot.setSpotId(1L);

        spot.setStatus(SpotStatus.RESERVED);

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.of(spot));

        when(parkingSpotRepository.save(spot))
                .thenReturn(spot);

        when(responseMapper.mapTo(spot))
                .thenReturn(new ParkingSpotResponseDto());

        ParkingSpotResponseDto response =
                service.occupySpot(1L);

        assertNotNull(response);

        assertEquals(
                SpotStatus.OCCUPIED,
                spot.getStatus()
        );
    }

    @Test
    void releaseSpot_ShouldThrow_WhenInvalidStatus() {

        ParkingSpot spot =
                new ParkingSpot();

        spot.setSpotId(1L);

        spot.setStatus(SpotStatus.AVAILABLE);

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.of(spot));

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.releaseSpot(1L)
                );

        assertEquals(
                HttpStatus.CONFLICT,
                ex.getStatusCode()
        );
    }

    @Test
    void releaseSpot_ShouldRelease_WhenOccupied() {

        ParkingSpot spot =
                new ParkingSpot();

        spot.setSpotId(1L);

        spot.setStatus(SpotStatus.OCCUPIED);

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.of(spot));

        when(parkingSpotRepository.save(spot))
                .thenReturn(spot);

        when(responseMapper.mapTo(spot))
                .thenReturn(new ParkingSpotResponseDto());

        ParkingSpotResponseDto response =
                service.releaseSpot(1L);

        assertNotNull(response);

        assertEquals(
                SpotStatus.AVAILABLE,
                spot.getStatus()
        );
    }

    @Test
    void validateLotExists_ShouldThrow_WhenLotMissing() {

        FeignException exception =
                FeignException.errorStatus(
                        "methodKey",
                        feign.Response.builder()
                                .status(404)
                                .reason("Not Found")
                                .request(
                                        feign.Request.create(
                                                feign.Request.HttpMethod.GET,
                                                "/",
                                                java.util.Map.of(),
                                                null,
                                                null,
                                                null
                                        )
                                )
                                .build()
                );

        when(parkingLotServiceClient.getLotById(1L))
                .thenThrow(exception);

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.getSpotsByLot(1L)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                ex.getStatusCode()
        );
    }


    @Test
    void deleteSpot_ShouldDeleteById_WhenExists() {

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.of(new ParkingSpot()));

        service.deleteSpot(1L);

        verify(parkingSpotRepository)
                .deleteBySpotId(1L);
    }

    @Test
    void updateSpot_ShouldSave_WhenValid() {

        ParkingSpot existing =
                new ParkingSpot();

        existing.setSpotId(1L);

        existing.setLotId(1L);

        existing.setSpotNumber("OLD");

        ParkingSpotRequestDto request =
                buildRequest();

        when(parkingSpotRepository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(parkingLotServiceClient.getLotById(1L))
                .thenReturn(buildLot());

        when(parkingSpotRepository
                .existsByLotIdAndSpotNumberIgnoreCase(1L, "A1"))
                .thenReturn(false);

        when(parkingSpotRepository.save(any(ParkingSpot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(responseMapper.mapTo(any(ParkingSpot.class)))
                .thenReturn(new ParkingSpotResponseDto());

        ParkingSpotResponseDto result =
                service.updateSpot(1L, request);

        assertNotNull(result);

        verify(parkingSpotRepository)
                .save(any(ParkingSpot.class));
    }

    @Test
    void countAvailable_ShouldReturnCount() {

        when(parkingLotServiceClient.getLotById(1L))
                .thenReturn(buildLot());

        when(parkingSpotRepository.countByLotIdAndStatus(
                1L,
                SpotStatus.AVAILABLE
        )).thenReturn(5L);

        long count =
                service.countAvailable(1L);

        assertEquals(5L, count);
    }

    @Test
    void getAvailableSpots_ShouldReturnList() {

        when(parkingLotServiceClient.getLotById(1L))
                .thenReturn(buildLot());

        when(parkingSpotRepository.findByLotIdAndStatus(
                1L,
                SpotStatus.AVAILABLE
        )).thenReturn(List.of(new ParkingSpot()));

        when(responseMapper.mapTo(any(ParkingSpot.class)))
                .thenReturn(new ParkingSpotResponseDto());

        List<ParkingSpotResponseDto> result =
                service.getAvailableSpots(1L);

        assertEquals(1, result.size());
    }

    private ParkingSpotRequestDto buildRequest() {

        ParkingSpotRequestDto request =
                new ParkingSpotRequestDto();

        request.setLotId(1L);

        request.setSpotNumber("A1");

        request.setFloor(1);

        request.setSpotType(SpotType.BIKE);

        request.setVehicleType(VehicleType.FOUR_WHEELER);

        request.setHandicapped(false);

        request.setEVCharging(false);

        request.setPricePerHour(20.0);

        return request;
    }

    private ParkingLotLookupResponseDto buildLot() {

        ParkingLotLookupResponseDto dto =
                new ParkingLotLookupResponseDto();

        dto.setLotId(1L);

        dto.setName("Lot A");

        dto.setOpen(true);

        return dto;
    }
}