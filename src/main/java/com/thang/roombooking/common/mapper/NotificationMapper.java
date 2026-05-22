package com.thang.roombooking.common.mapper;

import com.thang.roombooking.common.dto.response.NotificationResponse;
import com.thang.roombooking.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    @Mapping(target = "readStatus", expression = "java(notification.isRead() ? \"READ\" : \"UNREAD\")")
    @Mapping(target = "isRead", expression = "java(notification.isRead())")
    NotificationResponse toNotificationResponse(Notification notification);
}
