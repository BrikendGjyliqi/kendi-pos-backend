package com.kendi.pos.staff;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@CrossOrigin(origins = "*")
public class StaffController {

    private final StaffRepository repo;

    public StaffController(StaffRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<StaffDto> getAll() {
        return repo.findAll().stream()
                .map(StaffDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffDto> getOne(@PathVariable String id) {
        return repo.findById(id)
                .map(s -> ResponseEntity.ok(StaffDto.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody StaffCreateRequest req) {
        if (req.getPin() == null || !req.getPin().matches("\\d{4}")) {
            return ResponseEntity.badRequest().body("PIN duhet të jetë 4 shifra");
        }

        Staff staff = new Staff();
        staff.setName(req.getName());
        staff.setPinHash(BCrypt.hashpw(req.getPin(), BCrypt.gensalt()));
        staff.setRole(req.getRole());
        staff.setActive(true);
        staff.setCreatedAt(System.currentTimeMillis());

        return ResponseEntity.ok(StaffDto.from(repo.save(staff)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody StaffCreateRequest req) {
        return repo.findById(id)
                .map(existing -> {
                    existing.setName(req.getName());
                    existing.setRole(req.getRole());
                    if (req.getPin() != null && !req.getPin().isEmpty()) {
                        if (!req.getPin().matches("\\d{4}")) {
                            return ResponseEntity.badRequest().body("PIN duhet të jetë 4 shifra");
                        }
                        existing.setPinHash(BCrypt.hashpw(req.getPin(), BCrypt.gensalt()));
                    }
                    return ResponseEntity.ok(StaffDto.from(repo.save(existing)));
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