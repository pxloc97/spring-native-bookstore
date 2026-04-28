package com.locpham.bookstore.orderservice.adapter.in.messaging;

public record InventoryDecisionMessage(Long orderId, String status, String reason) {}
