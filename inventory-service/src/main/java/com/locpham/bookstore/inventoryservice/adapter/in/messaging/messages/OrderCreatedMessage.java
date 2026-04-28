package com.locpham.bookstore.inventoryservice.adapter.in.messaging.messages;

import java.util.List;
import java.util.UUID;

public record OrderCreatedMessage(UUID orderId, List<OrderItem> items) {
    public record OrderItem(String isbn, int quantity) {}
}
