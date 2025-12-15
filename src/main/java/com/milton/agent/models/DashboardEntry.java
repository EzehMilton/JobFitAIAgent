package com.milton.agent.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dashboard_entries")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // dummy = 1 for now

    private String roleTitle;
    private String companyName;

    @Column(length = 5000)
    private String jobDescription;

    private int score;

    @Column(length = 100)
    private String recommendation;  // e.g. "🟢 Apply Now", "🟠 Consider Applying", etc.

    private boolean suggestionsAvailable;        // for scores < 40
    private boolean improveScoreAvailable;       // for scores 40-74
    private boolean cvUpgradeAvailable;          // for scores 75-85
    private boolean interviewPrepAvailable;      // for scores > 85

    private String createdAt;  // store as text for now, e.g. LocalDateTime.toString()
}
