package com.kendi.pos.product;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "category_id", nullable = false)
    private String categoryId;

    @Column(nullable = false)
    private int price;  // ne cent

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    // ─── Stock tracking ───
    @Column(name = "track_stock", nullable = false)
    private boolean trackStock = false;

    @Column(name = "auto_deduct_on_sale", nullable = false)
    private boolean autoDeductOnSale = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_unit")
    private StockUnit stockUnit;

    @Column(name = "stock_quantity", nullable = false)
    private double stockQuantity = 0.0;

    @Column(name = "low_stock_threshold", nullable = false)
    private double lowStockThreshold = 0.0;

    public Product() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isTrackStock() { return trackStock; }
    public void setTrackStock(boolean trackStock) { this.trackStock = trackStock; }
    public boolean isAutoDeductOnSale() { return autoDeductOnSale; }
    public void setAutoDeductOnSale(boolean autoDeductOnSale) { this.autoDeductOnSale = autoDeductOnSale; }
    public StockUnit getStockUnit() { return stockUnit; }
    public void setStockUnit(StockUnit stockUnit) { this.stockUnit = stockUnit; }
    public double getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(double stockQuantity) { this.stockQuantity = stockQuantity; }
    public double getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(double lowStockThreshold) { this.lowStockThreshold = lowStockThreshold; }
}