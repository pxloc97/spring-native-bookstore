package com.locpham.bookstore.inventoryservice.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(@NotNull Integer delta) {}
