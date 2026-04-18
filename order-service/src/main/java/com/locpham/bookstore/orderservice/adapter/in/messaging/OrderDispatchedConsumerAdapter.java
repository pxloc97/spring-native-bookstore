package com.locpham.bookstore.orderservice.adapter.in.messaging;

import com.locpham.bookstore.orderservice.application.command.MarkOrderDispatchedCommand;
import com.locpham.bookstore.orderservice.application.port.in.MarkOrderDispatchedUseCase;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

@Configuration
public class OrderDispatchedConsumerAdapter {
    private static final Logger logger =
            LoggerFactory.getLogger(OrderDispatchedConsumerAdapter.class);

    @Bean
    public Consumer<Flux<OrderDispatchedMessage>> dispatchOrder(
            MarkOrderDispatchedUseCase markOrderDispatchedUseCase) {
        return flux ->
                flux.map(message -> new MarkOrderDispatchedCommand(message.orderId()))
                        .flatMap(markOrderDispatchedUseCase::markOrderDispatched)
                        .doOnNext(
                                order ->
                                        logger.info(
                                                "The order with id {} is dispatched", order.id()))
                        .subscribe();
    }
}
