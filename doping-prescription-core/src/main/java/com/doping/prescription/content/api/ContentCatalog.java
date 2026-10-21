package com.doping.prescription.content.api;

import java.util.Map;

/** Read access to the video catalogue for other modules (docs/mimari.md §1). */
public interface ContentCatalog {

    /**
     * Number of LIVE videos per topic, for "Bu konudan N video hazır". A video counts for its primary topic: the
     * topic with the highest weight in content_topic_map (docs/algoritma.md §2). Topics without videos are absent.
     */
    Map<Integer, Integer> liveVideoCountByTopic();
}
