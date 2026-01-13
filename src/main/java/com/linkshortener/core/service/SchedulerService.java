package com.linkshortener.core.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Сервис планировщика для фоновых задач.
 */
public class SchedulerService {
    private static SchedulerService instance;
    private final ScheduledExecutorService scheduler;
    private final LinkService linkService;

    private SchedulerService() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.linkService = LinkService.getInstance();
    }

    public static synchronized SchedulerService getInstance() {
        if (instance == null) {
            instance = new SchedulerService();
        }
        return instance;
    }

    /**
     * Запускает фоновые задачи.
     */
    public void start() {
        // Очистка просроченных ссылок каждый час
        scheduler.scheduleAtFixedRate(this::cleanupExpiredLinks, 1, 1, TimeUnit.HOURS);

        // Логирование статистики каждые 30 минут
        scheduler.scheduleAtFixedRate(this::logStatistics, 0, 30, TimeUnit.MINUTES);

        System.out.println("Фоновые задачи запущены");
    }

    /**
     * Очищает просроченные ссылки.
     */
    private void cleanupExpiredLinks() {
        try {
            var expired = linkService.cleanupExpiredLinks();
            if (!expired.isEmpty()) {
                System.out.println("Удалено просроченных ссылок: " + expired.size());
            }
        } catch (Exception e) {
            System.err.println("Ошибка при очистке ссылок: " + e.getMessage());
        }
    }

    /**
     * Логирует статистику.
     */
    private void logStatistics() {
        try {
            // Здесь можно добавить логирование в файл
            System.out.println("Статистика системы обновлена");
        } catch (Exception e) {
            System.err.println("Ошибка при логировании статистики: " + e.getMessage());
        }
    }

    /**
     * Останавливает планировщик.
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Фоновые задачи остановлены");
    }
}