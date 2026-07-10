package com.kendi.pos.supplier;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")
public class SupplierController {

    private final SupplierRepository repo;

    public SupplierController(SupplierRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Supplier> getAll(@RequestParam(required = false) Boolean activeOnly) {
        if (Boolean.TRUE.equals(activeOnly)) {
            return repo.findByActiveTrue();
        }
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getOne(@PathVariable String id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Supplier create(@RequestBody Supplier supplier) {
        supplier.setCreatedAt(System.currentTimeMillis());
        supplier.setActive(true);
        return repo.save(supplier);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Supplier> update(@PathVariable String id, @RequestBody Supplier updated) {
        return repo.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setContactPerson(updated.getContactPerson());
                    existing.setPhone(updated.getPhone());
                    existing.setEmail(updated.getEmail());
                    existing.setAddress(updated.getAddress());
                    existing.setNotes(updated.getNotes());
                    existing.setActive(updated.isActive());
                    return ResponseEntity.ok(repo.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}