package org.example.seatsapi.repository;

import org.example.seatsapi.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    Optional<Reservation> findByUserIdAndShowIdAndIdempotencyKey(UUID userId, UUID showId, String id);
}
