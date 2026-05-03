package com.locpham.bookstore.catalogservice.domain.book;

import com.locpham.bookstore.catalogservice.domain.audit.AuditMetadata;

public record Book(
        Long id,
        String isbn,
        String title,
        String author,
        Double price,
        String publisher,
        AuditMetadata auditMetadata) {
    public static Book build(
            String isbn, String title, String author, Double price, String publisher) {
        return new Book(null, isbn, title, author, price, publisher, null);
    }
}
