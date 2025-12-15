package com.milton.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track and limit requests per user session.
 * Limits each user to a configurable number of requests per calendar day.
 * Resets at midnight daily.
 */
@Slf4j
@Service
public class RateLimitService {

    private final int maxRequestsPerDay;

    // Map: userId -> Daily request info
    private final Map<Long, DailyRequestInfo> userRequestMap = new ConcurrentHashMap<>();

    public RateLimitService(@Value("${jobfit.rate-limit.max-daily-scans:10}") int maxRequestsPerDay) {
        this.maxRequestsPerDay = maxRequestsPerDay;
    }

    /**
     * Check if a user has exceeded the daily rate limit.
     *
     * @param userId The user ID to check
     * @return true if request is allowed, false if limit exceeded
     */
    public boolean isAllowed(Long userId) {
        LocalDate today = LocalDate.now();
        DailyRequestInfo info = userRequestMap.computeIfAbsent(userId, k -> new DailyRequestInfo(today));

        synchronized (info) {
            // If it's a new day, reset the counter
            if (!info.date.equals(today)) {
                log.info("New day detected for user {}. Resetting counter from {} to 0", userId, info.requestCount);
                info.date = today;
                info.requestCount = 0;
            }

            if (info.requestCount >= maxRequestsPerDay) {
                log.warn("Daily rate limit exceeded for user: {} (Date: {})", userId, today);
                return false;
            }

            info.requestCount++;
            log.info("Request count for user {} on {}: {}/{}", userId, today, info.requestCount, maxRequestsPerDay);
            return true;
        }
    }

    /**
     * Get remaining requests for a user for today.
     *
     * @param userId The user ID
     * @return Number of remaining requests (0-maxRequestsPerDay)
     */
    public int getRemainingRequests(Long userId) {
        LocalDate today = LocalDate.now();
        DailyRequestInfo info = userRequestMap.get(userId);

        if (info == null || !info.date.equals(today)) {
            return maxRequestsPerDay;
        }

        return Math.max(0, maxRequestsPerDay - info.requestCount);
    }

    /**
     * Get total requests made by a user today.
     *
     * @param userId The user ID
     * @return Number of requests made today
     */
    public int getRequestCount(Long userId) {
        LocalDate today = LocalDate.now();
        DailyRequestInfo info = userRequestMap.get(userId);

        if (info == null || !info.date.equals(today)) {
            return 0;
        }

        return info.requestCount;
    }

    /**
     * Reset rate limit for a specific user (admin use).
     *
     * @param userId The user ID to reset
     */
    public void resetUser(Long userId) {
        userRequestMap.remove(userId);
        log.info("Daily rate limit reset for user: {}", userId);
    }

    /**
     * Clean up old entries at midnight daily.
     * Removes data from previous days to free memory.
     * Runs at 00:05 AM every day.
     */
    @Scheduled(cron = "0 5 0 * * *") // Run at 00:05 AM every day
    public void cleanupOldEntries() {
        LocalDate today = LocalDate.now();
        int removedCount = 0;

        for (Map.Entry<Long, DailyRequestInfo> entry : userRequestMap.entrySet()) {
            if (entry.getValue().date.isBefore(today)) {
                userRequestMap.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Daily cleanup: Removed {} old user entries from previous days", removedCount);
        } else {
            log.debug("Daily cleanup: No old entries to remove");
        }
    }

    /**
     * Get total number of users being tracked.
     */
    public int getTrackedUserCount() {
        return userRequestMap.size();
    }

    public int getMaxRequestsPerDay() {
        return maxRequestsPerDay;
    }

    /**
     * Inner class to store daily request information per IP
     */
    private static class DailyRequestInfo {
        int requestCount = 0;
        LocalDate date;

        DailyRequestInfo(LocalDate date) {
            this.date = date;
        }
    }
}
