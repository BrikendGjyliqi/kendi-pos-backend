package com.kendi.pos.restotable;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class ReservationDtos {

    public record CreateReservationRequest(
            @NotNull(message = "Table is required")
            Long tableId,

            @NotBlank(message = "Guest name is required")
            @Size(max = 100)
            String guestName,

            @Size(max = 30)
            String guestPhone,

            @NotNull(message = "Guest count is required")
            @Min(value = 1, message = "At least 1 guest")
            Integer guestCount,

            @NotNull(message = "Reservation time is required")
            LocalDateTime reservationTime,

            String requestedBy
    ) {}

    public record ReservationResponse(
            Long id,
            Long tableId,
            String tableName,
            String guestName,
            String guestPhone,
            Integer guestCount,
            LocalDateTime reservationTime,
            ReservationStatus status,
            String requestedBy,
            LocalDateTime confirmedAt,
            LocalDateTime arrivedAt,
            LocalDateTime noShowAt,
            LocalDateTime createdAt
    ) {
        public static ReservationResponse from(Reservation r, String tableName) {
            return new ReservationResponse(
                    r.getId(),
                    r.getTableId(),
                    tableName,
                    r.getGuestName(),
                    r.getGuestPhone(),
                    r.getGuestCount(),
                    r.getReservationTime(),
                    r.getStatus(),
                    r.getRequestedBy(),
                    r.getConfirmedAt(),
                    r.getArrivedAt(),
                    r.getNoShowAt(),
                    r.getCreatedAt()
            );
        }
    }
}