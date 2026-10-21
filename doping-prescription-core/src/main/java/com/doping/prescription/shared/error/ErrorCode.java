package com.doping.prescription.shared.error;

/** Machine-readable error codes of the API (docs/api.md, Hata kodları) and their HTTP status. */
public enum ErrorCode {
    INVALID_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    QUIZ_TOKEN_INVALID(403),
    NOT_FOUND(404),
    ATTEMPT_ALREADY_SUBMITTED(409),
    VIDEO_NOT_WATCHED(409),
    QUIZ_EXPIRED(410),
    RATE_LIMITED(429),
    INTERNAL_ERROR(500);

    private final int httpStatus;

    ErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public int httpStatus() {
        return httpStatus;
    }
}
