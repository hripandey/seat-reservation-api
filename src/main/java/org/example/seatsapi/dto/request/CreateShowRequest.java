package org.example.seatsapi.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateShowRequest(
        @NotBlank
        String name,
        @NotEmpty
        List<@NotBlank String> seats,
        @NotNull
        @Positive
        Long pricePaise
) {
}
