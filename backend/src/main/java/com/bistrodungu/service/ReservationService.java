package com.bistrodungu.service;

import com.bistrodungu.model.Reservation;
import com.bistrodungu.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public List<Reservation> findAll() {
        return reservationRepository.findAll();
    }

    public Reservation findById(Long id) {
        return reservationRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Reservation not found with id: " + id));
    }

    public Reservation create(Reservation reservation) {
        return reservationRepository.save(reservation);
    }

    public Reservation update(Long id, Reservation updated) {
        Reservation existing = findById(id);
        existing.setCustomerName(updated.getCustomerName());
        existing.setCustomerEmail(updated.getCustomerEmail());
        existing.setReservationDate(updated.getReservationDate());
        existing.setNumberOfGuests(updated.getNumberOfGuests());
        existing.setStatus(updated.getStatus());
        return reservationRepository.save(existing);
    }

    public void delete(Long id) {
        reservationRepository.deleteById(id);
    }
}
