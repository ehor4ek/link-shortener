package com.linkshortener.config;

import java.util.Properties;

/**
 * Конфигурация приложения.
 * Загружает настройки из файла свойств.
 */
public class AppConfig {
    private static AppConfig instance;
    private final Properties properties;

    private String baseUrl;
    private int codeLength;
    private int defaultTtlHours;
    private int defaultClickLimit;
    private boolean notificationsEnabled;

    private AppConfig() {
        properties = ConfigLoader.loadProperties();
        loadConfig();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    private void loadConfig() {
        this.baseUrl = properties.getProperty("shortlink.base.url", "http://localhost:8080/");
        this.codeLength = Integer.parseInt(properties.getProperty("shortlink.code.length", "8"));
        this.defaultTtlHours = Integer.parseInt(properties.getProperty("shortlink.ttl.hours", "24"));
        this.defaultClickLimit = Integer.parseInt(properties.getProperty("default.click.limit", "10"));
        this.notificationsEnabled = Boolean.parseBoolean(
                properties.getProperty("notification.enabled", "true")
        );
    }

    // Геттеры
    public String getBaseUrl() { return baseUrl; }
    public int getCodeLength() { return codeLength; }
    public int getDefaultTtlHours() { return defaultTtlHours; }
    public int getDefaultClickLimit() { return defaultClickLimit; }
    public boolean isNotificationsEnabled() { return notificationsEnabled; }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}