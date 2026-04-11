package com.locpham.bookstore.orderservice.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FlashSaleRequest(
        @NotBlank(message = "The book ISBN must be defined") String isbn,
        @NotNull(message = "The book quantity must be defined.") int quantity) {}
