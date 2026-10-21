package com.doping.prescription.shared.error;

/** A record does not exist or belongs to another user; both cases answer 404 so ownership is not revealed. */
public class NotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String userMessage) {
        super(ErrorCode.NOT_FOUND, userMessage);
    }
}
