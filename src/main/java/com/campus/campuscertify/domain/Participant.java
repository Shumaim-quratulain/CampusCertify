package com.campus.campuscertify.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable because the UI edits participants live.
 * Normalizes (trims) on every write so all downstream comparisons see canonical values.
 */
public class Participant {

    private final String id;
    private String name;
    private List<String> completedActivityIds;

    public Participant(String id, String name, List<String> completedActivityIds) {
        this.id = trim(id);
        this.name = trim(name);
        this.completedActivityIds = normalizeIds(completedActivityIds);
    }

    public Participant copy() {
        return new Participant(id, name, completedActivityIds);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = trim(name);
    }

    /** List, not Set: a Set would silently swallow a repeat and hide DUPLICATE_PARTICIPATION. */
    public List<String> completedActivityIds() {
        return Collections.unmodifiableList(completedActivityIds);
    }

    public void setCompletedActivityIds(List<String> ids) {
        this.completedActivityIds = normalizeIds(ids);
    }

    private static String trim(String value) {
        return value == null ? "" : value.strip();
    }

    /** Blank tokens come from trailing commas in the UI input, not from real data. */
    private static List<String> normalizeIds(List<String> ids) {
        List<String> normalized = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                String trimmed = trim(id);
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }
        return normalized;
    }
}
