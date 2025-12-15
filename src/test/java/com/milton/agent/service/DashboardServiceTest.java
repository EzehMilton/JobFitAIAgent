package com.milton.agent.service;

import com.milton.agent.models.DashboardEntry;
import com.milton.agent.repository.DashboardEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardEntryRepository repository;

    private DashboardService dashboardService;

    private final int suggestionsThreshold = 40;
    private final int improveScoreLower = 40;
    private final int improveScoreUpper = 74;
    private final int cvUpgradeLower = 75;
    private final int cvUpgradeUpper = 85;
    private final int interviewPrepThreshold = 85;
    private final int excellentThreshold = 90;
    private final int goodThreshold = 70;
    private final int partialThreshold = 50;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                repository,
                suggestionsThreshold,
                improveScoreLower,
                improveScoreUpper,
                cvUpgradeLower,
                cvUpgradeUpper,
                interviewPrepThreshold,
                excellentThreshold,
                goodThreshold,
                partialThreshold
        );
    }

    @Test
    void saveEntry_WithScoreBelow40_ShouldSetSuggestionsAvailable() {
        // Arrange
        String role = "Software Engineer";
        String company = "Tech Corp";
        String jobDescription = "Job description";
        int score = 35;

        ArgumentCaptor<DashboardEntry> entryCaptor = ArgumentCaptor.forClass(DashboardEntry.class);

        // Act
        dashboardService.saveEntry(role, company, jobDescription, score);

        // Assert
        verify(repository, times(1)).save(entryCaptor.capture());
        DashboardEntry savedEntry = entryCaptor.getValue();

        assertEquals(role, savedEntry.getRoleTitle());
        assertEquals(company, savedEntry.getCompanyName());
        assertEquals(jobDescription, savedEntry.getJobDescription());
        assertEquals(score, savedEntry.getScore());
        assertTrue(savedEntry.isSuggestionsAvailable(), "Suggestions should be available for score < 40");
        assertFalse(savedEntry.isImproveScoreAvailable());
        assertFalse(savedEntry.isCvUpgradeAvailable());
        assertFalse(savedEntry.isInterviewPrepAvailable());
        assertNotNull(savedEntry.getCreatedAt());
    }

    @Test
    void saveEntry_WithScoreBetween40And74_ShouldSetImproveScoreAvailable() {
        // Arrange
        String role = "Product Manager";
        String company = "Startup Inc";
        String jobDescription = "PM role";
        int score = 60;

        ArgumentCaptor<DashboardEntry> entryCaptor = ArgumentCaptor.forClass(DashboardEntry.class);

        // Act
        dashboardService.saveEntry(role, company, jobDescription, score);

        // Assert
        verify(repository, times(1)).save(entryCaptor.capture());
        DashboardEntry savedEntry = entryCaptor.getValue();

        assertEquals(score, savedEntry.getScore());
        assertFalse(savedEntry.isSuggestionsAvailable());
        assertTrue(savedEntry.isImproveScoreAvailable(), "Improve score should be available for score 40-74");
        assertFalse(savedEntry.isCvUpgradeAvailable());
        assertFalse(savedEntry.isInterviewPrepAvailable());
    }

    @Test
    void saveEntry_WithScoreBetween75And85_ShouldSetCvUpgradeAvailable() {
        // Arrange
        String role = "Data Scientist";
        String company = "AI Company";
        String jobDescription = "ML role";
        int score = 80;

        ArgumentCaptor<DashboardEntry> entryCaptor = ArgumentCaptor.forClass(DashboardEntry.class);

        // Act
        dashboardService.saveEntry(role, company, jobDescription, score);

        // Assert
        verify(repository, times(1)).save(entryCaptor.capture());
        DashboardEntry savedEntry = entryCaptor.getValue();

        assertEquals(score, savedEntry.getScore());
        assertFalse(savedEntry.isSuggestionsAvailable());
        assertFalse(savedEntry.isImproveScoreAvailable());
        assertTrue(savedEntry.isCvUpgradeAvailable(), "CV upgrade should be available for score 75-85");
        assertFalse(savedEntry.isInterviewPrepAvailable());
    }

    @Test
    void saveEntry_WithScoreAbove85_ShouldSetInterviewPrepAvailable() {
        // Arrange
        String role = "Senior Engineer";
        String company = "Big Tech";
        String jobDescription = "Senior role";
        int score = 92;

        ArgumentCaptor<DashboardEntry> entryCaptor = ArgumentCaptor.forClass(DashboardEntry.class);

        // Act
        dashboardService.saveEntry(role, company, jobDescription, score);

        // Assert
        verify(repository, times(1)).save(entryCaptor.capture());
        DashboardEntry savedEntry = entryCaptor.getValue();

        assertEquals(score, savedEntry.getScore());
        assertFalse(savedEntry.isSuggestionsAvailable());
        assertFalse(savedEntry.isImproveScoreAvailable());
        assertFalse(savedEntry.isCvUpgradeAvailable());
        assertTrue(savedEntry.isInterviewPrepAvailable(), "Interview prep should be available for score > 85");
    }

    @Test
    void saveEntry_WithBoundaryScore40_ShouldSetImproveScoreAvailable() {
        // Arrange
        int score = 40;

        ArgumentCaptor<DashboardEntry> entryCaptor = ArgumentCaptor.forClass(DashboardEntry.class);

        // Act
        dashboardService.saveEntry("Role", "Company", "Description", score);

        // Assert
        verify(repository, times(1)).save(entryCaptor.capture());
        DashboardEntry savedEntry = entryCaptor.getValue();

        assertFalse(savedEntry.isSuggestionsAvailable(), "Score 40 should not show suggestions");
        assertTrue(savedEntry.isImproveScoreAvailable(), "Score 40 should show improve score");
    }

    @Test
    void saveEntry_WithBoundaryScore74_ShouldSetImproveScoreAvailable() {
        // Arrange
        int score = 74;

        ArgumentCaptor<DashboardEntry> entryCaptor = ArgumentCaptor.forClass(DashboardEntry.class);

        // Act
        dashboardService.saveEntry("Role", "Company", "Description", score);

        // Assert
        verify(repository, times(1)).save(entryCaptor.capture());
        DashboardEntry savedEntry = entryCaptor.getValue();

        assertFalse(savedEntry.isSuggestionsAvailable());
        assertTrue(savedEntry.isImproveScoreAvailable(), "Score 74 should show improve score");
        assertFalse(savedEntry.isCvUpgradeAvailable());
    }

    @Test
    void saveEntry_WithBoundaryScore75_ShouldSetCvUpgradeAvailable() {
        // Arrange
        int score = 75;

        ArgumentCaptor<DashboardEntry> entryCaptor = ArgumentCaptor.forClass(DashboardEntry.class);

        // Act
        dashboardService.saveEntry("Role", "Company", "Description", score);

        // Assert
        verify(repository, times(1)).save(entryCaptor.capture());
        DashboardEntry savedEntry = entryCaptor.getValue();

        assertFalse(savedEntry.isImproveScoreAvailable(), "Score 75 should not show improve score");
        assertTrue(savedEntry.isCvUpgradeAvailable(), "Score 75 should show CV upgrade");
    }

    @Test
    void saveEntry_WithBoundaryScore85_ShouldSetCvUpgradeAvailable() {
        // Arrange
        int score = 85;

        ArgumentCaptor<DashboardEntry> entryCaptor = ArgumentCaptor.forClass(DashboardEntry.class);

        // Act
        dashboardService.saveEntry("Role", "Company", "Description", score);

        // Assert
        verify(repository, times(1)).save(entryCaptor.capture());
        DashboardEntry savedEntry = entryCaptor.getValue();

        assertTrue(savedEntry.isCvUpgradeAvailable(), "Score 85 should show CV upgrade");
        assertFalse(savedEntry.isInterviewPrepAvailable(), "Score 85 should not show interview prep");
    }

    @Test
    void saveEntry_WithBoundaryScore86_ShouldSetInterviewPrepAvailable() {
        // Arrange
        int score = 86;

        ArgumentCaptor<DashboardEntry> entryCaptor = ArgumentCaptor.forClass(DashboardEntry.class);

        // Act
        dashboardService.saveEntry("Role", "Company", "Description", score);

        // Assert
        verify(repository, times(1)).save(entryCaptor.capture());
        DashboardEntry savedEntry = entryCaptor.getValue();

        assertFalse(savedEntry.isCvUpgradeAvailable(), "Score 86 should not show CV upgrade");
        assertTrue(savedEntry.isInterviewPrepAvailable(), "Score 86 should show interview prep");
    }

    @Test
    void getAllEntries_ShouldReturnEntriesFromRepository() {
        // Arrange
        List<DashboardEntry> mockEntries = Arrays.asList(
                DashboardEntry.builder().id(1L).roleTitle("Role 1").score(50).build(),
                DashboardEntry.builder().id(2L).roleTitle("Role 2").score(80).build()
        );
        when(repository.findByUserIdOrderByIdDesc(1L)).thenReturn(mockEntries);

        // Act
        List<DashboardEntry> result = dashboardService.getAllEntries();

        // Assert
        assertEquals(2, result.size());
        assertEquals("Role 1", result.get(0).getRoleTitle());
        assertEquals("Role 2", result.get(1).getRoleTitle());
        verify(repository, times(1)).findByUserIdOrderByIdDesc(1L);
    }

    @Test
    void getEntryById_ShouldReturnEntry_WhenExists() {
        // Arrange
        Long entryId = 5L;
        DashboardEntry mockEntry = DashboardEntry.builder()
                .id(entryId)
                .roleTitle("Test Role")
                .score(75)
                .build();
        when(repository.findById(entryId)).thenReturn(java.util.Optional.of(mockEntry));

        // Act
        DashboardEntry result = dashboardService.getEntryById(entryId);

        // Assert
        assertNotNull(result);
        assertEquals(entryId, result.getId());
        assertEquals("Test Role", result.getRoleTitle());
        verify(repository, times(1)).findById(entryId);
    }

    @Test
    void getEntryById_ShouldReturnNull_WhenNotExists() {
        // Arrange
        Long entryId = 999L;
        when(repository.findById(entryId)).thenReturn(java.util.Optional.empty());

        // Act
        DashboardEntry result = dashboardService.getEntryById(entryId);

        // Assert
        assertNull(result);
        verify(repository, times(1)).findById(entryId);
    }

    @Test
    void canAddNewEntry_ShouldReturnTrue_WhenBelowMaxRows() {
        // Arrange
        when(repository.countByUserId(1L)).thenReturn(15L);

        // Act
        boolean result = dashboardService.canAddNewEntry();

        // Assert
        assertTrue(result, "Should be able to add new entry when count is below 20");
        verify(repository, times(1)).countByUserId(1L);
    }

    @Test
    void canAddNewEntry_ShouldReturnFalse_WhenAtMaxRows() {
        // Arrange
        when(repository.countByUserId(1L)).thenReturn(20L);

        // Act
        boolean result = dashboardService.canAddNewEntry();

        // Assert
        assertFalse(result, "Should not be able to add new entry when count is at 20");
        verify(repository, times(1)).countByUserId(1L);
    }

    @Test
    void canAddNewEntry_ShouldReturnFalse_WhenAboveMaxRows() {
        // Arrange
        when(repository.countByUserId(1L)).thenReturn(25L);

        // Act
        boolean result = dashboardService.canAddNewEntry();

        // Assert
        assertFalse(result, "Should not be able to add new entry when count exceeds 20");
    }

    @Test
    void canAddNewEntry_ShouldReturnTrue_WhenZeroEntries() {
        // Arrange
        when(repository.countByUserId(1L)).thenReturn(0L);

        // Act
        boolean result = dashboardService.canAddNewEntry();

        // Assert
        assertTrue(result, "Should be able to add entry when no entries exist");
    }
}