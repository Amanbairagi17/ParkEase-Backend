package com.parkease.parkingspot_service.controller;

import com.parkease.parkingspot_service.dtos.ParkingSpotBulkRequestDto;
import com.parkease.parkingspot_service.dtos.ParkingSpotRequestDto;
import com.parkease.parkingspot_service.dtos.ParkingSpotResponseDto;
import com.parkease.parkingspot_service.entity.SpotType;
import com.parkease.parkingspot_service.entity.VehicleType;
import com.parkease.parkingspot_service.service.ParkingSpotService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingSpotControllerTest {

    @InjectMocks
    private ParkingSpotController controller;

    @Mock
    private ParkingSpotService parkingSpotService;

    @Test
    void addSpot_ShouldReturnCreated() {

        when(parkingSpotService.addSpot(any(ParkingSpotRequestDto.class)))
                .thenReturn(buildResponse());

        ResponseEntity<ParkingSpotResponseDto> response =
                controller.addSpot(buildRequest());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        assertNotNull(response.getBody());

        verify(parkingSpotService)
                .addSpot(any(ParkingSpotRequestDto.class));
    }

    @Test
    void addBulkSpots_ShouldReturnCreated() {

        ParkingSpotBulkRequestDto bulk =
                new ParkingSpotBulkRequestDto();

        bulk.setSpots(List.of(buildRequest()));

        when(parkingSpotService.addBulkSpots(any(ParkingSpotBulkRequestDto.class)))
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<ParkingSpotResponseDto>> response =
                controller.addBulkSpots(bulk);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        assertEquals(1, response.getBody().size());
    }

    @Test
    void getSpotById_ShouldReturnSpot() {

        when(parkingSpotService.getSpotById(1L))
                .thenReturn(buildResponse());

        ResponseEntity<ParkingSpotResponseDto> response =
                controller.getSpotById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertNotNull(response.getBody());
    }

    @Test
    void getSpotsByLot_ShouldReturnList() {

        when(parkingSpotService.getSpotsByLot(1L))
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<ParkingSpotResponseDto>> response =
                controller.getSpotsByLot(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAvailableSpots_ShouldReturnList() {

        when(parkingSpotService.getAvailableSpots(1L))
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<ParkingSpotResponseDto>> response =
                controller.getAvailableSpots(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(1, response.getBody().size());
    }

    @Test
    void reserveSpot_ShouldReturnOk() {

        when(parkingSpotService.reserveSpot(1L))
                .thenReturn(buildResponse());

        ResponseEntity<ParkingSpotResponseDto> response =
                controller.reserveSpot(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void occupySpot_ShouldReturnOk() {

        when(parkingSpotService.occupySpot(1L))
                .thenReturn(buildResponse());

        ResponseEntity<ParkingSpotResponseDto> response =
                controller.occupySpot(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void releaseSpot_ShouldReturnOk() {

        when(parkingSpotService.releaseSpot(1L))
                .thenReturn(buildResponse());

        ResponseEntity<ParkingSpotResponseDto> response =
                controller.releaseSpot(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateSpot_ShouldReturnUpdatedSpot() {

        when(parkingSpotService.updateSpot(anyLong(), any(ParkingSpotRequestDto.class)))
                .thenReturn(buildResponse());

        ResponseEntity<ParkingSpotResponseDto> response =
                controller.updateSpot(1L, buildRequest());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteSpot_ShouldReturnNoContent() {

        doNothing().when(parkingSpotService)
                .deleteSpot(1L);

        ResponseEntity<Void> response =
                controller.deleteSpot(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        verify(parkingSpotService)
                .deleteSpot(1L);
    }

    @Test
    void countAvailable_ShouldReturnCount() {

        when(parkingSpotService.countAvailable(1L))
                .thenReturn(5L);

        ResponseEntity<Long> response =
                controller.countAvailable(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(5L, response.getBody());
    }

    private ParkingSpotRequestDto buildRequest() {

        ParkingSpotRequestDto request =
                new ParkingSpotRequestDto();

        request.setLotId(1L);
        request.setSpotNumber("A1");
        request.setFloor(1);
        request.setSpotType(SpotType.TRUCK);
        request.setVehicleType(VehicleType.FOUR_WHEELER);
        request.setHandicapped(false);
        request.setEVCharging(false);
        request.setPricePerHour(50.0);

        return request;
    }

    private ParkingSpotResponseDto buildResponse() {

        ParkingSpotResponseDto dto =
                new ParkingSpotResponseDto();

        dto.setSpotId(1L);
        dto.setLotId(1L);
        dto.setSpotNumber("A1");

        return dto;
    }
}