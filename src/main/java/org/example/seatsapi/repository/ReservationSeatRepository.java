package org.example.seatsapi.repository;

import org.example.seatsapi.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, UUID> {
    List<ReservationSeat> findByReservationId(UUID id);

    @Query(value = """
            SELECT COUNT(rs.id)
            FROM reservation_seats rs
            JOIN reservations r ON r.id = rs.reservation_id
            WHERE r.show_id = :showId
              AND r.user_id = :userId
              AND r.status IN ('HELD', 'CONFIRMED')
            """, nativeQuery = true)
    long countActiveSeatsForUserAndShow(@Param("userId") UUID userId, @Param("showId") UUID showId);
}
