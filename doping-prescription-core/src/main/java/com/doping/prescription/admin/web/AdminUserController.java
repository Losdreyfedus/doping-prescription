package com.doping.prescription.admin.web;

import com.doping.prescription.auth.api.UserDirectory;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Users for the admin demo control (docs/api.md); ADMIN only, enforced by the security rules on /api/admin/**. */
@RestController
class AdminUserController {

    private final UserDirectory users;

    AdminUserController(UserDirectory users) {
        this.users = users;
    }

    @GetMapping("/api/admin/users")
    List<UserDirectory.UserSummary> users() {
        return users.allUsers();
    }
}
