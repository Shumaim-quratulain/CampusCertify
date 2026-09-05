package com.campus.campuscertify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.campus.campuscertify.domain.ErrorCode;
import com.campus.campuscertify.domain.Participant;
import com.campus.campuscertify.domain.ValidationError;
import com.campus.campuscertify.state.BoardState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticipantValidatorTest {

    private final ParticipantValidator validator = new ParticipantValidator();
    private final BoardState state = new BoardState();

    private List<ValidationError> validate(Participant... participants) {
        return validator.validate(List.of(participants), state.activityIndex());
    }

    @Test
    @DisplayName("Built-in records produce no validation errors")
    void builtInRecordsAreValid() {
        assertThat(validator.validate(state.participants(), state.activityIndex())).isEmpty();
    }

    @Test
    @DisplayName("Repeated activity id for one participant reports DUPLICATE_PARTICIPATION with C01 and A01")
    void duplicateParticipation() {
        List<ValidationError> errors = validate(new Participant("C01", "Asha", List.of("A01", "A02", "A03", "A01")));

        assertThat(errors).extracting(ValidationError::code, ValidationError::participantId,
                        ValidationError::offendingValue)
                .containsExactly(tuple(ErrorCode.DUPLICATE_PARTICIPATION, "C01", "A01"));
    }

    @Test
    @DisplayName("Activity id outside the fixed table reports UNKNOWN_ACTIVITY")
    void unknownActivity() {
        List<ValidationError> errors = validate(new Participant("C01", "Asha", List.of("A01", "A99")));

        assertThat(errors).extracting(ValidationError::code, ValidationError::participantId,
                        ValidationError::offendingValue)
                .containsExactly(tuple(ErrorCode.UNKNOWN_ACTIVITY, "C01", "A99"));
    }

    @Test
    @DisplayName("Blank id or blank name reports INVALID_PARTICIPANT")
    void invalidParticipant() {
        List<ValidationError> errors = validate(
                new Participant("   ", "Asha", List.of("A01")),
                new Participant("C02", "  ", List.of("A01")));

        assertThat(errors).extracting(ValidationError::code, ValidationError::offendingValue)
                .containsExactly(
                        tuple(ErrorCode.INVALID_PARTICIPANT, "participantId"),
                        tuple(ErrorCode.INVALID_PARTICIPANT, "participantName"));
    }

    @Test
    @DisplayName("Repeated participant id reports DUPLICATE_PARTICIPANT_ID once, on the second occurrence")
    void duplicateParticipantId() {
        List<ValidationError> errors = validate(
                new Participant("C01", "Asha", List.of("A01")),
                new Participant("C01", "Clone", List.of("A02")));

        assertThat(errors).extracting(ValidationError::code, ValidationError::participantId)
                .containsExactly(tuple(ErrorCode.DUPLICATE_PARTICIPANT_ID, "C01"));
    }

    @Test
    @DisplayName("Validation collects every error rather than stopping at the first")
    void collectsAllErrors() {
        List<ValidationError> errors = validate(
                new Participant("C01", "Asha", List.of("A99", "A02", "A02")),
                new Participant("C01", "", List.of("A88")));

        assertThat(errors).extracting(ValidationError::code).containsExactly(
                ErrorCode.UNKNOWN_ACTIVITY,
                ErrorCode.DUPLICATE_PARTICIPATION,
                ErrorCode.INVALID_PARTICIPANT,
                ErrorCode.DUPLICATE_PARTICIPANT_ID,
                ErrorCode.UNKNOWN_ACTIVITY);
    }

    @Test
    @DisplayName("Untrimmed ids do not create phantom participants that bypass uniqueness")
    void trimmingPreventsPhantomIds() {
        List<ValidationError> errors = validate(
                new Participant("C01", "Asha", List.of("A01")),
                new Participant("  C01  ", "Clone", List.of("A02")));

        assertThat(errors).extracting(ValidationError::code)
                .containsExactly(ErrorCode.DUPLICATE_PARTICIPANT_ID);
    }

    @Test
    @DisplayName("Empty completed-activity list is valid input")
    void emptyActivityListIsValid() {
        assertThat(validate(new Participant("C01", "Asha", List.of()))).isEmpty();
    }
}
