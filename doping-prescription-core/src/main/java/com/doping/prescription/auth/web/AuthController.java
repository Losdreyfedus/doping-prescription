package com.doping.prescription.auth.web;

import com.doping.prescription.auth.api.CurrentUser;
import com.doping.prescription.auth.application.DemoLoginService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Demo login and the current user (docs/api.md). */
@RestController
class AuthController {

    private final DemoLoginService login;
    private final AuthWebMapper mapper;

    AuthController(DemoLoginService login, AuthWebMapper mapper) {
        this.login = login;
        this.mapper = mapper;
    }

    @GetMapping("/api/auth/demo-users")
    List<AuthDtos.DemoUserResponse> demoUsers() {
        return login.demoUsers().stream().map(mapper::toDemoUser).toList();
    }

    @PostMapping("/api/auth/demo-login")
    AuthDtos.DemoLoginResponse demoLogin(@Valid @RequestBody AuthDtos.DemoLoginRequest request) {
        DemoLoginService.LoginResult result = login.login(request.slug());
        return new AuthDtos.DemoLoginResponse(result.token(), mapper.toMe(result.user()));
    }

    @GetMapping("/api/me")
    AuthDtos.MeResponse me(CurrentUser user) {
        return mapper.toMe(login.me(user.userId()));
    }
}
