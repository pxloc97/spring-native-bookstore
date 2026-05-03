package com.locpham.bookstore.catalogservice.application.port.in;

import com.locpham.bookstore.catalogservice.domain.book.Book;
import com.locpham.bookstore.catalogservice.domain.book.exception.BookNotFoundException;

public interface ViewBookDetailUseCase {
    Book viewBookDetail(String isbn) throws BookNotFoundException;
}
