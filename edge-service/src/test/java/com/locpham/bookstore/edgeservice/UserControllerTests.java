package com.locpham.bookstore.edgeservice;

import com.locpham.bookstore.edgeservice.security.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.locpham.bookstore.edgeservice.UserController.ROLE_CLAIMS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.config.enabled=false"
)
class UserControllerTests {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webClient;

    @MockitoBean
    private ReactiveClientRegistrationRepository clientRegistrationRepository;

    @BeforeEach
    void setupClientRegistration() {
        webClient = WebTestClient
                .bindToApplicationContext(context)
                .apply(SecurityMockServerConfigurers.springSecurity())
                .configureClient()
                .build();

        when(clientRegistrationRepository.findByRegistrationId(anyString()))
                .thenReturn(Mono.just(testClientRegistration()));
    }

    @Test
    void whenNotAuthenticatedThen401() {
        webClient
                .get()
                .uri("/user")
                .exchange()
                .expectStatus()
                .isFound();
    }

    @Test
    void whenAuthenticatedThenReturnUser() {
        var expectedUser = new User("jon.snow", "Jon", "Snow", List.of("employee", "customer"));
        var oidcUser = mock(OidcUser.class);
        when(oidcUser.getPreferredUsername()).thenReturn(expectedUser.username());
        when(oidcUser.getGivenName()).thenReturn(expectedUser.firstName());
        when(oidcUser.getFamilyName()).thenReturn(expectedUser.lastName());
        when(oidcUser.getClaimAsStringList(ROLE_CLAIMS)).thenReturn(expectedUser.roles());
        var authentication = new TestingAuthenticationToken(
                oidcUser,
                null,
                new SimpleGrantedAuthority("ROLE_USER")
        );
        authentication.setAuthenticated(true);

        webClient
                .mutateWith(configureMockOidcLogin(expectedUser))
                .mutateWith(SecurityMockServerConfigurers.mockAuthentication(authentication))
                .get()
                .uri("/user")
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(User.class)
                .value(user -> assertThat(user).isEqualTo(expectedUser));
    }

    private SecurityMockServerConfigurers.OidcLoginMutator configureMockOidcLogin(User expectedUser) {
        return SecurityMockServerConfigurers.mockOidcLogin().idToken(builder -> {
            builder.claim(StandardClaimNames.PREFERRED_USERNAME, expectedUser.username());
            builder.claim(StandardClaimNames.GIVEN_NAME, expectedUser.firstName());
            builder.claim(StandardClaimNames.FAMILY_NAME, expectedUser.lastName());
            builder.claim(ROLE_CLAIMS, expectedUser.roles());
        });
    }

    private ClientRegistration testClientRegistration() {
        return ClientRegistration.withRegistrationId("test")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("test")
                .authorizationUri("https://sso.example.com/auth")
                .tokenUri("https://sso.example.com/token")
                .redirectUri("https://bookstore.example.com")
                .build();
    }
}
