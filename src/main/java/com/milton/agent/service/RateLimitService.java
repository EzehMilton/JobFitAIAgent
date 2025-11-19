package com.milton.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to track and limit requests per IP address.
 * Limits each IP to a configurable number of requests per calendar day.
 * Resets at midnight daily.
 */
@Slf4j
@Service
public class RateLimitService {

    private final int maxRequestsPerDay;

    // Map: IP address -> Daily request info
    private final Map<String, DailyRequestInfo> ipRequestMap = new ConcurrentHashMap<>();

    public RateLimitService(@Value("${jobfit.rate-limit.max-daily-scans:10}") int maxRequestsPerDay) {
        this.maxRequestsPerDay = maxRequestsPerDay;
    }

    /**
     * Check if an IP address has exceeded the daily rate limit.
     *
     * @param ipAddress The IP address to check
     * @return true if request is allowed, false if limit exceeded
     */
    public boolean isAllowed(String ipAddress) {
        LocalDate today = LocalDate.now();
        DailyRequestInfo info = ipRequestMap.computeIfAbsent(ipAddress, k -> new DailyRequestInfo(today));

        synchronized (info) {
            // If it's a new day, reset the counter
            if (!info.date.equals(today)) {
                log.info("New day detected for IP {}. Resetting counter from {} to 0", ipAddress, info.requestCount);
                info.date = today;
                info.requestCount = 0;
            }

            if (info.requestCount >= maxRequestsPerDay) {
                log.warn("Daily rate limit exceeded for IP: {} (Date: {})", ipAddress, today);
                return false;
            }

            info.requestCount++;
            log.info("Request count for IP {} on {}: {}/{}", ipAddress, today, info.requestCount, maxRequestsPerDay);
            return true;
        }
    }

    /**
     * Get remaining requests for an IP address for today.
     *
     * @param ipAddress The IP address
     * @return Number of remaining requests (0-maxRequestsPerDay)
     */
    public int getRemainingRequests(String ipAddress) {
        LocalDate today = LocalDate.now();
        DailyRequestInfo info = ipRequestMap.get(ipAddress);

        if (info == null || !info.date.equals(today)) {
            return maxRequestsPerDay;
        }

        return Math.max(0, maxRequestsPerDay - info.requestCount);
    }

    /**
     * Get total requests made by an IP address today.
     *
     * @param ipAddress The IP address
     * @return Number of requests made today
     */
    public int getRequestCount(String ipAddress) {
        LocalDate today = LocalDate.now();
        DailyRequestInfo info = ipRequestMap.get(ipAddress);

        if (info == null || !info.date.equals(today)) {
            return 0;
        }

        return info.requestCount;
    }

    /**
     * Reset rate limit for a specific IP (admin use).
     *
     * @param ipAddress The IP address to reset
     */
    public void resetIp(String ipAddress) {
        ipRequestMap.remove(ipAddress);
        log.info("Daily rate limit reset for IP: {}", ipAddress);
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

        for (Map.Entry<String, DailyRequestInfo> entry : ipRequestMap.entrySet()) {
            if (entry.getValue().date.isBefore(today)) {
                ipRequestMap.remove(entry.getKey());
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.info("Daily cleanup: Removed {} old IP entries from previous days", removedCount);
        } else {
            log.debug("Daily cleanup: No old entries to remove");
        }
    }

    /**
     * Get total number of IPs being tracked.
     */
    public int getTrackedIpCount() {
        return ipRequestMap.size();
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
