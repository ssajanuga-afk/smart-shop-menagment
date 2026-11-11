package com.example.smartshopmanager.models;

public class Expense {
    private int expenseId;
    private String description;
    private double amount;
    private String category;
    private String date; // yyyy-MM-dd
    private String recordedBy;

    public Expense(int expenseId, String description, double amount, String category, String date) {
        this(expenseId, description, amount, category, date, "");
    }

    public Expense(int expenseId, String description, double amount, String category, String date, String recordedBy) {
        this.expenseId = expenseId;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.recordedBy = recordedBy;
    }

    public Expense(String description, double amount, String category, String date) {
        this(description, amount, category, date, "");
    }

    public Expense(String description, double amount, String category, String date, String recordedBy) {
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.recordedBy = recordedBy;
    }

    public int getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(int expenseId) {
        this.expenseId = expenseId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }
}
