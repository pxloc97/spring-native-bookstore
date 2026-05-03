package com.locpham.bookstore.catalogservice.adapter.in.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record BookRequest(
        @NotBlank(message = "The book ISBN must be defined.")
                @Pattern(
                        regexp = "^([0-9]{10}|[0-9]{13})$",
                        message = "The ISBN format must follow the standards ISBN-10 or ISBN-13.")
                String isbn,
        @NotBlank(message = "The book title must be defined.") String title,
        @NotBlank(message = "The book author must be defined.") String author,
        @NotNull(message = "The book price must be defined.")
                @Positive(message = "The book price must be greater than zero.")
                Double price,
        String publisher) {

    public com.locpham.bookstore.catalogservice.domain.book.Book toDomain() {
        return com.locpham.bookstore.catalogservice.domain.book.Book.build(
                isbn, title, author, price, publisher);
    }
}
