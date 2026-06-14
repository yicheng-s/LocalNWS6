package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.db.DatabaseManager;
import com.sunyicheng.myapplication.model.FixedAsset;
import com.sunyicheng.myapplication.util.DateUtils;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.*;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.utils.Color;
import ohos.agp.utils.TextAlignment;
import ohos.agp.window.dialog.ToastDialog;

/**
 * 固定资产新增/编辑页面 (适配 API 6)
 */
public class FixedAssetEditSlice extends AbilitySlice {

    private DatabaseManager db;
    private TextField editName, editBarcode, editCategory, editLocation;
    private TextField editDepartment, editQuantity, editUnit, editPurchaseDate, editValue, editRemark;
    private FixedAsset editingAsset;
    private boolean isEditMode = false;
    private Button btnDelete;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        db = DatabaseManager.getInstance();

        int assetId = intent.getIntParam("asset_id", -1);
        if (assetId > 0) {
            isEditMode = true;
            editingAsset = db.getFixedAssetById(assetId);
        }
        buildUI();
    }

    private void buildUI() {
        DirectionalLayout root = new DirectionalLayout(getContext());
        root.setOrientation(Component.VERTICAL);
        root.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_PARENT));

        ShapeElement rootBg = new ShapeElement();
        rootBg.setRgbColor(RgbColor.fromArgbInt(0xFFF5F5F5));
        root.setBackground(rootBg);

        // 标题栏
        DirectionalLayout titleBar = new DirectionalLayout(getContext());
        titleBar.setOrientation(Component.HORIZONTAL);
        titleBar.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 56));
        titleBar.setPadding(8, 8, 8, 8);

        ShapeElement titleBg = new ShapeElement();
        titleBg.setRgbColor(RgbColor.fromArgbInt(0xFF1E88E5));
        titleBar.setBackground(titleBg);

        Button btnBack = new Button(getContext());
        btnBack.setText("返回");
        btnBack.setTextSize(14);
        btnBack.setTextColor(Color.WHITE);
        btnBack.setBackground(titleBg);
        btnBack.setLayoutConfig(new DirectionalLayout.LayoutConfig(60, 40));
        btnBack.setClickedListener(c -> terminate());
        titleBar.addComponent(btnBack);

        Text titleText = new Text(getContext());
        titleText.setText(isEditMode ? "编辑固定资产" : "新增固定资产");
        titleText.setTextSize(20);
        titleText.setTextColor(Color.WHITE);
        titleText.setTextAlignment(TextAlignment.CENTER);
        DirectionalLayout.LayoutConfig tConf = new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_PARENT);
        tConf.weight = 1;
        titleText.setLayoutConfig(tConf);
        titleBar.addComponent(titleText);

        Button btnSave = new Button(getContext());
        btnSave.setText("保存");
        btnSave.setTextSize(14);
        btnSave.setTextColor(Color.WHITE);
        ShapeElement saveBg = new ShapeElement();
        saveBg.setRgbColor(RgbColor.fromArgbInt(0xFF43A047));
        btnSave.setBackground(saveBg);
        btnSave.setLayoutConfig(new DirectionalLayout.LayoutConfig(80, 40));
        btnSave.setClickedListener(c -> saveAsset());
        titleBar.addComponent(btnSave);

        root.addComponent(titleBar);

        // 表单
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_PARENT));

        DirectionalLayout form = new DirectionalLayout(getContext());
        form.setOrientation(Component.VERTICAL);
        form.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT));
        form.setPadding(16, 16, 16, 16);

        editName = addFormField(form, "资产名称 *", "如: 铁皮文件柜");
        editBarcode = addFormField(form, "资产编号/条码", "手动输入或扫描");
        editCategory = addFormField(form, "分类", "如: 家具、电子设备");
        editLocation = addFormField(form, "存放位置", "如: 3楼档案室");
        editDepartment = addFormField(form, "所属部门", "如: 行政部");
        editQuantity = addFormField(form, "数量", "1");
        editUnit = addFormField(form, "单位", "如: 个、台、套");
        editPurchaseDate = addFormField(form, "购入日期", "如: 2024-01-15");
        editValue = addFormField(form, "价值(元)", "0");
        editRemark = addFormField(form, "备注", "");

        // 删除按钮
        btnDelete = new Button(getContext());
        btnDelete.setText("删除此资产");
        btnDelete.setTextSize(16);
        btnDelete.setTextColor(Color.WHITE);
        ShapeElement delBg = new ShapeElement();
        delBg.setRgbColor(RgbColor.fromArgbInt(0xFFE53935));
        btnDelete.setBackground(delBg);
        btnDelete.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 48));
        btnDelete.setMarginTop(24);
        btnDelete.setVisibility(isEditMode ? Component.VISIBLE : Component.HIDE);
        btnDelete.setClickedListener(c -> {
            if (editingAsset != null) {
                db.deleteFixedAsset(editingAsset.getId());
                showToast("资产已删除");
            }
            setResult(new Intent());
            terminate();
        });
        form.addComponent(btnDelete);

        scrollView.addComponent(form);
        root.addComponent(scrollView);
        setUIContent(root);

        if (isEditMode && editingAsset != null) loadAssetData();
    }

    private TextField addFormField(DirectionalLayout parent, String label, String hint) {
        Text labelText = new Text(getContext());
        labelText.setText(label);
        labelText.setTextSize(14);
        labelText.setTextColor(new Color(0xFF666666));
        labelText.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 30));
        parent.addComponent(labelText);

        TextField field = new TextField(getContext());
        field.setHint(hint);
        field.setTextSize(16);
        field.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 48));
        field.setPadding(12, 8, 12, 8);

        ShapeElement fieldBg = new ShapeElement();
        fieldBg.setRgbColor(RgbColor.fromArgbInt(0xFFFFFFFF));
        fieldBg.setCornerRadius(4f);
        field.setBackground(fieldBg);
        field.setMarginBottom(12);
        parent.addComponent(field);

        return field;
    }

    private void loadAssetData() {
        editName.setText(editingAsset.getName());
        editBarcode.setText(editingAsset.getBarcode());
        editCategory.setText(editingAsset.getCategory());
        editLocation.setText(editingAsset.getLocation());
        editDepartment.setText(editingAsset.getDepartment());
        editQuantity.setText(String.valueOf(editingAsset.getQuantity()));
        editUnit.setText(editingAsset.getUnit());
        editPurchaseDate.setText(editingAsset.getPurchaseDate());
        editValue.setText(String.valueOf((int) editingAsset.getValue()));
        editRemark.setText(editingAsset.getRemark());
    }

    private void saveAsset() {
        String name = editName.getText();
        if (name == null || name.trim().isEmpty()) {
            showToast("请输入资产名称"); return;
        }
        String now = DateUtils.getCurrentDateTime();
        if (isEditMode && editingAsset != null) {
            editingAsset.setName(name.trim());
            editingAsset.setBarcode(getText(editBarcode));
            editingAsset.setCategory(getText(editCategory));
            editingAsset.setLocation(getText(editLocation));
            editingAsset.setDepartment(getText(editDepartment));
            editingAsset.setQuantity(parseInt(editQuantity.getText(), 1));
            editingAsset.setUnit(getText(editUnit));
            editingAsset.setPurchaseDate(getText(editPurchaseDate));
            editingAsset.setValue(parseDouble(editValue.getText(), 0));
            editingAsset.setRemark(getText(editRemark));
            editingAsset.setUpdatedAt(now);
            db.updateFixedAsset(editingAsset);
            showToast("资产更新成功");
        } else {
            FixedAsset asset = new FixedAsset();
            asset.setName(name.trim());
            asset.setBarcode(getText(editBarcode));
            asset.setCategory(getText(editCategory));
            asset.setLocation(getText(editLocation));
            asset.setDepartment(getText(editDepartment));
            asset.setQuantity(parseInt(editQuantity.getText(), 1));
            asset.setUnit(getText(editUnit));
            asset.setPurchaseDate(getText(editPurchaseDate));
            asset.setValue(parseDouble(editValue.getText(), 0));
            asset.setRemark(getText(editRemark));
            asset.setCreatedAt(now);
            asset.setUpdatedAt(now);
            db.insertFixedAsset(asset);
            showToast("资产添加成功");
        }
        // API 6: setResult 只接受 Intent
        setResult(new Intent());
        terminate();
    }

    private String getText(TextField field) {
        String text = field.getText();
        return (text != null && !text.trim().isEmpty()) ? text.trim() : null;
    }
    private int parseInt(String str, int defaultVal) {
        try { return Integer.parseInt(str); } catch (Exception e) { return defaultVal; }
    }
    private double parseDouble(String str, double defaultVal) {
        try { return Double.parseDouble(str); } catch (Exception e) { return defaultVal; }
    }
    private void showToast(String message) {
        new ToastDialog(getContext()).setText(message).setDuration(2000).show();
    }
}
