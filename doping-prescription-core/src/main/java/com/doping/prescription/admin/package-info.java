/** Demo control, feed debugging and content management APIs. */
@ApplicationModule(
        displayName = "Admin",
        allowedDependencies = {
            "auth :: api",
            "content :: api",
            "exam :: api",
            "feed :: api",
            "learner :: api",
            "points :: api",
            "quiz :: api",
            "shared"
        })
package com.doping.prescription.admin;

import org.springframework.modulith.ApplicationModule;
