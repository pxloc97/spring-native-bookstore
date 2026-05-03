package com.locpham.bookstore.catalogservice.application.port.in;

import com.locpham.bookstore.catalogservice.domain.book.Book;

public interface ViewListBookUseCase {
    Iterable<Book> viewBookList();
}
