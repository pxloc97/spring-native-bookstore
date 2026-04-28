package com.locpham.bookstore.inventoryservice.application.port.in;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface ReleaseStockUseCase {
    Mono<Void> releaseForOrder(UUID orderId);
}
