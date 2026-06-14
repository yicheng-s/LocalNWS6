package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.ResourceTable;
import com.sunyicheng.myapplication.db.DatabaseManager;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.*;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.utils.Color;
import ohos.agp.utils.TextAlignment;

/**
 * 主页面 - 功能菜单 (2列×3行布局)
 */
public class MainSlice extends AbilitySlice {

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_slice_main);
        buildMenuGrid();
        updateStats();
    }

    @Override
    protected void onActive() {
        super.onActive();
        updateStats();
    }

    private void buildMenuGrid() {
        DirectionalLayout row1 = (DirectionalLayout) findComponentById(ResourceTable.Id_menu_row1);
        DirectionalLayout row2 = (DirectionalLayout) findComponentById(ResourceTable.Id_menu_row2);
        DirectionalLayout row3 = (DirectionalLayout) findComponentById(ResourceTable.Id_menu_row3);

        // 第一行：入库 | 出库
        addMenuCell(row1, "📥", "入库操作", () -> present(new StockInSlice(), new Intent()));
        addMenuCell(row1, "📤", "出库操作", () -> present(new StockOutSlice(), new Intent()));

        // 第二行：商品管理 | 记录查询
        addMenuCell(row2, "📦", "商品管理", () -> present(new ProductListSlice(), new Intent()));
        addMenuCell(row2, "📋", "记录查询", () -> present(new RecordListSlice(), new Intent()));

        // 第三行：固定资产 | 统计报表
        addMenuCell(row3, "🗄️", "固定资产", () -> present(new FixedAssetListSlice(), new Intent()));
        addMenuCell(row3, "📊", "统计报表", () -> present(new StatisticsSlice(), new Intent()));
    }

    private void addMenuCell(DirectionalLayout row, String icon, String title, Runnable action) {
        DirectionalLayout cell = new DirectionalLayout(getContext());
        cell.setOrientation(Component.VERTICAL);
        cell.setAlignment(TextAlignment.CENTER);

        // weight=1 让两个按钮均分行宽
        DirectionalLayout.LayoutConfig config = new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_PARENT);
        config.weight = 1;
        cell.setLayoutConfig(config);

        // 圆角白色背景
        ShapeElement bg = new ShapeElement();
        bg.setRgbColor(RgbColor.fromArgbInt(0xFFFFFFFF));
        bg.setCornerRadius(16f);
        cell.setBackground(bg);

        // 外边距
        config.setMargins(10, 10, 10, 10);
        cell.setPadding(12, 16, 12, 16);

        // 图标
        Text iconText = new Text(getContext());
        iconText.setText(icon);
        iconText.setTextSize(52);
        iconText.setTextAlignment(TextAlignment.CENTER);
        iconText.setWidth(DirectionalLayout.LayoutConfig.MATCH_PARENT);
        iconText.setHeight(60);
        cell.addComponent(iconText);

        // 标题
        Text titleText = new Text(getContext());
        titleText.setText(title);
        titleText.setTextSize(17);
        titleText.setTextColor(new Color(0xFF333333));
        titleText.setTextAlignment(TextAlignment.CENTER);
        titleText.setWidth(DirectionalLayout.LayoutConfig.MATCH_PARENT);
        titleText.setHeight(36);
        cell.addComponent(titleText);

        // 点击事件
        cell.setClickedListener(component -> action.run());

        row.addComponent(cell);
    }

    private void updateStats() {
        DatabaseManager db = DatabaseManager.getInstance();
        int[] productStats = db.getProductStatistics();
        Object[] assetStats = db.getFixedAssetStatistics();

        Text statProducts = (Text) findComponentById(ResourceTable.Id_stat_products);
        Text statRecords = (Text) findComponentById(ResourceTable.Id_stat_records);
        Text statAssets = (Text) findComponentById(ResourceTable.Id_stat_assets);
        Text statStock = (Text) findComponentById(ResourceTable.Id_stat_stock);

        if (statProducts != null) statProducts.setText("商品种类: " + productStats[0]);
        if (statRecords != null) {
            int[] recordStats = db.getStockRecordStatistics();
            statRecords.setText("出入库记录: " + (recordStats[0] + recordStats[2]));
        }
        if (statAssets != null) statAssets.setText("固定资产: " + assetStats[0]);
        if (statStock != null) statStock.setText("库存总量: " + productStats[1]);
    }
}
