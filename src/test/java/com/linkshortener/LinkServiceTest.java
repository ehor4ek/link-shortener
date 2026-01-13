package com.linkshortener;

import com.linkshortener.core.ShortLink;
import com.linkshortener.core.exception.*;
import com.linkshortener.core.service.LinkService;
import com.linkshortener.storage.LinkStorage;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkServiceTest {
    private LinkService linkService;
    private UUID testUserId;
    private LinkStorage storage;

    @BeforeEach
    void setUp() {
        storage = LinkStorage.getInstance();
        storage.clear();
        linkService = LinkService.getInstance();
        testUserId = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() {
        storage.clear();
    }

    @Test
    void testCreateShortLink() throws Exception {
        String url = "https://example.com";
        ShortLink link = linkService.createShortLink(url, testUserId, null);

        assertNotNull(link);
        assertEquals(url, link.getOriginalUrl());
        assertEquals(testUserId, link.getOwnerId());
        assertTrue(link.isActive());
    }

    @Test
    void testUniqueLinksForDifferentUsers() throws Exception {
        String url = "https://example.com";
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        ShortLink link1 = linkService.createShortLink(url, user1, null);
        ShortLink link2 = linkService.createShortLink(url, user2, null);

        assertNotEquals(link1.getShortCode(), link2.getShortCode());
    }

    @Test
    void testSameLinkForSameUser() throws Exception {
        String url = "https://example.com";

        ShortLink link1 = linkService.createShortLink(url, testUserId, null);
        // Вторая попытка должна вернуть существующую ссылку
        ShortLink link2 = linkService.createShortLink(url, testUserId, null);

        assertEquals(link1.getShortCode(), link2.getShortCode());
    }

    @Test
    void testInvalidUrlThrowsException() {
        String invalidUrl = "not-a-valid-url";
        assertThrows(InvalidUrlException.class,
            () -> linkService.createShortLink(invalidUrl, testUserId, null));
    }

    @Test
    void testClickLimit() throws Exception {
        String url = "https://example.com";
        int limit = 3;

        ShortLink link = linkService.createShortLink(url, testUserId, limit);
        final String shortCode = link.getShortCode();

        // Совершаем переходы до лимита (3 перехода должны быть успешны)
        for (int i = 0; i < limit; i++) {
            String result = linkService.getOriginalUrl(shortCode);
            assertEquals(url, result);
            System.out.println("Переход " + (i + 1) + " успешен");
        }

        // После трех переходов ссылка все еще активна, счетчик = 3
        ShortLink afterThreeClicks = linkService.getLinkInfo(shortCode, testUserId);
        assertEquals(3, afterThreeClicks.getClicksCount());
        assertTrue(afterThreeClicks.isActive());

        // Четвертый переход должен превысить лимит
        assertThrows(LimitExceededException.class,
            () -> linkService.getOriginalUrl(shortCode));

        // Теперь ссылка должна быть неактивна, счетчик все еще = 3
        ShortLink finalLink = linkService.getLinkInfo(shortCode, testUserId);
        assertEquals(3, finalLink.getClicksCount());
        assertFalse(finalLink.isActive());
    }

    @Test
    void testLinkNotFound() {
        assertThrows(LinkNotFoundException.class,
            () -> linkService.getOriginalUrl("NONEXISTENT"));
    }

    @Test
    void testUpdateClickLimit() throws Exception {
        String url = "https://example.com";
        ShortLink link = linkService.createShortLink(url, testUserId, 5);

        // Увеличиваем лимит
        ShortLink updated = linkService.updateClickLimit(
            link.getShortCode(), testUserId, 10);

        assertEquals(10, updated.getClickLimit());
    }

    @Test
    void testDeleteLink() throws Exception {
        String url = "https://example.com";
        ShortLink link = linkService.createShortLink(url, testUserId, null);
        final String shortCode = link.getShortCode();

        boolean deleted = linkService.deleteLink(shortCode, testUserId);
        assertTrue(deleted);

        // После удаления ссылка не должна находиться
        assertThrows(LinkNotFoundException.class,
            () -> linkService.getOriginalUrl(shortCode));
    }

    @Test
    void testAccessControl() throws Exception {
        String url = "https://example.com";
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        ShortLink link = linkService.createShortLink(url, ownerId, null);
        final String shortCode = link.getShortCode();

        // Другой пользователь не может получить информацию
        assertThrows(SecurityException.class,
            () -> linkService.getLinkInfo(shortCode, otherUserId));

        // И не может удалить
        boolean deleted = linkService.deleteLink(shortCode, otherUserId);
        assertFalse(deleted);
    }
}
