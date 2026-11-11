package com.example.smartshopmanager.models;

public class Sale {
    private int saleId;
    private String saleDate;
    private double subtotal;
    private double discountPercent;
    private double totalAmount;
    private String paymentMethod;
    private int customerId;
    private String customerName; // For UI display
    private int cashierUserId;
    private String cashierName;

    public Sale(int saleId, String saleDate, double totalAmount, String paymentMethod, int customerId, String customerName) {
        this(saleId, saleDate, totalAmount, 0.0, totalAmount, paymentMethod, customerId, customerName, 0, "");
    }

    public Sale(int saleId, String saleDate, double subtotal, double discountPercent, double totalAmount,
                String paymentMethod, int customerId, String customerName, int cashierUserId, String cashierName) {
        this.saleId = saleId;
        this.saleDate = saleDate;
        this.subtotal = subtotal;
        this.discountPercent = discountPercent;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.customerId = customerId;
        this.customerName = customerName;
        this.cashierUserId = cashierUserId;
        this.cashierName = cashierName;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public String getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(String saleDate) {
        this.saleDate = saleDate;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getCashierUserId() {
        return cashierUserId;
    }

    public void setCashierUserId(int cashierUserId) {
        this.cashierUserId = cashierUserId;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }
}
