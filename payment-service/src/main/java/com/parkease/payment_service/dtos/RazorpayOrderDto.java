package com.parkease.payment_service.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RazorpayOrderDto {
    private String orderId;
    private String currency;
    private Integer amount;
    private String key;
}
