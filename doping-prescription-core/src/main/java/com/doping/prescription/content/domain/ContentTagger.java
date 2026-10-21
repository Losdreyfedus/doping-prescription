package com.doping.prescription.content.domain;

import java.util.List;

/**
 * Suggests topics for a video (extension point, docs/mimari.md §10). Implementations are deterministic rules; runtime
 * LLMs are not allowed (AGENTS.md rule 1).
 */
public interface ContentTagger {

    /**
     * @param dictionary the editor-maintained keyword dictionary; implementations may ignore it
     * @return suggested topics, best first
     */
    List<TagSuggestion> suggest(String title, String description, List<TaggingKeyword> dictionary);
}
