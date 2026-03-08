package com.locpham.bookstore.catalogservice.domain;

public class BookNotFoundException extends RuntimeException {
    public BookNotFoundException(String ibsn) {
        super("The book with ISBN " + ibsn + " was not found");
    }
}
