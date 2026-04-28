package com.locpham.bookstore.orderservice.application.port.in;

import reactor.core.publisher.Mono;

public interface ProcessInventoryDecisionUseCase {
    Mono<Void> processDecision(Long orderId, DecisionStatus status);

    enum DecisionStatus {
        RESERVED,
        REJECTED
    }
}
