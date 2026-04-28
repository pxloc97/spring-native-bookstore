package com.locpham.bookstore.inventoryservice.domain;

public record InventoryItem(
        Long id, String isbn, int availableQuantity, int reservedQuantity, long version) {

    public InventoryItem {
        if (isbn == null || isbn.isBlank()) {
            throw new IllegalArgumentException("ISBN must not be blank");
        }
        if (availableQuantity < 0) {
            throw new IllegalArgumentException("Available quantity must be non-negative");
        }
        if (reservedQuantity < 0) {
            throw new IllegalArgumentException("Reserved quantity must be non-negative");
        }
        if (version < 0) {
            throw new IllegalArgumentException("Version must be non-negative");
        }
    }

    public static InventoryItem create(String isbn, int availableQuantity) {
        return new InventoryItem(null, isbn, availableQuantity, 0, 0);
    }

    public InventoryItem reserve(int quantity) {
        if (availableQuantity < quantity) {
            throw new InsufficientStockException(
                    "Insufficient stock for ISBN "
                            + isbn
                            + ": requested "
                            + quantity
                            + ", available "
                            + availableQuantity);
        }
        return new InventoryItem(
                id, isbn, availableQuantity - quantity, reservedQuantity + quantity, version);
    }

    public InventoryItem release(int quantity) {
        if (reservedQuantity < quantity) {
            throw new InventoryException(
                    "Cannot release "
                            + quantity
                            + " for ISBN "
                            + isbn
                            + ": reserved "
                            + reservedQuantity);
        }
        return new InventoryItem(
                id, isbn, availableQuantity + quantity, reservedQuantity - quantity, version);
    }

    public InventoryItem adjust(int delta) {
        int newAvailable = availableQuantity + delta;
        if (newAvailable < 0) {
            throw new InventoryException(
                    "Cannot adjust stock for ISBN "
                            + isbn
                            + ": result would be negative ("
                            + newAvailable
                            + ")");
        }
        return new InventoryItem(id, isbn, newAvailable, reservedQuantity, version);
    }
}
