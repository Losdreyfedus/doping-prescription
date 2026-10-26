/** Candidate pools, ranking, feed queue and interactions. */
@ApplicationModule(
        displayName = "Feed",
        allowedDependencies = {"auth :: api", "content :: api", "learner :: api", "points :: api", "shared"})
package com.doping.prescription.feed;

import org.springframework.modulith.ApplicationModule;
