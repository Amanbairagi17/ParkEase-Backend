package com.parkease.booking_service.controller;

import com.parkease.booking_service.dtos.BookingEstimateRequestDto;
import com.parkease.booking_service.dtos.BookingEstimateResponseDto;
import com.parkease.booking_service.dtos.BookingRequestDto;
import com.parkease.booking_service.dtos.BookingResponseDto;
import com.parkease.booking_service.entity.BookingStatus;
import com.parkease.booking_service.entity.BookingType;
import com.parkease.booking_service.entity.PricingType;
import com.parkease.booking_service.service.BookingService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @InjectMocks
    private BookingController bookingController;

    @Mock
    private BookingService bookingService;

    @Test
    void createBooking_ShouldReturnCreated() {

        BookingRequestDto request = buildRequest();

        BookingResponseDto responseDto = buildResponse();

        when(bookingService.createBooking(any(BookingRequestDto.class)))
                .thenReturn(responseDto);

        ResponseEntity<BookingResponseDto> response =
                bookingController.createBooking(
                        request,
                        "user@demo.com"
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        assertNotNull(response.getBody());

        assertEquals(
                10L,
                response.getBody().getBookingId()
        );

        verify(bookingService)
                .createBooking(any(BookingRequestDto.class));
    }

    @Test
    void getBookingById_ShouldReturnOk() {

        BookingResponseDto responseDto = buildResponse();

        when(bookingService.getBookingById(10L))
                .thenReturn(responseDto);

        ResponseEntity<BookingResponseDto> response =
                bookingController.getBookingById(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                10L,
                response.getBody().getBookingId()
        );

        verify(bookingService).getBookingById(10L);
    }

    @Test
    void getBookingsByUser_ShouldReturnOk() {

        when(bookingService.getBookingsByUser(1L))
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<BookingResponseDto>> response =
                bookingController.getBookingsByUser(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                1,
                response.getBody().size()
        );

        verify(bookingService).getBookingsByUser(1L);
    }

    @Test
    void getBookingsByLot_ShouldReturnOk() {

        when(bookingService.getBookingsByLot(2L))
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<BookingResponseDto>> response =
                bookingController.getBookingsByLot(2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                1,
                response.getBody().size()
        );

        verify(bookingService).getBookingsByLot(2L);
    }

    @Test
    void getActiveBookingsByLot_ShouldReturnOk() {

        when(bookingService.getActiveBookingsByLot(2L))
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<BookingResponseDto>> response =
                bookingController.getActiveBookingsByLot(2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                1,
                response.getBody().size()
        );

        verify(bookingService)
                .getActiveBookingsByLot(2L);
    }

    @Test
    void getActiveBookings_ShouldReturnOk() {

        when(bookingService.getActiveBookings())
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<BookingResponseDto>> response =
                bookingController.getActiveBookings();

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                1,
                response.getBody().size()
        );

        verify(bookingService).getActiveBookings();
    }

    @Test
    void cancelBooking_ShouldReturnOk() {

        BookingResponseDto responseDto = buildResponse();

        responseDto.setStatus(BookingStatus.CANCELLED);

        when(bookingService.cancelBooking(10L))
                .thenReturn(responseDto);

        ResponseEntity<BookingResponseDto> response =
                bookingController.cancelBooking(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                BookingStatus.CANCELLED,
                response.getBody().getStatus()
        );

        verify(bookingService).cancelBooking(10L);
    }

    @Test
    void checkIn_ShouldReturnOk() {

        BookingResponseDto responseDto = buildResponse();

        responseDto.setStatus(BookingStatus.ACTIVE);

        when(bookingService.checkIn(10L))
                .thenReturn(responseDto);

        ResponseEntity<BookingResponseDto> response =
                bookingController.checkIn(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                BookingStatus.ACTIVE,
                response.getBody().getStatus()
        );

        verify(bookingService).checkIn(10L);
    }

    @Test
    void checkOut_ShouldReturnOk() {

        BookingResponseDto responseDto = buildResponse();

        responseDto.setStatus(BookingStatus.COMPLETED);

        when(bookingService.checkOut(
                10L,
                new BigDecimal("50")
        )).thenReturn(responseDto);

        ResponseEntity<BookingResponseDto> response =
                bookingController.checkOut(
                        10L,
                        new BigDecimal("50")
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                BookingStatus.COMPLETED,
                response.getBody().getStatus()
        );

        verify(bookingService)
                .checkOut(
                        10L,
                        new BigDecimal("50")
                );
    }

    @Test
    void extendBooking_ShouldReturnOk() {

        LocalDateTime newEnd =
                LocalDateTime.now().plusHours(5);

        when(bookingService.extendBooking(
                anyLong(),
                any(LocalDateTime.class)
        )).thenReturn(buildResponse());

        ResponseEntity<BookingResponseDto> response =
                bookingController.extendBooking(
                        10L,
                        newEnd
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(bookingService)
                .extendBooking(
                        anyLong(),
                        any(LocalDateTime.class)
                );
    }

    @Test
    void calculateAmount_ShouldReturnOk() {

        LocalDateTime start =
                LocalDateTime.now();

        LocalDateTime end =
                start.plusHours(2);

        when(bookingService.calculateAmount(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                any(BigDecimal.class)
        )).thenReturn(new BigDecimal("100"));

        ResponseEntity<BigDecimal> response =
                bookingController.calculateAmount(
                        start,
                        end,
                        new BigDecimal("50")
                );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                new BigDecimal("100"),
                response.getBody()
        );

        verify(bookingService)
                .calculateAmount(
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        any(BigDecimal.class)
                );
    }

    @Test
    void getBookingHistory_ShouldReturnOk() {

        when(bookingService.getBookingHistory(1L))
                .thenReturn(List.of(buildResponse()));

        ResponseEntity<List<BookingResponseDto>> response =
                bookingController.getBookingHistory(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                1,
                response.getBody().size()
        );

        verify(bookingService).getBookingHistory(1L);
    }

    @Test
    void estimateBooking_ShouldReturnOk() {

        BookingEstimateRequestDto request =
                new BookingEstimateRequestDto();

        request.setLotId(2L);
        request.setSpotId(1L);

        request.setStartTime(
                LocalDateTime.now().plusHours(1)
        );

        request.setEndTime(
                LocalDateTime.now().plusHours(2)
        );

        BookingEstimateResponseDto responseDto =
                BookingEstimateResponseDto.builder()
                        .lotId(2L)
                        .spotId(1L)
                        .totalAmount(new BigDecimal("50"))
                        .build();

        when(bookingService.estimateBooking(
                any(BookingEstimateRequestDto.class)
        )).thenReturn(responseDto);

        ResponseEntity<BookingEstimateResponseDto> response =
                bookingController.estimateBooking(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                new BigDecimal("50"),
                response.getBody().getTotalAmount()
        );

        verify(bookingService)
                .estimateBooking(
                        any(BookingEstimateRequestDto.class)
                );
    }

    @Test
    void getBookingEstimate_ShouldReturnOk() {

        when(bookingService.getBookingEstimate(10L))
                .thenReturn(new BigDecimal("150"));

        ResponseEntity<BigDecimal> response =
                bookingController.getBookingEstimate(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                new BigDecimal("150"),
                response.getBody()
        );

        verify(bookingService).getBookingEstimate(10L);
    }

    private BookingRequestDto buildRequest() {

        BookingRequestDto request =
                new BookingRequestDto();

        request.setUserId(1L);
        request.setLotId(2L);
        request.setSpotId(3L);

        request.setVehiclePlate("MP04AB1234");
        request.setVehicleType("CAR");

        request.setBookingType(BookingType.PRE_BOOKING);

        request.setPricingType(PricingType.HOURLY);

        request.setStartTime(
                LocalDateTime.now().plusHours(1)
        );

        request.setEndTime(
                LocalDateTime.now().plusHours(2)
        );

        request.setEmail("user@demo.com");

        return request;
    }

    private BookingResponseDto buildResponse() {

        BookingResponseDto response =
                new BookingResponseDto();

        response.setBookingId(10L);

        response.setUserId(1L);
        response.setLotId(2L);
        response.setSpotId(3L);

        response.setVehiclePlate("MP04AB1234");
        response.setVehicleType("CAR");

        response.setBookingType(BookingType.WALK_IN);

        response.setPricingType(PricingType.HOURLY);

        response.setStatus(BookingStatus.RESERVED);

        response.setTotalAmount(
                new BigDecimal("100")
        );

        return response;
    }

    @Test
    void createBooking_ShouldSetEmailFromHeader() {

        BookingRequestDto request = buildRequest();

        when(bookingService.createBooking(any()))
                .thenReturn(buildResponse());

        bookingController.createBooking(
                request,
                "header@demo.com"
        );

        assertEquals(
                "header@demo.com",
                request.getEmail()
        );
    }

    @Test
    void checkOut_ShouldHandleNullHourlyRate() {

        when(bookingService.checkOut(10L, null))
                .thenReturn(buildResponse());

        ResponseEntity<BookingResponseDto> response =
                bookingController.checkOut(10L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}