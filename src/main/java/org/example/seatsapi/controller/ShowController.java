package org.example.seatsapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.example.seatsapi.dto.request.CreateShowRequest;
import org.example.seatsapi.dto.request.ReserveSeatRequest;
import org.example.seatsapi.dto.response.ReservationResponse;
import org.example.seatsapi.entity.Show;
import org.example.seatsapi.service.ReservationService;
import org.example.seatsapi.service.ShowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;
    private final ReservationService reservationService;

    @PostMapping
    public ResponseEntity<Show> createShow(@Valid @RequestBody CreateShowRequest createShowRequest) {
        Show show = showService.createShow(createShowRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(show);
    }

    @PostMapping("/{id}/reserve")
    public ResponseEntity<ReservationResponse> reserveSeats(@PathVariable UUID id,
                                                            @Valid @RequestBody ReserveSeatRequest reserveSeatRequest,
                                                            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.name());
        ReservationResponse response = reservationService.reserveSeats(userId, id, reserveSeatRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
