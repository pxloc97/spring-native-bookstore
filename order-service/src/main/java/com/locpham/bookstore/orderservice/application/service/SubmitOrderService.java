package com.locpham.bookstore.orderservice.application.service;

import com.locpham.bookstore.orderservice.application.command.SubmitOrderCommand;
import com.locpham.bookstore.orderservice.application.port.in.SubmitOrderUseCase;
import com.locpham.bookstore.orderservice.application.port.out.CatalogBookPort;
import com.locpham.bookstore.orderservice.application.port.out.OrderCommandPort;
import com.locpham.bookstore.orderservice.application.port.out.OrderEventPublisherPort;
import com.locpham.bookstore.orderservice.domain.model.BookSnapshot;
import com.locpham.bookstore.orderservice.domain.model.Order;
import com.locpham.bookstore.orderservice.domain.model.OrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
public class SubmitOrderService implements SubmitOrderUseCase {
    private final CatalogBookPort catalogBookPort;
    private final OrderCommandPort orderCommandPort;
    private final OrderEventPublisherPort eventPublisher;

    public SubmitOrderService(
            CatalogBookPort catalogBookPort,
            OrderCommandPort orderCommandPort,
            OrderEventPublisherPort eventPublisher) {
        this.catalogBookPort = catalogBookPort;
        this.orderCommandPort = orderCommandPort;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public Mono<Order> submitOrder(SubmitOrderCommand command) {
        return catalogBookPort
                .loadBook(command.isbn())
                .map(book -> buildPendingOrder(book, command.quantity()))
                .switchIfEmpty(Mono.just(buildRejectedOrder(command.isbn(), command.quantity())))
                .flatMap(orderCommandPort::save)
                .doOnNext(
                        order -> {
                            if (order.status() == OrderStatus.PENDING) {
                                eventPublisher.publishOrderCreated(order).subscribe();
                            }
                        });
    }

    private Order buildRejectedOrder(String isbn, int quantity) {
        return Order.createRejected(isbn, null, 0.0, quantity);
    }

    private Order buildPendingOrder(BookSnapshot book, int quantity) {
        return Order.createPending(book.isbn(), book.title(), book.price(), quantity);
    }
}
