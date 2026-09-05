package com.campus.campuscertify.service;

import com.campus.campuscertify.domain.Activity;
import com.campus.campuscertify.domain.ErrorCode;
import com.campus.campuscertify.domain.Participant;
import com.campus.campuscertify.domain.ValidationError;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Collects every input problem instead of throwing: invalid input is expected domain output,
 * not an exceptional condition, and the spec requires naming each offending value.
 */
@Component
public class ParticipantValidator {

    public List<ValidationError> validate(List<Participant> participants, Map<String, Activity> activityIndex) {
        List<ValidationError> errors = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (Participant participant : participants) {
            checkIdentity(participant, errors);
            checkUniqueId(participant, seenIds, errors);
            checkActivities(participant, activityIndex, errors);
        }
        return errors;
    }

    private void checkIdentity(Participant participant, List<ValidationError> errors) {
        if (participant.id().isEmpty()) {
            errors.add(new ValidationError(ErrorCode.INVALID_PARTICIPANT, participant.id(), "participantId"));
        }
        if (participant.name().isEmpty()) {
            errors.add(new ValidationError(ErrorCode.INVALID_PARTICIPANT, participant.id(), "participantName"));
        }
    }

    private void checkUniqueId(Participant participant, Set<String> seenIds, List<ValidationError> errors) {
        if (!participant.id().isEmpty() && !seenIds.add(participant.id())) {
            errors.add(new ValidationError(ErrorCode.DUPLICATE_PARTICIPANT_ID, participant.id(), participant.id()));
        }
    }

    private void checkActivities(
            Participant participant, Map<String, Activity> activityIndex, List<ValidationError> errors) {
        Set<String> seenActivities = new HashSet<>();

        for (String activityId : participant.completedActivityIds()) {
            if (!activityIndex.containsKey(activityId)) {
                errors.add(new ValidationError(ErrorCode.UNKNOWN_ACTIVITY, participant.id(), activityId));
            } else if (!seenActivities.add(activityId)) {
                errors.add(new ValidationError(ErrorCode.DUPLICATE_PARTICIPATION, participant.id(), activityId));
            }
        }
    }
}
