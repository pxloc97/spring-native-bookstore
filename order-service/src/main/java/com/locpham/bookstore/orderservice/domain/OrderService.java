package com.locpham.bookstore.orderservice.domain;

import com.locpham.bookstore.orderservice.book.Book;
import com.locpham.bookstore.orderservice.book.BookClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class OrderService {
    private final BookClient bookClient;
    private final OrderRepository orderRepository;

    public OrderService(BookClient bookClient,OrderRepository orderRepository) {
        this.bookClient = bookClient;
        this.orderRepository = orderRepository;
    }

    public Flux<Order> findAll() {
        return orderRepository.findAll();
    }

    public Mono<Order> submitOrder(String isbn, int quantity) {
        return bookClient.getBookByIsbn(isbn)
                        .map(book -> buildAcceptedOrder(book, quantity))
                        .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
                        .flatMap(orderRepository::save);
    }

    public static Order buildRejectedOrder(String isbn, int quantity) {
        return Order.build(isbn, "", 0.0, quantity, OrderStatus.REJECTED);
    }

    public static Order buildAcceptedOrder(Book book, int quantity) {
        return Order.build(book.isbn(), "", book.price(), quantity, OrderStatus.ACCEPTED);
    }
}
