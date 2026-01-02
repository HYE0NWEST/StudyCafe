package com.studycafe.domain.seat;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat,Long> {
    Optional<Seat> findBySeatNumber(Integer seatNumber);
}
