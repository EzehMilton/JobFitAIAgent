package com.milton.agent.service;

import com.milton.agent.models.DashboardEntry;
import com.milton.agent.repository.DashboardEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardEntryRepository repository;

    private static final int MAX_ROWS = 20;
    private static final Long DUMMY_USER_ID = 1L;

    public List<DashboardEntry> getAllEntries() {
        return repository.findByUserIdOrderByIdDesc(DUMMY_USER_ID);
    }

    public boolean canAddNewEntry() {
        return repository.countByUserId(DUMMY_USER_ID) < MAX_ROWS;
    }

    public void saveEntry(String role, String company, String jobDescription, int score) {

        boolean showUpgrade = score >= 75 && score <= 85;
        boolean showInterviewPrep = score > 85;

        DashboardEntry entry = DashboardEntry.builder()
                .userId(DUMMY_USER_ID)
                .roleTitle(role)
                .companyName(company)
                .jobDescription(jobDescription)
                .score(score)
                .cvUpgradeAvailable(showUpgrade)
                .interviewPrepAvailable(showInterviewPrep)
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();

        repository.save(entry);
    }
}
