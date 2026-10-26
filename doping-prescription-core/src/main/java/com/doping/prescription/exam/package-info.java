/** Mini exams: solving, submitting and publishing results. */
@ApplicationModule(
        displayName = "Exam",
        allowedDependencies = {"auth :: api", "content :: api", "shared"})
package com.doping.prescription.exam;

import org.springframework.modulith.ApplicationModule;
