package com.example.smartshopmanager.models;

/**
 * Represents a shop staff user account with a role and hashed PIN.
 */
public class User {
    private int userId;
    private String fullName;
    private String username;
    private String pinHash;
    private String role;
    private boolean active;
    private String createdAt;

    public User(int userId, String fullName, String username, String pinHash, String role, boolean active, String createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.pinHash = pinHash;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public User(String fullName, String username, String role, boolean active) {
        this.fullName = fullName;
        this.username = username;
        this.role = role;
        this.active = active;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}
