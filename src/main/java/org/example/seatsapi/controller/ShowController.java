package org.example.seatsapi.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.seatsapi.dto.request.CreateShowRequest;
import org.example.seatsapi.entity.Show;
import org.example.seatsapi.service.ShowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @PostMapping
    public ResponseEntity<Show> createShow(@Valid @RequestBody CreateShowRequest createShowRequest) {
        Show show = showService.createShow(createShowRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(show);
    }
}
