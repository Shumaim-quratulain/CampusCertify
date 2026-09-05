package com.campus.campuscertify.state;

import static org.assertj.core.api.Assertions.assertThat;

import com.campus.campuscertify.domain.Activity;
import com.campus.campuscertify.domain.Category;
import com.campus.campuscertify.domain.Participant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoardStateTest {

    private BoardState state;

    @BeforeEach
    void setUp() {
        state = new BoardState();
    }

    @Test
    @DisplayName("Fixed activity table matches the spec exactly")
    void activityTableMatchesSpec() {
        assertThat(state.activities()).containsExactly(
                new Activity("A01", "Emerging Tech Talk", Category.LEARN, 2),
                new Activity("A02", "Soldering Mini Lab", Category.BUILD, 3),
                new Activity("A03", "Project Pitch Circle", Category.SHARE, 2),
                new Activity("A04", "Open Source Clinic", Category.BUILD, 2));
    }

    @Test
    @DisplayName("Built-in participants match the spec on startup")
    void builtInParticipantsMatchSpec() {
        assertThat(state.participants())
                .extracting(Participant::id, Participant::name, Participant::completedActivityIds)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("C01", "Asha", List.of("A01", "A02", "A03")),
                        org.assertj.core.groups.Tuple.tuple("C02", "Bilal", List.of("A01", "A03", "A04")),
                        org.assertj.core.groups.Tuple.tuple("C03", "Chen", List.of("A01", "A02", "A04")),
                        org.assertj.core.groups.Tuple.tuple("C04", "Divya", List.of("A02", "A03", "A04")),
                        org.assertj.core.groups.Tuple.tuple("C05", "Eshan", List.of("A01", "A03")));
    }

    @Test
    @DisplayName("Reset restores the five built-in rows byte-for-byte after arbitrary mutation")
    void resetRestoresStartupState() {
        state.remove("C01");
        state.upsert(new Participant("C09", "Intruder", List.of("A01")));
        state.findParticipant("C05").orElseThrow().setCompletedActivityIds(List.of());

        state.reset();

        assertThat(state.participants())
                .extracting(Participant::id, Participant::name, Participant::completedActivityIds)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("C01", "Asha", List.of("A01", "A02", "A03")),
                        org.assertj.core.groups.Tuple.tuple("C02", "Bilal", List.of("A01", "A03", "A04")),
                        org.assertj.core.groups.Tuple.tuple("C03", "Chen", List.of("A01", "A02", "A04")),
                        org.assertj.core.groups.Tuple.tuple("C04", "Divya", List.of("A02", "A03", "A04")),
                        org.assertj.core.groups.Tuple.tuple("C05", "Eshan", List.of("A01", "A03")));
    }

    @Test
    @DisplayName("Activity lookup trims and reports unknown ids as empty")
    void activityLookup() {
        assertThat(state.findActivity("  A02 ")).contains(new Activity("A02", "Soldering Mini Lab", Category.BUILD, 3));
        assertThat(state.findActivity("A99")).isEmpty();
    }

    @Test
    @DisplayName("Upsert replaces an existing participant rather than duplicating it")
    void upsertReplaces() {
        state.upsert(new Participant("C05", "Eshan", List.of("A01", "A03", "A04")));

        assertThat(state.participants()).hasSize(5);
        assertThat(state.findParticipant("C05").orElseThrow().completedActivityIds())
                .containsExactly("A01", "A03", "A04");
    }
}
