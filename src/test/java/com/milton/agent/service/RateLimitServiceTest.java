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
        String ip = "192.0.2.1";
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.isAllowed(ip), "Request " + (i + 1) + " should be allowed");
        }
        assertFalse(rateLimitService.isAllowed(ip), "11th request must be blocked");
    }

    @Test
    void remainingRequestsReflectUsage() {
        String ip = "198.51.100.2";
        assertEquals(10, rateLimitService.getRemainingRequests(ip));

        rateLimitService.isAllowed(ip);
        rateLimitService.isAllowed(ip);
        rateLimitService.isAllowed(ip);

        assertEquals(7, rateLimitService.getRemainingRequests(ip));
        assertEquals(3, rateLimitService.getRequestCount(ip));
    }

    @Test
    void counterResetsWhenNewDayStarts() throws Exception {
        String ip = "203.0.113.5";

        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.isAllowed(ip));
        }
        assertFalse(rateLimitService.isAllowed(ip), "Limit reached for current day");

        // Force the stored entry to look like it belongs to yesterday.
        Map<String, ?> map = extractIpRequestMap();
        Object dailyInfo = map.get(ip);
        setField(dailyInfo, "date", LocalDate.now().minusDays(1));
        setField(dailyInfo, "requestCount", 5);

        assertTrue(rateLimitService.isAllowed(ip), "Counters should reset when a new day starts");
        assertEquals(9, rateLimitService.getRemainingRequests(ip));
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> extractIpRequestMap() throws Exception {
        Field field = RateLimitService.class.getDeclaredField("ipRequestMap");
        field.setAccessible(true);
        return (Map<String, ?>) field.get(rateLimitService);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
