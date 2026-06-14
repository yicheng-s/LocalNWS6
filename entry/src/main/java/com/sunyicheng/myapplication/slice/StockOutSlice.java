package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.ResourceTable;
import com.sunyicheng.myapplication.db.DatabaseManager;
import com.sunyicheng.myapplication.model.Product;
import com.sunyicheng.myapplication.model.StockRecord;
import com.sunyicheng.myapplication.util.DateUtils;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.*;
import ohos.agp.utils.Color;
import ohos.agp.window.dialog.ToastDialog;

/**
 * 出库操作
 */
public class StockOutSlice extends AbilitySlice {

    private static final int REQUEST_SCAN = 202;
    private static final int REQUEST_SIGNATURE = 302;

    private DatabaseManager db;
    private TextField editBarcode, editProductName, editQuantity, editDepartment, editPerson, editRemark;
    private Text signStatus, infoStock;
    private Product currentProduct;
    private String signaturePath;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        db = DatabaseManager.getInstance();
        setUIContent(ResourceTable.Layout_slice_stock_out);

        editBarcode = (TextField) findComponentById(ResourceTable.Id_edit_barcode);
        editProductName = (TextField) findComponentById(ResourceTable.Id_edit_product_name);
        editQuantity = (TextField) findComponentById(ResourceTable.Id_edit_quantity);
        editDepartment = (TextField) findComponentById(ResourceTable.Id_edit_department);
        editPerson = (TextField) findComponentById(ResourceTable.Id_edit_person);
        editRemark = (TextField) findComponentById(ResourceTable.Id_edit_remark);
        signStatus = (Text) findComponentById(ResourceTable.Id_sign_status);

        // 动态创建库存信息显示
        infoStock = new Text(getContext());
        infoStock.setText("当前库存量: --");
        infoStock.setTextSize(18);
        infoStock.setTextColor(new Color(0xFF333333));
        infoStock.setHeight(36);
        infoStock.setWidth(DirectionalLayout.LayoutConfig.MATCH_PARENT);
        infoStock.setPadding(8, 4, 8, 4);
        infoStock.setMarginTop(4);

        Component btnQuery = findComponentById(ResourceTable.Id_btn_query_product);
        if (btnQuery != null && btnQuery.getComponentParent() instanceof DirectionalLayout) {
            ((DirectionalLayout) btnQuery.getComponentParent()).addComponent(infoStock);
        }

        findComponentById(ResourceTable.Id_btn_back).setClickedListener(c -> terminate());
        findComponentById(ResourceTable.Id_btn_scan).setClickedListener(c ->
                presentForResult(new BarcodeScanSlice(), new Intent(), REQUEST_SCAN));
        findComponentById(ResourceTable.Id_btn_query_product).setClickedListener(c -> queryProduct());
        findComponentById(ResourceTable.Id_btn_sign).setClickedListener(c ->
                presentForResult(new SignatureSlice(), new Intent(), REQUEST_SIGNATURE));
        findComponentById(ResourceTable.Id_btn_confirm_stock_out).setClickedListener(c -> confirmStockOut());
    }

    private void queryProduct() {
        String barcode = editBarcode.getText();
        String name = editProductName.getText();
        currentProduct = null;

        if (barcode != null && !barcode.trim().isEmpty())
            currentProduct = db.getProductByBarcode(barcode.trim());
        if (currentProduct == null && name != null && !name.trim().isEmpty())
            currentProduct = db.getProductByName(name.trim());


        if (currentProduct != null) {
            if (infoStock != null) infoStock.setText("当前库存量: " + currentProduct.getStockQuantity());
            editProductName.setText(currentProduct.getName());
            if (currentProduct.getBarcode() != null) editBarcode.setText(currentProduct.getBarcode());
        } else {
            if (infoStock != null) infoStock.setText("当前库存量: --");
        }
    }

    @Override
    protected void onResult(int requestCode, Intent resultIntent) {
        if (requestCode == REQUEST_SCAN && resultIntent != null) {
            String barcode = resultIntent.getStringParam("barcode");
            if (barcode != null && !barcode.isEmpty()) {
                editBarcode.setText(barcode); queryProduct();
            }
        } else if (requestCode == REQUEST_SIGNATURE && resultIntent != null) {
            signaturePath = resultIntent.getStringParam("signature_path");
            if (signaturePath != null && signStatus != null) {
                signStatus.setText("已签字");
                signStatus.setTextColor(new Color(0xFF43A047));
            }
        }
    }

    private void confirmStockOut() {
        if (currentProduct == null) { showToast("请先查询商品信息"); return; }
        String qtyStr = editQuantity.getText();
        if (qtyStr == null || qtyStr.trim().isEmpty()) { showToast("请输入出库数量"); return; }
        int quantity;
        try { quantity = Integer.parseInt(qtyStr.trim());
            if (quantity <= 0) { showToast("出库数量必须大于0"); return; }
        } catch (NumberFormatException e) { showToast("出库数量格式不正确"); return; }
        if (quantity > currentProduct.getStockQuantity()) {
            showToast("库存不足！当前库存: " + currentProduct.getStockQuantity()); return;
        }
        String person = editPerson.getText();
        if (person == null || person.trim().isEmpty()) { showToast("请输入领取人姓名"); return; }
        if (signaturePath == null) { showToast("请先签字确认"); return; }

        String now = DateUtils.getCurrentDateTime();
        String department = editDepartment.getText();
        db.updateProductStock(currentProduct.getId(), currentProduct.getStockQuantity() - quantity, now);

        StockRecord record = new StockRecord();
        record.setProductId(currentProduct.getId()); record.setType("out");
        record.setQuantity(quantity);
        record.setDepartment(department != null ? department.trim() : null);
        record.setPersonName(person.trim()); record.setSignaturePath(signaturePath);
        record.setRemark(editRemark.getText()); record.setCreatedAt(now);

        if (db.insertStockRecord(record) > 0) {
            showToast("出库成功！"); clearForm();
        } else { showToast("出库失败"); }
    }

    private void clearForm() {
        editBarcode.setText(""); editProductName.setText(""); editQuantity.setText("");
        editPerson.setText(""); editDepartment.setText(""); editRemark.setText("");
        signaturePath = null; currentProduct = null;
        if (infoStock != null) infoStock.setText("当前库存量: --");
        if (signStatus != null) { signStatus.setText("尚未签字"); signStatus.setTextColor(new Color(0xFFE53935)); }
    }

    private void showToast(String m) {
        new ToastDialog(getContext()).setText(m).setDuration(2000).show();
    }
}
