package com.hirehub.notification;

import com.hirehub.common.enums.Role;
import com.hirehub.common.exception.ForbiddenException;
import com.hirehub.common.exception.ResourceNotFoundException;
import com.hirehub.notification.dto.NotificationResponse;
import com.hirehub.notification.entity.Notification;
import com.hirehub.notification.repository.NotificationRepository;
import com.hirehub.user.entity.User;
import com.hirehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Authentication auth, int page, int size) {
        User user = findUserByEmail(auth.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Authentication auth) {
        User user = findUserByEmail(auth.getName());
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    @Transactional
    public NotificationResponse markAsRead(Authentication auth, UUID notificationId) {
        User user = findUserByEmail(auth.getName());
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId.toString()));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only mark your own notifications as read");
        }

        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Transactional
    public int markAllAsRead(Authentication auth) {
        User user = findUserByEmail(auth.getName());
        int count = notificationRepository.markAllAsRead(user.getId());
        log.info("Marked {} notifications as read for user {}", count, user.getId());
        return count;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .link(notification.getLink())
                .read(notification.getRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
