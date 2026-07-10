package com.kendi.pos.delivery;

import com.kendi.pos.product.Product;
import com.kendi.pos.product.ProductRepository;
import com.kendi.pos.supplier.Supplier;
import com.kendi.pos.supplier.SupplierRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@CrossOrigin(origins = "*")
public class DeliveryController {

    private final DeliveryRepository repo;
    private final SupplierRepository supplierRepo;
    private final ProductRepository productRepo;

    public DeliveryController(
            DeliveryRepository repo,
            SupplierRepository supplierRepo,
            ProductRepository productRepo
    ) {
        this.repo = repo;
        this.supplierRepo = supplierRepo;
        this.productRepo = productRepo;
    }

    @GetMapping
    public List<Delivery> getAll(@RequestParam(required = false) String supplierId) {
        if (supplierId != null) {
            return repo.findBySupplierIdOrderByDeliveryDateDesc(supplierId);
        }
        return repo.findAllByOrderByDeliveryDateDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Delivery> getOne(@PathVariable String id) {
        return repo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET file (PDF/imazh) qe u ruajt me kete delivery
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> getFile(@PathVariable String id) {
        Delivery d = repo.findById(id).orElse(null);
        if (d == null || d.getInvoiceFile() == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = d.getInvoiceContentType() != null
                ? d.getInvoiceContentType() : "application/octet-stream";
        String fileName = d.getInvoiceFileName() != null
                ? d.getInvoiceFileName() : "invoice-" + id;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(d.getInvoiceFile());
    }

    // GET metadata — a ka file ruajtur?
    @GetMapping("/{id}/file-info")
    public ResponseEntity<Object> getFileInfo(@PathVariable String id) {
        return repo.findById(id).map(d -> {
            boolean hasFile = d.getInvoiceFile() != null && d.getInvoiceFile().length > 0;
            return ResponseEntity.<Object>ok(java.util.Map.of(
                    "hasFile", hasFile,
                    "fileName", d.getInvoiceFileName() != null ? d.getInvoiceFileName() : "",
                    "contentType", d.getInvoiceContentType() != null ? d.getInvoiceContentType() : "",
                    "sizeBytes", hasFile ? d.getInvoiceFile().length : 0
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Delivery> create(@RequestBody Delivery incoming) {
        Supplier supplier = supplierRepo.findById(incoming.getSupplierId()).orElse(null);
        if (supplier == null) {
            return ResponseEntity.badRequest().build();
        }

        Delivery d = new Delivery();
        d.setSupplierId(supplier.getId());
        d.setSupplierName(supplier.getName());
        d.setDeliveryDate(incoming.getDeliveryDate() > 0 ? incoming.getDeliveryDate() : System.currentTimeMillis());
        d.setStatus("confirmed");
        d.setDocumentRef(incoming.getDocumentRef());
        d.setNotes(incoming.getNotes());
        d.setStaffId(incoming.getStaffId());
        d.setCreatedAt(System.currentTimeMillis());

        int total = 0;
        if (incoming.getItems() != null) {
            for (DeliveryItem incomingItem : incoming.getItems()) {
                if (incomingItem.getQuantity() <= 0) continue;

                Product product = productRepo.findById(incomingItem.getProductId()).orElse(null);
                if (product == null) continue;

                DeliveryItem item = new DeliveryItem();
                item.setDelivery(d);
                item.setProductId(product.getId());
                item.setProductName(product.getName());
                item.setQuantity(incomingItem.getQuantity());
                item.setUnitPriceCents(Math.max(0, incomingItem.getUnitPriceCents()));
                int lineTotal = (int) Math.round(incomingItem.getQuantity() * item.getUnitPriceCents());
                item.setLineTotalCents(lineTotal);
                total += lineTotal;
                d.getItems().add(item);

                if (product.isTrackStock()) {
                    product.setStockQuantity(product.getStockQuantity() + incomingItem.getQuantity());
                    productRepo.save(product);
                }
            }
        }

        d.setTotalCents(total);
        Delivery saved = repo.save(d);
        return ResponseEntity.ok(saved);
    }

    // Attach file te nje delivery ekzistuese
    @PostMapping("/{id}/attach-file")
    @Transactional
    public ResponseEntity<Delivery> attachFile(
            @PathVariable String id,
            @RequestBody FileUploadRequest req
    ) {
        return repo.findById(id).map(d -> {
            if (req.getFileBase64() != null && !req.getFileBase64().isEmpty()) {
                d.setInvoiceFile(java.util.Base64.getDecoder().decode(req.getFileBase64()));
                d.setInvoiceFileName(req.getFileName());
                d.setInvoiceContentType(req.getContentType());
                return ResponseEntity.ok(repo.save(d));
            }
            return ResponseEntity.badRequest().<Delivery>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Delivery d = repo.findById(id).orElse(null);
        if (d == null) {
            return ResponseEntity.notFound().build();
        }
        for (DeliveryItem item : d.getItems()) {
            productRepo.findById(item.getProductId()).ifPresent(product -> {
                if (product.isTrackStock()) {
                    double newQty = Math.max(0, product.getStockQuantity() - item.getQuantity());
                    product.setStockQuantity(newQty);
                    productRepo.save(product);
                }
            });
        }
        repo.delete(d);
        return ResponseEntity.noContent().build();
    }

    // DTO per file upload
    public static class FileUploadRequest {
        private String fileBase64;
        private String fileName;
        private String contentType;

        public String getFileBase64() { return fileBase64; }
        public void setFileBase64(String fileBase64) { this.fileBase64 = fileBase64; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
    }
}