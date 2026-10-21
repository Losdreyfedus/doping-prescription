package com.doping.prescription.shared.error;

/**
 * Base of all expected business errors. The message is Turkish and is shown to the user as the problem detail
 * (docs/api.md), so it must never contain personal data or answer keys.
 */
public abstract class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    /** @param code never null; subclasses pass a constant */
    protected DomainException(ErrorCode code, String userMessage) {
        super(userMessage);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
