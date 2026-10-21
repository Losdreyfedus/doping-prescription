package com.doping.prescription.quiz.application;

import com.doping.prescription.shared.error.DomainException;
import com.doping.prescription.shared.error.ErrorCode;

/** The quiz errors of docs/api.md (Hata kodları). */
final class QuizErrors {

    private QuizErrors() {}

    static final class TokenInvalid extends DomainException {

        private static final long serialVersionUID = 1L;

        TokenInvalid() {
            super(ErrorCode.QUIZ_TOKEN_INVALID, "Soru oturumu geçersiz.");
        }
    }

    static final class TokenExpired extends DomainException {

        private static final long serialVersionUID = 1L;

        TokenExpired() {
            super(ErrorCode.QUIZ_EXPIRED, "Sorunun süresi doldu.");
        }
    }

    static final class VideoNotWatched extends DomainException {

        private static final long serialVersionUID = 1L;

        VideoNotWatched() {
            super(ErrorCode.VIDEO_NOT_WATCHED, "Soruyu açmak için videoyu izlemelisin.");
        }
    }

    static final class RateLimited extends DomainException {

        private static final long serialVersionUID = 1L;

        RateLimited() {
            super(ErrorCode.RATE_LIMITED, "Çok hızlı cevap veriyorsun, biraz bekle.");
        }
    }

    static final class UnknownOption extends DomainException {

        private static final long serialVersionUID = 1L;

        UnknownOption() {
            super(ErrorCode.INVALID_REQUEST, "Seçilen şık geçersiz.");
        }
    }
}
