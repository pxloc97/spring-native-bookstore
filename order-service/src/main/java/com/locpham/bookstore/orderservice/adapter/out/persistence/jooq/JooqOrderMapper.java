package com.locpham.bookstore.orderservice.adapter.out.persistence.jooq;

import com.locpham.bookstore.orderservice.adapter.out.persistence.jooq.generated.tables.records.OrdersRecord;
import com.locpham.bookstore.orderservice.domain.model.AuditMetadata;
import com.locpham.bookstore.orderservice.domain.model.BookInfo;
import com.locpham.bookstore.orderservice.domain.model.Order;
import com.locpham.bookstore.orderservice.domain.model.OrderStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class JooqOrderMapper {

    public static OrdersRecord toRecord(Order order) {
        return new OrdersRecord()
                .setId(order.id())
                .setBookIsbn(order.book().isbn())
                .setBookName(order.book().title())
                .setBookPrice((float) order.book().price())
                .setQuantity(order.quantity())
                .setStatus(order.status().name())
                .setCreatedDate(LocalDateTime.ofInstant(order.audit().createdDate(), ZoneId.systemDefault()))
                .setLastModifiedDate(LocalDateTime.ofInstant(order.audit().lastModifiedDate(), ZoneId.systemDefault()))
                .setCreatedBy(order.audit().createdBy())
                .setLastModifiedBy(order.audit().lastModifiedBy())
                .setVersion(order.version());
    }

    public static Order toDomain(OrdersRecord record) {
        return new Order(
                record.getId(),
                new BookInfo(record.getBookIsbn(), record.getBookName(), record.getBookPrice()),
                record.getQuantity(),
                OrderStatus.valueOf(record.getStatus()),
                new AuditMetadata(
                        record.getCreatedDate() != null ? record.getCreatedDate().atZone(ZoneId.systemDefault()).toInstant() : null,
                        record.getLastModifiedDate() != null ? record.getLastModifiedDate().atZone(ZoneId.systemDefault()).toInstant() : null,
                        record.getCreatedBy(),
                        record.getLastModifiedBy()),
                record.getVersion());
    }
}
