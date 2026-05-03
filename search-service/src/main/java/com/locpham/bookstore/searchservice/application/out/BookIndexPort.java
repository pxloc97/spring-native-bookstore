package com.locpham.bookstore.searchservice.application.out;

import com.locpham.bookstore.searchservice.domain.BookDocument;
import reactor.core.publisher.Mono;

public interface BookIndexPort {
    Mono<BookDocument> save(BookDocument doc);
    Mono<Void> deleteByIsbn(String isbn);
    Mono<BookDocument> findByIsbn(String isbn);
}

