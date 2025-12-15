package com.milton.agent.service;

import com.milton.agent.models.DashboardEntry;
import com.milton.agent.repository.DashboardEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class DashboardService {

    private final DashboardEntryRepository repository;

    private final int suggestionsThreshold;
    private final int improveScoreLower;
    private final int improveScoreUpper;
    private final int cvUpgradeLower;
    private final int cvUpgradeUpper;
    private final int interviewPrepThreshold;
    private final int excellentThreshold;
    private final int goodThreshold;
    private final int partialThreshold;

    private static final DateTimeFormatter STORED_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy, HH:mm");
    private static final int MAX_ROWS = 20;
    private static final Long DUMMY_USER_ID = 1L;

    public DashboardService(DashboardEntryRepository repository,
                           @Value("${jobfit.score.suggestions-threshold:40}") int suggestionsThreshold,
                           @Value("${jobfit.score.improve-score-lower:40}") int improveScoreLower,
                           @Value("${jobfit.score.improve-score-upper:74}") int improveScoreUpper,
                           @Value("${jobfit.score.cv-upgrade-lower:75}") int cvUpgradeLower,
                           @Value("${jobfit.score.cv-upgrade-upper:85}") int cvUpgradeUpper,
                           @Value("${jobfit.score.interview-prep-threshold:85}") int interviewPrepThreshold,
                           @Value("${jobfit.score.excellent-threshold:90}") int excellentThreshold,
                           @Value("${jobfit.score.good-threshold:70}") int goodThreshold,
                           @Value("${jobfit.score.partial-threshold:50}") int partialThreshold) {
        this.repository = repository;
        this.suggestionsThreshold = suggestionsThreshold;
        this.improveScoreLower = improveScoreLower;
        this.improveScoreUpper = improveScoreUpper;
        this.cvUpgradeLower = cvUpgradeLower;
        this.cvUpgradeUpper = cvUpgradeUpper;
        this.interviewPrepThreshold = interviewPrepThreshold;
        this.excellentThreshold = excellentThreshold;
        this.goodThreshold = goodThreshold;
        this.partialThreshold = partialThreshold;
    }

    public List<DashboardEntry> getAllEntries() {
        return repository.findByUserIdOrderByIdDesc(DUMMY_USER_ID);
    }

    public DashboardEntry getEntryById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public boolean canAddNewEntry() {
        return repository.countByUserId(DUMMY_USER_ID) < MAX_ROWS;
    }

    public void saveEntry(String role, String company, String jobDescription, int score) {

        boolean showSuggestions = score < suggestionsThreshold;
        boolean showImproveScore = score >= improveScoreLower && score <= improveScoreUpper;
        boolean showUpgrade = score >= cvUpgradeLower && score <= cvUpgradeUpper;
        boolean showInterviewPrep = score > interviewPrepThreshold;

        DashboardEntry entry = DashboardEntry.builder()
                .userId(DUMMY_USER_ID)
                .roleTitle(role)
                .companyName(company)
                .jobDescription(jobDescription)
                .score(score)
                .recommendation(calculateRecommendation(score))
                .suggestionsAvailable(showSuggestions)
                .improveScoreAvailable(showImproveScore)
                .cvUpgradeAvailable(showUpgrade)
                .interviewPrepAvailable(showInterviewPrep)
                .createdAt(LocalDateTime.now().format(STORED_FORMATTER))
                .build();

        repository.save(entry);
    }

    private String calculateRecommendation(int score) {
        if (score >= excellentThreshold) {
            return "🟢 Apply Now";
        } else if (score >= goodThreshold) {
            return "🟠 Consider Applying";
        } else if (score >= partialThreshold) {
            return "🔵 Not Ready Yet";
        } else {
            return "🔴 Not Recommended";
        }
    }

    public int getTotalAnalyses(List<DashboardEntry> entries) {
        return entries == null ? 0 : entries.size();
    }

    public Integer getBestScore(List<DashboardEntry> entries) {
        return entries == null ? null : entries.stream()
                .map(DashboardEntry::getScore)
                .max(Integer::compareTo)
                .orElse(null);
    }

    public String getBestScoreLabel(List<DashboardEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        return entries.stream()
                .max((a, b) -> Integer.compare(a.getScore(), b.getScore()))
                .map(this::formatBestScore)
                .orElse(null);
    }

    public String getLastActivityLabel(List<DashboardEntry> entries) {
        Optional<LocalDateTime> latest = getLatestTimestamp(entries);
        if (latest.isEmpty()) {
            return null;
        }

        LocalDateTime latestTime = latest.get();
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        if (latestTime.toLocalDate().isEqual(now.toLocalDate())) {
            return "Today at " + latestTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        if (latestTime.toLocalDate().isEqual(now.toLocalDate().minusDays(1))) {
            return "Yesterday at " + latestTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        return latestTime.format(DISPLAY_FORMATTER);
    }

    private Optional<LocalDateTime> getLatestTimestamp(List<DashboardEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Optional.empty();
        }

        return entries.stream()
                .map(DashboardEntry::getCreatedAt)
                .map(this::parseCreatedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo);
    }

    private LocalDateTime parseCreatedAt(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(createdAt, STORED_FORMATTER);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String formatBestScore(DashboardEntry entry) {
        StringBuilder label = new StringBuilder();
        label.append(entry.getScore()).append("%");

        String role = entry.getRoleTitle();
        String company = entry.getCompanyName();
        if ((role != null && !role.isBlank()) || (company != null && !company.isBlank())) {
            label.append(" (");
            if (role != null && !role.isBlank()) {
                label.append(role.trim());
            }
            if (company != null && !company.isBlank()) {
                if (role != null && !role.isBlank()) {
                    label.append(" - ");
                }
                label.append(company.trim());
            }
            label.append(")");
        }
        return label.toString();
    }
}
