package com.locpham.bookstore.inventoryservice.application.port.out;

import com.locpham.bookstore.inventoryservice.domain.Reservation;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReservationPort {
    Flux<Reservation> findByOrderId(UUID orderId);

    Mono<Reservation> save(Reservation reservation);
}
