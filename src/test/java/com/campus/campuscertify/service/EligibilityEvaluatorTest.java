package com.campus.campuscertify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static com.campus.campuscertify.service.EligibilityEvaluator.REQUIRED_POINTS;

import com.campus.campuscertify.domain.Category;
import com.campus.campuscertify.domain.Participant;
import com.campus.campuscertify.domain.ParticipantResult;
import com.campus.campuscertify.state.BoardState;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EligibilityEvaluatorTest {

    private final EligibilityEvaluator evaluator = new EligibilityEvaluator();
    private final BoardState state = new BoardState();

    private ParticipantResult evaluate(String id, String name, List<String> activityIds) {
        return evaluator.evaluate(new Participant(id, name, activityIds), state.activityIndex());
    }

    @Test
    @DisplayName("C01 Asha: A01+A02+A03 = 7 points, all categories, eligible, no reasons")
    void c01IsEligible() {
        ParticipantResult result = evaluate("C01", "Asha", List.of("A01", "A02", "A03"));

        assertThat(result.totalPoints()).isEqualTo(7);
        assertThat(result.coveredCategories()).containsExactly(Category.LEARN, Category.BUILD, Category.SHARE);
        assertThat(result.eligible()).isTrue();
        assertThat(result.failureReasons()).isEmpty();
    }

    @Test
    @DisplayName("C02 Bilal: exactly 6 points is eligible, proving >= not >")
    void exactPointBoundaryIsEligible() {
        ParticipantResult result = evaluate("C02", "Bilal", List.of("A01", "A03", "A04"));

        assertThat(result.totalPoints()).isEqualTo(6);
        assertThat(result.eligible()).isTrue();
        assertThat(result.failureReasons()).isEmpty();
    }

    @Test
    @DisplayName("C03 Chen: 7 points but shows only MISSING_CATEGORY: SHARE")
    void c03MissingShareOnly() {
        ParticipantResult result = evaluate("C03", "Chen", List.of("A01", "A02", "A04"));

        assertThat(result.totalPoints()).isEqualTo(7);
        assertThat(result.eligible()).isFalse();
        assertThat(result.failureReasons()).containsExactly("MISSING_CATEGORY: SHARE");
    }

    @Test
    @DisplayName("C04 Divya: 7 points but shows only MISSING_CATEGORY: LEARN")
    void c04MissingLearnOnly() {
        ParticipantResult result = evaluate("C04", "Divya", List.of("A02", "A03", "A04"));

        assertThat(result.totalPoints()).isEqualTo(7);
        assertThat(result.eligible()).isFalse();
        assertThat(result.failureReasons()).containsExactly("MISSING_CATEGORY: LEARN");
    }

    @Test
    @DisplayName("C05 Eshan: 4 points, MISSING_CATEGORY: BUILD followed by POINTS_BELOW_6")
    void c05EvaluatesBothRequirements() {
        ParticipantResult result = evaluate("C05", "Eshan", List.of("A01", "A03"));

        assertThat(result.totalPoints()).isEqualTo(4);
        assertThat(result.eligible()).isFalse();
        assertThat(result.failureReasons()).containsExactly("MISSING_CATEGORY: BUILD", "POINTS_BELOW_6");
    }

    @Test
    @DisplayName("Empty completion list gives 0 points, no categories, all four reasons in order")
    void emptyCompletionList() {
        ParticipantResult result = evaluate("C01", "Asha", List.of());

        assertThat(result.totalPoints()).isZero();
        assertThat(result.coveredCategories()).isEmpty();
        assertThat(result.eligible()).isFalse();
        assertThat(result.failureReasons()).containsExactly(
                "MISSING_CATEGORY: LEARN",
                "MISSING_CATEGORY: BUILD",
                "MISSING_CATEGORY: SHARE",
                "POINTS_BELOW_6");
    }

    @Test
    @DisplayName("Adding A04 to C05 lifts it to 6 points with all three categories covered")
    void c05PlusA04BecomesEligible() {
        ParticipantResult result = evaluate("C05", "Eshan", List.of("A01", "A03", "A04"));

        assertThat(result.totalPoints()).isEqualTo(6);
        assertThat(result.coveredCategories()).containsExactly(Category.LEARN, Category.BUILD, Category.SHARE);
        assertThat(result.eligible()).isTrue();
        assertThat(result.failureReasons()).isEmpty();
    }

    @Test
    @DisplayName("Cheapest full-coverage combination is exactly 6 points, so >= vs > decides eligibility")
    void cheapestFullCoverageSitsOnTheBoundary() {
        ParticipantResult result = evaluate("CX", "Boundary", List.of("A01", "A04", "A03"));

        assertThat(result.totalPoints()).isEqualTo(REQUIRED_POINTS);
        assertThat(result.coveredCategories()).containsExactly(Category.LEARN, Category.BUILD, Category.SHARE);
        assertThat(result.eligible()).isTrue();
    }

    @Test
    @DisplayName("Dropping any category from the cheapest combination reports that category and the point shortfall")
    void bothRequirementsFailTogether() {
        ParticipantResult result = evaluate("CX", "Boundary", List.of("A01", "A03"));

        assertThat(result.totalPoints()).isEqualTo(4);
        assertThat(result.failureReasons()).containsExactly("MISSING_CATEGORY: BUILD", "POINTS_BELOW_6");
    }
}
