package com.linkshortener.storage;

import com.linkshortener.core.ShortLink;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище коротких ссылок.
 * Использует ConcurrentHashMap для потокобезопасности.
 */
public class LinkStorage {
    private static LinkStorage instance;
    private final Map<String, ShortLink> linksByCode;
    private final Map<String, String> codeByOriginalUrl; // Для уникальности на пользователя
    private final Map<String, List<String>> userLinks; // Ссылки пользователя

    private LinkStorage() {
        linksByCode = new ConcurrentHashMap<>();
        codeByOriginalUrl = new ConcurrentHashMap<>();
        userLinks = new ConcurrentHashMap<>();
    }

    public static synchronized LinkStorage getInstance() {
        if (instance == null) {
            instance = new LinkStorage();
        }
        return instance;
    }

    /**
     * Сохраняет ссылку в хранилище.
     */
    public void save(ShortLink link) {
        String key = link.getOwnerId() + ":" + link.getOriginalUrl();
        linksByCode.put(link.getShortCode(), link);
        codeByOriginalUrl.put(key, link.getShortCode());

        // Добавляем в список ссылок пользователя
        String userId = link.getOwnerId().toString();
        userLinks.computeIfAbsent(userId, k -> new ArrayList<>()).add(link.getShortCode());
    }

    /**
     * Находит ссылку по короткому коду.
     */
    public Optional<ShortLink> findByCode(String code) {
        return Optional.ofNullable(linksByCode.get(code));
    }

    /**
     * Проверяет, есть ли уже короткая ссылка для данной оригинальной у пользователя.
     */
    public Optional<String> findCodeByUrlAndUser(String originalUrl, UUID userId) {
        String key = userId + ":" + originalUrl;
        return Optional.ofNullable(codeByOriginalUrl.get(key));
    }

    /**
     * Получает все ссылки пользователя.
     */
    public List<ShortLink> getUserLinks(UUID userId) {
        List<String> codes = userLinks.getOrDefault(userId.toString(), Collections.emptyList());
        return codes.stream()
                .map(linksByCode::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Удаляет ссылку.
     */
    public boolean remove(String code, UUID userId) {
        ShortLink link = linksByCode.get(code);
        if (link == null || !link.getOwnerId().equals(userId)) {
            return false;
        }

        linksByCode.remove(code);
        String key = userId + ":" + link.getOriginalUrl();
        codeByOriginalUrl.remove(key);

        // Удаляем из списка пользователя
        String userIdStr = userId.toString();
        List<String> userLinksList = userLinks.get(userIdStr);
        if (userLinksList != null) {
            userLinksList.remove(code);
            if (userLinksList.isEmpty()) {
                userLinks.remove(userIdStr);
            }
        }

        return true;
    }

    /**
     * Удаляет все просроченные ссылки.
     */
    public List<ShortLink> removeExpiredLinks() {
        List<ShortLink> expired = new ArrayList<>();
        Iterator<Map.Entry<String, ShortLink>> iterator = linksByCode.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, ShortLink> entry = iterator.next();
            ShortLink link = entry.getValue();

            if (link.isExpired()) {
                expired.add(link);
                iterator.remove();

                // Удаляем из вспомогательных структур
                String key = link.getOwnerId() + ":" + link.getOriginalUrl();
                codeByOriginalUrl.remove(key);

                String userId = link.getOwnerId().toString();
                List<String> userLinksList = userLinks.get(userId);
                if (userLinksList != null) {
                    userLinksList.remove(link.getShortCode());
                    if (userLinksList.isEmpty()) {
                        userLinks.remove(userId);
                    }
                }
            }
        }

        return expired;
    }

    /**
     * Очищает хранилище (для тестов).
     */
    public void clear() {
        linksByCode.clear();
        codeByOriginalUrl.clear();
        userLinks.clear();
    }

    public int size() {
        return linksByCode.size();
    }
}