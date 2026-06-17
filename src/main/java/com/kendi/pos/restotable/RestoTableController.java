package com.kendi.pos.restotable;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class RestoTableController {

    private final RestoTableRepository repo;

    public RestoTableController(RestoTableRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<RestoTable> getAll() {
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestoTable> getOne(@PathVariable String id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public RestoTable create(@RequestBody RestoTable table) {
        table.setCreatedAt(System.currentTimeMillis());
        return repo.save(table);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestoTable> update(@PathVariable String id, @RequestBody RestoTable updated) {
        return repo.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setSortOrder(updated.getSortOrder());
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