package com.kendi.pos.order;

public class PaymentRequest {
    private String method;       // cash, card, other
    private Integer cashGiven;
    private Boolean fiscal;

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public Integer getCashGiven() { return cashGiven; }
    public void setCashGiven(Integer cashGiven) { this.cashGiven = cashGiven; }
    public Boolean getFiscal() { return fiscal; }
    public void setFiscal(Boolean fiscal) { this.fiscal = fiscal; }
}