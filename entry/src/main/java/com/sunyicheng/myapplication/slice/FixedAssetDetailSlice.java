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
 * 固定资产详情页 - 查看信息 + 状态切换
 */
public class FixedAssetDetailSlice extends AbilitySlice {

    private FixedAsset asset;
    private DatabaseManager db;
    private TextField dateInput;
    private DirectionalLayout dateLayout;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        db = DatabaseManager.getInstance();
        int assetId = intent.getIntParam("asset_id", -1);
        if (assetId <= 0) { terminate(); return; }
        asset = db.getFixedAssetById(assetId);
        if (asset == null) { terminate(); return; }
        buildUI();
    }

    @Override
    protected void onActive() {
        super.onActive();
        if (asset != null) {
            asset = db.getFixedAssetById(asset.getId());
            if (asset != null) buildUI();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 确保返回时通知列表页刷新
        setResult(new Intent());
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

        // ---- 标题栏 ----
        DirectionalLayout bar = new DirectionalLayout(getContext());
        bar.setOrientation(Component.HORIZONTAL);
        bar.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 56));
        ShapeElement barBg = new ShapeElement();
        barBg.setRgbColor(RgbColor.fromArgbInt(0xFF1E88E5));
        bar.setBackground(barBg);
        bar.setPadding(8, 8, 8, 8);

        Button btnBack = new Button(getContext());
        btnBack.setText("返回"); btnBack.setTextSize(14); btnBack.setTextColor(Color.WHITE);
        btnBack.setBackground(barBg);
        btnBack.setLayoutConfig(new DirectionalLayout.LayoutConfig(60, 40));
        btnBack.setClickedListener(c -> { setResult(new Intent()); terminate(); });
        bar.addComponent(btnBack);

        Text title = new Text(getContext());
        title.setText("资产详情"); title.setTextSize(20); title.setTextColor(Color.WHITE);
        title.setTextAlignment(TextAlignment.CENTER);
        DirectionalLayout.LayoutConfig tc = new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, DirectionalLayout.LayoutConfig.MATCH_PARENT);
        tc.weight = 1; title.setLayoutConfig(tc);
        bar.addComponent(title);

        Button btnEdit = new Button(getContext());
        btnEdit.setText("编辑"); btnEdit.setTextSize(14); btnEdit.setTextColor(Color.WHITE);
        ShapeElement editBg = new ShapeElement();
        editBg.setRgbColor(RgbColor.fromArgbInt(0xFF43A047));
        btnEdit.setBackground(editBg);
        btnEdit.setLayoutConfig(new DirectionalLayout.LayoutConfig(60, 40));
        btnEdit.setClickedListener(c -> {
            Intent i = new Intent();
            i.setParam("asset_id", asset.getId());
            presentForResult(new FixedAssetEditSlice(), i, 500);
        });
        bar.addComponent(btnEdit);
        root.addComponent(bar);

        // ---- 内容 ----
        ScrollView sv = new ScrollView(getContext());
        sv.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_PARENT));

        DirectionalLayout content = new DirectionalLayout(getContext());
        content.setOrientation(Component.VERTICAL);
        content.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT));
        content.setPadding(16, 16, 16, 16);

        content.addComponent(infoRow("名称", asset.getName()));
        content.addComponent(infoRow("编号/条码", nvl(asset.getBarcode())));
        content.addComponent(infoRow("分类", nvl(asset.getCategory())));
        content.addComponent(infoRow("位置", nvl(asset.getLocation())));
        content.addComponent(infoRow("部门", nvl(asset.getDepartment())));
        content.addComponent(infoRow("数量", asset.getQuantity() + " " + nvl(asset.getUnit())));
        content.addComponent(infoRow("购入日期", nvl(asset.getPurchaseDate())));
        content.addComponent(infoRow("价值", "¥" + String.format("%.2f", asset.getValue())));
        content.addComponent(infoRow("当前状态", asset.getStatus() != null ? asset.getStatus() : "正常"));
        content.addComponent(infoRow("状态日期", nvl(asset.getStatusDate())));
        content.addComponent(infoRow("备注", nvl(asset.getRemark())));

        // ---- 状态切换 ----
        content.addComponent(sectionTitle("状态切换"));

        // 日期输入
        dateLayout = new DirectionalLayout(getContext());
        dateLayout.setOrientation(Component.VERTICAL);
        dateLayout.setPadding(12, 8, 12, 8);
        dateLayout.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT));
        dateLayout.setVisibility(Component.HIDE);

        Text dl = new Text(getContext());
        dl.setText("状态变更日期（可留空强制切换）:");
        dl.setTextSize(14); dl.setTextColor(new Color(0xFF666666));
        dl.setHeight(30);
        dateLayout.addComponent(dl);

        dateInput = new TextField(getContext());
        dateInput.setHint("yyyy-MM-dd，可留空");
        dateInput.setTextSize(16);
        dateInput.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 48));
        dateInput.setPadding(12, 8, 12, 8);

        ShapeElement df = new ShapeElement();
        df.setRgbColor(RgbColor.fromArgbInt(0xFFFFFFFF));
        df.setCornerRadius(4f);
        dateInput.setBackground(df);
        dateLayout.addComponent(dateInput);
        content.addComponent(dateLayout);

        // 状态切换按钮行
        DirectionalLayout btnRow = new DirectionalLayout(getContext());
        btnRow.setOrientation(Component.HORIZONTAL);
        btnRow.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 56));
        btnRow.setMarginTop(8);

        String curStatus = asset.getStatus() != null ? asset.getStatus() : "正常";

        if (!"正常".equals(curStatus)) {
            btnRow.addComponent(statusBtn("设为正常", 0xFF43A047, "正常"));
        }
        if (!"维修".equals(curStatus)) {
            btnRow.addComponent(statusBtn("设为维修", 0xFFFF9800, "维修"));
        }
        if (!"报废".equals(curStatus)) {
            btnRow.addComponent(statusBtn("设为报废", 0xFFE53935, "报废"));
        }
        content.addComponent(btnRow);

        // 强制切换按钮（不填日期）
        Button forceBtn = new Button(getContext());
        forceBtn.setText("强制切换（不填日期直接切）");
        forceBtn.setTextSize(14);
        forceBtn.setTextColor(Color.WHITE);
        ShapeElement fb = new ShapeElement();
        fb.setRgbColor(RgbColor.fromArgbInt(0xFF757575));
        forceBtn.setBackground(fb);
        forceBtn.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 44));
        forceBtn.setMarginTop(8);
        forceBtn.setClickedListener(c -> {
            dateInput.setText("");
            dateLayout.setVisibility(Component.VISIBLE);
        });
        content.addComponent(forceBtn);

        sv.addComponent(content);
        root.addComponent(sv);
        setUIContent(root);
    }

    private Button statusBtn(String label, int color, final String newStatus) {
        Button btn = new Button(getContext());
        btn.setText(label);
        btn.setTextSize(14);
        btn.setTextColor(Color.WHITE);
        ShapeElement bg = new ShapeElement();
        bg.setRgbColor(RgbColor.fromArgbInt(color));
        btn.setBackground(bg);
        DirectionalLayout.LayoutConfig cfg = new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 48);
        cfg.weight = 1;
        cfg.setMargins(4, 0, 4, 0);
        btn.setLayoutConfig(cfg);
        btn.setClickedListener(c -> {
            dateLayout.setVisibility(Component.VISIBLE);
            // 记住要切换的目标状态
            btn.setTag(newStatus);
            // 同时把 dateInput 的 tag 也设为目标状态，确认时读取
            dateInput.setTag(newStatus);

            // 添加确认按钮
            confirmStatusChange(newStatus);
        });
        return btn;
    }

    private void confirmStatusChange(final String newStatus) {
        // 在 dateLayout 中动态添加确认按钮
        if (dateLayout.getChildCount() > 2) {
            dateLayout.removeComponentAt(2);
        }

        Button confirmBtn = new Button(getContext());
        confirmBtn.setText("确认切换为「" + newStatus + "」");
        confirmBtn.setTextSize(14);
        confirmBtn.setTextColor(Color.WHITE);
        ShapeElement cb = new ShapeElement();
        cb.setRgbColor(RgbColor.fromArgbInt(0xFF1E88E5));
        confirmBtn.setBackground(cb);
        confirmBtn.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 44));
        confirmBtn.setMarginTop(4);
        confirmBtn.setClickedListener(c -> doChangeStatus(newStatus));
        dateLayout.addComponent(confirmBtn);
    }

    private void doChangeStatus(String newStatus) {
        String date = dateInput.getText();
        String today = DateUtils.getCurrentDate();

        asset.setStatus(newStatus);
        asset.setStatusDate((date != null && !date.trim().isEmpty()) ? date.trim() : today);
        asset.setUpdatedAt(DateUtils.getCurrentDateTime());
        db.updateFixedAsset(asset);

        showToast("状态已切换为「" + newStatus + "」");
        // 刷新页面
        asset = db.getFixedAssetById(asset.getId());
        buildUI();
    }

    @Override
    protected void onResult(int requestCode, Intent resultIntent) {
        if (requestCode == 500) {
            asset = db.getFixedAssetById(asset.getId());
            if (asset != null) buildUI();
        }
    }

    private DirectionalLayout infoRow(String label, String value) {
        DirectionalLayout row = new DirectionalLayout(getContext());
        row.setOrientation(Component.HORIZONTAL);
        row.setPadding(12, 8, 12, 8);
        row.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT));
        ShapeElement rowBg = new ShapeElement();
        rowBg.setRgbColor(RgbColor.fromArgbInt(0xFFFFFFFF));
        rowBg.setCornerRadius(8f);
        row.setBackground(rowBg);
        row.setMarginTop(2); row.setMarginBottom(2);

        Text lt = new Text(getContext());
        lt.setText(label + ":");
        lt.setTextSize(15); lt.setTextColor(new Color(0xFF666666));
        lt.setLayoutConfig(new DirectionalLayout.LayoutConfig(100,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT));
        row.addComponent(lt);

        Text vt = new Text(getContext());
        vt.setText(value);
        vt.setTextSize(15); vt.setTextColor(new Color(0xFF333333));
        DirectionalLayout.LayoutConfig vc = new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT);
        vc.weight = 1; vt.setLayoutConfig(vc);
        row.addComponent(vt);
        return row;
    }

    private Text sectionTitle(String t) {
        Text tx = new Text(getContext());
        tx.setText(t); tx.setTextSize(18); tx.setTextColor(new Color(0xFF333333));
        tx.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 40));
        tx.setMarginTop(16); tx.setMarginBottom(4);
        return tx;
    }

    private String nvl(String s) { return s != null && !s.isEmpty() ? s : "无"; }

    private void showToast(String msg) {
        new ToastDialog(getContext()).setText(msg).setDuration(2000).show();
    }
}
