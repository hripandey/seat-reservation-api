package org.example.seatsapi.dto.response;

import org.example.seatsapi.enums.ReservationStatus;

import java.util.List;
import java.util.UUID;

public record ReservationResponse(
        UUID reservationId,
        UUID showId,
        UUID userId,
        List<String> seats,
        Long amountPaise,
        ReservationStatus status
) {
}
