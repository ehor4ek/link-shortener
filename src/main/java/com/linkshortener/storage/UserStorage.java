package com.linkshortener.storage;

import com.linkshortener.core.User;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище пользователей.
 */
public class UserStorage {
    private static UserStorage instance;
    private final Map<UUID, User> usersById;
    private final Map<String, UUID> userIdBySession;

    private UserStorage() {
        usersById = new ConcurrentHashMap<>();
        userIdBySession = new ConcurrentHashMap<>();
    }

    public static synchronized UserStorage getInstance() {
        if (instance == null) {
            instance = new UserStorage();
        }
        return instance;
    }

    /**
     * Сохраняет пользователя.
     */
    public void save(User user) {
        usersById.put(user.getId(), user);
        userIdBySession.put(user.getSessionId(), user.getId());
    }

    /**
     * Находит пользователя по ID.
     */
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(usersById.get(id));
    }

    /**
     * Находит пользователя по сессии.
     */
    public Optional<User> findBySession(String sessionId) {
        UUID userId = userIdBySession.get(sessionId);
        if (userId != null) {
            return Optional.ofNullable(usersById.get(userId));
        }
        return Optional.empty();
    }

    /**
     * Создает нового пользователя.
     */
    public User createNewUser() {
        User user = new User();
        save(user);
        return user;
    }

    /**
     * Удаляет пользователя и все его ссылки.
     */
    public boolean deleteUser(UUID userId) {
        User user = usersById.remove(userId);
        if (user != null) {
            userIdBySession.remove(user.getSessionId());
            return true;
        }
        return false;
    }

    /**
     * Получает всех пользователей.
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(usersById.values());
    }

    /**
     * Очищает хранилище (для тестов).
     */
    public void clear() {
        usersById.clear();
        userIdBySession.clear();
    }

    public int size() {
        return usersById.size();
    }
}