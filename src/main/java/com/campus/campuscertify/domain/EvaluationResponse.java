package com.campus.campuscertify.domain;

import java.util.List;

/**
 * Invariant: when {@code errors} is non-empty, {@code results} is empty and {@code summary} is null.
 * This makes "any input error clears result rows and counts" structural rather than a UI rule.
 */
public record EvaluationResponse(
        List<ValidationError> errors,
        List<ParticipantResult> results,
        EvaluationSummary summary) {

    public static EvaluationResponse ofErrors(List<ValidationError> errors) {
        return new EvaluationResponse(List.copyOf(errors), List.of(), null);
    }

    public static EvaluationResponse ofResults(List<ParticipantResult> results, EvaluationSummary summary) {
        return new EvaluationResponse(List.of(), List.copyOf(results), summary);
    }
}
