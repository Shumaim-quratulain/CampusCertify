package com.campus.campuscertify.domain;

public record ValidationError(ErrorCode code, String participantId, String offendingValue) {
}
