package com.locpham.bookstore.inventoryservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InventoryItemTest {

    @Test
    void create_shouldInitializeWithZeroReserved() {
        InventoryItem item = InventoryItem.create("1234567890", 10);

        assertThat(item.id()).isNull();
        assertThat(item.isbn()).isEqualTo("1234567890");
        assertThat(item.availableQuantity()).isEqualTo(10);
        assertThat(item.reservedQuantity()).isEqualTo(0);
        assertThat(item.version()).isEqualTo(0);
    }

    @Test
    void reserve_shouldDecrementAvailableAndIncrementReserved() {
        InventoryItem item = new InventoryItem(1L, "1234567890", 10, 0, 0);

        InventoryItem reserved = item.reserve(3);

        assertThat(reserved.availableQuantity()).isEqualTo(7);
        assertThat(reserved.reservedQuantity()).isEqualTo(3);
        assertThat(reserved.version()).isEqualTo(0);
    }

    @Test
    void reserve_shouldThrowWhenInsufficientStock() {
        InventoryItem item = new InventoryItem(1L, "1234567890", 2, 0, 0);

        assertThatThrownBy(() -> item.reserve(3))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void release_shouldIncrementAvailableAndDecrementReserved() {
        InventoryItem item = new InventoryItem(1L, "1234567890", 7, 3, 0);

        InventoryItem released = item.release(2);

        assertThat(released.availableQuantity()).isEqualTo(9);
        assertThat(released.reservedQuantity()).isEqualTo(1);
    }

    @Test
    void release_shouldThrowWhenReservedQuantityInsufficient() {
        InventoryItem item = new InventoryItem(1L, "1234567890", 10, 1, 0);

        assertThatThrownBy(() -> item.release(2))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("Cannot release");
    }

    @Test
    void adjust_shouldAddDeltaToAvailableQuantity() {
        InventoryItem item = new InventoryItem(1L, "1234567890", 10, 0, 0);

        InventoryItem adjusted = item.adjust(5);

        assertThat(adjusted.availableQuantity()).isEqualTo(15);
    }

    @Test
    void adjust_shouldSubtractDeltaWhenNegative() {
        InventoryItem item = new InventoryItem(1L, "1234567890", 10, 0, 0);

        InventoryItem adjusted = item.adjust(-3);

        assertThat(adjusted.availableQuantity()).isEqualTo(7);
    }

    @Test
    void adjust_shouldThrowWhenResultWouldBeNegative() {
        InventoryItem item = new InventoryItem(1L, "1234567890", 5, 0, 0);

        assertThatThrownBy(() -> item.adjust(-6))
                .isInstanceOf(InventoryException.class)
                .hasMessageContaining("result would be negative");
    }

    @Test
    void constructor_shouldRejectBlankIsbn() {
        assertThatThrownBy(() -> new InventoryItem(1L, "", 10, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISBN must not be blank");
    }

    @Test
    void constructor_shouldRejectNegativeAvailableQuantity() {
        assertThatThrownBy(() -> new InventoryItem(1L, "123", -1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Available quantity must be non-negative");
    }
}
