package com.locpham.bookstore.catalogservice.adapter.in.dto;

import com.locpham.bookstore.catalogservice.domain.book.Book;

public record BookResponse(
        Long id,
        String isbn,
        String title,
        String author,
        Double price,
        String publisher,
        String createdDate,
        String lastModifiedDate,
        Integer version) {

    public static BookResponse fromDomain(Book book) {
        return new BookResponse(
                book.id(),
                book.isbn(),
                book.title(),
                book.author(),
                book.price(),
                book.publisher(),
                book.auditMetadata() != null ? book.auditMetadata().createdDate().toString() : null,
                book.auditMetadata() != null
                        ? book.auditMetadata().lastModifiedDate().toString()
                        : null,
                book.auditMetadata() != null ? book.auditMetadata().version() : null);
    }
}
