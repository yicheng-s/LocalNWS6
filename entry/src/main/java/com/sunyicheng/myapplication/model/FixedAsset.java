package com.sunyicheng.myapplication.model;

/**
 * 固定资产实体类
 */
public class FixedAsset {
    private int id;
    private String name;
    private String barcode;
    private String category;
    private String location;
    private String department;
    private String status;
    private int quantity;
    private String unit;
    private String purchaseDate;
    private double value;
    private String remark;
    private String statusDate;    // 状态变更日期
    private String createdAt;
    private String updatedAt;

    public FixedAsset() {
        this.status = "正常";
        this.quantity = 1;
        this.value = 0.0;
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

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getStatusDate() { return statusDate; }
    public void setStatusDate(String statusDate) { this.statusDate = statusDate; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return name + " (" + (category != null ? category : "") + ")";
    }
}
