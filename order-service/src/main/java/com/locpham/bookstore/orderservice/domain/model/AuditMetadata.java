package com.locpham.bookstore.orderservice.domain.model;

import java.time.Instant;

public record AuditMetadata(
        Instant createdDate, Instant lastModifiedDate, String createdBy, String lastModifiedBy) {

    static AuditMetadata init() {
        return new AuditMetadata(Instant.now(), Instant.now(), null, null);
    }

    AuditMetadata update() {
        return new AuditMetadata(createdDate, Instant.now(), createdBy, lastModifiedBy);
    }
}
