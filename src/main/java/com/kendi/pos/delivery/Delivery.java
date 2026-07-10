package com.kendi.pos.delivery;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "supplier_id", nullable = false)
    private String supplierId;

    // Snapshot i emrit te furnitorit (nese fshihet furnitori, s'humbet)
    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    // Data e dorezimit (millis) — mund te jete e ndryshme prej createdAt
    @Column(name = "delivery_date", nullable = false)
    private long deliveryDate;

    // Statusi: 'pending' (i krijuar po s'osht konfirmu), 'confirmed' (stoku u perditesu)
    @Column(nullable = false)
    private String status = "confirmed";

    // Totali i faturës (shuma e krejt items × qmimi/cope)
    @Column(name = "total_cents", nullable = false)
    private int totalCents = 0;

    // Referenca e dokumentit (psh "FD-2026/1147")
    @Column(name = "document_ref")
    private String documentRef;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Ai qe e regjistroi
    @Column(name = "staff_id")
    private String staffId;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    // File i faturës (PDF ose imazh) — ruhet direkt ne DB
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "invoice_file", columnDefinition = "bytea")
    private byte[] invoiceFile;

    @Column(name = "invoice_file_name")
    private String invoiceFileName;

    @Column(name = "invoice_content_type")
    private String invoiceContentType;

    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<DeliveryItem> items = new ArrayList<>();

    public Delivery() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public long getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(long deliveryDate) { this.deliveryDate = deliveryDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getTotalCents() { return totalCents; }
    public void setTotalCents(int totalCents) { this.totalCents = totalCents; }
    public String getDocumentRef() { return documentRef; }
    public void setDocumentRef(String documentRef) { this.documentRef = documentRef; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public List<DeliveryItem> getItems() { return items; }
    public void setItems(List<DeliveryItem> items) { this.items = items; }
    public byte[] getInvoiceFile() { return invoiceFile; }
    public void setInvoiceFile(byte[] invoiceFile) { this.invoiceFile = invoiceFile; }
    public String getInvoiceFileName() { return invoiceFileName; }
    public void setInvoiceFileName(String invoiceFileName) { this.invoiceFileName = invoiceFileName; }
    public String getInvoiceContentType() { return invoiceContentType; }
    public void setInvoiceContentType(String invoiceContentType) { this.invoiceContentType = invoiceContentType; }
}
