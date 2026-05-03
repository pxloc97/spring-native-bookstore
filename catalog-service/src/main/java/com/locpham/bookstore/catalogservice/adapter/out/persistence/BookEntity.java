package com.locpham.bookstore.catalogservice.adapter.out.persistence;

import com.locpham.bookstore.catalogservice.domain.audit.AuditMetadata;
import com.locpham.bookstore.catalogservice.domain.book.Book;
import java.time.Instant;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table("book")
public record BookEntity(
        @Id Long id,
        String isbn,
        String title,
        String author,
        Double price,
        String publisher,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        @Version Integer version) {

    public static BookEntity fromDomain(Book book) {
        return new BookEntity(
                book.id(),
                book.isbn(),
                book.title(),
                book.author(),
                book.price(),
                book.publisher(),
                book.auditMetadata() != null ? book.auditMetadata().createdDate() : null,
                book.auditMetadata() != null ? book.auditMetadata().lastModifiedDate() : null,
                book.auditMetadata() != null ? book.auditMetadata().version() : null);
    }

    public Book toDomain() {
        return new com.locpham.bookstore.catalogservice.domain.book.Book(
                id,
                isbn,
                title,
                author,
                price,
                publisher,
                new AuditMetadata(createdDate, lastModifiedDate, version));
    }
}
