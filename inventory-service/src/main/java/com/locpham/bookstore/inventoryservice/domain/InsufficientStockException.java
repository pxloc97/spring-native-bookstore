package com.locpham.bookstore.inventoryservice.domain;

public class InsufficientStockException extends InventoryException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
