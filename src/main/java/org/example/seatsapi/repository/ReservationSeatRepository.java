package org.example.seatsapi.repository;

import org.example.seatsapi.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReservationSeatRepository extends JpaRepository<Reservation, UUID> {
}
