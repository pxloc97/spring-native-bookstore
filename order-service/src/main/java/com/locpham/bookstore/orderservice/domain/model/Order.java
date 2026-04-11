package com.locpham.bookstore.orderservice.domain.model;

public record Order(
        Long id,
        BookInfo book,
        int quantity,
        OrderStatus status,
        AuditMetadata audit,
        int version) {

    public static Order build(
            String isbn,
            String title,
            double price,
            int quantity,
            OrderStatus orderStatus) {
        return new Order(
                null,
                new BookInfo(isbn, title, price),
                quantity,
                orderStatus,
                AuditMetadata.init(),
                0);
    }

    public Order accept() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order must be PENDING to accept");
        }
        return new Order(id, book, quantity, OrderStatus.ACCEPTED, audit.update(), version);
    }

    public Order reject() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Order must be PENDING to reject");
        }
        return new Order(id, book, quantity, OrderStatus.REJECTED, audit.update(), version);
    }

    public Order markDispatched() {
        if (status != OrderStatus.ACCEPTED) {
            throw new IllegalStateException("Order must be ACCEPTED to mark as dispatched");
        }
        return new Order(id, book, quantity, OrderStatus.DISPATCHED, audit.update(), version + 1);
    }
}
