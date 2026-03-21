package com.locpham.bookstore.orderservice.event;

import com.locpham.bookstore.orderservice.domain.OrderService;
import com.locpham.bookstore.orderservice.domain.OrderStatus;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderFunctions {
    private static final Logger logger = LoggerFactory.getLogger(OrderFunctions.class);

    @Bean
    public Consumer<OrderDispatchedMessage> dispatchOrder(OrderService orderService) {
        return orderDispatchedMessage -> {
            logger.info(
                    "The order with id {} has been dispatched", orderDispatchedMessage.orderId());
            orderService.updateOrderStatus(
                    orderDispatchedMessage.orderId(), OrderStatus.DISPATCHED);
        };
    }
}
