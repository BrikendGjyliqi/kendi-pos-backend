package com.kendi.pos.supplier;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "supplier_orders")
public class SupplierOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "supplier_id", nullable = false)
    private String supplierId;

    @Column(name = "supplier_name", nullable = false)
    private String supplierName;

    // draft / sent / received / cancelled
    @Column(nullable = false)
    private String status = "draft";

    @Column(name = "order_ref", nullable = false)
    private String orderRef;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "staff_id")
    private String staffId;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "sent_at")
    private Long sentAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<SupplierOrderItem> items = new ArrayList<>();

    public SupplierOrder() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOrderRef() { return orderRef; }
    public void setOrderRef(String orderRef) { this.orderRef = orderRef; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public Long getSentAt() { return sentAt; }
    public void setSentAt(Long sentAt) { this.sentAt = sentAt; }
    public List<SupplierOrderItem> getItems() { return items; }
    public void setItems(List<SupplierOrderItem> items) { this.items = items; }
}