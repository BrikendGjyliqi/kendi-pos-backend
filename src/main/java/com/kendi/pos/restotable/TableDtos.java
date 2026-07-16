package com.kendi.pos.restotable;

import jakarta.validation.constraints.*;

public class TableDtos {

    public record CreateTableRequest(
            @NotBlank(message = "Name is required")
            @Size(max = 50, message = "Name must be under 50 characters")
            String name,

            @NotNull(message = "Seat count is required")
            @Min(value = 2, message = "Minimum 2 seats")
            @Max(value = 20, message = "Maximum 20 seats")
            Integer seatCount,

            @NotNull(message = "Section is required")
            Section section,

            Integer sortOrder
    ) {}

    public record UpdateTableRequest(
            @NotBlank String name,
            Integer seatCount,
            Section section,
            Integer sortOrder
    ) {}

    public record UpdatePositionRequest(
            @NotNull Integer positionX,
            @NotNull Integer positionY
    ) {}

    public record UpdateStatusRequest(
            @NotNull TableStatus status
    ) {}

    public record UpdateSizeRequest(
            @NotNull @Min(80) @Max(300) Integer size
    ) {}

    public record TableResponse(
            Long id,
            String name,
            Integer seatCount,
            Section section,
            TableStatus status,
            Integer positionX,
            Integer positionY,
            Integer sortOrder,
            Long createdAt,
            Integer size
    ) {
        public static TableResponse from(RestaurantTable t) {
            return new TableResponse(
                    t.getId(),
                    t.getName(),
                    t.getSeatCount(),
                    t.getSection(),
                    t.getStatus(),
                    t.getPositionX(),
                    t.getPositionY(),
                    t.getSortOrder(),
                    t.getCreatedAt() != null
                            ? t.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            : System.currentTimeMillis(),
                    t.getSize() != null ? t.getSize() : 150
            );
        }
    }
}