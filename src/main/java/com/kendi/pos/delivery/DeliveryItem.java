package com.kendi.pos.delivery;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "delivery_items")
public class DeliveryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    @JsonBackReference
    private Delivery delivery;

    @Column(name = "product_id", nullable = false)
    private String productId;

    // Snapshot i emrit te produktit
    @Column(name = "product_name", nullable = false)
    private String productName;

    // Sasia (double per kg, po per PIECE do jete numer i plote)
    @Column(nullable = false)
    private double quantity;

    // Cmimi per njesi ne cent — kjo osht qmimi qe blen furnitori, jo qmimi shitjes
    @Column(name = "unit_price_cents", nullable = false)
    private int unitPriceCents = 0;

    // Totali per kete rresht = quantity × unitPriceCents
    @Column(name = "line_total_cents", nullable = false)
    private int lineTotalCents = 0;

    public DeliveryItem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Delivery getDelivery() { return delivery; }
    public void setDelivery(Delivery delivery) { this.delivery = delivery; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public int getUnitPriceCents() { return unitPriceCents; }
    public void setUnitPriceCents(int unitPriceCents) { this.unitPriceCents = unitPriceCents; }
    public int getLineTotalCents() { return lineTotalCents; }
    public void setLineTotalCents(int lineTotalCents) { this.lineTotalCents = lineTotalCents; }
}