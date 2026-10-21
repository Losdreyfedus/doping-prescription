package com.doping.prescription.exam.application;

import com.doping.prescription.shared.error.DomainException;
import com.doping.prescription.shared.error.ErrorCode;

/** Writing to, or submitting again, an attempt that was already submitted (docs/api.md: 409). */
public class AttemptAlreadySubmittedException extends DomainException {

    private static final long serialVersionUID = 1L;

    public AttemptAlreadySubmittedException() {
        super(ErrorCode.ATTEMPT_ALREADY_SUBMITTED, "Bu deneme zaten gönderildi.");
    }
}
