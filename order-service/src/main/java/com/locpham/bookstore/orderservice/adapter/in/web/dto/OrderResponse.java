package com.locpham.bookstore.orderservice.adapter.in.web.dto;

import com.locpham.bookstore.orderservice.domain.model.Order;

public record OrderResponse(
        Long id,
        String isbn,
        String title,
        double price,
        int quantity,
        String status,
        String createdBy) {

    public static OrderResponse fromDomain(Order order) {
        return new OrderResponse(
                order.id(),
                order.book().isbn(),
                order.book().title(),
                order.book().price(),
                order.quantity(),
                order.status().name(),
                order.audit().createdBy());
    }
}
