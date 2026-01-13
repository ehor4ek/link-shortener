package com.linkshortener.core;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Класс, представляющий короткую ссылку.
 * Хранит информацию о ссылке, её владельце и ограничениях.
 */
public class ShortLink {
    private final String id;
    private final String originalUrl;
    private final String shortCode;
    private final UUID ownerId;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private int clickLimit;
    private int clicksCount;
    private boolean active;

    public ShortLink(String originalUrl, String shortCode, UUID ownerId,
                     int clickLimit, int ttlHours) {
        this.id = UUID.randomUUID().toString();
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.ownerId = ownerId;
        this.clickLimit = clickLimit;
        this.clicksCount = 0;
        this.active = true;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusHours(ttlHours);
    }

    // Геттеры
    public String getId() { return id; }
    public String getOriginalUrl() { return originalUrl; }
    public String getShortCode() { return shortCode; }
    public UUID getOwnerId() { return ownerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public int getClickLimit() { return clickLimit; }
    public int getClicksCount() { return clicksCount; }
    public boolean isActive() { return active; }

    /**
     * Увеличивает счетчик кликов и проверяет лимит.
     * @return true если лимит не превышен, false если превышен
     */

    public boolean incrementClicks() {
        if (!active) return false;

        // Проверяем, не достигнут ли уже лимит
        if (clicksCount >= clickLimit) {
            active = false;
            return false;
        }

        clicksCount++;

        // Если после увеличения достигли лимита, ссылка еще активна
        // (но следующий вызов уже не пройдет)
        if (clicksCount == clickLimit) {
            // Достигли лимита, но переход разрешен
            return true;
        }
        return true;
    }

    /**
     * Проверяет, истекло ли время жизни ссылки.
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Деактивирует ссылку.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Обновляет лимит кликов.
     */
    public void updateClickLimit(int newLimit) {
        this.clickLimit = newLimit;
        if (clicksCount >= newLimit) {
            active = false;
        } else {
            if (!active && !isExpired()) {
                active = true;
            }
        }
    }

    @Override
    public String toString() {
        return String.format("ShortLink{code='%s', original='%s', clicks=%d/%d, expires=%s}",
                shortCode, originalUrl, clicksCount, clickLimit, expiresAt);
    }
}
