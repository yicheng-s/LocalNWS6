package com.sunyicheng.myapplication.model;

/**
 * 商品实体类
 */
public class Product {
    private int id;
    private String name;
    private String barcode;
    private String category;
    private String unit;
    private int stockQuantity;
    private String description;
    private String createdAt;
    private String updatedAt;

    public Product() {
        this.stockQuantity = 0;
    }

    public Product(String name, String barcode, String category, String unit, int stockQuantity, String description) {
        this.name = name;
        this.barcode = barcode;
        this.category = category;
        this.unit = unit;
        this.stockQuantity = stockQuantity;
        this.description = description;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return name + " [" + (barcode != null ? barcode : "无条码") + "]";
    }
}
