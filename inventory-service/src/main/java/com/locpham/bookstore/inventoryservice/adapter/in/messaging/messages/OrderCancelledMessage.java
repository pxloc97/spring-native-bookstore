package com.locpham.bookstore.inventoryservice.adapter.in.messaging.messages;

import java.util.UUID;

public record OrderCancelledMessage(UUID orderId) {}
