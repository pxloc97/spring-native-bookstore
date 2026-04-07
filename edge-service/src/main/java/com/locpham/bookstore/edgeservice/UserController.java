package com.locpham.bookstore.edgeservice;

import com.locpham.bookstore.edgeservice.security.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class UserController {
    public static final String ROLE_CLAIMS = "roles";

    @GetMapping("user")
    public Mono<User> getUser(@AuthenticationPrincipal OidcUser oidcUser) {
        var user =
                new User(
                        oidcUser.getPreferredUsername(),
                        oidcUser.getGivenName(),
                        oidcUser.getFamilyName(),
                        oidcUser.getClaimAsStringList(ROLE_CLAIMS));
        return Mono.just(user);
    }
}
