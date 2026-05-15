package com.parkease.booking_service.service.Impl;

import com.parkease.booking_service.client.ParkingLotServiceClient;
import com.parkease.booking_service.client.ParkingSpotServiceClient;
import com.parkease.booking_service.client.PaymentServiceClient;
import com.parkease.booking_service.client.ReceiptServiceClient;
import com.parkease.booking_service.dtos.*;
import com.parkease.booking_service.entity.Booking;
import com.parkease.booking_service.entity.BookingStatus;
import com.parkease.booking_service.entity.BookingType;
import com.parkease.booking_service.entity.PricingType;
import com.parkease.booking_service.event.NotificationEventPublisher;
import com.parkease.booking_service.exception.BookingNotFoundException;
import com.parkease.booking_service.mapper.Impl.BookingRequestMapper;
import com.parkease.booking_service.mapper.Impl.BookingResponseMapper;
import com.parkease.booking_service.repository.BookingRepository;

import feign.FeignException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingRequestMapper bookingRequestMapper;

    @Mock
    private BookingResponseMapper bookingResponseMapper;

    @Mock
    private ParkingSpotServiceClient parkingSpotServiceClient;

    @Mock
    private ParkingLotServiceClient parkingLotServiceClient;

        @Mock
        private PaymentServiceClient paymentServiceClient;

        @Mock
        private ReceiptServiceClient receiptServiceClient;

    @Mock
    private NotificationEventPublisher notificationPublisher;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void createBooking_ShouldCreateAndPublish_WhenValid() {

        BookingRequestDto request = buildRequest();

        Booking booking = buildBooking();

        Booking saved = buildBooking();
        saved.setBookingId(10L);
        saved.setTotalAmount(new BigDecimal("50.00"));

        when(bookingRepository.findActiveBySpotId(1L))
                .thenReturn(Optional.empty());

        when(parkingSpotServiceClient.getSpotById(1L))
                .thenReturn(buildSpot("AVAILABLE", 50.0));

        when(bookingRequestMapper.mapFrom(request))
                .thenReturn(booking);

        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(saved);

        when(bookingResponseMapper.mapTo(saved))
                .thenReturn(new BookingResponseDto());

        BookingResponseDto response =
                bookingService.createBooking(request);

        assertNotNull(response);

        verify(parkingSpotServiceClient).reserveSpot(1L);
        verify(parkingLotServiceClient).decrementAvailable(2L);

        verify(notificationPublisher)
                .publishBookingConfirmed(
                        3L,
                        10L,
                        "user@demo.com"
                );
    }

    @Test
    void createBooking_ShouldThrow_WhenEndTimeInvalid() {

        BookingRequestDto request = buildRequest();

        request.setEndTime(
                request.getStartTime().minusMinutes(1)
        );

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(request)
        );
    }

        @Test
        void createBooking_ShouldThrow_WhenEndTimeMissing() {

                BookingRequestDto request = buildRequest();

                request.setEndTime(null);

                assertThrows(
                                IllegalStateException.class,
                                () -> bookingService.createBooking(request)
                );
        }

    @Test
    void createBooking_ShouldThrow_WhenActiveBookingExists() {

        BookingRequestDto request = buildRequest();

        when(bookingRepository.findActiveBySpotId(1L))
                .thenReturn(Optional.of(buildBooking()));

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(request)
        );
    }

    @Test
    void createBooking_ShouldThrow_WhenSpotUnavailable() {

        BookingRequestDto request = buildRequest();

        when(bookingRepository.findActiveBySpotId(1L))
                .thenReturn(Optional.empty());

        when(parkingSpotServiceClient.getSpotById(1L))
                .thenReturn(buildSpot("OCCUPIED", 50.0));

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(request)
        );
    }

    @Test
    void createBooking_ShouldThrow_WhenSpotLotMismatch() {

        BookingRequestDto request = buildRequest();

        when(bookingRepository.findActiveBySpotId(1L))
                .thenReturn(Optional.empty());

        ParkingSpotLookupResponseDto spot =
                buildSpot("AVAILABLE", 50.0);

        spot.setLotId(999L);

        when(parkingSpotServiceClient.getSpotById(1L))
                .thenReturn(spot);

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.createBooking(request)
        );
    }

    @Test
    void createBooking_ShouldRollback_WhenSaveFails() {

        BookingRequestDto request = buildRequest();

        Booking booking = buildBooking();

        when(bookingRepository.findActiveBySpotId(1L))
                .thenReturn(Optional.empty());

        when(parkingSpotServiceClient.getSpotById(1L))
                .thenReturn(buildSpot("AVAILABLE", 50.0));

        when(bookingRequestMapper.mapFrom(request))
                .thenReturn(booking);

        when(bookingRepository.save(any(Booking.class)))
                .thenThrow(new RuntimeException("db"));

        assertThrows(
                RuntimeException.class,
                () -> bookingService.createBooking(request)
        );

        verify(parkingSpotServiceClient).releaseSpot(1L);
        verify(parkingLotServiceClient).incrementAvailable(2L);
    }

    @Test
    void createBooking_ShouldThrow_WhenReserveSpotConflict() {

        BookingRequestDto request = buildRequest();

        when(bookingRepository.findActiveBySpotId(1L))
                .thenReturn(Optional.empty());

        when(parkingSpotServiceClient.getSpotById(1L))
                .thenReturn(buildSpot("AVAILABLE", 50.0));

        doThrow(mockFeignException(409))
                .when(parkingLotServiceClient)
                .decrementAvailable(2L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());

        verify(bookingRepository, never())
                .save(any(Booking.class));
    }

    @Test
    void createBooking_ShouldThrow_WhenLotUnavailable() {

        BookingRequestDto request = buildRequest();

        when(bookingRepository.findActiveBySpotId(1L))
                .thenReturn(Optional.empty());

        when(parkingSpotServiceClient.getSpotById(1L))
                .thenReturn(buildSpot("AVAILABLE", 50.0));

        when(parkingSpotServiceClient.reserveSpot(1L))
                .thenReturn(buildSpot("RESERVED", 50.0));

        doThrow(mockFeignException(409))
                .when(parkingLotServiceClient)
                .decrementAvailable(2L);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(request)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void createBooking_ShouldMapFeign404ToNotFound() {

        BookingRequestDto request = buildRequest();

        when(bookingRepository.findActiveBySpotId(1L))
                .thenReturn(Optional.empty());

        FeignException exception = mock(FeignException.class);

        when(exception.status()).thenReturn(404);

        when(parkingSpotServiceClient.getSpotById(1L))
                .thenThrow(exception);

        ResponseStatusException ex =
                assertThrows(
                        ResponseStatusException.class,
                        () -> bookingService.createBooking(request)
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                ex.getStatusCode()
        );
    }

    @Test
    void getBookingById_ShouldReturnBooking_WhenFound() {

        Booking booking = buildBooking();

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        when(bookingResponseMapper.mapTo(booking))
                .thenReturn(new BookingResponseDto());

        BookingResponseDto response =
                bookingService.getBookingById(10L);

        assertNotNull(response);
    }

    @Test
    void getBookingById_ShouldThrow_WhenMissing() {

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.getBookingById(10L)
        );
    }

    @Test
    void getBookingsByUser_ShouldMapList() {

        when(bookingRepository.findByUserId(3L))
                .thenReturn(List.of(buildBooking()));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        List<BookingResponseDto> result =
                bookingService.getBookingsByUser(3L);

        assertEquals(1, result.size());
    }

    @Test
    void getBookingsByLot_ShouldMapList() {

        when(bookingRepository.findByLotId(2L))
                .thenReturn(List.of(buildBooking()));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        List<BookingResponseDto> result =
                bookingService.getBookingsByLot(2L);

        assertEquals(1, result.size());
    }

    @Test
    void getActiveBookingsByLot_ShouldMapList() {

        when(bookingRepository.findByLotIdAndStatusIn(eq(2L), any(List.class)))
                .thenReturn(List.of(buildBooking()));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        List<BookingResponseDto> result =
                bookingService.getActiveBookingsByLot(2L);

        assertEquals(1, result.size());
    }

    @Test
    void getActiveBookings_ShouldMapList() {

                when(bookingRepository.findByStatusIn(any(List.class)))
                .thenReturn(List.of(buildBooking()));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        List<BookingResponseDto> result =
                bookingService.getActiveBookings();

        assertEquals(1, result.size());
    }

    @Test
    void cancelBooking_ShouldThrow_WhenCompleted() {

        Booking booking = buildBooking();

        booking.setStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.cancelBooking(10L)
        );
    }

    @Test
    void cancelBooking_ShouldThrow_WhenAlreadyCancelled() {

        Booking booking = buildBooking();

        booking.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.cancelBooking(10L)
        );
    }

    @Test
    void cancelBooking_ShouldReleaseSpot_WhenActive() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        bookingService.cancelBooking(10L);

        verify(parkingSpotServiceClient).releaseSpot(1L);

        verify(parkingLotServiceClient).incrementAvailable(2L);

        verify(notificationPublisher)
                .publishBookingCancelled(3L, 10L, null);
    }

//    @Test
//    void cancelBooking_ShouldIgnoreReleaseConflict() {
//
//        Booking booking = buildBooking();
//
//        booking.setStatus(BookingStatus.ACTIVE);
//
//        when(bookingRepository.findByBookingId(10L))
//                .thenReturn(Optional.of(booking));
//
//        when(parkingSpotServiceClient.releaseSpot(1L))
//                .thenThrow(mockFeignException(409));
//
//        when(bookingRepository.save(any(Booking.class)))
//                .thenAnswer(invocation -> invocation.getArgument(0));
//
//        when(bookingResponseMapper.mapTo(any(Booking.class)))
//                .thenReturn(new BookingResponseDto());
//
//        assertDoesNotThrow(() -> bookingService.cancelBooking(10L));
//    }

    @Test
    void checkIn_ShouldThrow_WhenNotReserved() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CANCELLED);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.checkIn(10L)
        );
    }

    @Test
    void checkIn_ShouldOccupySpot_WhenReserved() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        bookingService.checkIn(10L);

        verify(parkingSpotServiceClient).occupySpot(1L);

        verify(notificationPublisher)
                .publishCheckIn(3L, 10L, null);
    }

    @Test
    void checkOut_ShouldThrow_WhenNotActive() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CONFIRMED);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.checkOut(
                        10L,
                        new BigDecimal("50.00")
                )
        );
    }

    @Test
    void checkOut_ShouldThrow_WhenBookingMissing() {

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.empty());

        assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.checkOut(
                        10L,
                        new BigDecimal("50")
                )
        );
    }

    @Test
    void calculateAmount_ShouldThrow_WhenEndBeforeStart() {

        LocalDateTime start = LocalDateTime.now();

        LocalDateTime end = start.minusHours(1);

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.calculateAmount(
                        start,
                        end,
                        PricingType.HOURLY,
                        new BigDecimal("50"),
                        new BigDecimal("500")
                )
        );
    }

    @Test
    void calculateAmount_ShouldThrow_WhenStartMissing() {

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.calculateAmount(
                        null,
                        LocalDateTime.now(),
                        PricingType.HOURLY,
                        new BigDecimal("50"),
                        new BigDecimal("500")
                )
        );
    }

    @Test
    void calculateAmount_ShouldUseMinimumDailyRate() {

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);

        BigDecimal amount =
                bookingService.calculateAmount(
                        start,
                        end,
                        PricingType.DAILY,
                        new BigDecimal("10"),
                        new BigDecimal("100")
                );

        assertEquals(new BigDecimal("100.00"), amount);
    }

    @Test
    void checkOut_ShouldReleaseSpotAndPublish_WhenActive() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CHECKED_IN);
                booking.setCheckInTime(LocalDateTime.now().minusHours(1));

                PaymentResponseDto payment = new PaymentResponseDto();
                payment.setPaymentId(99L);
                payment.setAmount(new BigDecimal("100.00"));
                payment.setStatus("SUCCESS");

                ReceiptResponseDto receipt = new ReceiptResponseDto();
                receipt.setReceiptId("r-123");

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        when(paymentServiceClient.getByBookingId(10L))
                .thenReturn(payment);

        when(receiptServiceClient.getByPaymentId(99L))
                .thenReturn(receipt);

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        bookingService.checkOut(
                10L,
                new BigDecimal("50.00")
        );

        verify(parkingSpotServiceClient).releaseSpot(1L);

        verify(parkingLotServiceClient).incrementAvailable(2L);

        verify(notificationPublisher)
                .publishCheckOut(
                        eq(3L),
                        eq(10L),
                        isNull(),
                        anyString()
                );
    }

    @Test
    void checkOut_ShouldUseDefaultRate_WhenHourlyRateNull() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CHECKED_IN);
                booking.setCheckInTime(LocalDateTime.now().minusHours(1));

                PaymentResponseDto payment = new PaymentResponseDto();
                payment.setPaymentId(88L);
                payment.setAmount(new BigDecimal("100.00"));
                payment.setStatus("SUCCESS");

                ReceiptResponseDto receipt = new ReceiptResponseDto();
                receipt.setReceiptId("r-456");

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        when(paymentServiceClient.getByBookingId(10L))
                .thenReturn(payment);

        when(receiptServiceClient.getByPaymentId(88L))
                .thenReturn(receipt);

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        BookingResponseDto response = bookingService.checkOut(10L, null);

        assertNotNull(response);
        assertNotNull(booking.getTotalAmount());
    }

    @Test
    void markAsPaid_ShouldSetPaid_WhenReserved() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaid(false);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        bookingService.markAsPaid(10L);

        assertTrue(booking.isPaid());
                assertEquals(BookingStatus.PAID, booking.getStatus());
    }

    @Test
    void markAsPaid_ShouldKeepStatus_WhenActive() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CHECKED_IN);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        bookingService.markAsPaid(10L);

                assertEquals(BookingStatus.PAID, booking.getStatus());
        assertTrue(booking.isPaid());
    }

    @Test
    void markAsPaid_ShouldSetDuration_WhenMissing() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setDuration(null);
        booking.setStartTime(LocalDateTime.of(2024, 1, 1, 10, 0));
        booking.setEndTime(LocalDateTime.of(2024, 1, 1, 11, 30));

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        bookingService.markAsPaid(10L);

        assertEquals("1h 30m", booking.getDuration());
    }

    @Test
    void extendBooking_ShouldThrow_WhenInvalidEndTime() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CHECKED_IN);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.extendBooking(10L, null)
        );
    }

    @Test
    void extendBooking_ShouldThrow_WhenCompleted() {

        Booking booking = buildBooking();

        booking.setStatus(BookingStatus.COMPLETED);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.extendBooking(10L, LocalDateTime.now().plusHours(1))
        );
    }

    @Test
    void extendBooking_ShouldThrow_WhenNewEndBeforeBase() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setEndTime(LocalDateTime.now().plusHours(2));

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.extendBooking(10L, booking.getEndTime().minusMinutes(1))
        );
    }

    @Test
    void extendBooking_ShouldUseStartTime_WhenEndMissing() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setEndTime(null);
        booking.setStartTime(LocalDateTime.of(2024, 1, 1, 9, 0));

        LocalDateTime newEnd = LocalDateTime.of(2024, 1, 1, 11, 0);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        bookingService.extendBooking(10L, newEnd);

        assertEquals(newEnd, booking.getEndTime());
    }

    @Test
    void extendBooking_ShouldExtendSuccessfully() {

        Booking booking = buildBooking();

                booking.setStatus(BookingStatus.CHECKED_IN);

        LocalDateTime newEnd =
                booking.getEndTime().plusHours(2);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        BookingResponseDto response =
                bookingService.extendBooking(10L, newEnd);

        assertNotNull(response);

        assertEquals(newEnd, booking.getEndTime());
    }

    @Test
    void estimateBooking_ShouldThrow_WhenEndBeforeStart() {

        BookingEstimateRequestDto request =
                new BookingEstimateRequestDto();

        request.setLotId(2L);
        request.setSpotId(1L);

        request.setStartTime(
                LocalDateTime.now().plusHours(2)
        );

        request.setEndTime(
                LocalDateTime.now().plusHours(1)
        );

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.estimateBooking(request)
        );
    }

    @Test
    void estimateBooking_ShouldThrow_WhenSpotLotMismatch() {

        BookingEstimateRequestDto request =
                new BookingEstimateRequestDto();

        request.setLotId(2L);
        request.setSpotId(1L);

        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));

        ParkingSpotLookupResponseDto spot = buildSpot("AVAILABLE", 50.0);
        spot.setLotId(999L);

        when(parkingSpotServiceClient.getSpotById(1L))
                .thenReturn(spot);

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.estimateBooking(request)
        );
    }

    @Test
    void estimateBooking_ShouldReturnEstimate_WhenValid() {

        BookingEstimateRequestDto request =
                new BookingEstimateRequestDto();

        request.setLotId(2L);
        request.setSpotId(1L);

        request.setStartTime(
                LocalDateTime.now().plusHours(1)
        );

        request.setEndTime(
                LocalDateTime.now().plusHours(3)
        );

        when(parkingSpotServiceClient.getSpotById(1L))
                .thenReturn(buildSpot("AVAILABLE", 50.0));

        assertNotNull(
                bookingService.estimateBooking(request)
        );
    }

    @Test
    void estimateBooking_ShouldUseDefaultRate_WhenSpotRateMissing() {

        BookingEstimateRequestDto request =
                new BookingEstimateRequestDto();

        request.setLotId(2L);
        request.setSpotId(1L);
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusHours(2));

        when(parkingSpotServiceClient.getSpotById(1L))
                .thenReturn(buildSpot("AVAILABLE", null));

        BookingEstimateResponseDto response =
                bookingService.estimateBooking(request);

        assertEquals(new BigDecimal("50.00"), response.getHourlyRate());
    }

    @Test
    void getBookingEstimate_ShouldThrow_WhenEndTimeMissing() {

        Booking booking = buildBooking();

        booking.setEndTime(null);

        when(bookingRepository.findByBookingId(10L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                IllegalStateException.class,
                () -> bookingService.getBookingEstimate(10L)
        );
    }

    @Test
    void calculateAmount_ShouldUseHourlyRate() {

        LocalDateTime start = LocalDateTime.now();

        LocalDateTime end = start.plusHours(3);

        BigDecimal amount =
                bookingService.calculateAmount(
                        start,
                        end,
                        PricingType.HOURLY,
                        new BigDecimal("50"),
                        new BigDecimal("500")
                );

        assertEquals(
                new BigDecimal("150.00"),
                amount
        );
    }

    @Test
    void calculateAmount_ShouldUseDailyRate() {

        LocalDateTime start = LocalDateTime.now();

        LocalDateTime end = start.plusHours(26);

        BigDecimal amount =
                bookingService.calculateAmount(
                        start,
                        end,
                        PricingType.DAILY,
                        new BigDecimal("10"),
                        new BigDecimal("100")
                );

        assertEquals(
                new BigDecimal("200.00"),
                amount
        );
    }

    @Test
    void getBookingHistory_ShouldMapList() {

        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(3L))
                .thenReturn(List.of(buildBooking()));

        when(bookingResponseMapper.mapTo(any(Booking.class)))
                .thenReturn(new BookingResponseDto());

        List<BookingResponseDto> result =
                bookingService.getBookingHistory(3L);

        assertEquals(1, result.size());
    }

    private BookingRequestDto buildRequest() {

        BookingRequestDto request =
                new BookingRequestDto();

        request.setUserId(3L);
        request.setLotId(2L);
        request.setSpotId(1L);

        request.setVehiclePlate("AB12CD1234");
        request.setVehicleType("CAR");

        request.setBookingType(BookingType.WALK_IN);

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

    private Booking buildBooking() {

        Booking booking = new Booking();

        booking.setBookingId(10L);

        booking.setUserId(3L);
        booking.setLotId(2L);
        booking.setSpotId(1L);

        booking.setVehiclePlate("AB12CD1234");
        booking.setVehicleType("CAR");

        booking.setBookingType(BookingType.WALK_IN);

        booking.setPricingType(PricingType.HOURLY);

        booking.setStartTime(
                LocalDateTime.now().minusHours(1)
        );

        booking.setEndTime(
                LocalDateTime.now().plusHours(1)
        );

        booking.setStatus(BookingStatus.CONFIRMED);

        return booking;
    }

    private ParkingSpotLookupResponseDto buildSpot(
            String status,
            Double pricePerHour
    ) {

        ParkingSpotLookupResponseDto spot =
                new ParkingSpotLookupResponseDto();

        spot.setSpotId(1L);
        spot.setLotId(2L);

        spot.setStatus(status);

        spot.setPricePerHour(pricePerHour);

        return spot;
    }

        private FeignException mockFeignException(int status) {
                FeignException exception = mock(FeignException.class);
                when(exception.status()).thenReturn(status);
                return exception;
        }
}