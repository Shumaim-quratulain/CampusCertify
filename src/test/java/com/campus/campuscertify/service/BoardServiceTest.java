package com.campus.campuscertify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.campus.campuscertify.domain.ErrorCode;
import com.campus.campuscertify.domain.EvaluationResponse;
import com.campus.campuscertify.domain.Participant;
import com.campus.campuscertify.domain.ParticipantResult;
import com.campus.campuscertify.domain.ValidationError;
import com.campus.campuscertify.state.BoardState;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoardServiceTest {

    private BoardState state;
    private BoardService service;

    @BeforeEach
    void setUp() {
        state = new BoardState();
        service = new BoardService(state, new ParticipantValidator(), new EligibilityEvaluator());
    }

    @Test
    @DisplayName("Acceptance 1: built-in records give totals 7,6,7,7,4 with counts 2 eligible / 3 ineligible")
    void builtInOracle() {
        EvaluationResponse response = service.evaluate();

        assertThat(response.errors()).isEmpty();
        assertThat(response.results())
                .extracting(ParticipantResult::participantId, ParticipantResult::totalPoints,
                        ParticipantResult::eligible)
                .containsExactly(
                        tuple("C01", 7, true),
                        tuple("C02", 6, true),
                        tuple("C03", 7, false),
                        tuple("C04", 7, false),
                        tuple("C05", 4, false));
        assertThat(response.summary().eligibleCount()).isEqualTo(2);
        assertThat(response.summary().ineligibleCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Acceptance 2: C03 shows only SHARE, C04 only LEARN, C05 BUILD then POINTS_BELOW_6")
    void reasonsPerParticipant() {
        List<ParticipantResult> results = service.evaluate().results();

        assertThat(reasonsFor(results, "C01")).isEmpty();
        assertThat(reasonsFor(results, "C02")).isEmpty();
        assertThat(reasonsFor(results, "C03")).containsExactly("MISSING_CATEGORY: SHARE");
        assertThat(reasonsFor(results, "C04")).containsExactly("MISSING_CATEGORY: LEARN");
        assertThat(reasonsFor(results, "C05")).containsExactly("MISSING_CATEGORY: BUILD", "POINTS_BELOW_6");
    }

    @Test
    @DisplayName("Acceptance 3: adding A04 to C05 gives total 6 and counts 3 eligible / 2 ineligible")
    void addA04ToC05() {
        state.upsert(new Participant("C05", "Eshan", List.of("A01", "A03", "A04")));

        EvaluationResponse response = service.evaluate();

        ParticipantResult c05 = resultFor(response.results(), "C05");
        assertThat(c05.totalPoints()).isEqualTo(6);
        assertThat(c05.coveredCategories()).hasSize(3);
        assertThat(c05.eligible()).isTrue();
        assertThat(c05.failureReasons()).isEmpty();
        assertThat(response.summary().eligibleCount()).isEqualTo(3);
        assertThat(response.summary().ineligibleCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Acceptance 4: clearing C01 gives 0 points, four reasons, counts 1 eligible / 4 ineligible")
    void clearC01Activities() {
        service.reset();
        state.upsert(new Participant("C01", "Asha", List.of()));

        EvaluationResponse response = service.evaluate();

        ParticipantResult c01 = resultFor(response.results(), "C01");
        assertThat(c01.totalPoints()).isZero();
        assertThat(c01.coveredCategories()).isEmpty();
        assertThat(c01.failureReasons()).containsExactly(
                "MISSING_CATEGORY: LEARN",
                "MISSING_CATEGORY: BUILD",
                "MISSING_CATEGORY: SHARE",
                "POINTS_BELOW_6");
        assertThat(response.summary().eligibleCount()).isEqualTo(1);
        assertThat(response.summary().ineligibleCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Acceptance 5: a second A01 on C01 reports DUPLICATE_PARTICIPATION with no stale rows or counts")
    void duplicateParticipationClearsResults() {
        service.reset();
        service.evaluate();

        state.upsert(new Participant("C01", "Asha", List.of("A01", "A02", "A03", "A01")));
        EvaluationResponse response = service.evaluate();

        assertThat(response.errors())
                .extracting(ValidationError::code, ValidationError::participantId, ValidationError::offendingValue)
                .containsExactly(tuple(ErrorCode.DUPLICATE_PARTICIPATION, "C01", "A01"));
        assertThat(response.results()).isEmpty();
        assertThat(response.summary()).isNull();
    }

    @Test
    @DisplayName("Eligible participants come first, then ineligible, each group sorted by id ascending")
    void orderingIsEligibleFirstThenIdAscending() {
        service.reset();
        state.upsert(new Participant("C05", "Eshan", List.of("A01", "A03", "A04")));
        state.upsert(new Participant("C02", "Bilal", List.of("A01")));

        List<String> ids = service.evaluate().results().stream()
                .map(ParticipantResult::participantId)
                .toList();

        assertThat(ids).containsExactly("C01", "C05", "C02", "C03", "C04");
    }

    @Test
    @DisplayName("Reset restores the built-in oracle after edits")
    void resetRestoresOracle() {
        state.upsert(new Participant("C01", "Asha", List.of()));
        state.remove("C02");

        service.reset();
        EvaluationResponse response = service.evaluate();

        assertThat(response.results()).hasSize(5);
        assertThat(response.summary().eligibleCount()).isEqualTo(2);
        assertThat(response.summary().ineligibleCount()).isEqualTo(3);
    }

    private static ParticipantResult resultFor(List<ParticipantResult> results, String id) {
        return results.stream()
                .filter(r -> r.participantId().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no result for " + id));
    }

    private static List<String> reasonsFor(List<ParticipantResult> results, String id) {
        return resultFor(results, id).failureReasons();
    }
}
