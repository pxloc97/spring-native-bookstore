package com.locpham.bookstore.orderservice.adapter.out.persistence.jooq;

import com.locpham.bookstore.orderservice.adapter.out.persistence.jooq.generated.tables.Orders;
import com.locpham.bookstore.orderservice.application.port.out.OrderCommandPort;
import com.locpham.bookstore.orderservice.application.port.out.OrderQueryPort;
import com.locpham.bookstore.orderservice.domain.model.Order;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.locpham.bookstore.orderservice.adapter.out.persistence.jooq.generated.tables.Orders.ORDERS;

@Repository
public class JooqOrderRepositoryAdapter implements OrderCommandPort, OrderQueryPort {

    private final DSLContext dsl;

    public JooqOrderRepositoryAdapter(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Mono<Order> save(Order order) {
        var record = JooqOrderMapper.toRecord(order);

        return Mono.from(dsl.insertInto(ORDERS)
                .set(ORDERS.BOOK_ISBN, record.getBookIsbn())
                .set(ORDERS.BOOK_NAME, record.getBookName())
                .set(ORDERS.BOOK_PRICE, record.getBookPrice())
                .set(ORDERS.QUANTITY, record.getQuantity())
                .set(ORDERS.STATUS, record.getStatus())
                .set(ORDERS.CREATED_DATE, record.getCreatedDate())
                .set(ORDERS.LAST_MODIFIED_DATE, record.getLastModifiedDate())
                .set(ORDERS.CREATED_BY, record.getCreatedBy())
                .set(ORDERS.LAST_MODIFIED_BY, record.getLastModifiedBy())
                .set(ORDERS.VERSION, record.getVersion())
                .returning(ORDERS.ID, ORDERS.BOOK_ISBN, ORDERS.BOOK_NAME, ORDERS.BOOK_PRICE,
                        ORDERS.QUANTITY, ORDERS.STATUS, ORDERS.CREATED_DATE, ORDERS.LAST_MODIFIED_DATE,
                        ORDERS.VERSION, ORDERS.CREATED_BY, ORDERS.LAST_MODIFIED_BY))
                .map(JooqOrderMapper::toDomain);
    }

    @Override
    public Flux<Order> findAll() {
        return Flux.from(dsl.selectFrom(ORDERS))
                .map(JooqOrderMapper::toDomain);
    }

    @Override
    public Mono<Order> findById(Long id) {
        return Mono.from(dsl.selectFrom(ORDERS)
                .where(ORDERS.ID.eq(id)))
                .map(JooqOrderMapper::toDomain);
    }

    @Override
    public Flux<Order> findByCreatedBy(String createdBy) {
        return Flux.from(dsl.selectFrom(ORDERS)
                .where(ORDERS.CREATED_BY.eq(createdBy)))
                .map(JooqOrderMapper::toDomain);
    }
}
