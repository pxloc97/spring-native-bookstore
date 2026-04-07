package com.locpham.bookstore.orderservice.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.locpham.bookstore.orderservice.config.SecurityConfig;
import com.locpham.bookstore.orderservice.domain.Order;
import com.locpham.bookstore.orderservice.domain.OrderService;
import com.locpham.bookstore.orderservice.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerWebFluxTests {

    @Autowired WebTestClient webClient;

    @MockitoBean OrderService orderService;

    @MockitoBean ReactiveJwtDecoder reactiveJwtDecoder;

    @Test
    void whenBookNotAvailableThenRejectOrder() {
        var orderRequest = new OrderRequest("1234567890", 3);
        var expectedOrder =
                OrderService.buildRejectedOrder(orderRequest.isbn(), orderRequest.quantity());
        given(orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity()))
                .willReturn(Mono.just(expectedOrder));
        given(reactiveJwtDecoder.decode(anyString()))
                .willReturn(
                        Mono.just(
                                Jwt.withTokenValue("token")
                                        .header("alg", "none")
                                        .claim("sub", "user-id")
                                        .build()));

        webClient
                .post()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth("token"))
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBody(Order.class)
                .value(
                        actualOrder -> {
                            assertThat(actualOrder).isNotNull();
                            assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
                        });
    }

    @Test
    void whenGetOrdersThenReturnOnlyCurrentUserOrders() {
        var order =
                new Order(
                        1L,
                        "1234567890",
                        "Book",
                        9.9,
                        1,
                        OrderStatus.ACCEPTED,
                        null,
                        null,
                        "user-id",
                        "user-id",
                        0);
        given(orderService.getAllOrders("user-id")).willReturn(Flux.just(order));
        given(reactiveJwtDecoder.decode(anyString()))
                .willReturn(
                        Mono.just(
                                Jwt.withTokenValue("token")
                                        .header("alg", "none")
                                        .claim("sub", "user-id")
                                        .build()));

        webClient
                .get()
                .uri("/orders")
                .headers(headers -> headers.setBearerAuth("token"))
                .exchange()
                .expectStatus()
                .is2xxSuccessful()
                .expectBodyList(Order.class)
                .value(orders -> assertThat(orders).hasSize(1).containsExactly(order));
    }

    @Test
    void whenSubmitOrderWithoutTokenThenUnauthorized() {
        var orderRequest = new OrderRequest("1234567890", 3);

        webClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus()
                .isUnauthorized();
    }
}
