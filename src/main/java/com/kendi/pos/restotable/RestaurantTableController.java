package com.kendi.pos.restotable;

import com.kendi.pos.restotable.TableDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "http://localhost:5173")
public class RestaurantTableController {

    private final RestaurantTableService service;

    public RestaurantTableController(RestaurantTableService service) {
        this.service = service;
    }

    @GetMapping
    public List<TableResponse> findAll(@RequestParam(required = false) Section section) {
        return service.findAll(section);
    }

    @GetMapping("/{id}")
    public TableResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<TableResponse> create(@Valid @RequestBody CreateTableRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PutMapping("/{id}")
    public TableResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTableRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/position")
    public TableResponse updatePosition(@PathVariable Long id, @Valid @RequestBody UpdatePositionRequest request) {
        return service.updatePosition(id, request);
    }

    @PatchMapping("/{id}/size")
    public TableResponse updateSize(@PathVariable Long id, @Valid @RequestBody UpdateSizeRequest request) {
        return service.updateSize(id, request);
    }

    @PatchMapping("/{id}/status")
    public TableResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}