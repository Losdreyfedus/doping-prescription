package com.doping.prescription.feed.web;

import com.doping.prescription.auth.api.CurrentUser;
import com.doping.prescription.feed.application.FeedService;
import com.doping.prescription.feed.application.FeedViews.FeedPage;
import com.doping.prescription.feed.application.InteractionService;
import com.doping.prescription.feed.application.InteractionService.Interaction;
import com.doping.prescription.feed.domain.InteractionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Feed endpoints of docs/api.md; the user always comes from the token. */
@RestController
class FeedController {

    /** docs/api.md: limit defaults to 10, at most 20 (prescription.feed.page-size / page-size-max). */
    private static final int MAX_LIMIT = 20;

    private static final int MAX_BATCH = 100;

    private final FeedService feed;
    private final InteractionService interactions;

    FeedController(FeedService feed, InteractionService interactions) {
        this.feed = feed;
        this.interactions = interactions;
    }

    @GetMapping("/api/feed")
    FeedPage feed(
            CurrentUser user,
            @RequestParam(required = false) @Nullable String cursor,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_LIMIT) int limit,
            @RequestParam(required = false) @Nullable Integer topicId,
            @RequestParam(defaultValue = "false") boolean refresh) {
        return feed.page(user.userId(), cursor, limit, topicId, refresh);
    }

    @PostMapping("/api/feed/interactions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void interactions(CurrentUser user, @Valid @RequestBody InteractionBatch batch) {
        interactions.record(
                user.userId(),
                batch.events().stream()
                        .map(event -> new Interaction(
                                event.clientEventId(),
                                event.impressionId(),
                                event.event(),
                                event.watchMs(),
                                event.clientTs()))
                        .toList());
    }

    record InteractionBatch(@NotEmpty @Size(max = MAX_BATCH) List<@Valid InteractionEvent> events) {}

    /**
     * @param watchMs real video milliseconds watched, if relevant for the event
     */
    record InteractionEvent(
            @NotNull @Size(min = 1, max = 64) String clientEventId,
            @NotNull UUID impressionId,
            @NotNull InteractionType event,

            @PositiveOrZero @Schema(nullable = true) @Nullable
            Integer watchMs,

            @NotNull Instant clientTs) {}
}
