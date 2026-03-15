package com.locpham.bookstore.orderservice.book;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "polar")
public record ClientProperties(@NotBlank String catalogServiceUrl) {}