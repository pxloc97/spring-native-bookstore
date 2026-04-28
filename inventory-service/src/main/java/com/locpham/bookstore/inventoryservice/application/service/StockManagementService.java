package com.locpham.bookstore.inventoryservice.application.service;

import com.locpham.bookstore.inventoryservice.application.port.in.ManageStockUseCase;
import com.locpham.bookstore.inventoryservice.application.port.out.InventoryPort;
import com.locpham.bookstore.inventoryservice.domain.InventoryItem;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class StockManagementService implements ManageStockUseCase {

    private final InventoryPort inventoryPort;

    public StockManagementService(InventoryPort inventoryPort) {
        this.inventoryPort = inventoryPort;
    }

    @Override
    public Mono<InventoryItem> addStock(String isbn, int quantity) {
        return inventoryPort
                .findByIsbn(isbn)
                .defaultIfEmpty(InventoryItem.create(isbn, 0))
                .map(item -> item.adjust(quantity))
                .flatMap(inventoryPort::save);
    }

    @Override
    public Mono<InventoryItem> reduceStock(String isbn, int quantity) {
        return inventoryPort
                .findByIsbn(isbn)
                .switchIfEmpty(
                        Mono.error(
                                new IllegalArgumentException(
                                        "Inventory not found for ISBN: " + isbn)))
                .map(item -> item.adjust(-quantity))
                .flatMap(inventoryPort::save);
    }

    @Override
    public Mono<InventoryItem> queryStock(String isbn) {
        return inventoryPort.findByIsbn(isbn);
    }
}
