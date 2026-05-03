package com.locpham.bookstore.catalogservice.application.port.in;

import com.locpham.bookstore.catalogservice.domain.book.Book;

public interface EditBookUseCase {
    Book editBookDetails(Book book);

    void deleteBook(String isbn);
}
