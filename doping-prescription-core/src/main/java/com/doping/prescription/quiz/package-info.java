/** Micro-quiz tokens and server-side answer checking. */
@ApplicationModule(
        displayName = "Quiz",
        allowedDependencies = {
            "auth :: api",
            "content :: api",
            "feed :: api",
            "learner :: api",
            "points :: api",
            "shared"
        })
package com.doping.prescription.quiz;

import org.springframework.modulith.ApplicationModule;
