package com.parkease.notification_service.mapper.Impl;

import com.parkease.notification_service.dtos.NotificationResponseDto;
import com.parkease.notification_service.entity.Notification;
import com.parkease.notification_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationResponseMapper implements Mapper<NotificationResponseDto, Notification> {

    private final ModelMapper mapper;

    @Override
    public NotificationResponseDto mapTo(Notification notification) {
        return mapper.map(notification, NotificationResponseDto.class);
    }

    @Override
    public Notification mapFrom(NotificationResponseDto notificationResponseDto) {
        return mapper.map(notificationResponseDto, Notification.class);
    }
}
