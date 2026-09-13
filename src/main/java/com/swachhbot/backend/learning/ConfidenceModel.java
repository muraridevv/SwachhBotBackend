package com.swachhbot.backend.learning;

import com.swachhbot.backend.domain.learning.InsightStatus;
import org.springframework.stereotype.Component;

/**
 * Converts raw evidence counts into a confidence score.
 *
 * <p>Uses a saturating curve {@code e / (e + K)} so that a single observation
 * gives low confidence while repeated observations converge towards — but never
 * reach — certainty. This keeps the robot appropriately humble.
 */
@Component
public class ConfidenceModel {

    /** Half-saturation point: evidence == K gives 0.5 confidence. */
    public static final double K = 3.0;

    /** Evidence needed before an insight is considered trustworthy enough to act on. */
    public static final int CONFIRMED_THRESHOLD = 3;

    public double confidenceFor(int evidenceCount) {
        if (evidenceCount <= 0) {
            return 0.0;
        }
        return Math.min(0.99, evidenceCount / (evidenceCount + K));
    }

    public InsightStatus statusFor(int evidenceCount) {
        return evidenceCount >= CONFIRMED_THRESHOLD ? InsightStatus.CONFIRMED : InsightStatus.EMERGING;
    }

    /** Confidence at or above which learned knowledge may influence a plan. */
    public boolean isActionable(double confidence) {
        return confidence >= confidenceFor(CONFIRMED_THRESHOLD);
    }
}
