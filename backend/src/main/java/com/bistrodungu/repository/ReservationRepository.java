package com.bistrodungu.repository;

import com.bistrodungu.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByReservationDateBetween(LocalDateTime start, LocalDateTime end);

    List<Reservation> findByStatus(Reservation.ReservationStatus status);

    List<Reservation> findByTableId(Long tableId);
}
