package com.kendi.pos.order;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "table_id", nullable = false)
    private String tableId;

    @Column(nullable = false)
    private String status;  // open, closed, paid, cancelled

    @Column(nullable = false)
    private int subtotal;

    @Column(nullable = false)
    private int discount;

    @Column(nullable = false)
    private int total;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "cash_given")
    private Integer cashGiven;

    @Column
    private Boolean fiscal;

    @Column(name = "staff_id")
    private String staffId;

    @Column(name = "opened_at", nullable = false)
    private long openedAt;

    @Column(name = "closed_at")
    private Long closedAt;

    @Column(name = "paid_at")
    private Long paidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    public Order() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTableId() { return tableId; }
    public void setTableId(String tableId) { this.tableId = tableId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSubtotal() { return subtotal; }
    public void setSubtotal(int subtotal) { this.subtotal = subtotal; }
    public int getDiscount() { return discount; }
    public void setDiscount(int discount) { this.discount = discount; }
    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Integer getCashGiven() { return cashGiven; }
    public void setCashGiven(Integer cashGiven) { this.cashGiven = cashGiven; }
    public Boolean getFiscal() { return fiscal; }
    public void setFiscal(Boolean fiscal) { this.fiscal = fiscal; }
    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }
    public long getOpenedAt() { return openedAt; }
    public void setOpenedAt(long openedAt) { this.openedAt = openedAt; }
    public Long getClosedAt() { return closedAt; }
    public void setClosedAt(Long closedAt) { this.closedAt = closedAt; }
    public Long getPaidAt() { return paidAt; }
    public void setPaidAt(Long paidAt) { this.paidAt = paidAt; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}