package com.kendi.pos.order;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository repo;

    public OrderController(OrderRepository repo) {
        this.repo = repo;
    }

    // GET te gjitha (me filter opsionale per status dhe tableId)
    @GetMapping
    public List<Order> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String tableId
    ) {
        if (status != null && tableId != null) {
            return repo.findByTableIdAndStatus(tableId, status);
        }
        if (status != null) {
            return repo.findByStatus(status);
        }
        if (tableId != null) {
            return repo.findByTableId(tableId);
        }
        return repo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOne(@PathVariable String id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST krijo porosi te re
    @PostMapping
    @Transactional
    public Order create(@RequestBody Order order) {
        order.setOpenedAt(System.currentTimeMillis());
        if (order.getStatus() == null) {
            order.setStatus("open");
        }
        // Lidh OrderItem me Order
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                item.setOrder(order);
                if (item.getAddedAt() == 0) {
                    item.setAddedAt(System.currentTimeMillis());
                }
            }
        }
        recalculate(order);
        return repo.save(order);
    }

    // PUT update porosi (shtimi/heqja e items, ndryshim sasie, koment)
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Order> update(@PathVariable String id, @RequestBody Order updated) {
        return repo.findById(id)
                .map(existing -> {
                    if (!"open".equals(existing.getStatus())) {
                        return ResponseEntity.badRequest().<Order>build();
                    }

                    existing.getItems().clear();
                    if (updated.getItems() != null) {
                        for (OrderItem item : updated.getItems()) {
                            item.setOrder(existing);
                            if (item.getAddedAt() == 0) {
                                item.setAddedAt(System.currentTimeMillis());
                            }
                            existing.getItems().add(item);
                        }
                    }
                    existing.setDiscount(updated.getDiscount());
                    recalculate(existing);
                    return ResponseEntity.ok(repo.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST close — mbyll porosine (status: open -> closed)
    @PostMapping("/{id}/close")
    @Transactional
    public ResponseEntity<Order> close(@PathVariable String id) {
        return repo.findById(id)
                .map(o -> {
                    if (!"open".equals(o.getStatus())) {
                        return ResponseEntity.badRequest().<Order>build();
                    }
                    if (o.getItems().isEmpty()) {
                        repo.delete(o);
                        return ResponseEntity.noContent().<Order>build();
                    }
                    o.setStatus("closed");
                    o.setClosedAt(System.currentTimeMillis());
                    return ResponseEntity.ok(repo.save(o));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST pay — paguaj porosi te vetme
    @PostMapping("/{id}/pay")
    @Transactional
    public ResponseEntity<Order> pay(@PathVariable String id, @RequestBody PaymentRequest req) {
        return repo.findById(id)
                .map(o -> {
                    if (!"open".equals(o.getStatus()) && !"closed".equals(o.getStatus())) {
                        return ResponseEntity.badRequest().<Order>build();
                    }
                    o.setStatus("paid");
                    o.setPaymentMethod(req.getMethod());
                    o.setCashGiven(req.getCashGiven());
                    o.setFiscal(req.getFiscal());
                    long now = System.currentTimeMillis();
                    o.setPaidAt(now);
                    if (o.getClosedAt() == null) o.setClosedAt(now);
                    return ResponseEntity.ok(repo.save(o));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST cancel
    @PostMapping("/{id}/cancel")
    @Transactional
    public ResponseEntity<Order> cancel(@PathVariable String id) {
        return repo.findById(id)
                .map(o -> {
                    if (!"open".equals(o.getStatus()) && !"closed".equals(o.getStatus())) {
                        return ResponseEntity.badRequest().<Order>build();
                    }
                    o.setStatus("cancelled");
                    o.setClosedAt(System.currentTimeMillis());
                    return ResponseEntity.ok(repo.save(o));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST pay-all per tavoline
    @PostMapping("/table/{tableId}/pay-all")
    @Transactional
    public List<Order> payAllForTable(
            @PathVariable String tableId,
            @RequestBody PaymentRequest req
    ) {
        List<Order> unpaid = repo.findByTableId(tableId).stream()
                .filter(o -> "open".equals(o.getStatus()) || "closed".equals(o.getStatus()))
                .filter(o -> !o.getItems().isEmpty())
                .toList();

        long now = System.currentTimeMillis();
        boolean firstOrder = true;
        for (Order o : unpaid) {
            o.setStatus("paid");
            o.setPaymentMethod(req.getMethod());
            o.setFiscal(req.getFiscal());
            o.setPaidAt(now);
            if (o.getClosedAt() == null) o.setClosedAt(now);
            // cashGiven shkon vetem te porosia e pare
            if (firstOrder && req.getCashGiven() != null) {
                o.setCashGiven(req.getCashGiven());
                firstOrder = false;
            }
            repo.save(o);
        }
        return unpaid;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void recalculate(Order order) {
        int subtotal = 0;
        for (OrderItem item : order.getItems()) {
            subtotal += item.getPrice() * item.getQuantity();
        }
        order.setSubtotal(subtotal);
        order.setTotal(Math.max(0, subtotal - order.getDiscount()));
    }
}