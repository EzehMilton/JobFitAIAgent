package com.milton.agent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(10);
    }

    @Test
    void allowsTenRequestsPerDayAndBlocksEleventh() {
        Long userId = 12345L;
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.isAllowed(userId), "Request " + (i + 1) + " should be allowed");
        }
        assertFalse(rateLimitService.isAllowed(userId), "11th request must be blocked");
    }

    @Test
    void remainingRequestsReflectUsage() {
        Long userId = 67890L;
        assertEquals(10, rateLimitService.getRemainingRequests(userId));

        rateLimitService.isAllowed(userId);
        rateLimitService.isAllowed(userId);
        rateLimitService.isAllowed(userId);

        assertEquals(7, rateLimitService.getRemainingRequests(userId));
        assertEquals(3, rateLimitService.getRequestCount(userId));
    }

    @Test
    void counterResetsWhenNewDayStarts() throws Exception {
        Long userId = 11111L;

        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.isAllowed(userId));
        }
        assertFalse(rateLimitService.isAllowed(userId), "Limit reached for current day");

        // Force the stored entry to look like it belongs to yesterday.
        Map<Long, ?> map = extractUserRequestMap();
        Object dailyInfo = map.get(userId);
        setField(dailyInfo, "date", LocalDate.now().minusDays(1));
        setField(dailyInfo, "requestCount", 5);

        assertTrue(rateLimitService.isAllowed(userId), "Counters should reset when a new day starts");
        assertEquals(9, rateLimitService.getRemainingRequests(userId));
    }

    @SuppressWarnings("unchecked")
    private Map<Long, ?> extractUserRequestMap() throws Exception {
        Field field = RateLimitService.class.getDeclaredField("userRequestMap");
        field.setAccessible(true);
        return (Map<Long, ?>) field.get(rateLimitService);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
