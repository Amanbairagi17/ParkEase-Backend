package com.parkease.vehicle_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.vehicle_service.dtos.VehicleRequestDto;
import com.parkease.vehicle_service.dtos.VehicleResponseDto;
import com.parkease.vehicle_service.service.VehicleService;
import com.parkease.vehicle_service.utils.VehicleSecurity;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@AutoConfigureMockMvc(addFilters = false)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private VehicleSecurity vehicleSecurity;

    @Test
    void registerVehicle_ShouldReturnCreated()
            throws Exception {

        when(vehicleService.createVehicle(
                any(VehicleRequestDto.class)))
                .thenReturn(new VehicleResponseDto());

        mockMvc.perform(
                        post("/api/vehicle/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                buildRequest()
                                        )
                                )
                )
                .andExpect(status().isCreated());
    }

    @Test
    void getVehicleById_ShouldReturnOk()
            throws Exception {

        when(vehicleService.findVehicleById(1L))
                .thenReturn(new VehicleResponseDto());

        mockMvc.perform(
                        get("/api/vehicle/getById/1")
                )
                .andExpect(status().isOk());
    }

    @Test
    void getVehiclesByOwner_ShouldReturnOk()
            throws Exception {

        when(vehicleService.getVehiclesByOwner(1L))
                .thenReturn(
                        List.of(new VehicleResponseDto())
                );

        mockMvc.perform(
                        get("/api/vehicle/getVehiclesByOwner/1")
                )
                .andExpect(status().isOk());
    }

    @Test
    void getVehiclesByUser_ShouldReturnOk()
            throws Exception {

        when(vehicleService.getVehiclesByOwner(1L))
                .thenReturn(
                        List.of(new VehicleResponseDto())
                );

        mockMvc.perform(
                        get("/api/vehicle/user/1")
                )
                .andExpect(status().isOk());
    }

    @Test
    void getByLicensePlate_ShouldReturnOk()
            throws Exception {

        when(vehicleService.getByLicensePlate(
                "MP09AB1234"))
                .thenReturn(new VehicleResponseDto());

        mockMvc.perform(
                        get("/api/vehicle/getByLicensePlate/MP09AB1234")
                )
                .andExpect(status().isOk());
    }

    @Test
    void updateVehicle_ShouldReturnOk()
            throws Exception {

        when(vehicleService.updateVehicle(
                eq(1L),
                any(VehicleRequestDto.class)
        )).thenReturn(new VehicleResponseDto());

        mockMvc.perform(
                        put("/api/vehicle/update/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                buildRequest()
                                        )
                                )
                )
                .andExpect(status().isOk());
    }

    @Test
    void deleteVehicle_ShouldReturnOk()
            throws Exception {

        doNothing()
                .when(vehicleService)
                .deleteVehicle(1L);

        mockMvc.perform(
                        delete("/api/vehicle/delete/1")
                )
                .andExpect(status().isOk());
    }

    @Test
    void getVehicleType_ShouldReturnOk()
            throws Exception {

        when(vehicleService.getVehicleType(1L))
                .thenReturn("4W");

        mockMvc.perform(
                        get("/api/vehicle/getVehicleType/1")
                )
                .andExpect(status().isOk());
    }

    @Test
    void isEVVehicle_ShouldReturnOk()
            throws Exception {

        when(vehicleService.isEVVehicle(1L))
                .thenReturn(true);

        mockMvc.perform(
                        get("/api/vehicle/isEVVehicle/1")
                )
                .andExpect(status().isOk());
    }

    @Test
    void getAllVehicles_ShouldReturnOk()
            throws Exception {

        when(vehicleService.getAllVehicles())
                .thenReturn(
                        List.of(new VehicleResponseDto())
                );

        mockMvc.perform(
                        get("/api/vehicle/getAllVehicles")
                )
                .andExpect(status().isOk());
    }

    @Test
    void getAvailableVehicleTypes_ShouldReturnOk()
            throws Exception {

        mockMvc.perform(
                        get("/api/vehicle/types")
                )
                .andExpect(status().isOk());
    }

    private VehicleRequestDto buildRequest() {

        VehicleRequestDto dto =
                new VehicleRequestDto();

        dto.setOwnerId(1L);

        dto.setLicensePlate("MP09AB1234");

        dto.setMake("Honda");

        dto.setModel("City");

        dto.setColor("White");

        dto.setVehicleType("4W");

        dto.setIsEV(false);

        return dto;
    }
}