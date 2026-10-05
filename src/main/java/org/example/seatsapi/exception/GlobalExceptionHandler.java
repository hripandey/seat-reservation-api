package org.example.seatsapi.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SeatAlreadyTakenException.class)
    public ResponseEntity<ApiError> handleSeatAlreadyTaken(SeatAlreadyTakenException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        "SEAT_ALREADY_TAKEN",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(IdempotencyKeyReuseException.class)
    public ResponseEntity<ApiError> handleIdempotencyReuse(IdempotencyKeyReuseException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        "IDEMPOTENCY_KEY_REUSED",
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(UserReservationLimitExceededException.class)
    public ResponseEntity<ApiError> handleUserReservationLimitExceeded(UserReservationLimitExceededException exception) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiError(
                        "USER_RESERVATION_LIMIT_EXCEEDED",
                        exception.getMessage()
                ));
    }
}
