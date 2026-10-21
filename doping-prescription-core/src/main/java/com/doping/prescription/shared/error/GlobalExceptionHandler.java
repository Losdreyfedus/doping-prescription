package com.doping.prescription.shared.error;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns every error into the problem detail format of docs/api.md; unexpected errors reveal nothing to the client. */
@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String INVALID_REQUEST_DETAIL = "İstek geçersiz.";
    static final String NOT_FOUND_DETAIL = "Kayıt bulunamadı.";
    static final String UNEXPECTED_DETAIL = "Beklenmeyen bir hata oluştu. Lütfen tekrar dene.";

    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException exception) {
        return ProblemDetails.of(exception.code(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException exception) {
        List<Map<String, String>> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        return ProblemDetails.invalid(INVALID_REQUEST_DETAIL, fieldErrors);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail unreadable() {
        return ProblemDetails.of(ErrorCode.INVALID_REQUEST, INVALID_REQUEST_DETAIL);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail noSuchEndpoint() {
        return ProblemDetails.of(ErrorCode.NOT_FOUND, NOT_FOUND_DETAIL);
    }

    /** Last resort: log with the request id, answer without details. */
    @ExceptionHandler(RuntimeException.class)
    ProblemDetail unexpected(RuntimeException exception) {
        log.error("Unexpected error requestId={}", MDC.get(RequestIdFilter.MDC_KEY), exception);
        return ProblemDetails.of(ErrorCode.INTERNAL_ERROR, UNEXPECTED_DETAIL);
    }
}
