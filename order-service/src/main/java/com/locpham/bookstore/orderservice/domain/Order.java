package com.locpham.bookstore.orderservice.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("orders")
public record Order(
        @Id
        Long id,
        String bookIsbn,
        String bookName,
        Double bookPrice,
        Integer quantity,
        OrderStatus status,

        @CreatedDate
        Instant createdDate,

        @LastModifiedDate
        Instant lastModifiedDate,

        @Version
        int version
) {

    public static Order build(
            String bookIsbn, String bookName, double bookPrice, int quantity, OrderStatus orderStatus
    ) {
        return new Order(null, bookIsbn, bookName, bookPrice, quantity, orderStatus, null, null, 0);
    }
}
