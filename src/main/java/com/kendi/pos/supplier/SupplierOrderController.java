package com.kendi.pos.supplier;

import com.kendi.pos.product.Product;
import com.kendi.pos.product.ProductRepository;
import com.kendi.pos.product.StockUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/supplier-orders")
@CrossOrigin(origins = "*")
public class SupplierOrderController {

    private final SupplierOrderRepository repo;
    private final SupplierRepository supplierRepo;
    private final ProductRepository productRepo;

    public SupplierOrderController(
            SupplierOrderRepository repo,
            SupplierRepository supplierRepo,
            ProductRepository productRepo
    ) {
        this.repo = repo;
        this.supplierRepo = supplierRepo;
        this.productRepo = productRepo;
    }

    @GetMapping
    public List<SupplierOrder> getAll(@RequestParam(required = false) String supplierId) {
        if (supplierId != null) {
            return repo.findBySupplierIdOrderByCreatedAtDesc(supplierId);
        }
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierOrder> getOne(@PathVariable String id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST — krijo dhe menjehere "dërgo" porosinë (status: sent)
    @PostMapping
    @Transactional
    public ResponseEntity<SupplierOrder> create(@RequestBody SupplierOrder incoming) {
        Supplier supplier = supplierRepo.findById(incoming.getSupplierId()).orElse(null);
        if (supplier == null) {
            return ResponseEntity.badRequest().build();
        }

        SupplierOrder o = new SupplierOrder();
        o.setSupplierId(supplier.getId());
        o.setSupplierName(supplier.getName());
        o.setStatus("sent");
        o.setNotes(incoming.getNotes());
        o.setStaffId(incoming.getStaffId());
        o.setCreatedAt(System.currentTimeMillis());
        o.setSentAt(System.currentTimeMillis());

        // Generate order reference: PO-2026/XXXX
        long count = repo.count() + 1;
        String ref = String.format("PO-%d/%04d",
                LocalDateTime.now().getYear(), count);
        o.setOrderRef(ref);

        if (incoming.getItems() != null) {
            for (SupplierOrderItem incomingItem : incoming.getItems()) {
                if (incomingItem.getQuantity() <= 0) continue;

                Product product = productRepo.findById(incomingItem.getProductId()).orElse(null);
                if (product == null) continue;

                SupplierOrderItem item = new SupplierOrderItem();
                item.setOrder(o);
                item.setProductId(product.getId());
                item.setProductName(product.getName());
                item.setQuantity(incomingItem.getQuantity());
                item.setStockUnit(product.getStockUnit() != null
                        ? product.getStockUnit().name() : "PIECE");
                item.setNote(incomingItem.getNote());
                o.getItems().add(item);
            }
        }

        SupplierOrder saved = repo.save(o);
        return ResponseEntity.ok(saved);
    }

    // GET PDF i porosise
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generatePdf(@PathVariable String id) {
        SupplierOrder order = repo.findById(id).orElse(null);
        if (order == null) return ResponseEntity.notFound().build();

        Supplier supplier = supplierRepo.findById(order.getSupplierId()).orElse(null);

        try {
            byte[] pdfBytes = SupplierOrderPdfGenerator.generate(order, supplier);
            String filename = "porosia-" + order.getOrderRef().replace("/", "-") + ".pdf";

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filename + "\"")
                    .body(pdfBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!repo.existsById(id)) return ResponseEntity.notFound().build();
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}