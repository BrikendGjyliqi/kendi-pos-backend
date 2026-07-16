package com.kendi.pos.restotable;

import com.kendi.pos.restotable.ReservationDtos.*;
import com.kendi.pos.restotable.ReservationService.ReservationStats;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:5173")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReservationResponse> findAll(@RequestParam(required = false) ReservationStatus status) {
        if (status != null) return service.findByStatus(status);
        return service.findAll();
    }

    @GetMapping("/stats/today")
    public ReservationStats todayStats() {
        return service.todayStats();
    }

    // Kamarier krijoi kerkese
    @PostMapping("/requests")
    public ResponseEntity<ReservationResponse> createRequest(@Valid @RequestBody CreateReservationRequest request) {
        return ResponseEntity.status(201).body(service.createRequest(request));
    }

    // Admin veprime
    @PatchMapping("/{id}/confirm")
    public ReservationResponse confirm(@PathVariable Long id) {
        return service.confirm(id);
    }

    @PatchMapping("/{id}/decline")
    public ReservationResponse decline(@PathVariable Long id) {
        return service.decline(id);
    }

    @PatchMapping("/{id}/arrived")
    public ReservationResponse markArrived(@PathVariable Long id) {
        return service.markArrived(id);
    }

    @PatchMapping("/{id}/no-show")
    public ReservationResponse markNoShow(@PathVariable Long id) {
        return service.markNoShow(id);
    }
    @GetMapping("/history")
    public List<ReservationResponse> history(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return service.getHistory(status, from, to);
    }

    @GetMapping("/stats/range")
    public ReservationService.RangeStats rangeStats(
            @RequestParam String from,
            @RequestParam String to
    ) {
        return service.getRangeStats(from, to);
    }
}