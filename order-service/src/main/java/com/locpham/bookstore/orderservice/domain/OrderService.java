package com.locpham.bookstore.orderservice.domain;

import com.locpham.bookstore.orderservice.book.Book;
import com.locpham.bookstore.orderservice.book.BookClient;
import com.locpham.bookstore.orderservice.event.OrderAcceptedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final BookClient bookClient;
    private final OrderRepository orderRepository;
    private final StreamBridge streamBridge;

    public OrderService(
            BookClient bookClient, OrderRepository orderRepository, StreamBridge streamBridge) {
        this.bookClient = bookClient;
        this.orderRepository = orderRepository;
        this.streamBridge = streamBridge;
    }

    public Flux<Order> findAll() {
        return orderRepository.findAll();
    }

    public Mono<Order> submitOrder(String isbn, int quantity) {
        return bookClient
                .getBookByIsbn(isbn)
                .map(book -> buildAcceptedOrder(book, quantity))
                .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
                .flatMap(orderRepository::save)
                .doOnNext(this::publishOrderAcceptedEvent);
    }

    public static Order buildRejectedOrder(String isbn, int quantity) {
        return Order.build(isbn, "", 0.0, quantity, OrderStatus.REJECTED);
    }

    public static Order buildAcceptedOrder(Book book, int quantity) {
        return Order.build(book.isbn(), "", book.price(), quantity, OrderStatus.ACCEPTED);
    }

    public void updateOrderStatus(Long orderId, OrderStatus status) {
        orderRepository
                .findById(orderId)
                .map(
                        existingOrder ->
                                new Order(
                                        existingOrder.id(),
                                        existingOrder.bookIsbn(),
                                        existingOrder.bookName(),
                                        existingOrder.bookPrice(),
                                        existingOrder.quantity(),
                                        status,
                                        existingOrder.createdDate(),
                                        existingOrder.lastModifiedDate(),
                                        existingOrder.version()))
                .flatMap(orderRepository::save)
                .subscribe();
    }

    public void publishOrderAcceptedEvent(Order order) {
        if (!order.status().equals(OrderStatus.ACCEPTED)) {
            return;
        }

        OrderAcceptedMessage orderAcceptedMessage = new OrderAcceptedMessage(order.id());
        log.info("Sending order accepted event with id: {}", order.id());
        var result = streamBridge.send("order-accepted", orderAcceptedMessage);
        log.info("Result of sending data for order with id {}: {}", order.id(), result);
    }
}
