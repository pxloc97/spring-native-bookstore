package com.locpham.bookstore.inventoryservice.domain;

import java.util.UUID;

public record InventoryDecision(UUID orderId, DecisionStatus status, String reason) {

    public InventoryDecision {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status must not be null");
        }
    }

    public static InventoryDecision reserved(UUID orderId) {
        return new InventoryDecision(orderId, DecisionStatus.RESERVED, null);
    }

    public static InventoryDecision rejected(UUID orderId, String reason) {
        return new InventoryDecision(orderId, DecisionStatus.REJECTED, reason);
    }

    public enum DecisionStatus {
        RESERVED,
        REJECTED
    }
}
