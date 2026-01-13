package com.linkshortener.core.generator;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

/**
 * Генератор уникальных коротких кодов для ссылок.
 * Использует алфавит из безопасных символов URL.
 */
public class ShortCodeGenerator {
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> GENERATED_CODES = new HashSet<>();

    /**
     * Генерирует уникальный короткий код заданной длины.
     * Гарантирует уникальность даже для разных пользователей.
     */
    public static String generateCode(int length) {
        String code;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            if (attempts++ > maxAttempts) {
                throw new IllegalStateException("Не удалось сгенерировать уникальный код");
            }
            code = generateRandomCode(length);
        } while (GENERATED_CODES.contains(code));

        GENERATED_CODES.add(code);
        return code;
    }

    /**
     * Генерирует случайный код из алфавита.
     */
    private static String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    /**
     * Освобождает код при удалении ссылки.
     */
    public static void releaseCode(String code) {
        GENERATED_CODES.remove(code);
    }

    /**
     * Проверяет, используется ли код.
     */
    public static boolean isCodeUsed(String code) {
        return GENERATED_CODES.contains(code);
    }
}