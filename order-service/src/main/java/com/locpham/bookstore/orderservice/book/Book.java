package com.locpham.bookstore.orderservice.book;

public record Book (
        String isbn,
        String title,
        String author,
        double price
) {
}