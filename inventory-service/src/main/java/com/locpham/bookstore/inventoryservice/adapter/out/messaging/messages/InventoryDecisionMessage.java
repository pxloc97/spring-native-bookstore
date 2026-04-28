package com.locpham.bookstore.inventoryservice.adapter.out.messaging.messages;

import java.util.UUID;

public record InventoryDecisionMessage(UUID orderId, String status, String reason) {}
