package com.campus.campuscertify.state;

import com.campus.campuscertify.domain.Activity;
import com.campus.campuscertify.domain.Category;
import com.campus.campuscertify.domain.Participant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for board data. Single-user local tool, so no synchronization.
 * LinkedHashMap gives O(1) lookup by id plus a stable display order from one structure.
 */
@Component
public class BoardState {

    private final Map<String, Activity> activities = buildActivities();
    private final Map<String, Participant> participants = new LinkedHashMap<>();

    public BoardState() {
        reset();
    }

    private static Map<String, Activity> buildActivities() {
        Map<String, Activity> fixed = new LinkedHashMap<>();
        for (Activity activity : List.of(
                new Activity("A01", "Emerging Tech Talk", Category.LEARN, 2),
                new Activity("A02", "Soldering Mini Lab", Category.BUILD, 3),
                new Activity("A03", "Project Pitch Circle", Category.SHARE, 2),
                new Activity("A04", "Open Source Clinic", Category.BUILD, 2))) {
            fixed.put(activity.id(), activity);
        }
        return Collections.unmodifiableMap(fixed);
    }

    /** Sole definition of the built-in rows, used by both the constructor and reset(). */
    private static List<Participant> seedParticipants() {
        return List.of(
                new Participant("C01", "Asha", List.of("A01", "A02", "A03")),
                new Participant("C02", "Bilal", List.of("A01", "A03", "A04")),
                new Participant("C03", "Chen", List.of("A01", "A02", "A04")),
                new Participant("C04", "Divya", List.of("A02", "A03", "A04")),
                new Participant("C05", "Eshan", List.of("A01", "A03")));
    }

    public List<Activity> activities() {
        return List.copyOf(activities.values());
    }

    public Map<String, Activity> activityIndex() {
        return activities;
    }

    public Optional<Activity> findActivity(String id) {
        return Optional.ofNullable(activities.get(id == null ? null : id.strip()));
    }

    public List<Participant> participants() {
        return new ArrayList<>(participants.values());
    }

    public Optional<Participant> findParticipant(String id) {
        return Optional.ofNullable(participants.get(id == null ? null : id.strip()));
    }

    public void upsert(Participant participant) {
        participants.put(participant.id(), participant);
    }

    public boolean remove(String id) {
        return participants.remove(id == null ? null : id.strip()) != null;
    }

    public void reset() {
        participants.clear();
        seedParticipants().forEach(this::upsert);
    }
}
