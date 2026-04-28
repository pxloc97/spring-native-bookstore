package com.locpham.bookstore.inventoryservice.application.port.in;

import com.locpham.bookstore.inventoryservice.domain.InventoryItem;
import reactor.core.publisher.Mono;

public interface ManageStockUseCase {
    Mono<InventoryItem> addStock(String isbn, int quantity);

    Mono<InventoryItem> reduceStock(String isbn, int quantity);

    Mono<InventoryItem> queryStock(String isbn);
}
