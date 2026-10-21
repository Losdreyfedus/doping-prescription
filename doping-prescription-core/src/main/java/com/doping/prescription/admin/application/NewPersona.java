package com.doping.prescription.admin.application;

import com.doping.prescription.admin.domain.DemoPersona;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** {@code NEW}: no data at all; the erase before {@link #apply} is the whole persona. */
@Component
class NewPersona implements DemoPersona {

    @Override
    public String name() {
        return "NEW";
    }

    @Override
    public void apply(UUID userId) {
        // nothing to add
    }
}
