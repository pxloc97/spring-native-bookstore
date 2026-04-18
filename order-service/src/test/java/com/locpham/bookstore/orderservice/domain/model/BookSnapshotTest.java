package com.locpham.bookstore.orderservice.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class BookSnapshotTest {

    @Test
    void shouldCreateBookSnapshot() {
        var book = new BookSnapshot("1234567890", "Test Book", 9.99);

        assertEquals("1234567890", book.isbn());
        assertEquals("Test Book", book.title());
        assertEquals(9.99, book.price());
    }

    @Test
    void shouldEqualsCorrectly() {
        var book1 = new BookSnapshot("1234567890", "Test Book", 9.99);
        var book2 = new BookSnapshot("1234567890", "Test Book", 9.99);

        assertEquals(book1, book2);
        assertEquals(book1.hashCode(), book2.hashCode());
    }
}
