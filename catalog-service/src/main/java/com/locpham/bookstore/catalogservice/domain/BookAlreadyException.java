package com.locpham.bookstore.catalogservice.domain;

public class BookAlreadyException extends RuntimeException {
    public BookAlreadyException(String isbn) {
        super("The book with ISBN " + isbn + " already exists");
    }
}
