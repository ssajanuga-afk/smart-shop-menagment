package com.example.smartshopmanager.models;

public class Product {
    private int productId;
    private String name;
    private int categoryId;
    private double price;
    private double costPrice;
    private int quantity;
    private String categoryName; // For UI convenience
    private String imageUrl; // For API products
    private boolean active = true;

    public Product(int productId, String name, int categoryId, double price, double costPrice, int quantity) {
        this.productId = productId;
        this.name = name;
        this.categoryId = categoryId;
        this.price = price;
        this.costPrice = costPrice;
        this.quantity = quantity;
        this.active = true;
    }

    public Product(String name, int categoryId, double price, double costPrice, int quantity) {
        this.name = name;
        this.categoryId = categoryId;
        this.price = price;
        this.costPrice = costPrice;
        this.quantity = quantity;
    }

    // Constructor for API products
    public Product(String name, double price, String categoryName, String imageUrl) {
        this.name = name;
        this.price = price;
        this.categoryName = categoryName;
        this.imageUrl = imageUrl;
        this.costPrice = price * 0.7; // default estimate
        this.quantity = 10; // default initial stock
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
