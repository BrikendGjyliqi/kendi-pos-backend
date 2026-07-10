package com.kendi.pos.supplier;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "supplier_order_items")
public class SupplierOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private SupplierOrder order;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private double quantity;

    @Column(name = "stock_unit")
    private String stockUnit;

    // Note qe menaxheri mund t'a shtoje (opsional)
    @Column(columnDefinition = "TEXT")
    private String note;

    public SupplierOrderItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public SupplierOrder getOrder() { return order; }
    public void setOrder(SupplierOrder order) { this.order = order; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public String getStockUnit() { return stockUnit; }
    public void setStockUnit(String stockUnit) { this.stockUnit = stockUnit; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}