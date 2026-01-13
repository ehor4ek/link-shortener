package com.linkshortener.core.service;

import com.linkshortener.core.ShortLink;
import com.linkshortener.core.User;
import com.linkshortener.core.exception.*;
import com.linkshortener.core.generator.ShortCodeGenerator;
import com.linkshortener.config.AppConfig;
import com.linkshortener.storage.LinkStorage;
import com.linkshortener.util.UrlValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для работы с короткими ссылками.
 */
public class LinkService {
    private static LinkService instance;
    private final LinkStorage linkStorage;
    private final AppConfig config;
    private final NotificationService notificationService;

    private LinkService() {
        this.linkStorage = LinkStorage.getInstance();
        this.config = AppConfig.getInstance();
        this.notificationService = NotificationService.getInstance();
    }

    public static synchronized LinkService getInstance() {
        if (instance == null) {
            instance = new LinkService();
        }
        return instance;
    }

    /**
     * Создает короткую ссылку.
     */
    /**
     * Создает короткую ссылку.
     */
    public ShortLink createShortLink(String originalUrl, UUID userId, Integer customClickLimit)
        throws InvalidUrlException {

        // Валидация URL
        if (!UrlValidator.isValid(originalUrl)) {
            throw new InvalidUrlException("Некорректный URL: " + originalUrl);
        }

        // Проверяем, есть ли уже ссылка у этого пользователя на этот URL
        Optional<String> existingCode = linkStorage.findCodeByUrlAndUser(originalUrl, userId);
        if (existingCode.isPresent()) {
            // Возвращаем существующую ссылку, если она есть
            Optional<ShortLink> existingLink = linkStorage.findByCode(existingCode.get());
            if (existingLink.isPresent()) {
                return existingLink.get();
            }
            // Если ссылка не найдена (возможно, была удалена), продолжаем создание новой
        }

        // Генерируем уникальный код
        String shortCode = ShortCodeGenerator.generateCode(config.getCodeLength());

        // Определяем лимит кликов
        int clickLimit = (customClickLimit != null && customClickLimit > 0)
            ? customClickLimit
            : config.getDefaultClickLimit();

        // Создаем ссылку
        ShortLink link = new ShortLink(
            originalUrl,
            shortCode,
            userId,
            clickLimit,
            config.getDefaultTtlHours()
        );

        // Сохраняем
        linkStorage.save(link);

        return link;
    }

    /**
     * Получает оригинальный URL по короткому коду.
     */

    public String getOriginalUrl(String shortCode)
        throws LinkNotFoundException, LinkExpiredException, LimitExceededException {

        ShortLink link = linkStorage.findByCode(shortCode)
            .orElseThrow(() -> new LinkNotFoundException("Ссылка не найдена: " + shortCode));

        // Проверяем активность
        if (!link.isActive()) {
            // Если ссылка неактивна, проверяем причину
            if (link.isExpired()) {
                notificationService.notifyLinkExpired(link);
                throw new LinkExpiredException("Срок действия ссылки истек");
            } else {
                notificationService.notifyLimitExceeded(link);
                throw new LimitExceededException("Лимит переходов исчерпан");
            }
        }

        // Проверяем срок действия
        if (link.isExpired()) {
            link.deactivate();
            notificationService.notifyLinkExpired(link);
            throw new LinkExpiredException("Срок действия ссылки истек");
        }

        // Увеличиваем счетчик кликов
        boolean withinLimit = link.incrementClicks();

        if (!withinLimit) {
            notificationService.notifyLimitExceeded(link);
            throw new LimitExceededException("Лимит переходов исчерпан");
        }

        return link.getOriginalUrl();
    }

    /**
     * Получает информацию о ссылке.
     */
    public ShortLink getLinkInfo(String shortCode, UUID userId) throws LinkNotFoundException {
        ShortLink link = linkStorage.findByCode(shortCode)
                .orElseThrow(() -> new LinkNotFoundException("Ссылка не найдена"));

        // Проверяем права доступа
        if (!link.getOwnerId().equals(userId)) {
            throw new SecurityException("Нет доступа к этой ссылке");
        }

        return link;
    }

    /**
     * Получает все ссылки пользователя.
     */
    public List<ShortLink> getUserLinks(UUID userId) {
        return linkStorage.getUserLinks(userId);
    }

    /**
     * Обновляет лимит кликов для ссылки.
     */
    public ShortLink updateClickLimit(String shortCode, UUID userId, int newLimit)
            throws LinkNotFoundException {

        ShortLink link = getLinkInfo(shortCode, userId);
        link.updateClickLimit(newLimit);

        // Уведомляем об изменении
        notificationService.notifyLinkUpdated(link,
                "Лимит кликов изменен на " + newLimit);

        return link;
    }

    /**
     * Удаляет ссылку.
     */
    public boolean deleteLink(String shortCode, UUID userId) {
        boolean removed = linkStorage.remove(shortCode, userId);
        if (removed) {
            ShortCodeGenerator.releaseCode(shortCode);
        }
        return removed;
    }

    /**
     * Проверяет все ссылки на истечение срока.
     */
    public List<ShortLink> cleanupExpiredLinks() {
        List<ShortLink> expired = linkStorage.removeExpiredLinks();

        // Отправляем уведомления
        expired.forEach(notificationService::notifyLinkExpired);

        return expired;
    }

    /**
     * Получает полную короткую ссылку.
     */
    public String getFullShortUrl(String shortCode) {
        return config.getBaseUrl() + shortCode;
    }
}
