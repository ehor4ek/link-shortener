package com.linkshortener;

import com.linkshortener.config.AppConfig;
import com.linkshortener.core.ShortLink;
import com.linkshortener.core.User;
import com.linkshortener.core.exception.*;
import com.linkshortener.core.service.*;
import com.linkshortener.util.ConsoleHelper;
import com.linkshortener.util.UrlValidator;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Главный класс приложения - сервис сокращения ссылок.
 */
public class Main {
    private static User currentUser;
    private static UserService userService;
    private static LinkService linkService;
    private static NotificationService notificationService;
    private static SchedulerService schedulerService;

    public static void main(String[] args) {
        try {
            initializeServices();
            runApplication();
        } catch (Exception e) {
            System.err.println("Критическая ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdownServices();
        }
    }

    private static void initializeServices() {
        System.out.println("Инициализация сервиса сокращения ссылок...");

        userService = UserService.getInstance();
        linkService = LinkService.getInstance();
        notificationService = NotificationService.getInstance();
        schedulerService = SchedulerService.getInstance();

        // Создаем или загружаем пользователя
        String sessionId = "USER-" + UUID.randomUUID().toString().substring(0, 6);
        currentUser = userService.getOrCreateUser(sessionId);

        // Запускаем фоновые задачи
        schedulerService.start();

        System.out.println("Добро пожаловать, пользователь " + currentUser.getId());
        System.out.println("Ваш идентификатор сессии: " + sessionId);
    }

    private static void runApplication() {
        boolean running = true;

        while (running) {
            try {
                ConsoleHelper.clearScreen();
                printMenu();

                String choice = ConsoleHelper.readLine("\nВыберите действие: ");

                switch (choice) {
                    case "1" -> createShortLink();
                    case "2" -> openShortLink();
                    case "3" -> viewMyLinks();
                    case "4" -> updateLinkLimit();
                    case "5" -> deleteLink();
                    case "6" -> viewNotifications();
                    case "7" -> showUserInfo();
                    case "8" -> showHelp();
                    case "0" -> running = false;
                    default -> System.out.println("Неверный выбор. Попробуйте снова.");
                }

                if (running && !choice.equals("0")) {
                    ConsoleHelper.waitForEnter();
                }

            } catch (Exception e) {
                System.err.println("Ошибка: " + e.getMessage());
                ConsoleHelper.waitForEnter();
            }
        }
    }

    private static void printMenu() {
        ConsoleHelper.printTitle("СЕРВИС СОКРАЩЕНИЯ ССЫЛОК");
        System.out.println("Текущий пользователь: " + currentUser.getId());
        System.out.println();
        System.out.println("1. Создать короткую ссылку");
        System.out.println("2. Перейти по короткой ссылке");
        System.out.println("3. Мои ссылки");
        System.out.println("4. Изменить лимит переходов");
        System.out.println("5. Удалить ссылку");
        System.out.println("6. Просмотреть уведомления");
        System.out.println("7. Информация о пользователе");
        System.out.println("8. Помощь");
        System.out.println("0. Выход");
        ConsoleHelper.printSeparator();
    }

    private static void createShortLink() {
        ConsoleHelper.printTitle("СОЗДАНИЕ КОРОТКОЙ ССЫЛКИ");

        String url = ConsoleHelper.readLine("Введите URL для сокращения: ");

        // Нормализуем URL
        url = UrlValidator.normalize(url);

        if (!UrlValidator.isValid(url)) {
            System.out.println("Ошибка: некорректный URL");
            return;
        }

        // Запрашиваем лимит кликов
        Integer clickLimit = null;
        String limitChoice = ConsoleHelper.readLine(
            "Установить лимит кликов? (д/н, по умолчанию " +
                AppConfig.getInstance().getDefaultClickLimit() + "): "
        );

        if (limitChoice.equalsIgnoreCase("д") || limitChoice.equalsIgnoreCase("y")) {
            int limit = ConsoleHelper.readInt("Введите лимит кликов: ");
            if (limit > 0) {
                clickLimit = limit;
            } else {
                System.out.println("Лимит должен быть положительным числом. Используется значение по умолчанию.");
            }
        }

        try {
            // Создаем короткую ссылку
            ShortLink link = linkService.createShortLink(url, currentUser.getId(), clickLimit);
            String shortUrl = linkService.getFullShortUrl(link.getShortCode());

            System.out.println("\n✓ Ссылка успешно создана!");
            System.out.println("Оригинальный URL: " + link.getOriginalUrl());
            System.out.println("Короткая ссылка: " + shortUrl);
            System.out.println("Код ссылки: " + link.getShortCode());
            System.out.println("Лимит переходов: " + link.getClickLimit());
            System.out.println("Истекает: " + link.getExpiresAt());

            // Добавляем ссылку пользователю
            currentUser.addLink(link.getShortCode());

        } catch (InvalidUrlException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }
    }

    private static void openShortLink() {
        ConsoleHelper.printTitle("ПЕРЕХОД ПО КОРОТКОЙ ССЫЛКЕ");

        String shortCode = ConsoleHelper.readLine("Введите код короткой ссылки: ");

        if (shortCode == null || shortCode.trim().isEmpty()) {
            System.out.println("Ошибка: код не может быть пустым");
            return;
        }

        try {
            // Получаем оригинальный URL
            String originalUrl = linkService.getOriginalUrl(shortCode);

            System.out.println("\n✓ Переход по ссылке: " + originalUrl);

            // Пытаемся открыть в браузере
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                try {
                    Desktop.getDesktop().browse(new URI(originalUrl));
                    System.out.println("Браузер открыт!");
                } catch (Exception e) {
                    System.out.println("Не удалось открыть браузер: " + e.getMessage());
                    System.out.println("Скопируйте ссылку вручную: " + originalUrl);
                }
            } else {
                System.out.println("Автоматическое открытие браузера не поддерживается");
                System.out.println("Скопируйте ссылку: " + originalUrl);
            }

        } catch (LinkNotFoundException e) {
            System.out.println("Ошибка: ссылка не найдена");
        } catch (LinkExpiredException e) {
            System.out.println("Ошибка: срок действия ссылки истек");
        } catch (LimitExceededException e) {
            System.out.println("Ошибка: лимит переходов исчерпан");
        }
    }

    private static void viewMyLinks() {
        ConsoleHelper.printTitle("МОИ ССЫЛКИ");

        List<ShortLink> links = linkService.getUserLinks(currentUser.getId());

        if (links.isEmpty()) {
            System.out.println("У вас нет созданных ссылок");
            return;
        }

        System.out.printf("Всего ссылок: %d\n\n", links.size());

        for (int i = 0; i < links.size(); i++) {
            ShortLink link = links.get(i);
            String status = link.isActive() ? "АКТИВНА" : "НЕАКТИВНА";
            String expired = link.isExpired() ? " (ИСТЕКЛА)" : "";

            System.out.printf("%d. %s - %s%s\n", i + 1, link.getShortCode(), status, expired);
            System.out.println("   URL: " + link.getOriginalUrl());
            System.out.println("   Короткая: " + linkService.getFullShortUrl(link.getShortCode()));
            System.out.println("   Переходы: " + link.getClicksCount() + "/" + link.getClickLimit());
            System.out.println("   Создана: " + link.getCreatedAt());
            System.out.println("   Истекает: " + link.getExpiresAt());
            System.out.println();
        }
    }

    private static void updateLinkLimit() {
        ConsoleHelper.printTitle("ИЗМЕНЕНИЕ ЛИМИТА ПЕРЕХОДОВ");

        String shortCode = ConsoleHelper.readLine("Введите код ссылки: ");

        if (shortCode == null || shortCode.trim().isEmpty()) {
            System.out.println("Ошибка: код не может быть пустым");
            return;
        }

        try {
            // Получаем текущую информацию
            ShortLink link = linkService.getLinkInfo(shortCode, currentUser.getId());
            System.out.println("Текущий лимит: " + link.getClickLimit());
            System.out.println("Текущие переходы: " + link.getClicksCount());

            // Запрашиваем новый лимит
            int newLimit = ConsoleHelper.readInt("Введите новый лимит переходов: ");

            if (newLimit <= 0) {
                System.out.println("Ошибка: лимит должен быть положительным числом");
                return;
            }

            // Обновляем
            ShortLink updated = linkService.updateClickLimit(shortCode, currentUser.getId(), newLimit);

            System.out.println("\n✓ Лимит обновлен!");
            System.out.println("Новый лимит: " + updated.getClickLimit());

        } catch (LinkNotFoundException e) {
            System.out.println("Ошибка: " + e.getMessage());
        } catch (SecurityException e) {
            System.out.println("Ошибка: нет доступа к этой ссылке");
        }
    }

    private static void deleteLink() {
        ConsoleHelper.printTitle("УДАЛЕНИЕ ССЫЛКИ");

        String shortCode = ConsoleHelper.readLine("Введите код ссылки для удаления: ");

        if (shortCode == null || shortCode.trim().isEmpty()) {
            System.out.println("Ошибка: код не может быть пустым");
            return;
        }

        String confirm = ConsoleHelper.readLine(
                "Вы уверены, что хотите удалить ссылку " + shortCode + "? (д/н): "
        );

        if (!confirm.equalsIgnoreCase("д")) {
            System.out.println("Удаление отменено");
            return;
        }

        boolean deleted = linkService.deleteLink(shortCode, currentUser.getId());

        if (deleted) {
            System.out.println("\n✓ Ссылка успешно удалена");
            currentUser.removeLink(shortCode);
        } else {
            System.out.println("Ошибка: ссылка не найдена или нет прав для удаления");
        }
    }

    private static void viewNotifications() {
        notificationService.printUserNotifications(currentUser);
    }

    private static void showUserInfo() {
        ConsoleHelper.printTitle("ИНФОРМАЦИЯ О ПОЛЬЗОВАТЕЛЕ");

        System.out.println("ID пользователя: " + currentUser.getId());
        System.out.println("ID сессии: " + currentUser.getSessionId());
        System.out.println("Дата регистрации: " + currentUser.getCreatedAt());
        System.out.println("Количество ссылок: " + currentUser.getOwnedLinks().size());
        System.out.println("Непрочитанных уведомлений: " + currentUser.getNotifications().size());
    }

    private static void showHelp() {
        ConsoleHelper.printTitle("ПОМОЩЬ");

        System.out.println("""
            Сервис сокращения ссылок позволяет:

            1. Создавать короткие ссылки из длинных URL
               - Каждая ссылка уникальна для каждого пользователя
               - Даже один URL у разных пользователей даст разные короткие ссылки

            2. Устанавливать лимиты переходов
               - По умолчанию: 10 переходов
               - Можно изменить в любое время

            3. Автоматическое удаление ссылок
               - Ссылки автоматически удаляются через 24 часа
               - Фоновая очистка каждые 60 минут

            4. Уведомления
               - При истечении срока ссылки
               - При достижении лимита переходов
               - При изменениях в ссылках

            5. Управление ссылками
               - Просмотр всех ваших ссылок
               - Изменение параметров
               - Удаление ссылок
            """);
    }

    private static void shutdownServices() {
        System.out.println("\nЗавершение работы приложения...");

        try {
            schedulerService.shutdown();
            System.out.println("Все данные сохранены");
        } catch (Exception e) {
            System.err.println("Ошибка при завершении: " + e.getMessage());
        }

        System.out.println("До свидания!");
    }
}
