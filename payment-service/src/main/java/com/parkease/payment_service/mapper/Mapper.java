package com.parkease.payment_service.mapper;

public interface Mapper<A,B> {
    A mapTo (B b);
    B mapFrom (A a);
}
