package com.locpham.bookstore.orderservice.application.service;

import static org.mockito.BDDMockito.given;

import com.locpham.bookstore.orderservice.application.port.out.OrderQueryPort;
import com.locpham.bookstore.orderservice.application.query.GetOrdersQuery;
import com.locpham.bookstore.orderservice.domain.model.Order;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class GetOrdersServiceTest {

    @Mock private OrderQueryPort orderQueryPort;
    @InjectMocks private GetOrdersService getOrdersService;

    private static final String USER_ID = "123";

    @Nested
    class GetOrders {
        @Test
        void whenOrdersExist_shouldReturnOrders() {
            var command = new GetOrdersQuery(USER_ID);

            given(orderQueryPort.findByCreatedBy("123"))
                    .willReturn(
                            Flux.just(
                                    Order.createAccepted("123", "Title", 9.99, 2),
                                    Order.createAccepted("124", "Title", 9.99, 2)));

            StepVerifier.create(getOrdersService.getOrders(command))
                    .expectNextCount(2)
                    .verifyComplete();
        }

        @Test
        void whenOrderNotExist() {
            var command = new GetOrdersQuery(USER_ID);

            given(orderQueryPort.findByCreatedBy(USER_ID)).willReturn(Flux.empty());

            StepVerifier.create(getOrdersService.getOrders(command))
                    .expectNextCount(0)
                    .verifyComplete();
        }
    }
}
