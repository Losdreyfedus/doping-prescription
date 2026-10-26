package com.doping.prescription.feed.application;

import com.doping.prescription.content.api.ContentCatalog;
import com.doping.prescription.feed.persistence.SavedContentQueries;
import com.doping.prescription.shared.error.NotFoundException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The bookmark on a Dopingle card (docs/api.md, PUT/DELETE /api/feed/saved/:contentId); both calls are idempotent. */
@Service
public class SavedContentService {

    private final SavedContentQueries saved;
    private final ContentCatalog content;
    private final Clock clock;

    SavedContentService(SavedContentQueries saved, ContentCatalog content, Clock clock) {
        this.saved = saved;
        this.content = content;
        this.clock = clock;
    }

    /** @throws NotFoundException if the video does not exist */
    @Transactional
    public void save(UUID userId, Integer contentId) {
        content.video(contentId).orElseThrow(() -> new NotFoundException("Video bulunamadı."));
        saved.save(userId, contentId, clock.instant());
    }

    @Transactional
    public void remove(UUID userId, Integer contentId) {
        saved.remove(userId, contentId);
    }
}
