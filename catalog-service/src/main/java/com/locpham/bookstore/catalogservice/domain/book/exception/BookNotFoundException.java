package com.locpham.bookstore.catalogservice.domain.book.exception;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String ibsn) {
        super("The book with ISBN " + ibsn + " was not found");
    }
}
