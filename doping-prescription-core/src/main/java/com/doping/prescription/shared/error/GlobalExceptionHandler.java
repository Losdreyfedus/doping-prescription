package com.doping.prescription.shared.error;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into the problem detail format of docs/api.md; unexpected errors reveal nothing to the client.
 * Spring MVC's own errors (missing parameter, unreadable body, wrong method or content type, unknown path) come through
 * {@link ResponseEntityExceptionHandler} and get a {@code code} and a Turkish detail like every other error.
 */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String INVALID_REQUEST_DETAIL = "İstek geçersiz.";
    static final String NOT_FOUND_DETAIL = "Kayıt bulunamadı.";
    static final String METHOD_NOT_ALLOWED_DETAIL = "Bu adres bu istek yöntemini desteklemiyor.";
    static final String UNSUPPORTED_MEDIA_TYPE_DETAIL = "İstek gövdesi JSON olmalı.";
    static final String UNEXPECTED_DETAIL = "Beklenmeyen bir hata oluştu. Lütfen tekrar dene.";

    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException exception) {
        return ProblemDetails.of(exception.code(), exception.getMessage());
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<Map<String, String>> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        ProblemDetail problem = ProblemDetails.invalid(INVALID_REQUEST_DETAIL, fieldErrors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    /**
     * Every framework error ends here: its English problem detail is replaced by ours, keeping the status and headers
     * (e.g. {@code Allow} on 405). Bodies that already carry a code, like the validation error above, pass unchanged.
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("Unexpected framework error requestId={}", MDC.get(RequestIdFilter.MDC_KEY), exception);
        }
        Object problem = hasCode(body) ? body : frameworkProblem(status);
        return super.handleExceptionInternal(exception, problem, headers, status, request);
    }

    /** Last resort: log with the request id, answer without details. */
    @ExceptionHandler(RuntimeException.class)
    ProblemDetail unexpected(RuntimeException exception) {
        log.error("Unexpected error requestId={}", MDC.get(RequestIdFilter.MDC_KEY), exception);
        return ProblemDetails.of(ErrorCode.INTERNAL_ERROR, UNEXPECTED_DETAIL);
    }

    private static boolean hasCode(@Nullable Object body) {
        if (!(body instanceof ProblemDetail problem)) {
            return false;
        }
        Map<String, Object> properties = problem.getProperties();
        return properties != null && properties.containsKey(ProblemDetails.CODE);
    }

    /**
     * Statuses without an own code keep their status: 4xx are reported as an invalid request, 5xx as unexpected.
     */
    private static ProblemDetail frameworkProblem(HttpStatusCode status) {
        return switch (HttpStatus.resolve(status.value())) {
            case BAD_REQUEST -> ProblemDetails.of(ErrorCode.INVALID_REQUEST, INVALID_REQUEST_DETAIL);
            case NOT_FOUND -> ProblemDetails.of(ErrorCode.NOT_FOUND, NOT_FOUND_DETAIL);
            case METHOD_NOT_ALLOWED -> ProblemDetails.of(ErrorCode.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED_DETAIL);
            case UNSUPPORTED_MEDIA_TYPE ->
                ProblemDetails.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE, UNSUPPORTED_MEDIA_TYPE_DETAIL);
            case null, default ->
                status.is5xxServerError()
                        ? ProblemDetails.of(status, ErrorCode.INTERNAL_ERROR, UNEXPECTED_DETAIL)
                        : ProblemDetails.of(status, ErrorCode.INVALID_REQUEST, INVALID_REQUEST_DETAIL);
        };
    }
}
