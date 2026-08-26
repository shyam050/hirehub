package com.hirehub.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hirehub.common.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {
    private UUID id;
    private String title;
    private String message;
    private NotificationType type;
    private String link;
    private Boolean read;
    private OffsetDateTime createdAt;
}
