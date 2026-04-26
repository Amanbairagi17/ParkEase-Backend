package com.parkease.auth_service.mapper;

public interface Mapper <A,B>{
    A mapTo(B b);
    B mapFrom(A a);
}
