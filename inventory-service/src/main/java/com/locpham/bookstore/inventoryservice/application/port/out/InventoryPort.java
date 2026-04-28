package com.locpham.bookstore.inventoryservice.application.port.out;

import com.locpham.bookstore.inventoryservice.domain.InventoryItem;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface InventoryPort {
    Mono<InventoryItem> findByIsbn(String isbn);

    Flux<InventoryItem> findAllByIsbn(List<String> isbns);

    Mono<InventoryItem> save(InventoryItem item);

    Flux<InventoryItem> saveAll(List<InventoryItem> items);
}
