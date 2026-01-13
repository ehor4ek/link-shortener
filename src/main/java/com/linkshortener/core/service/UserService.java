package com.linkshortener.core.service;

import com.linkshortener.core.User;
import com.linkshortener.storage.UserStorage;

import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для работы с пользователями.
 */
public class UserService {
    private static UserService instance;
    private final UserStorage userStorage;

    private UserService() {
        this.userStorage = UserStorage.getInstance();
    }

    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    /**
     * Получает или создает пользователя по сессии.
     */
    public User getOrCreateUser(String sessionId) {
        // Пытаемся найти по сессии
        Optional<User> existingUser = userStorage.findBySession(sessionId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        // Создаем нового пользователя
        User newUser = userStorage.createNewUser();
        System.out.println("Создан новый пользователь: " + newUser.getId());
        return newUser;
    }

    /**
     * Получает пользователя по ID.
     */
    public Optional<User> getUserById(UUID userId) {
        return userStorage.findById(userId);
    }

    /**
     * Удаляет пользователя.
     */
    public boolean deleteUser(UUID userId) {
        return userStorage.deleteUser(userId);
    }

    /**
     * Получает количество пользователей.
     */
    public int getUserCount() {
        return userStorage.size();
    }
}