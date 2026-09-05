package com.campus.campuscertify.service;

import com.campus.campuscertify.domain.Activity;
import com.campus.campuscertify.domain.Category;
import com.campus.campuscertify.domain.Participant;
import com.campus.campuscertify.domain.ParticipantResult;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Precondition: only called after validation passed, so every activity id resolves. */
@Component
public class EligibilityEvaluator {

    public static final int REQUIRED_POINTS = 6;
    private static final String MISSING_CATEGORY_PREFIX = "MISSING_CATEGORY: ";
    private static final String POINTS_BELOW_THRESHOLD = "POINTS_BELOW_" + REQUIRED_POINTS;

    public ParticipantResult evaluate(Participant participant, Map<String, Activity> activityIndex) {
        Set<Category> covered = coveredCategories(participant, activityIndex);
        int totalPoints = totalPoints(participant, activityIndex);
        boolean eligible = covered.size() == Category.values().length && totalPoints >= REQUIRED_POINTS;

        return new ParticipantResult(
                participant.id(),
                participant.name(),
                totalPoints,
                covered,
                eligible,
                eligible ? List.of() : failureReasons(covered, totalPoints));
    }

    /** EnumSet iterates in declaration order, so LEARN/BUILD/SHARE ordering is structural. */
    private Set<Category> coveredCategories(Participant participant, Map<String, Activity> activityIndex) {
        Set<Category> covered = EnumSet.noneOf(Category.class);
        for (String activityId : participant.completedActivityIds()) {
            covered.add(activityIndex.get(activityId).category());
        }
        return covered;
    }

    private int totalPoints(Participant participant, Map<String, Activity> activityIndex) {
        int total = 0;
        for (String activityId : participant.completedActivityIds()) {
            total += activityIndex.get(activityId).points();
        }
        return total;
    }

    /** Both requirements are evaluated completely; no early return after the first failure. */
    private List<String> failureReasons(Set<Category> covered, int totalPoints) {
        List<String> reasons = new ArrayList<>();
        for (Category category : Category.values()) {
            if (!covered.contains(category)) {
                reasons.add(MISSING_CATEGORY_PREFIX + category);
            }
        }
        if (totalPoints < REQUIRED_POINTS) {
            reasons.add(POINTS_BELOW_THRESHOLD);
        }
        return reasons;
    }
}
