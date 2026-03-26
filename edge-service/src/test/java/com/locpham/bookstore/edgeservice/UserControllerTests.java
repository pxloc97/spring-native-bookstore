package com.locpham.bookstore.edgeservice;

import com.locpham.bookstore.edgeservice.security.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserControllerTests {

    @Test
    void whenAuthenticatedThenReturnUser() {
        var controller = new UserController();
        var oidcUser = mock(OidcUser.class);
        when(oidcUser.getPreferredUsername()).thenReturn("jon.snow");
        when(oidcUser.getGivenName()).thenReturn("Jon");
        when(oidcUser.getFamilyName()).thenReturn("Snow");

        var user = controller.getUser(oidcUser).block();

        assertThat(user).isEqualTo(new User("jon.snow", "Jon", "Snow", List.of("employee", "customer")));
    }
}
