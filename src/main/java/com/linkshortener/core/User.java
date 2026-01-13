package com.linkshortener.core;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Класс пользователя системы.
 * Каждый пользователь идентифицируется по UUID.
 */
public class User {
    private final UUID id;
    private final String sessionId;
    private final LocalDateTime createdAt;
    private final List<String> ownedLinks;
    private final Set<String> notifications;

    public User() {
        this.id = UUID.randomUUID();
        this.sessionId = generateSessionId();
        this.createdAt = LocalDateTime.now();
        this.ownedLinks = new ArrayList<>();
        this.notifications = new LinkedHashSet<>();
    }

    private String generateSessionId() {
        return "SESS-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Геттеры
    public UUID getId() { return id; }
    public String getSessionId() { return sessionId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<String> getOwnedLinks() { return Collections.unmodifiableList(ownedLinks); }
    public Set<String> getNotifications() { return Collections.unmodifiableSet(notifications); }

    /**
     * Добавляет ссылку пользователю.
     */
    public void addLink(String shortCode) {
        ownedLinks.add(shortCode);
    }

    /**
     * Добавляет уведомление.
     */
    public void addNotification(String message) {
        notifications.add(LocalDateTime.now() + " - " + message);
        // Ограничиваем количество уведомлений
        if (notifications.size() > 50) {
            Iterator<String> it = notifications.iterator();
            it.next();
            it.remove();
        }
    }

    /**
     * Проверяет, принадлежит ли ссылка пользователю.
     */
    public boolean ownsLink(String shortCode) {
        return ownedLinks.contains(shortCode);
    }

    /**
     * Удаляет ссылку у пользователя.
     */
    public boolean removeLink(String shortCode) {
        return ownedLinks.remove(shortCode);
    }

    @Override
    public String toString() {
        return String.format("User{id=%s, links=%d, notifications=%d}",
                id, ownedLinks.size(), notifications.size());
    }
}