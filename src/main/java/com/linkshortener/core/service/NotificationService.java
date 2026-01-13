package com.linkshortener.core.service;

import com.linkshortener.core.ShortLink;
import com.linkshortener.core.User;
import com.linkshortener.config.AppConfig;
import com.linkshortener.storage.UserStorage;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Сервис уведомлений для пользователей.
 */
public class NotificationService {
    private static NotificationService instance;
    private final AppConfig config;
    private final UserStorage userStorage;
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private NotificationService() {
        this.config = AppConfig.getInstance();
        this.userStorage = UserStorage.getInstance();
    }

    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    /**
     * Уведомляет о истечении срока ссылки.
     */
    public void notifyLinkExpired(ShortLink link) {
        if (!config.isNotificationsEnabled()) return;

        String message = String.format(
                "Ссылка %s истекла %s. Создайте новую ссылку.",
                link.getShortCode(),
                link.getExpiresAt().format(FORMATTER)
        );

        sendNotification(link.getOwnerId(), message);
    }

    /**
     * Уведомляет о превышении лимита кликов.
     */
    public void notifyLimitExceeded(ShortLink link) {
        if (!config.isNotificationsEnabled()) return;

        String message = String.format(
                "Ссылка %s достигла лимита кликов (%d). Создайте новую ссылку.",
                link.getShortCode(),
                link.getClickLimit()
        );

        sendNotification(link.getOwnerId(), message);
    }

    /**
     * Уведомляет об изменении ссылки.
     */
    public void notifyLinkUpdated(ShortLink link, String details) {
        if (!config.isNotificationsEnabled()) return;

        String message = String.format(
                "Ссылка %s обновлена: %s",
                link.getShortCode(),
                details
        );

        sendNotification(link.getOwnerId(), message);
    }

    /**
     * Отправляет уведомление пользователю.
     */
    private void sendNotification(UUID userId, String message) {
        userStorage.findById(userId).ifPresent(user -> {
            user.addNotification(message);
            System.out.println("Уведомление для пользователя " + userId + ": " + message);
        });
    }

    /**
     * Получает уведомления пользователя.
     */
    public void printUserNotifications(User user) {
        System.out.println("\n=== Уведомления пользователя " + user.getId() + " ===");

        var notifications = user.getNotifications();
        if (notifications.isEmpty()) {
            System.out.println("Уведомлений нет.");
        } else {
            notifications.forEach(System.out::println);
        }

        System.out.println("=== Конец уведомлений ===\n");
    }
}