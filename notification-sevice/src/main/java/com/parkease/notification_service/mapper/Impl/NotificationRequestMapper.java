package com.parkease.notification_service.mapper.Impl;

import com.parkease.notification_service.dtos.NotificationRequestDto;
import com.parkease.notification_service.entity.Notification;
import com.parkease.notification_service.mapper.Mapper;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRequestMapper implements Mapper<NotificationRequestDto, Notification> {

    private final ModelMapper mapper;

    @Override
    public NotificationRequestDto mapTo(Notification notification) {
        return mapper.map(notification, NotificationRequestDto.class);
    }

    @Override
    public Notification mapFrom(NotificationRequestDto notificationRequestDto) {
        return mapper.map(notificationRequestDto, Notification.class);
    }
}
