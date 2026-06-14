package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.ResourceTable;
import com.sunyicheng.myapplication.db.DatabaseManager;
import com.sunyicheng.myapplication.model.Product;
import com.sunyicheng.myapplication.util.DateUtils;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.*;
import ohos.agp.window.dialog.ToastDialog;

/**
 * 商品新增/编辑页面 (适配 API 6)
 */
public class ProductEditSlice extends AbilitySlice {

    private static final int REQUEST_SCAN = 200;

    private DatabaseManager db;
    private TextField editName, editBarcode, editCategory, editUnit, editStock, editDescription;
    private Button btnDelete;
    private Text titleText;
    private Product editingProduct;
    private boolean isEditMode = false;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        db = DatabaseManager.getInstance();
        setUIContent(ResourceTable.Layout_slice_product_edit);

        initViews();

        int productId = intent.getIntParam("product_id", -1);
        if (productId > 0) {
            isEditMode = true;
            editingProduct = db.getProductById(productId);
            if (editingProduct != null) {
                loadProductData();
            }
        }
    }

    private void initViews() {
        editName = (TextField) findComponentById(ResourceTable.Id_edit_name);
        editBarcode = (TextField) findComponentById(ResourceTable.Id_edit_barcode);
        editCategory = (TextField) findComponentById(ResourceTable.Id_edit_category);
        editUnit = (TextField) findComponentById(ResourceTable.Id_edit_unit);
        editStock = (TextField) findComponentById(ResourceTable.Id_edit_stock);
        editDescription = (TextField) findComponentById(ResourceTable.Id_edit_description);
        btnDelete = (Button) findComponentById(ResourceTable.Id_btn_delete);
        titleText = (Text) findComponentById(ResourceTable.Id_title_text);

        if (isEditMode) {
            titleText.setText("编辑商品");
            btnDelete.setVisibility(Component.VISIBLE);
        }

        findComponentById(ResourceTable.Id_btn_back).setClickedListener(c -> terminate());
        findComponentById(ResourceTable.Id_btn_save).setClickedListener(c -> saveProduct());
        findComponentById(ResourceTable.Id_btn_scan_barcode).setClickedListener(c -> {
            presentForResult(new BarcodeScanSlice(), new Intent(), REQUEST_SCAN);
        });
        btnDelete.setClickedListener(c -> deleteProduct());
    }

    private void loadProductData() {
        editName.setText(editingProduct.getName());
        editBarcode.setText(editingProduct.getBarcode());
        editCategory.setText(editingProduct.getCategory());
        editUnit.setText(editingProduct.getUnit());
        editStock.setText(String.valueOf(editingProduct.getStockQuantity()));
        editDescription.setText(editingProduct.getDescription());
    }

    @Override
    protected void onResult(int requestCode, Intent resultIntent) {
        if (requestCode == REQUEST_SCAN && resultIntent != null) {
            String barcode = resultIntent.getStringParam("barcode");
            if (barcode != null && !barcode.isEmpty()) {
                editBarcode.setText(barcode);
            }
        }
    }

    private void saveProduct() {
        String name = editName.getText();
        if (name == null || name.trim().isEmpty()) {
            showToast("请输入商品名称");
            return;
        }

        String barcode = editBarcode.getText();
        String category = editCategory.getText();
        String unit = editUnit.getText();
        String stockStr = editStock.getText();
        String description = editDescription.getText();
        String now = DateUtils.getCurrentDateTime();

        int stock = 0;
        if (stockStr != null && !stockStr.isEmpty()) {
            try {
                stock = Integer.parseInt(stockStr);
            } catch (NumberFormatException e) {
                showToast("库存数量格式不正确");
                return;
            }
        }

        if (isEditMode && editingProduct != null) {
            editingProduct.setName(name.trim());
            editingProduct.setBarcode(barcode != null ? barcode.trim() : null);
            editingProduct.setCategory(category != null ? category.trim() : null);
            editingProduct.setUnit(unit != null ? unit.trim() : null);
            editingProduct.setStockQuantity(stock);
            editingProduct.setDescription(description != null ? description.trim() : null);
            editingProduct.setUpdatedAt(now);
            db.updateProduct(editingProduct);
            showToast("商品更新成功");
        } else {
            if (barcode != null && !barcode.trim().isEmpty()) {
                Product exist = db.getProductByBarcode(barcode.trim());
                if (exist != null) {
                    showToast("该条码已存在，请检查"); return;
                }
            }
            // 检查名称唯一性
            if (name != null && !name.trim().isEmpty()) {
                Product exist = db.getProductByName(name.trim());
                if (exist != null) {
                    showToast("该商品名称已存在，请检查"); return;
                }
            }

            Product product = new Product();
            product.setName(name.trim());
            product.setBarcode(barcode != null ? barcode.trim() : null);
            product.setCategory(category != null ? category.trim() : null);
            product.setUnit(unit != null ? unit.trim() : "个");
            product.setStockQuantity(stock);
            product.setDescription(description != null ? description.trim() : null);
            product.setCreatedAt(now);
            product.setUpdatedAt(now);

            long id = db.insertProduct(product);
            if (id > 0) {
                showToast("商品添加成功");
            } else {
                showToast("添加失败，请重试");
                return;
            }
        }

        // API 6: setResult 只接受 Intent
        setResult(new Intent());
        terminate();
    }

    private void deleteProduct() {
        if (editingProduct == null) return;
        db.deleteProduct(editingProduct.getId());
        showToast("商品已删除");
        setResult(new Intent());
        terminate();
    }

    private void showToast(String message) {
        new ToastDialog(getContext())
                .setText(message)
                .setDuration(2000)
                .show();
    }
}
