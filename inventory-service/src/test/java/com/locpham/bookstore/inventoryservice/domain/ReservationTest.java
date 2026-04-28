package com.locpham.bookstore.inventoryservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservationTest {

    @Test
    void create_shouldSetStatusToReserved() {
        UUID orderId = UUID.randomUUID();
        Reservation reservation = Reservation.create(orderId, "1234567890", 5);

        assertThat(reservation.reservationId()).isNull();
        assertThat(reservation.orderId()).isEqualTo(orderId);
        assertThat(reservation.isbn()).isEqualTo("1234567890");
        assertThat(reservation.quantity()).isEqualTo(5);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void markReleased_shouldChangeStatusToReleased() {
        UUID orderId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        Reservation reservation =
                new Reservation(
                        reservationId, orderId, "1234567890", 5, ReservationStatus.RESERVED);

        Reservation released = reservation.markReleased();

        assertThat(released.status()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(released.reservationId()).isEqualTo(reservationId);
    }

    @Test
    void markReleased_shouldThrowWhenNotReserved() {
        UUID orderId = UUID.randomUUID();
        Reservation reservation =
                new Reservation(
                        UUID.randomUUID(), orderId, "1234567890", 5, ReservationStatus.RELEASED);

        assertThatThrownBy(reservation::markReleased)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be RESERVED to release");
    }
}
