package com.parkease.parkinglot_service.controller;

import com.parkease.parkinglot_service.dtos.ParkingLotRequestDto;
import com.parkease.parkinglot_service.dtos.ParkingLotResponseDto;
import com.parkease.parkinglot_service.service.ParkingLotService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingLotControllerTest {

    @InjectMocks
    private ParkingLotController controller;

    @Mock
    private ParkingLotService parkingLotService;

    @Test
    void createLot_ShouldReturnCreated() {

        ParkingLotRequestDto request = buildRequest();

        ParkingLotResponseDto responseDto = buildResponse();

        when(parkingLotService.createLot(any(ParkingLotRequestDto.class)))
                .thenReturn(responseDto);

        ResponseEntity<ParkingLotResponseDto> response =
                controller.createLot(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        assertNotNull(response.getBody());

        verify(parkingLotService)
                .createLot(any(ParkingLotRequestDto.class));
    }

    @Test
    void getLots_ShouldReturnList() {

        when(parkingLotService.searchLots(anyString()))
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<ParkingLotResponseDto>> response =
                controller.getLots("park");

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                1,
                response.getBody().size()
        );
    }

    @Test
    void getLots_ShouldHandleNullSearch() {

        when(parkingLotService.searchLots(""))
                .thenReturn(List.of());

        ResponseEntity<List<ParkingLotResponseDto>> response =
                controller.getLots(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                0,
                response.getBody().size()
        );
    }

    @Test
    void getLotById_ShouldReturnLot() {

        when(parkingLotService.getLotById(10L))
                .thenReturn(buildResponse());

        ResponseEntity<ParkingLotResponseDto> response =
                controller.getLotById(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertNotNull(response.getBody());
    }

    @Test
    void getLotsByCity_ShouldReturnList() {

        when(parkingLotService.getLotsByCity("Indore"))
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<ParkingLotResponseDto>> response =
                controller.getLotsByCity("Indore");

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                1,
                response.getBody().size()
        );
    }

    @Test
    void getNearbyLots_ShouldReturnList() {

        when(parkingLotService.getNearbyLots(
                anyDouble(),
                anyDouble(),
                anyDouble()
        )).thenReturn(List.of(buildResponse()));

        ResponseEntity<List<ParkingLotResponseDto>> response =
                controller.getNearbyLots(
                        10.0,
                        20.0,
                        5.0
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                1,
                response.getBody().size()
        );
    }

    @Test
    void getLotsByManager_ShouldReturnList() {

        when(parkingLotService.getLotsByManager(1L))
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<ParkingLotResponseDto>> response =
                controller.getLotsByManager(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                1,
                response.getBody().size()
        );
    }

    @Test
    void updateLot_ShouldReturnUpdatedLot() {

        when(parkingLotService.updateLot(
                anyLong(),
                any(ParkingLotRequestDto.class)
        )).thenReturn(buildResponse());

        ResponseEntity<ParkingLotResponseDto> response =
                controller.updateLot(
                        10L,
                        buildRequest()
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertNotNull(response.getBody());
    }

    @Test
    void toggleOpen_ShouldReturnNoContent() {

        doNothing().when(parkingLotService)
                .toggleOpen(10L);

        ResponseEntity<Void> response =
                controller.toggleOpen(10L);

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );

        verify(parkingLotService)
                .toggleOpen(10L);
    }

    @Test
    void deleteLot_ShouldReturnNoContent() {

        doNothing().when(parkingLotService)
                .deleteLot(10L);

        ResponseEntity<Void> response =
                controller.deleteLot(10L);

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );

        verify(parkingLotService)
                .deleteLot(10L);
    }

    @Test
    void decrementAvailable_ShouldReturnNoContent() {

        doNothing().when(parkingLotService)
                .decrementAvailable(10L);

        ResponseEntity<Void> response =
                controller.decrementAvailable(10L);

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );
    }

    @Test
    void incrementAvailable_ShouldReturnNoContent() {

        doNothing().when(parkingLotService)
                .incrementAvailable(10L);

        ResponseEntity<Void> response =
                controller.incrementAvailable(10L);

        assertEquals(
                HttpStatus.NO_CONTENT,
                response.getStatusCode()
        );
    }

    @Test
    void searchLots_ShouldReturnList() {

        when(parkingLotService.searchLots("mall"))
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<ParkingLotResponseDto>> response =
                controller.searchLots("mall");

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                1,
                response.getBody().size()
        );
    }

    private ParkingLotRequestDto buildRequest() {

        ParkingLotRequestDto request =
                new ParkingLotRequestDto();

        request.setName("Lot A");
        request.setAddress("Address");
        request.setCity("Indore");

        request.setLatitude(10.0);
        request.setLongitude(20.0);

        request.setTotalSpots(100L);

        request.setManagerId(1L);

        request.setOpen(true);

        request.setApproved(true);

        return request;
    }

    private ParkingLotResponseDto buildResponse() {

        ParkingLotResponseDto dto =
                new ParkingLotResponseDto();

        dto.setLotId(10L);

        dto.setName("Lot A");

        dto.setCity("Indore");

        dto.setAvailableSpots(100);

        return dto;
    }
}