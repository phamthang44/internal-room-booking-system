package com.thang.roombooking.service;

import com.thang.roombooking.common.dto.model.NotificationPayload;

public interface NotificationService {
    void notifyUser(String userId, String type, String message);

    void notifyUser(String userId, NotificationPayload payload);
}
