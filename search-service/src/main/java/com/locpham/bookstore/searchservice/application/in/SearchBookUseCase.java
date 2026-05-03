package com.locpham.bookstore.searchservice.application.in;

import com.locpham.bookstore.searchservice.domain.BookDocument;
import org.springframework.data.domain.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.awt.print.Pageable;

public interface SearchBookUseCase  {
    Mono<Page<BookDocument>> search(String query, Pageable pageable);
    Mono<Page<BookDocument>> searchByAuthor(String author, Pageable pageable);
    Flux<String> suggest(String prefix);
}
