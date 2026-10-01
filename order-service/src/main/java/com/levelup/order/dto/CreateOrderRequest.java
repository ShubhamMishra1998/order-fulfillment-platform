package com.levelup.order.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank(message = "Customer ID must not be blank")
        String customerId
) {
}
