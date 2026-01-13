package com.linkshortener.config;

import java.io.*;
import java.util.Properties;

/**
 * Загрузчик конфигурации из файла.
 */
public class ConfigLoader {

    /**
     * Загружает свойства из файла application.properties.
     * Если файл не найден, создает его с настройками по умолчанию.
     */
    public static Properties loadProperties() {
        Properties properties = new Properties();
        File configFile = new File("application.properties");

        try {
            if (configFile.exists()) {
                try (InputStream input = new FileInputStream(configFile)) {
                    properties.load(input);
                }
            } else {
                // Создаем файл с настройками по умолчанию
                createDefaultConfig(configFile);
                properties.load(new FileReader(configFile));
            }
        } catch (IOException e) {
            System.err.println("Ошибка загрузки конфигурации: " + e.getMessage());
            loadDefaultProperties(properties);
        }

        return properties;
    }

    private static void createDefaultConfig(File configFile) throws IOException {
        try (OutputStream output = new FileOutputStream(configFile)) {
            String defaultConfig = """
                # Конфигурация приложения
                app.name=LinkShortener
                app.version=1.0
                
                # Настройки коротких ссылок
                shortlink.base.url=http://localhost:8080/
                shortlink.code.length=8
                shortlink.ttl.hours=24
                
                # Лимиты по умолчанию
                default.click.limit=10
                
                # Настройки уведомлений
                notification.enabled=true
                """;
            output.write(defaultConfig.getBytes());
        }
    }

    private static void loadDefaultProperties(Properties properties) {
        properties.setProperty("shortlink.base.url", "http://localhost:8080/");
        properties.setProperty("shortlink.code.length", "8");
        properties.setProperty("shortlink.ttl.hours", "24");
        properties.setProperty("default.click.limit", "10");
        properties.setProperty("notification.enabled", "true");
    }
}