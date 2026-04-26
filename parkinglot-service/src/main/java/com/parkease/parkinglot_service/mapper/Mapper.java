package com.parkease.parkinglot_service.mapper;

public interface Mapper<A, B> {
    A mapTo(B b);

    B mapFrom(A a);
}
