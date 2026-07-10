package com.kendi.pos.product;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductRepository repo;

    public ProductController(ProductRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Product> getAll(@RequestParam(required = false) String categoryId) {
        if (categoryId != null) {
            return repo.findByCategoryId(categoryId);
        }
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getOne(@PathVariable String id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET vetem produktet qe gjurmojne stokun
    @GetMapping("/stock")
    public List<Product> getStockProducts() {
        return repo.findAll().stream()
                .filter(Product::isTrackStock)
                .toList();
    }

    // GET produktet me stok te ulet
    @GetMapping("/low-stock")
    public List<Product> getLowStock() {
        return repo.findAll().stream()
                .filter(Product::isTrackStock)
                .filter(p -> p.getStockQuantity() <= p.getLowStockThreshold())
                .toList();
    }

    @PostMapping
    public Product create(@RequestBody Product product) {
        product.setCreatedAt(System.currentTimeMillis());
        product.setActive(true);
        return repo.save(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable String id, @RequestBody Product updated) {
        return repo.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setCategoryId(updated.getCategoryId());
                    existing.setPrice(updated.getPrice());
                    existing.setSortOrder(updated.getSortOrder());
                    existing.setActive(updated.isActive());
                    existing.setTrackStock(updated.isTrackStock());
                    existing.setAutoDeductOnSale(updated.isAutoDeductOnSale());
                    existing.setStockUnit(updated.getStockUnit());
                    existing.setLowStockThreshold(updated.getLowStockThreshold());
                    // Kujdes: stockQuantity nuk update-het direkt ktu, vetem me endpoint adjust-stock
                    return ResponseEntity.ok(repo.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST rregullo stokun (shto ose hiq)
    // Body: { "delta": 48.0, "reason": "Delivery from Coca-Cola Kosova" }
    @PostMapping("/{id}/adjust-stock")
    @Transactional
    public ResponseEntity<Product> adjustStock(
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        return repo.findById(id)
                .map(p -> {
                    if (!p.isTrackStock()) {
                        return ResponseEntity.badRequest().<Product>build();
                    }
                    double delta = ((Number) body.getOrDefault("delta", 0)).doubleValue();
                    double newQty = Math.max(0, p.getStockQuantity() + delta);
                    p.setStockQuantity(newQty);
                    return ResponseEntity.ok(repo.save(p));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST set stokun ne nje vlere direkt (per korrigjim)
    @PostMapping("/{id}/set-stock")
    @Transactional
    public ResponseEntity<Product> setStock(
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        return repo.findById(id)
                .map(p -> {
                    if (!p.isTrackStock()) {
                        return ResponseEntity.badRequest().<Product>build();
                    }
                    double qty = ((Number) body.getOrDefault("quantity", 0)).doubleValue();
                    p.setStockQuantity(Math.max(0, qty));
                    return ResponseEntity.ok(repo.save(p));
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