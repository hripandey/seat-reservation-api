package org.example.seatsapi.exception;

public class UserReservationLimitExceededException extends RuntimeException {
    public UserReservationLimitExceededException(String message) {
        super(message);
    }
}
