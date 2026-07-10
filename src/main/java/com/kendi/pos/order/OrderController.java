package com.kendi.pos.order;

import com.kendi.pos.product.Product;
import com.kendi.pos.product.ProductRepository;
import com.kendi.pos.product.StockUnit;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository repo;
    private final ProductRepository productRepo;

    public OrderController(OrderRepository repo, ProductRepository productRepo) {
        this.repo = repo;
        this.productRepo = productRepo;
    }

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

    @PostMapping
    @Transactional
    public Order create(@RequestBody Order order) {
        order.setOpenedAt(System.currentTimeMillis());
        if (order.getStatus() == null) {
            order.setStatus("open");
        }
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

    // POST pay — porosi e vetme
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
                    o.setTipAmount(req.getTipAmount());
                    o.setTipPercent(req.getTipPercent());
                    long now = System.currentTimeMillis();
                    o.setPaidAt(now);
                    if (o.getClosedAt() == null) o.setClosedAt(now);

                    // Pakeso stokun per items me autoDeduct
                    deductStockForOrder(o);

                    return ResponseEntity.ok(repo.save(o));
                })
                .orElse(ResponseEntity.notFound().build());
    }

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

        if (unpaid.isEmpty()) return unpaid;

        long now = System.currentTimeMillis();
        int totalSum = unpaid.stream().mapToInt(Order::getTotal).sum();
        Integer totalTip = req.getTipAmount();
        int tipAssigned = 0;

        boolean firstOrder = true;
        for (int i = 0; i < unpaid.size(); i++) {
            Order o = unpaid.get(i);
            o.setStatus("paid");
            o.setPaymentMethod(req.getMethod());
            o.setFiscal(req.getFiscal());
            o.setPaidAt(now);
            if (o.getClosedAt() == null) o.setClosedAt(now);

            if (totalTip != null && totalTip > 0 && totalSum > 0) {
                int share;
                if (i == unpaid.size() - 1) {
                    share = totalTip - tipAssigned;
                } else {
                    share = (int) Math.round((double) totalTip * o.getTotal() / totalSum);
                    tipAssigned += share;
                }
                o.setTipAmount(share);
                o.setTipPercent(req.getTipPercent());
            }

            if (firstOrder && req.getCashGiven() != null) {
                o.setCashGiven(req.getCashGiven());
                firstOrder = false;
            }

            // Pakeso stokun per items me autoDeduct
            deductStockForOrder(o);

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

    // ─── Stock deduction ───
    // Per çdo item ne porosi: nese produkti ka trackStock=true dhe autoDeductOnSale=true
    // dhe stockUnit=PIECE, atehere pakeso quantity nga stoku.
    // KG nuk pakesohet automatikisht (pret Fazen 1b — recete).
    private void deductStockForOrder(Order order) {
        for (OrderItem item : order.getItems()) {
            productRepo.findById(item.getProductId()).ifPresent(product -> {
                if (product.isTrackStock()
                        && product.isAutoDeductOnSale()
                        && product.getStockUnit() == StockUnit.PIECE) {
                    double newQty = Math.max(0, product.getStockQuantity() - item.getQuantity());
                    product.setStockQuantity(newQty);
                    productRepo.save(product);
                }
            });
        }
    }
}