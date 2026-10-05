package org.example.seatsapi.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.example.seatsapi.dto.request.ReserveSeatRequest;
import org.example.seatsapi.dto.response.ReservationResponse;
import org.example.seatsapi.entity.Reservation;
import org.example.seatsapi.entity.ReservationSeat;
import org.example.seatsapi.entity.Seat;
import org.example.seatsapi.entity.Show;
import org.example.seatsapi.enums.ReservationStatus;
import org.example.seatsapi.enums.SeatStatus;
import org.example.seatsapi.exception.IdempotencyKeyReuseException;
import org.example.seatsapi.exception.SeatAlreadyTakenException;
import org.example.seatsapi.exception.UserReservationLimitExceededException;
import org.example.seatsapi.repository.ReservationRepository;
import org.example.seatsapi.repository.ReservationSeatRepository;
import org.example.seatsapi.repository.SeatRepository;
import org.example.seatsapi.repository.ShowRepository;
import org.example.seatsapi.util.RequestHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private static final int MAX_SEATS_PER_USER_PER_SHOW = 4;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final EntityManager entityManager;

    @Transactional
    public ReservationResponse reserveSeats(UUID userId, UUID showId, ReserveSeatRequest request) {
        validateRequest(request);

        List<String> requestedSeats = request.seats().stream()
                .sorted()
                .toList();

        String requestHash = RequestHashUtil.hashSeats(requestedSeats);

        /*
         * Serialize requests using the same idempotency key.
         *
         * This lock exists only for the duration of this transaction.
         * This lock protects same request being submitted concurrently.
         */
        String lockKey =
                userId + ":" + showId + ":" + request.idempotencyKey();

        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0))")
                .setParameter("lockKey", lockKey)
                .getSingleResult();

        /*
         * At this point, another request using the same
         * idempotency key cannot be executing concurrently.
         */

        Optional<Reservation> existingReservation = reservationRepository.findByUserIdAndShowIdAndIdempotencyKey(userId, showId, request.idempotencyKey());
        if (existingReservation.isPresent()) {
            Reservation existing = existingReservation.get();
            // If the same idempotency key is used with different request
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyReuseException("Idempotency Key was already reused with different request");
            }
            return toResponse(existing);
        }

        /*
         * Check per-user reservation limit.
         */

        String userShowLockKey = "reservation-limit:" + userId + ":" + showId;
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtextextended(:lockKey, 0))")
                .setParameter("lockKey", userShowLockKey)
                .getSingleResult();

        long currActiveSeats = reservationSeatRepository.countActiveSeatsForUserAndShow(userId, showId);
        if (currActiveSeats + requestedSeats.size() > MAX_SEATS_PER_USER_PER_SHOW) {
            throw new UserReservationLimitExceededException(
                    "User reservation limit of " + MAX_SEATS_PER_USER_PER_SHOW + " seats per show exceeded"
            );
        }

        /*
         * Load the show.
         */
        Show show = showRepository.findById(showId).orElseThrow(() ->
                new IllegalArgumentException("No show exists"));


        /*
         * Lock all requested seats.
         */
        List<Seat> seats = seatRepository.findAndLockSeats(showId, requestedSeats);

        /*
         * Make sure every requested seat actually exists.
         */
        if (seats.size() != requestedSeats.size()) {
            throw new IllegalArgumentException("One or more requested seats do not exist");
        }
        /*
         * Handle partial requests: all-or-nothing
         */
        for (Seat seat : seats) {
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatAlreadyTakenException("Seat " + seat.getSeatNumber() + " is already taken");
            }
        }

        long amountPaise = show.getPricePaise() * seats.size();

        Reservation reservation = new Reservation();

        reservation.setShow(show);
        reservation.setUserId(userId);
        reservation.setAmountPaise(amountPaise);
        reservation.setStatus(ReservationStatus.HELD);
        reservation.setIdempotencyKey(request.idempotencyKey());
        reservation.setRequestHash(requestHash);
        reservation.setCreatedAt(Instant.now());

        Reservation savedReservation = reservationRepository.save(reservation);

        /*
         * Move seats from AVAILABLE -> HELD.
         */
        for (Seat seat : seats) {
            seat.setStatus(SeatStatus.HELD);
            ReservationSeat reservationSeat = new ReservationSeat();
            reservationSeat.setReservation(savedReservation);
            reservationSeat.setSeat(seat);
            reservationSeatRepository.save(reservationSeat);
        }
        return toResponse(savedReservation, seats);
    }

    private void validateRequest(ReserveSeatRequest request) {
        long distinctCount = request.seats().stream()
                .distinct()
                .count();
        if (distinctCount != request.seats().size()) {
            throw new IllegalArgumentException("Duplicate seats are not allowed");
        }
    }

    private ReservationResponse toResponse(
            Reservation reservation,
            List<Seat> seats
    ) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getShow().getId(),
                reservation.getUserId(),
                seats.stream()
                        .map(Seat::getSeatNumber)
                        .toList(),
                reservation.getAmountPaise(),
                reservation.getStatus()
        );
    }

    private ReservationResponse toResponse(
            Reservation reservation
    ) {
        List<String> seats =
                reservationSeatRepository.findByReservationId(reservation.getId())
                        .stream()
                        .map(rs -> rs.getSeat().getSeatNumber())
                        .toList();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getShow().getId(),
                reservation.getUserId(),
                seats,
                reservation.getAmountPaise(),
                reservation.getStatus()
        );
    }
}
