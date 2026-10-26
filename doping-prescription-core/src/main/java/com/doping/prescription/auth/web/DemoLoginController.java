package com.doping.prescription.auth.web;

import com.doping.prescription.auth.application.AuthProperties;
import com.doping.prescription.auth.application.DemoLoginService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Passwordless demo login (docs/api.md). Anyone can become any user here, so the endpoints exist only where
 * {@code prescription.auth.demo-login-enabled} is true (the demo profile); elsewhere they answer 404.
 */
@RestController
@ConditionalOnBooleanProperty(AuthProperties.DEMO_LOGIN_ENABLED)
class DemoLoginController {

    private final DemoLoginService login;
    private final AuthWebMapper mapper;

    DemoLoginController(DemoLoginService login, AuthWebMapper mapper) {
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
}
