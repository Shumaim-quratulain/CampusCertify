package com.campus.campuscertify.web;

import com.campus.campuscertify.domain.Participant;
import java.util.List;

/** Carries raw, untrimmed input from the browser; normalization happens in the Participant constructor. */
public record ParticipantDto(String id, String name, List<String> completedActivityIds) {

    public static ParticipantDto from(Participant participant) {
        return new ParticipantDto(participant.id(), participant.name(), participant.completedActivityIds());
    }

    public Participant toDomain() {
        return new Participant(id, name, completedActivityIds);
    }

    public Participant toDomain(String idOverride) {
        return new Participant(idOverride, name, completedActivityIds);
    }
}
