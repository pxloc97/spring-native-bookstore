package com.locpham.bookstore.dispatcherservice.function;

import com.locpham.bookstore.dispatcherservice.message.OrderAcceptedMessage;
import com.locpham.bookstore.dispatcherservice.message.OrderDispatchedMessage;
import java.util.function.Function;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
public class DispatcherFunctions {
    private static final Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger(DispatcherFunctions.class);

    @Bean
    public Function<OrderAcceptedMessage, Long> pack() {
        return orderAcceptedMessage -> {
            LOGGER.info("The order with id {} is packed", orderAcceptedMessage.orderId());
            return orderAcceptedMessage.orderId();
        };
    }

    @Bean
    public Function<Flux<Long>, Flux<OrderDispatchedMessage>> label() {
        return orderFlux ->
                orderFlux.map(
                        orderId -> {
                            LOGGER.info("The order id {} is labeled", orderId);
                            return new OrderDispatchedMessage(orderId);
                        });
    }
}
