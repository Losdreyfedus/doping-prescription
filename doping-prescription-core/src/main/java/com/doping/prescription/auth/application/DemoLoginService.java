package com.doping.prescription.auth.application;

import com.doping.prescription.auth.api.UserDirectory;
import com.doping.prescription.auth.domain.Role;
import com.doping.prescription.auth.persistence.UserAccount;
import com.doping.prescription.auth.persistence.UserAccountRepository;
import com.doping.prescription.shared.error.NotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Demo login: pick a user, get a token. There are no passwords (AGENTS.md §3). */
@Service
public class DemoLoginService implements UserDirectory {

    static final String USER_NOT_FOUND = "Kullanıcı bulunamadı.";

    /** Students first (in seed order), admin last. */
    private static final Comparator<UserAccount> DISPLAY_ORDER = Comparator.comparing(
                    (UserAccount user) -> user.getRole() == Role.ADMIN)
            .thenComparing(UserAccount::getId);

    private final UserAccountRepository users;
    private final TokenService tokens;

    DemoLoginService(UserAccountRepository users, TokenService tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @Transactional(readOnly = true)
    public List<UserAccount> demoUsers() {
        return users.findAll().stream().sorted(DISPLAY_ORDER).toList();
    }

    @Transactional(readOnly = true)
    public LoginResult login(String slug) {
        UserAccount user = users.findBySlug(slug).orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
        return new LoginResult(tokens.issue(user), user);
    }

    @Transactional(readOnly = true)
    public UserAccount me(UUID userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummary> allUsers() {
        return demoUsers().stream().map(DemoLoginService::summary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> findBySlug(String slug) {
        return users.findBySlug(slug).map(DemoLoginService::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSummary> findById(UUID userId) {
        return users.findById(userId).map(DemoLoginService::summary);
    }

    private static UserSummary summary(UserAccount user) {
        return new UserSummary(
                user.getId(),
                user.getSlug(),
                user.getDisplayName(),
                user.getRole().name(),
                user.isPersonalizationEnabled());
    }

    /**
     * @param token signed session token
     * @param user  the logged-in user
     */
    public record LoginResult(String token, UserAccount user) {}
}
