package org.example.seatsapi.service;

import lombok.RequiredArgsConstructor;
import org.example.seatsapi.dto.request.CreateShowRequest;
import org.example.seatsapi.entity.Seat;
import org.example.seatsapi.entity.Show;
import org.example.seatsapi.enums.SeatStatus;
import org.example.seatsapi.repository.SeatRepository;
import org.example.seatsapi.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public Show createShow(CreateShowRequest showRequest) {
        Show show = new Show();
        show.setName(showRequest.name());
        show.setPricePaise(showRequest.pricePaise());
        showRepository.save(show);
        List<Seat> seats = showRequest.seats()
                .stream()
                .map(seatNumber -> {
                    Seat seat = new Seat();
                    seat.setShow(show);
                    seat.setSeatNumber(seatNumber);
                    seat.setStatus(SeatStatus.AVAILABLE);
                    return seat;
                })
                .toList();
        seatRepository.saveAll(seats);
        return show;
    }
}
