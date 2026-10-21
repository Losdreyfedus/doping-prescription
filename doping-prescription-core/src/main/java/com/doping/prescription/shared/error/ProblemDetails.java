package com.doping.prescription.shared.error;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Builds the RFC 9457 problem details of docs/api.md: standard fields plus {@code code}. */
public final class ProblemDetails {

    public static final String CODE = "code";
    public static final String ERRORS = "errors";

    private ProblemDetails() {}

    public static ProblemDetail of(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(code.httpStatus()), detail);
        problem.setProperty(CODE, code.name());
        return problem;
    }

    /** A validation error with one entry per invalid field. */
    public static ProblemDetail invalid(String detail, List<Map<String, String>> fieldErrors) {
        ProblemDetail problem = of(ErrorCode.INVALID_REQUEST, detail);
        problem.setProperty(ERRORS, fieldErrors);
        return problem;
    }
}
