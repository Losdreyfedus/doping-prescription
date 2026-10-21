package com.doping.prescription.admin.domain;

import java.util.UUID;

/**
 * A prepared demo state (docs/api.md, POST /api/admin/demo/persona; docs/mimari.md §10). The learner's data is erased
 * before {@link #apply}; a new persona is a new bean, selected by its {@link #name}.
 */
public interface DemoPersona {

    /** The name the API receives, e.g. {@code SCRIPTED_IN_PROGRESS}. */
    String name();

    /** Brings the freshly erased learner into this persona's state through the modules' public APIs. */
    void apply(UUID userId);
}
