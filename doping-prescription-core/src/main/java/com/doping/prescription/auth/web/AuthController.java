package com.doping.prescription.auth.web;

import com.doping.prescription.auth.api.CurrentUser;
import com.doping.prescription.auth.application.DemoLoginService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** The current user (docs/api.md); available in every profile, unlike {@link DemoLoginController}. */
@RestController
class AuthController {

    private final DemoLoginService login;
    private final AuthWebMapper mapper;

    AuthController(DemoLoginService login, AuthWebMapper mapper) {
        this.login = login;
        this.mapper = mapper;
    }

    @GetMapping("/api/me")
    AuthDtos.MeResponse me(CurrentUser user) {
        return mapper.toMe(login.me(user.userId()));
    }
}
