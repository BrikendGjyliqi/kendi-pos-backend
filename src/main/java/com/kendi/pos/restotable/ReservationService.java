package com.kendi.pos.restotable;

import com.kendi.pos.restotable.ReservationDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepo;
    private final RestaurantTableRepository tableRepo;

    public ReservationService(ReservationRepository reservationRepo, RestaurantTableRepository tableRepo) {
        this.reservationRepo = reservationRepo;
        this.tableRepo = tableRepo;
    }

    private String tableName(Long tableId) {
        return tableRepo.findById(tableId)
                .map(RestaurantTable::getName)
                .orElse("Unknown");
    }

    private ReservationResponse toResponse(Reservation r) {
        return ReservationResponse.from(r, tableName(r.getTableId()));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findAll() {
        return reservationRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> findByStatus(ReservationStatus status) {
        return reservationRepo.findAllByStatusOrderByReservationTimeAsc(status)
                .stream().map(this::toResponse).toList();
    }

    // Kamarieri krijoi kerkese
    public ReservationResponse createRequest(CreateReservationRequest request) {
        // Verifiko qe tavolina ekziston
        RestaurantTable table = tableRepo.findById(request.tableId())
                .orElseThrow(() -> new RuntimeException("Table not found: " + request.tableId()));

        Reservation r = new Reservation();
        r.setTableId(request.tableId());
        r.setGuestName(request.guestName().trim());
        r.setGuestPhone(request.guestPhone());
        r.setGuestCount(request.guestCount());
        r.setReservationTime(request.reservationTime());
        r.setRequestedBy(request.requestedBy());
        r.setStatus(ReservationStatus.PENDING_REQUEST);

        return toResponse(reservationRepo.save(r));
    }

    // Admini konfirmoi kerkesen
    public ReservationResponse confirm(Long id) {
        Reservation r = reservationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + id));

        if (r.getStatus() != ReservationStatus.PENDING_REQUEST) {
            throw new RuntimeException("Only pending requests can be confirmed. Current status: " + r.getStatus());
        }

        r.setStatus(ReservationStatus.CONFIRMED);
        r.setConfirmedAt(LocalDateTime.now());

        // Ndrysho statusin e tavolines ne RESERVED
        RestaurantTable table = tableRepo.findById(r.getTableId())
                .orElseThrow(() -> new RuntimeException("Table not found"));
        table.setStatus(TableStatus.RESERVED);
        tableRepo.save(table);

        return toResponse(reservationRepo.save(r));
    }

    // Admini refuzoi kerkesen
    public ReservationResponse decline(Long id) {
        Reservation r = reservationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + id));

        if (r.getStatus() != ReservationStatus.PENDING_REQUEST) {
            throw new RuntimeException("Only pending requests can be declined");
        }

        r.setStatus(ReservationStatus.DECLINED);
        return toResponse(reservationRepo.save(r));
    }

    // Klienti arriti - tavolina behet ON_DINE
    public ReservationResponse markArrived(Long id) {
        Reservation r = reservationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + id));

        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("Only confirmed reservations can be marked as arrived");
        }

        r.setStatus(ReservationStatus.ARRIVED);
        r.setArrivedAt(LocalDateTime.now());

        // Tavolina behet ON_DINE
        RestaurantTable table = tableRepo.findById(r.getTableId())
                .orElseThrow(() -> new RuntimeException("Table not found"));
        table.setStatus(TableStatus.ON_DINE);
        tableRepo.save(table);

        return toResponse(reservationRepo.save(r));
    }

    // Klienti s'erdhi - tavolina liron
    public ReservationResponse markNoShow(Long id) {
        Reservation r = reservationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found: " + id));

        if (r.getStatus() != ReservationStatus.CONFIRMED) {
            throw new RuntimeException("Only confirmed reservations can be marked as no-show");
        }

        r.setStatus(ReservationStatus.NO_SHOW);
        r.setNoShowAt(LocalDateTime.now());

        // Tavolina behet AVAILABLE prap
        RestaurantTable table = tableRepo.findById(r.getTableId())
                .orElseThrow(() -> new RuntimeException("Table not found"));
        table.setStatus(TableStatus.AVAILABLE);
        tableRepo.save(table);

        return toResponse(reservationRepo.save(r));
    }

    /**
     * Kur krijohet nje porosi per nje tavoline, ky metod thirret automatikisht.
     * Nese ka nje rezervim CONFIRMED per ate tavoline sot, e ben ARRIVED.
     * Kjo bon qe tavolina automatikisht te kaloje nga RESERVED ne ON_DINE.
     */
    public void markArrivedByTable(Long tableId) {
        List<Reservation> confirmed = reservationRepo.findAllByTableIdAndStatus(
                tableId, ReservationStatus.CONFIRMED);

        if (confirmed.isEmpty()) {
            // Ska rezervim - vetem set tavolinen ne ON_DINE
            RestaurantTable table = tableRepo.findById(tableId).orElse(null);
            if (table != null && table.getStatus() != TableStatus.ON_DINE) {
                table.setStatus(TableStatus.ON_DINE);
                tableRepo.save(table);
            }
            return;
        }

        // Merr rezervimin me te afert ne kohe
        Reservation r = confirmed.stream()
                .min((a, b) -> a.getReservationTime().compareTo(b.getReservationTime()))
                .orElse(null);

        if (r != null) {
            r.setStatus(ReservationStatus.ARRIVED);
            r.setArrivedAt(LocalDateTime.now());
            reservationRepo.save(r);
        }

        // Set tavolinen ON_DINE
        RestaurantTable table = tableRepo.findById(tableId).orElse(null);
        if (table != null) {
            table.setStatus(TableStatus.ON_DINE);
            tableRepo.save(table);
        }
    }

    /**
     * Kur porosia e fundit paguhet, tavolina behet AVAILABLE prap.
     */
    public void releaseTable(Long tableId) {
        RestaurantTable table = tableRepo.findById(tableId).orElse(null);
        if (table != null) {
            table.setStatus(TableStatus.AVAILABLE);
            tableRepo.save(table);
        }
    }

    // Statistika per sot
    @Transactional(readOnly = true)
    public ReservationStats todayStats() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        long arrived = reservationRepo.countByStatusAndReservationTimeBetween(
                ReservationStatus.ARRIVED, startOfDay, endOfDay);
        long noShow = reservationRepo.countByStatusAndReservationTimeBetween(
                ReservationStatus.NO_SHOW, startOfDay, endOfDay);
        long pending = reservationRepo.countByStatusAndReservationTimeBetween(
                ReservationStatus.CONFIRMED, startOfDay, endOfDay);

        long total = arrived + noShow;
        double showUpRate = total > 0 ? (arrived * 100.0 / total) : 0.0;

        return new ReservationStats(arrived, noShow, pending, showUpRate);
    }

    public record ReservationStats(
            long arrivedToday,
            long noShowToday,
            long upcomingToday,
            double showUpRate
    ) {}

    @Transactional(readOnly = true)
    public List<ReservationResponse> getHistory(String status, String from, String to) {
        LocalDateTime fromDate = from != null
                ? LocalDateTime.parse(from + "T00:00:00")
                : LocalDateTime.now().minusMonths(1);
        LocalDateTime toDate = to != null
                ? LocalDateTime.parse(to + "T23:59:59")
                : LocalDateTime.now().plusDays(1);

        // Filtrojme sipas updatedAt (kur ndryshoi statusi), jo reservationTime
        List<Reservation> all = reservationRepo.findAllByUpdatedAtBetween(fromDate, toDate);

        // Filter sipas status nese jepet
        if (status != null && !status.isEmpty() && !status.equals("ALL")) {
            try {
                ReservationStatus statusEnum = ReservationStatus.valueOf(status);
                all = all.stream()
                        .filter(r -> r.getStatus() == statusEnum)
                        .toList();
            } catch (IllegalArgumentException e) {
                // ignore
            }
        } else {
            all = all.stream()
                    .filter(r -> r.getStatus() == ReservationStatus.ARRIVED
                            || r.getStatus() == ReservationStatus.NO_SHOW
                            || r.getStatus() == ReservationStatus.DECLINED
                            || r.getStatus() == ReservationStatus.CANCELLED)
                    .toList();
        }

        return all.stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RangeStats getRangeStats(String from, String to) {
        LocalDateTime fromDate = LocalDateTime.parse(from + "T00:00:00");
        LocalDateTime toDate = LocalDateTime.parse(to + "T23:59:59");

        long arrived = reservationRepo.countByStatusAndUpdatedAtBetween(
                ReservationStatus.ARRIVED, fromDate, toDate);
        long noShow = reservationRepo.countByStatusAndUpdatedAtBetween(
                ReservationStatus.NO_SHOW, fromDate, toDate);
        long declined = reservationRepo.countByStatusAndUpdatedAtBetween(
                ReservationStatus.DECLINED, fromDate, toDate);
        long cancelled = reservationRepo.countByStatusAndUpdatedAtBetween(
                ReservationStatus.CANCELLED, fromDate, toDate);
        long confirmed = reservationRepo.countByStatusAndUpdatedAtBetween(
                ReservationStatus.CONFIRMED, fromDate, toDate);

        long total = arrived + noShow;
        double showUpRate = total > 0 ? (arrived * 100.0 / total) : 0.0;

        return new RangeStats(arrived, noShow, declined, cancelled, confirmed, showUpRate, from, to);
    }

    public record RangeStats(
            long arrived,
            long noShow,
            long declined,
            long cancelled,
            long confirmed,
            double showUpRate,
            String fromDate,
            String toDate
    ) {}
}