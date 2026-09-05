package com.campus.campuscertify.domain;

import java.util.List;
import java.util.Set;

public record ParticipantResult(
        String participantId,
        String participantName,
        int totalPoints,
        Set<Category> coveredCategories,
        boolean eligible,
        List<String> failureReasons) {
}
