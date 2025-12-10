package com.milton.agent.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MatchPresentationService {

    private final int upgradeScoreLowerBound;
    private final int upgradeScoreUpperBound;
    private final int excellentThreshold;
    private final int goodThreshold;
    private final int partialThreshold;

    public MatchPresentationService(@Value("${jobfit.score.cv-upgrade-lower:75}") int upgradeScoreLowerBound,
                                    @Value("${jobfit.score.cv-upgrade-upper:85}") int upgradeScoreUpperBound,
                                    @Value("${jobfit.score.excellent-threshold:90}") int excellentThreshold,
                                    @Value("${jobfit.score.good-threshold:70}") int goodThreshold,
                                    @Value("${jobfit.score.partial-threshold:50}") int partialThreshold) {
        if (upgradeScoreLowerBound >= upgradeScoreUpperBound) {
            log.warn("Invalid upgrade score bounds configured: lower={} upper={}", upgradeScoreLowerBound, upgradeScoreUpperBound);
        }
        this.upgradeScoreLowerBound = upgradeScoreLowerBound;
        this.upgradeScoreUpperBound = upgradeScoreUpperBound;
        this.excellentThreshold = excellentThreshold;
        this.goodThreshold = goodThreshold;
        this.partialThreshold = partialThreshold;
    }

    public String toMatchLabel(int score) {
        if (score >= excellentThreshold) return "EXCELLENT MATCH";
        if (score >= goodThreshold) return "GOOD MATCH";
        if (score >= partialThreshold) return "PARTIAL MATCH";
        return "WEAK MATCH";
    }

    public String toMatchClass(int score) {
        if (score >= excellentThreshold) return "match-badge-excellent";
        if (score >= goodThreshold) return "match-badge-good";
        if (score >= partialThreshold) return "match-badge-partial";
        return "match-badge-weak";
    }

    public String toMatchTheme(int score) {
        if (score >= excellentThreshold) return "match-theme-excellent";
        if (score >= goodThreshold) return "match-theme-good";
        if (score >= partialThreshold) return "match-theme-partial";
        return "match-theme-weak";
    }

    public boolean shouldShowUpgradeButton(int score) {
        if (upgradeScoreLowerBound >= upgradeScoreUpperBound) {
            log.warn("Invalid upgrade score bounds configured: lower={} upper={}", upgradeScoreLowerBound, upgradeScoreUpperBound);
            return false;
        }
        return score >= upgradeScoreLowerBound && score <= upgradeScoreUpperBound;
    }

    public boolean shouldShowInterviewPrepButton(int score) {
        if (upgradeScoreLowerBound >= upgradeScoreUpperBound) {
            log.warn("Invalid upgrade score bounds configured: lower={} upper={}", upgradeScoreLowerBound, upgradeScoreUpperBound);
            return false;
        }
        return score > upgradeScoreUpperBound;
    }
}
