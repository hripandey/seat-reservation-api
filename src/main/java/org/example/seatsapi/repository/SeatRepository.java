package org.example.seatsapi.repository;

import org.example.seatsapi.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    // ORDER BY gives deterministic order that prevents deadlock
    @Query(value = """
            SELECT *
            FROM seats
            WHERE show_id = :showId
              AND seat_number IN (:seatNumbers)
            ORDER BY seat_number
            FOR UPDATE
            """, nativeQuery = true)
    List<Seat> findAndLockSeats(@Param("showId") UUID showId, @Param("seatNumbers") List<String> seatNumbers);
}
