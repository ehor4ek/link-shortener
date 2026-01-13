package com.linkshortener.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Валидатор URL.
 */
public class UrlValidator {
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$",
            Pattern.CASE_INSENSITIVE
    );

    private UrlValidator() {}

    /**
     * Проверяет, является ли строка валидным URL.
     */
    public static boolean isValid(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // Проверка по регулярному выражению
        if (!URL_PATTERN.matcher(url).matches()) {
            return false;
        }

        // Дополнительная проверка через класс URL
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Нормализует URL (добавляет протокол, если отсутствует).
     */
    public static String normalize(String url) {
        if (url == null) return null;

        url = url.trim();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        return "https://" + url;
    }
}