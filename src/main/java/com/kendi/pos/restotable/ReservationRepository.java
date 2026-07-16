package com.kendi.pos.restotable;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByStatus(ReservationStatus status);
    List<Reservation> findAllByStatusOrderByReservationTimeAsc(ReservationStatus status);
    List<Reservation> findAllByTableIdAndStatus(Long tableId, ReservationStatus status);
    List<Reservation> findAllByReservationTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Reservation> findAllByUpdatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByStatusAndReservationTimeBetween(ReservationStatus status, LocalDateTime start, LocalDateTime end);
    long countByStatusAndUpdatedAtBetween(ReservationStatus status, LocalDateTime start, LocalDateTime end);
}