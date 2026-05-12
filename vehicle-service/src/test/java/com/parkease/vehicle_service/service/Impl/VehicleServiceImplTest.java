package com.parkease.vehicle_service.service.Impl;

import com.parkease.vehicle_service.client.UserServiceClient;
import com.parkease.vehicle_service.dtos.UserLookupResponseDto;
import com.parkease.vehicle_service.dtos.VehicleRequestDto;
import com.parkease.vehicle_service.dtos.VehicleResponseDto;
import com.parkease.vehicle_service.entity.Vehicle;
import com.parkease.vehicle_service.exception.VehicleNotFoundException;
import com.parkease.vehicle_service.mapper.Impl.VehicleRequestMapper;
import com.parkease.vehicle_service.mapper.Impl.VehicleResponseMapper;
import com.parkease.vehicle_service.repository.VehicleRepository;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
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
class VehicleServiceImplTest {

    @Mock
    private VehicleRepository repository;
    @Mock
    private VehicleRequestMapper requestMapper;
    @Mock
    private VehicleResponseMapper responseMapper;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private VehicleServiceImpl service;

    @Test
    void createVehicle_ShouldSave_WhenValid() {
        VehicleRequestDto dto = buildRequest("2W");
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(10L);

        when(request.getHeader("X-User-Id")).thenReturn("5");
        when(userServiceClient.getUserById(5L)).thenReturn(new UserLookupResponseDto());
        when(requestMapper.mapFrom(dto)).thenReturn(vehicle);
        when(repository.save(vehicle)).thenReturn(vehicle);
        when(responseMapper.mapTo(vehicle)).thenReturn(new VehicleResponseDto());

        VehicleResponseDto response = service.createVehicle(dto);

        assertNotNull(response);
        assertEquals(5L, vehicle.getOwnerId());
    }

    @Test
    void findVehicleById_ShouldThrow_WhenMissing() {
        when(repository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> service.findVehicleById(10L));
    }

    @Test
    void getByLicensePlate_ShouldThrow_WhenMissing() {
        when(repository.findByLicensePlate("AB12CD1234")).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> service.getByLicensePlate("AB12CD1234"));
    }

    @Test
    void getVehiclesByOwner_ShouldReturnList() {
        when(repository.findByOwnerId(5L)).thenReturn(List.of(new Vehicle()));
        when(responseMapper.mapTo(any(Vehicle.class))).thenReturn(new VehicleResponseDto());

        List<VehicleResponseDto> result = service.getVehiclesByOwner(5L);

        assertEquals(1, result.size());
    }

    @Test
    void updateVehicle_ShouldThrow_WhenOwnerInvalid() {
        Vehicle existing = new Vehicle();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        FeignException exception = mock(FeignException.class);
        when(exception.status()).thenReturn(404);
        when(userServiceClient.getUserById(1L)).thenThrow(exception);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.updateVehicle(1L, buildRequest("2W")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void updateVehicle_ShouldThrow_WhenVehicleTypeInvalid() {
        Vehicle existing = new Vehicle();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(userServiceClient.getUserById(1L)).thenReturn(new UserLookupResponseDto());

        VehicleRequestDto dto = buildRequest("BAD");

        assertThrows(IllegalArgumentException.class, () -> service.updateVehicle(1L, dto));
    }

    @Test
    void deleteVehicle_ShouldDelete_WhenExists() {
        Vehicle existing = new Vehicle();
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.deleteVehicle(1L);

        verify(repository).delete(existing);
    }

    @Test
    void getAllVehicles_ShouldReturnList() {
        when(repository.findAll()).thenReturn(List.of(new Vehicle()));
        when(responseMapper.mapTo(any(Vehicle.class))).thenReturn(new VehicleResponseDto());

        List<VehicleResponseDto> result = service.getAllVehicles();

        assertEquals(1, result.size());
    }

    @Test
    void getVehicleType_ShouldReturnType() {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleType(com.parkease.vehicle_service.entity.VehicleType.TWO_WHEELER);
        when(repository.findById(1L)).thenReturn(Optional.of(vehicle));

        String type = service.getVehicleType(1L);

        assertEquals("TWO_WHEELER", type);
    }

    @Test
    void isEVVehicle_ShouldReturnFlag() {
        Vehicle vehicle = new Vehicle();
        vehicle.setIsEV(true);
        when(repository.findById(1L)).thenReturn(Optional.of(vehicle));

        Boolean result = service.isEVVehicle(1L);

        assertTrue(result);
    }

    @Test
    void createVehicle_ShouldThrow_WhenVehicleTypeInvalid() {

        VehicleRequestDto dto = buildRequest("INVALID");

        when(request.getHeader("X-User-Id"))
                .thenReturn("5");

        when(userServiceClient.getUserById(5L))
                .thenReturn(new UserLookupResponseDto());

        when(requestMapper.mapFrom(any(VehicleRequestDto.class)))
                .thenReturn(new Vehicle());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createVehicle(dto)
        );
    }

    @Test
    void getVehicleType_ShouldThrow_WhenMissing() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                VehicleNotFoundException.class,
                () -> service.getVehicleType(1L)
        );
    }

    @Test
    void isEVVehicle_ShouldThrow_WhenMissing() {

        when(repository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                VehicleNotFoundException.class,
                () -> service.isEVVehicle(1L)
        );
    }

    @Test
    void updateVehicle_ShouldUpdate_WhenValid() {

        Vehicle existing = new Vehicle();

        VehicleRequestDto dto = buildRequest("4W");

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        when(userServiceClient.getUserById(1L))
                .thenReturn(new UserLookupResponseDto());

        when(repository.save(any(Vehicle.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(responseMapper.mapTo(any(Vehicle.class)))
                .thenReturn(new VehicleResponseDto());

        VehicleResponseDto result =
                service.updateVehicle(1L, dto);

        assertNotNull(result);

        verify(repository)
                .save(existing);
    }

    @Test
    void updateVehicle_ShouldThrow_WhenAuthServiceFails() {

        Vehicle existing = new Vehicle();

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        FeignException exception =
                mock(FeignException.class);

        when(exception.status())
                .thenReturn(500);

        when(userServiceClient.getUserById(1L))
                .thenThrow(exception);

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.updateVehicle(
                                1L,
                                buildRequest("2W")
                        )
                );

        assertEquals(
                HttpStatus.BAD_GATEWAY,
                ex.getStatusCode()
        );
    }

    private VehicleRequestDto buildRequest(String type) {
        VehicleRequestDto dto = new VehicleRequestDto();
        dto.setOwnerId(1L);
        dto.setLicensePlate("AB12CD1234");
        dto.setMake("Make");
        dto.setModel("Model");
        dto.setColor("Blue");
        dto.setVehicleType(type);
        dto.setIsEV(false);
        return dto;
    }
}
