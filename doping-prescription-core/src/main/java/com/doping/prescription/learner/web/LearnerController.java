package com.doping.prescription.learner.web;

import com.doping.prescription.auth.api.CurrentUser;
import com.doping.prescription.learner.application.ProfileViewService;
import com.doping.prescription.learner.application.ProfileViewService.ProfileView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Learner endpoints of docs/api.md; the user always comes from the token. */
@RestController
class LearnerController {

    private final ProfileViewService profiles;

    LearnerController(ProfileViewService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/api/learner/profile")
    ProfileView profile(CurrentUser user) {
        return profiles.profile(user.userId());
    }
}
