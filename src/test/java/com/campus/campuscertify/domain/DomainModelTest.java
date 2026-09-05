package com.campus.campuscertify.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DomainModelTest {

    @Test
    @DisplayName("Category declaration order is the contracted reason order: LEARN, BUILD, SHARE")
    void categoryOrderMatchesContract() {
        assertThat(Category.values()).containsExactly(Category.LEARN, Category.BUILD, Category.SHARE);
    }

    @Test
    @DisplayName("Participant trims id, name and activity ids on construction")
    void participantNormalizesOnWrite() {
        Participant p = new Participant("  C01 ", "  Asha  ", List.of(" A01", "A02 ", "  "));

        assertThat(p.id()).isEqualTo("C01");
        assertThat(p.name()).isEqualTo("Asha");
        assertThat(p.completedActivityIds()).containsExactly("A01", "A02");
    }

    @Test
    @DisplayName("Participant keeps repeated activity ids so DUPLICATE_PARTICIPATION stays detectable")
    void participantPreservesDuplicates() {
        Participant p = new Participant("C01", "Asha", List.of("A01", "A01"));

        assertThat(p.completedActivityIds()).containsExactly("A01", "A01");
    }

    @Test
    @DisplayName("Evaluation envelope with errors carries no results and no summary")
    void errorEnvelopeClearsResults() {
        EvaluationResponse response = EvaluationResponse.ofErrors(
                List.of(new ValidationError(ErrorCode.DUPLICATE_PARTICIPATION, "C01", "A01")));

        assertThat(response.errors()).hasSize(1);
        assertThat(response.results()).isEmpty();
        assertThat(response.summary()).isNull();
    }
}
