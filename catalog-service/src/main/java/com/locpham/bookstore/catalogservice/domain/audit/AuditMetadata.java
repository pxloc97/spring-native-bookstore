package com.locpham.bookstore.catalogservice.domain.audit;

import java.time.Instant;

public record AuditMetadata(Instant createdDate, Instant lastModifiedDate, Integer version) {}
