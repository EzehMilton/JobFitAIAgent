package com.milton.agent.service;

import com.milton.agent.models.DashboardEntry;
import com.milton.agent.repository.DashboardEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DashboardService {

    private final DashboardEntryRepository repository;

    private final int suggestionsThreshold;
    private final int improveScoreLower;
    private final int improveScoreUpper;
    private final int cvUpgradeLower;
    private final int cvUpgradeUpper;
    private final int interviewPrepThreshold;

    private static final int MAX_ROWS = 20;
    private static final Long DUMMY_USER_ID = 1L;

    public DashboardService(DashboardEntryRepository repository,
                           @Value("${jobfit.score.suggestions-threshold:40}") int suggestionsThreshold,
                           @Value("${jobfit.score.improve-score-lower:40}") int improveScoreLower,
                           @Value("${jobfit.score.improve-score-upper:74}") int improveScoreUpper,
                           @Value("${jobfit.score.cv-upgrade-lower:75}") int cvUpgradeLower,
                           @Value("${jobfit.score.cv-upgrade-upper:85}") int cvUpgradeUpper,
                           @Value("${jobfit.score.interview-prep-threshold:85}") int interviewPrepThreshold) {
        this.repository = repository;
        this.suggestionsThreshold = suggestionsThreshold;
        this.improveScoreLower = improveScoreLower;
        this.improveScoreUpper = improveScoreUpper;
        this.cvUpgradeLower = cvUpgradeLower;
        this.cvUpgradeUpper = cvUpgradeUpper;
        this.interviewPrepThreshold = interviewPrepThreshold;
    }

    public List<DashboardEntry> getAllEntries() {
        return repository.findByUserIdOrderByIdDesc(DUMMY_USER_ID);
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
                .suggestionsAvailable(showSuggestions)
                .improveScoreAvailable(showImproveScore)
                .cvUpgradeAvailable(showUpgrade)
                .interviewPrepAvailable(showInterviewPrep)
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();

        repository.save(entry);
    }
}
