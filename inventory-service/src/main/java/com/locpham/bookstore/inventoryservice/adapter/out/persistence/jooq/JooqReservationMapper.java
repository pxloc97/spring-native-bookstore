package com.locpham.bookstore.inventoryservice.adapter.out.persistence.jooq;

import com.locpham.bookstore.inventoryservice.adapter.out.persistence.jooq.generated.tables.records.ReservationRecord;
import com.locpham.bookstore.inventoryservice.domain.Reservation;
import com.locpham.bookstore.inventoryservice.domain.ReservationStatus;

public class JooqReservationMapper {

    public static ReservationRecord toRecord(Reservation reservation) {
        return new ReservationRecord()
                .setId(reservation.reservationId())
                .setOrderId(reservation.orderId())
                .setIsbn(reservation.isbn())
                .setQuantity(reservation.quantity())
                .setStatus(reservation.status().name());
    }

    public static Reservation toDomain(ReservationRecord record) {
        return new Reservation(
                record.getId(),
                record.getOrderId(),
                record.getIsbn(),
                record.getQuantity(),
                ReservationStatus.valueOf(record.getStatus()));
    }
}
