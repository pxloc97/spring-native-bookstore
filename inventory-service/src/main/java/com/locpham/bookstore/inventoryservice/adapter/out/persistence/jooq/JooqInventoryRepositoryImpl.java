package com.locpham.bookstore.inventoryservice.adapter.out.persistence.jooq;

import static com.locpham.bookstore.inventoryservice.adapter.out.persistence.jooq.generated.tables.Inventory.INVENTORY;

import com.locpham.bookstore.inventoryservice.adapter.out.persistence.jooq.generated.tables.records.InventoryRecord;
import com.locpham.bookstore.inventoryservice.application.port.out.InventoryPort;
import com.locpham.bookstore.inventoryservice.domain.InventoryItem;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class JooqInventoryRepositoryImpl implements InventoryPort {

    private final DSLContext dsl;

    public JooqInventoryRepositoryImpl(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Mono<InventoryItem> findByIsbn(String isbn) {
        return Mono.from(dsl.selectFrom(INVENTORY).where(INVENTORY.ISBN.eq(isbn)))
                .map(JooqInventoryMapper::toDomain);
    }

    @Override
    public Flux<InventoryItem> findAllByIsbn(List<String> isbns) {
        return Flux.from(dsl.selectFrom(INVENTORY).where(INVENTORY.ISBN.in(isbns)))
                .map(JooqInventoryMapper::toDomain);
    }

    @Override
    public Mono<InventoryItem> save(InventoryItem item) {
        InventoryRecord record = JooqInventoryMapper.toRecord(item);
        if (item.id() == null) {
            return Mono.from(
                            dsl.insertInto(INVENTORY)
                                    .set(INVENTORY.ISBN, record.getIsbn())
                                    .set(
                                            INVENTORY.AVAILABLE_QUANTITY,
                                            record.getAvailableQuantity())
                                    .set(INVENTORY.RESERVED_QUANTITY, record.getReservedQuantity())
                                    .set(INVENTORY.VERSION, record.getVersion())
                                    .returning(INVENTORY.fields()))
                    .map(JooqInventoryMapper::toDomain);
        } else {
            // Optimistic lock: match current version, bump version on successful update.
            long nextVersion = item.version() + 1;
            return Mono.from(
                            dsl.update(INVENTORY)
                                    .set(
                                            INVENTORY.AVAILABLE_QUANTITY,
                                            record.getAvailableQuantity())
                                    .set(INVENTORY.RESERVED_QUANTITY, record.getReservedQuantity())
                                    .set(INVENTORY.VERSION, nextVersion)
                                    .where(INVENTORY.ID.eq(item.id()))
                                    .and(INVENTORY.VERSION.eq(item.version()))
                                    .returning(INVENTORY.fields()))
                    .map(JooqInventoryMapper::toDomain)
                    .switchIfEmpty(
                            Mono.error(
                                    new OptimisticLockingFailureException(
                                            "Inventory update lost optimistic lock for id "
                                                    + item.id()
                                                    + " (expected version "
                                                    + item.version()
                                                    + ")")));
        }
    }

    @Override
    public Flux<InventoryItem> saveAll(List<InventoryItem> items) {
        return Flux.fromIterable(items).flatMap(this::save);
    }
}
