package com.locpham.bookstore.inventoryservice.adapter.out.persistence.jooq;

import com.locpham.bookstore.inventoryservice.adapter.out.persistence.jooq.generated.tables.records.InventoryRecord;
import com.locpham.bookstore.inventoryservice.domain.InventoryItem;

public class JooqInventoryMapper {

    public static InventoryRecord toRecord(InventoryItem item) {
        return new InventoryRecord()
                .setId(item.id())
                .setIsbn(item.isbn())
                .setAvailableQuantity(item.availableQuantity())
                .setReservedQuantity(item.reservedQuantity())
                .setVersion(item.version());
    }

    public static InventoryItem toDomain(InventoryRecord record) {
        return new InventoryItem(
                record.getId(),
                record.getIsbn(),
                record.getAvailableQuantity(),
                record.getReservedQuantity(),
                record.getVersion());
    }
}
