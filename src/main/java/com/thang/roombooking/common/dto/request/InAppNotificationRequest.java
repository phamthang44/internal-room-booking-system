package com.thang.roombooking.common.dto.request;

import com.thang.roombooking.common.dto.model.NotificationPayload;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InAppNotificationRequest implements Serializable {
    private Long userId;
    private NotificationPayload payload;
}
