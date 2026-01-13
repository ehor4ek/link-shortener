package com.linkshortener;

import com.linkshortener.core.ShortLink;
import com.linkshortener.core.exception.*;
import com.linkshortener.core.service.LinkService;
import com.linkshortener.core.service.UserService;
import com.linkshortener.storage.LinkStorage;
import com.linkshortener.storage.UserStorage;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IntegrationTest {
    private LinkService linkService;
    private UserService userService;
    private LinkStorage linkStorage;
    private UserStorage userStorage;
    private UUID user1;
    private UUID user2;

    @BeforeEach
    void setUp() {
        linkStorage = LinkStorage.getInstance();
        userStorage = UserStorage.getInstance();
        linkService = LinkService.getInstance();
        userService = UserService.getInstance();

        linkStorage.clear();
        userStorage.clear();

        user1 = UUID.randomUUID();
        user2 = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        linkStorage.clear();
        userStorage.clear();
    }

    @Test
    void testMultipleUsersMultipleLinks() throws Exception {
        // Пользователь 1 создает две ссылки
        ShortLink link1 = linkService.createShortLink(
            "https://example1.com", user1, 5);
        ShortLink link2 = linkService.createShortLink(
            "https://example2.com", user1, 10);

        // Пользователь 2 создает ссылку на тот же URL, что и пользователь 1
        ShortLink link3 = linkService.createShortLink(
            "https://example1.com", user2, 3);

        // Все ссылки должны быть разными
        assertNotEquals(link1.getShortCode(), link3.getShortCode());

        // Проверяем количество ссылок у пользователей
        List<ShortLink> user1Links = linkService.getUserLinks(user1);
        List<ShortLink> user2Links = linkService.getUserLinks(user2);

        assertEquals(2, user1Links.size());
        assertEquals(1, user2Links.size());
    }



    @Test
    void testUserIsolation() throws Exception {
        // Пользователь 1 создает ссылку
        ShortLink user1Link = linkService.createShortLink(
            "https://private.com", user1, 5);

        // Пользователь 2 не должен видеть эту ссылку в своем списке
        List<ShortLink> user2Links = linkService.getUserLinks(user2);
        assertTrue(user2Links.isEmpty());

        // Пользователь 2 не может управлять ссылкой пользователя 1
        assertThrows(SecurityException.class,
            () -> linkService.getLinkInfo(user1Link.getShortCode(), user2));

        // Пользователь 2 не может удалить ссылку пользователя 1
        boolean deleted = linkService.deleteLink(user1Link.getShortCode(), user2);
        assertFalse(deleted);

        // Но пользователь 1 может
        boolean deletedByOwner = linkService.deleteLink(user1Link.getShortCode(), user1);
        assertTrue(deletedByOwner);
    }

    @Test
    void testClickCountingAndLimits() throws Exception {
        String url = "https://example.com";
        int limit = 3;

        ShortLink link = linkService.createShortLink(url, user1, limit);

        // Совершаем переходы
        assertEquals(0, link.getClicksCount());

        // Первый переход
        linkService.getOriginalUrl(link.getShortCode());
        ShortLink updatedLink1 = linkService.getLinkInfo(link.getShortCode(), user1);
        assertEquals(1, updatedLink1.getClicksCount());
        assertTrue(updatedLink1.isActive());

        // Второй переход
        linkService.getOriginalUrl(link.getShortCode());
        ShortLink updatedLink2 = linkService.getLinkInfo(link.getShortCode(), user1);
        assertEquals(2, updatedLink2.getClicksCount());
        assertTrue(updatedLink2.isActive());

        // Третий переход (достигаем лимита, ссылка еще активна)
        linkService.getOriginalUrl(link.getShortCode());
        ShortLink updatedLink3 = linkService.getLinkInfo(link.getShortCode(), user1);
        assertEquals(3, updatedLink3.getClicksCount());
        assertTrue(updatedLink3.isActive());

        // Четвертый переход должен превысить лимит
        assertThrows(LimitExceededException.class,
            () -> linkService.getOriginalUrl(link.getShortCode()));

        // Теперь ссылка должна быть неактивна, счетчик все еще = 3
        ShortLink finalLink = linkService.getLinkInfo(link.getShortCode(), user1);
        assertEquals(3, finalLink.getClicksCount());
        assertFalse(finalLink.isActive());
    }

    @Test
    void testNotificationSystem() throws Exception {
        String url = "https://example.com";
        int limit = 1;

        ShortLink link = linkService.createShortLink(url, user1, limit);

        // Первый переход - успешно (достигаем лимита)
        linkService.getOriginalUrl(link.getShortCode());

        // Проверяем, что ссылка еще активна при достижении лимита
        ShortLink afterFirstClick = linkService.getLinkInfo(link.getShortCode(), user1);
        assertEquals(1, afterFirstClick.getClicksCount());
        assertTrue(afterFirstClick.isActive());

        // Второй переход - превышение лимита
        // Должно сгенерироваться уведомление
        assertThrows(LimitExceededException.class,
            () -> linkService.getOriginalUrl(link.getShortCode()));

        // Теперь ссылка должна быть неактивна, счетчик = 1
        ShortLink finalLink = linkService.getLinkInfo(link.getShortCode(), user1);
        assertEquals(1, finalLink.getClicksCount());
        assertFalse(finalLink.isActive());
    }

    @Test
    void testConcurrentAccess() throws Exception {
        // Тест конкурентного доступа (упрощенный)
        String url = "https://example.com";
        int limit = 100;

        ShortLink link = linkService.createShortLink(url, user1, limit);

        // Сохраняем код ссылки в отдельную переменную, которая будет effectively final
        final String shortCode = link.getShortCode();

        // Имитируем несколько "потоков", делающих переходы
        int threadCount = 5;
        int clicksPerThread = 10;

        for (int t = 0; t < threadCount; t++) {
            for (int i = 0; i < clicksPerThread; i++) {
                try {
                    linkService.getOriginalUrl(shortCode);
                } catch (LimitExceededException e) {
                    // Ожидаемо, если лимит превышен
                    break;
                }
            }
        }

        // Проверяем конечное состояние
        ShortLink finalLink = linkService.getLinkInfo(shortCode, user1); // Используем новую переменную
        assertTrue(finalLink.getClicksCount() >= 50); // Минимум 50 кликов
        assertTrue(finalLink.getClicksCount() <= 100); // Но не больше лимита
    }
}
