package com.kendi.pos.order;

public class PaymentRequest {
    private String method;       // cash, card, other
    private Integer cashGiven;
    private Boolean fiscal;
    private Integer tipAmount;    // tip ne cent
    private Integer tipPercent;   // 0, 5, 10, 15, ose custom

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Integer getCashGiven() { return cashGiven; }
    public void setCashGiven(Integer cashGiven) { this.cashGiven = cashGiven; }
    public Boolean getFiscal() { return fiscal; }
    public void setFiscal(Boolean fiscal) { this.fiscal = fiscal; }
    public Integer getTipAmount() { return tipAmount; }
    public void setTipAmount(Integer tipAmount) { this.tipAmount = tipAmount; }
    public Integer getTipPercent() { return tipPercent; }
    public void setTipPercent(Integer tipPercent) { this.tipPercent = tipPercent; }
}