package com.locpham.bookstore.catalogservice.application.port.out;

import com.locpham.bookstore.catalogservice.domain.book.Book;
import java.util.Optional;

public interface BookRepository {
    Optional<Book> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    void deleteByIsbn(String isbn);

    Iterable<Book> findAll();

    Book save(Book book);
}
