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

    @Column(length = 2000)
    private String jobDescription;

    private int score;

    private boolean cvUpgradeAvailable;
    private boolean interviewPrepAvailable;

    private String createdAt;  // store as text for now, e.g. LocalDateTime.toString()
}
