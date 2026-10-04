package org.example.seatsapi.repository;

import org.example.seatsapi.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, UUID> {
    List<ReservationSeat> findByReservationId(UUID id);
}
