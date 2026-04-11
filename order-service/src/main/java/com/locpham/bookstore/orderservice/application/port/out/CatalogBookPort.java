package com.locpham.bookstore.orderservice.application.port.out;

import com.locpham.bookstore.orderservice.domain.model.BookSnapshot;
import reactor.core.publisher.Mono;

public interface CatalogBookPort {
    Mono<BookSnapshot> loadBook(String isbn);
}
