package com.sunyicheng.myapplication.model;

/**
 * 出入库记录实体类
 */
public class StockRecord {
    private int id;
    private int productId;
    private String type;  // "in" 入库, "out" 出库
    private int quantity;
    private String department;
    private String personName;
    private String signaturePath;
    private String remark;
    private String createdAt;

    // 关联查询字段
    private String productName;
    private String productBarcode;

    public StockRecord() {}

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPersonName() { return personName; }
    public void setPersonName(String personName) { this.personName = personName; }

    public String getSignaturePath() { return signaturePath; }
    public void setSignaturePath(String signaturePath) { this.signaturePath = signaturePath; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductBarcode() { return productBarcode; }
    public void setProductBarcode(String productBarcode) { this.productBarcode = productBarcode; }

    public String getTypeDisplay() {
        return "in".equals(type) ? "入库" : "出库";
    }
}
