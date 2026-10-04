package org.example.seatsapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReserveSeatRequest(
        @NotEmpty
        List<@NotBlank String> seats,

        @NotBlank
        String idempotencyKey
) {
}
