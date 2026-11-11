package com.example.smartshopmanager.models;

public class SaleItem {
    private int itemId;
    private int saleId;
    private int productId;
    private String productName; // For cart / receipt snapshot
    private int quantity;
    private double unitPrice;
    private double costPrice;

    public SaleItem(int productId, String productName, int quantity, double unitPrice) {
        this(productId, productName, quantity, unitPrice, 0.0);
    }

    public SaleItem(int productId, String productName, int quantity, double unitPrice, double costPrice) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.costPrice = costPrice;
    }

    public SaleItem(int itemId, int saleId, int productId, int quantity, double unitPrice) {
        this.itemId = itemId;
        this.saleId = saleId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    public double getTotalPrice() {
        return quantity * unitPrice;
    }
}
