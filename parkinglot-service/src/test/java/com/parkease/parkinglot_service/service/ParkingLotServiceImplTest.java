package com.parkease.parkinglot_service.service;

import com.parkease.parkinglot_service.client.UserServiceClient;
import com.parkease.parkinglot_service.dtos.ParkingLotRequestDto;
import com.parkease.parkinglot_service.dtos.ParkingLotResponseDto;
import com.parkease.parkinglot_service.dtos.UserLookupResponseDto;
import com.parkease.parkinglot_service.entity.ParkingLot;
import com.parkease.parkinglot_service.exception.ParkingLotNotFoundException;
import com.parkease.parkinglot_service.mapper.Impl.ParkingLotRequestMapper;
import com.parkease.parkinglot_service.mapper.Impl.ParkingLotResponseMapper;
import com.parkease.parkinglot_service.repository.ParkingLotRepository;
import com.parkease.parkinglot_service.service.Impl.ParkingLotServiceImpl;
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
class ParkingLotServiceImplTest {

    @Mock
    private ParkingLotRepository parkingLotRepository;
    @Mock
    private ParkingLotRequestMapper requestMapper;
    @Mock
    private ParkingLotResponseMapper responseMapper;
    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private ParkingLotServiceImpl service;

    @Test
    void createLot_ShouldSave_WhenManagerValid() {
        ParkingLotRequestDto request = buildRequest();
        ParkingLot lot = new ParkingLot();
        ParkingLot saved = new ParkingLot();
        saved.setLotId(10L);

        when(userServiceClient.getUserById(1L)).thenReturn(buildUser("MANAGER"));
        when(requestMapper.mapFrom(request)).thenReturn(lot);
        when(parkingLotRepository.save(lot)).thenReturn(saved);
        when(responseMapper.mapTo(saved)).thenReturn(new ParkingLotResponseDto());

        ParkingLotResponseDto response = service.createLot(request);

        assertNotNull(response);
        assertEquals(request.getTotalSpots(), lot.getAvailableSpots());
    }

    @Test
    void createLot_ShouldThrow_WhenRoleInvalid() {
        ParkingLotRequestDto request = buildRequest();
        when(userServiceClient.getUserById(1L)).thenReturn(buildUser("DRIVER"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createLot(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getLotById_ShouldReturn_WhenFound() {
        ParkingLot lot = new ParkingLot();
        when(parkingLotRepository.findById(10L)).thenReturn(Optional.of(lot));
        when(responseMapper.mapTo(lot)).thenReturn(new ParkingLotResponseDto());

        ParkingLotResponseDto response = service.getLotById(10L);

        assertNotNull(response);
    }

    @Test
    void updateLot_ShouldThrow_WhenManagerDifferent() {
        ParkingLotRequestDto request = buildRequest();
        request.setManagerId(2L);
        ParkingLot lot = new ParkingLot();
        lot.setLotId(10L);
        lot.setManagerId(1L);
        lot.setTotalSpots(10L);
        lot.setAvailableSpots(5L);

        when(parkingLotRepository.findById(10L)).thenReturn(Optional.of(lot));
        when(userServiceClient.getUserById(2L)).thenReturn(buildUser("MANAGER"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.updateLot(10L, request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void updateLot_ShouldThrow_WhenTotalSpotsLessThanOccupied() {
        ParkingLotRequestDto request = buildRequest();
        request.setTotalSpots(4L);
        ParkingLot lot = new ParkingLot();
        lot.setLotId(10L);
        lot.setManagerId(1L);
        lot.setTotalSpots(10L);
        lot.setAvailableSpots(2L);

        when(parkingLotRepository.findById(10L)).thenReturn(Optional.of(lot));
        when(userServiceClient.getUserById(1L)).thenReturn(buildUser("MANAGER"));

        assertThrows(IllegalStateException.class, () -> service.updateLot(10L, request));
    }

    @Test
    void updateLot_ShouldSave_WhenValid() {
        ParkingLotRequestDto request = buildRequest();
        ParkingLot lot = new ParkingLot();
        lot.setLotId(10L);
        lot.setManagerId(1L);
        lot.setTotalSpots(10L);
        lot.setAvailableSpots(5L);

        when(parkingLotRepository.findById(10L)).thenReturn(Optional.of(lot));
        when(userServiceClient.getUserById(1L)).thenReturn(buildUser("MANAGER"));
        when(parkingLotRepository.save(lot)).thenReturn(lot);
        when(responseMapper.mapTo(lot)).thenReturn(new ParkingLotResponseDto());

        ParkingLotResponseDto response = service.updateLot(10L, request);

        assertNotNull(response);
    }

    @Test
    void decrementAvailable_ShouldThrow_WhenNoSpots() {
        ParkingLot lot = new ParkingLot();
        lot.setLotId(10L);
        lot.setAvailableSpots(0L);
        when(parkingLotRepository.findById(10L)).thenReturn(Optional.of(lot));

        assertThrows(IllegalStateException.class, () -> service.decrementAvailable(10L));
    }

    @Test
    void decrementAvailable_ShouldSave_WhenSpotsAvailable() {
        ParkingLot lot = new ParkingLot();
        lot.setLotId(10L);
        lot.setAvailableSpots(2L);
        when(parkingLotRepository.findById(10L)).thenReturn(Optional.of(lot));

        service.decrementAvailable(10L);

        verify(parkingLotRepository).save(lot);
        assertEquals(1L, lot.getAvailableSpots());
    }

    @Test
    void incrementAvailable_ShouldThrow_WhenExceedsTotal() {
        ParkingLot lot = new ParkingLot();
        lot.setLotId(10L);
        lot.setAvailableSpots(5L);
        lot.setTotalSpots(5L);
        when(parkingLotRepository.findById(10L)).thenReturn(Optional.of(lot));

        assertThrows(IllegalStateException.class, () -> service.incrementAvailable(10L));
    }

    @Test
    void incrementAvailable_ShouldSave_WhenBelowTotal() {
        ParkingLot lot = new ParkingLot();
        lot.setLotId(10L);
        lot.setAvailableSpots(4L);
        lot.setTotalSpots(5L);
        when(parkingLotRepository.findById(10L)).thenReturn(Optional.of(lot));

        service.incrementAvailable(10L);

        verify(parkingLotRepository).save(lot);
        assertEquals(5L, lot.getAvailableSpots());
    }

    @Test
    void toggleOpen_ShouldToggleStatus() {
        ParkingLot lot = new ParkingLot();
        lot.setLotId(10L);
        lot.setOpen(true);
        when(parkingLotRepository.findById(10L)).thenReturn(Optional.of(lot));

        service.toggleOpen(10L);

        assertFalse(lot.isOpen());
        verify(parkingLotRepository).save(lot);
    }

    @Test
    void deleteLot_ShouldDelete_WhenExists() {
        ParkingLot lot = new ParkingLot();
        when(parkingLotRepository.findById(10L)).thenReturn(Optional.of(lot));

        service.deleteLot(10L);

        verify(parkingLotRepository).delete(lot);
    }

    @Test
    void getLotById_ShouldThrow_WhenMissing() {
        when(parkingLotRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ParkingLotNotFoundException.class, () -> service.getLotById(10L));
    }

    @Test
    void getLotsByCity_ShouldReturnList() {
        when(parkingLotRepository.findByCityIgnoreCase("pune")).thenReturn(List.of(new ParkingLot()));
        when(responseMapper.mapTo(any(ParkingLot.class))).thenReturn(new ParkingLotResponseDto());

        List<ParkingLotResponseDto> result = service.getLotsByCity("pune");

        assertEquals(1, result.size());
    }

    @Test
    void getNearbyLots_ShouldReturnList() {
        when(parkingLotRepository.findNearby(1.0, 1.0, 5.0)).thenReturn(List.of(new ParkingLot()));
        when(responseMapper.mapTo(any(ParkingLot.class))).thenReturn(new ParkingLotResponseDto());

        List<ParkingLotResponseDto> result = service.getNearbyLots(1.0, 1.0, 5.0);

        assertEquals(1, result.size());
    }

    @Test
    void getLotsByManager_ShouldReturnList() {
        when(parkingLotRepository.findByManagerId(1L)).thenReturn(List.of(new ParkingLot()));
        when(responseMapper.mapTo(any(ParkingLot.class))).thenReturn(new ParkingLotResponseDto());

        List<ParkingLotResponseDto> result = service.getLotsByManager(1L);

        assertEquals(1, result.size());
    }

    @Test
    void searchLots_ShouldReturnList() {
        when(parkingLotRepository.searchByKeyword("park")).thenReturn(List.of(new ParkingLot()));
        when(responseMapper.mapTo(any(ParkingLot.class))).thenReturn(new ParkingLotResponseDto());

        List<ParkingLotResponseDto> result = service.searchLots("park");

        assertEquals(1, result.size());
    }

    @Test
    void validateManager_ShouldThrow_WhenUserMissing() {
        ParkingLotRequestDto request = buildRequest();
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(404);
        when(userServiceClient.getUserById(1L)).thenThrow(exception);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createLot(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    private ParkingLotRequestDto buildRequest() {
        ParkingLotRequestDto request = new ParkingLotRequestDto();
        request.setName("Lot");
        request.setAddress("Addr");
        request.setCity("City");
        request.setLatitude(1.0);
        request.setLongitude(1.0);
        request.setTotalSpots(10L);
        request.setManagerId(1L);
        request.setOpen(true);
        request.setApproved(true);
        return request;
    }

    private UserLookupResponseDto buildUser(String role) {
        UserLookupResponseDto dto = new UserLookupResponseDto();
        dto.setRole(role);
        return dto;
    }
}
