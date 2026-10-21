package com.doping.prescription.feed.application;

import com.doping.prescription.shared.error.DomainException;
import com.doping.prescription.shared.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * The opaque {@code nextCursor}: the session and the next position (docs/api.md, GET /api/feed).
 *
 * @param position position of the next card in the session
 */
record FeedCursor(UUID sessionId, int position) {

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String SEPARATOR = ":";

    static FeedCursor start() {
        return new FeedCursor(UUID.randomUUID(), 0);
    }

    static FeedCursor decode(String cursor) {
        try {
            String[] parts = new String(DECODER.decode(cursor), StandardCharsets.UTF_8).split(SEPARATOR, 2);
            int position = Integer.parseInt(parts[1]);
            if (position < 0) {
                throw new InvalidCursorException();
            }
            return new FeedCursor(UUID.fromString(parts[0]), position);
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            throw new InvalidCursorException();
        }
    }

    String encode() {
        return ENCODER.encodeToString((sessionId + SEPARATOR + position).getBytes(StandardCharsets.UTF_8));
    }

    FeedCursor advance(int cards) {
        return new FeedCursor(sessionId, position + cards);
    }

    /** A cursor the client did not get from us (docs/api.md: 400). */
    static final class InvalidCursorException extends DomainException {

        private static final long serialVersionUID = 1L;

        InvalidCursorException() {
            super(ErrorCode.INVALID_REQUEST, "Akış imleci geçersiz.");
        }
    }
}
