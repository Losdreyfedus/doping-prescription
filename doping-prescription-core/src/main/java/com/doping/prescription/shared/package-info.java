/**
 * Shared infrastructure: error model, event envelope, idempotency and time.
 *
 * <p>Open module so other modules can use these types outside an {@code api} package. It must never contain
 * business logic (docs/adr/0005-modul-sinirlari-netlestirme.md).
 */
@ApplicationModule(displayName = "Shared", type = ApplicationModule.Type.OPEN)
package com.doping.prescription.shared;

import org.springframework.modulith.ApplicationModule;
