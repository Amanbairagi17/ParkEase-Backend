package com.parkease.booking_service.event;

import com.parkease.booking_service.dtos.PaymentSuccessEventDto;
import com.parkease.booking_service.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private PaymentEventListener listener;

    @Test
    void handlePaymentSuccess_ShouldIgnoreNonSuccessStatus() {
        PaymentSuccessEventDto event = new PaymentSuccessEventDto();
        event.setBookingId(10L);
        event.setStatus("FAILED");

        listener.handlePaymentSuccess(event);

        verify(bookingService, never()).markAsPaid(anyLong());
    }

    @Test
    void handlePaymentSuccess_ShouldInvokeBooking_WhenSuccess() {
        PaymentSuccessEventDto event = new PaymentSuccessEventDto();
        event.setBookingId(10L);
        event.setStatus("SUCCESS");

        listener.handlePaymentSuccess(event);

        verify(bookingService).markAsPaid(10L);
    }

    @Test
    void handlePaymentSuccess_ShouldSwallowExceptions() {
        PaymentSuccessEventDto event = new PaymentSuccessEventDto();
        event.setBookingId(10L);
        event.setStatus("SUCCESS");

        doThrow(new RuntimeException("fail")).when(bookingService).markAsPaid(10L);

        listener.handlePaymentSuccess(event);

        verify(bookingService).markAsPaid(10L);
    }

    @Test
    void handlePaymentSuccess_ShouldIgnoreNullStatus() {

        PaymentSuccessEventDto event =
                new PaymentSuccessEventDto();

        event.setBookingId(10L);

        listener.handlePaymentSuccess(event);

        verify(bookingService, never())
                .markAsPaid(anyLong());
    }

    @Test
    void handlePaymentSuccess_ShouldHandleLowercaseSuccess() {

        PaymentSuccessEventDto event =
                new PaymentSuccessEventDto();

        event.setBookingId(10L);
        event.setStatus("success");

        listener.handlePaymentSuccess(event);

        verify(bookingService)
                .markAsPaid(10L);
    }
}
