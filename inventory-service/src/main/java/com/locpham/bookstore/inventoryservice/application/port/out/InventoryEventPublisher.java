package com.locpham.bookstore.inventoryservice.application.port.out;

import com.locpham.bookstore.inventoryservice.domain.InventoryDecision;
import reactor.core.publisher.Mono;

public interface InventoryEventPublisher {
    Mono<Void> publishInventoryDecision(InventoryDecision decision);
}
