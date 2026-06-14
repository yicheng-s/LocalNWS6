package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.db.DatabaseManager;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.*;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.utils.Color;
import ohos.agp.utils.TextAlignment;

import java.util.List;

/**
 * 统计报表页面 (适配 API 6)
 */
public class StatisticsSlice extends AbilitySlice {

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        buildUI();
    }

    @Override
    protected void onActive() {
        super.onActive();
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
        titleText.setText("统计报表");
        titleText.setTextSize(20);
        titleText.setTextColor(Color.WHITE);
        titleText.setTextAlignment(TextAlignment.CENTER);
        DirectionalLayout.LayoutConfig tConf = new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_PARENT);
        tConf.weight = 1;
        titleText.setLayoutConfig(tConf);
        titleBar.addComponent(titleText);

        root.addComponent(titleBar);

        // 滚动内容
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_PARENT));

        DirectionalLayout content = new DirectionalLayout(getContext());
        content.setOrientation(Component.VERTICAL);
        content.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT));
        content.setPadding(16, 16, 16, 16);

        DatabaseManager db = DatabaseManager.getInstance();

        // ===== 商品统计 =====
        content.addComponent(createSectionTitle("商品库存统计"));
        int[] ps = db.getProductStatistics();

        DirectionalLayout pRow = new DirectionalLayout(getContext());
        pRow.setOrientation(Component.HORIZONTAL);
        pRow.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT));
        pRow.addComponent(createStatCard("商品种类", String.valueOf(ps[0]), 0xFF1E88E5));
        pRow.addComponent(createStatCard("库存总量", String.valueOf(ps[1]), 0xFF43A047));
        pRow.addComponent(createStatCard("低库存预警", String.valueOf(ps[2]), 0xFFE53935));
        content.addComponent(pRow);

        // 分类统计
        List<String[]> catStats = db.getStockByCategory();
        if (!catStats.isEmpty()) {
            content.addComponent(createSectionTitle("分类库存分布"));
            for (String[] row : catStats) {
                content.addComponent(createCatRow(row[0], row[1], row[2]));
            }
        }

        // ===== 出入库统计 =====
        content.addComponent(createSectionTitle("出入库统计"));
        int[] rs = db.getStockRecordStatistics();

        DirectionalLayout rRow = new DirectionalLayout(getContext());
        rRow.setOrientation(Component.HORIZONTAL);
        rRow.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT));
        rRow.addComponent(createStatCard("入库次数", String.valueOf(rs[0]), 0xFF43A047));
        rRow.addComponent(createStatCard("入库总量", String.valueOf(rs[1]), 0xFF43A047));
        rRow.addComponent(createStatCard("出库次数", String.valueOf(rs[2]), 0xFFFF9800));
        rRow.addComponent(createStatCard("出库总量", String.valueOf(rs[3]), 0xFFFF9800));
        content.addComponent(rRow);

        // ===== 固定资产统计 =====
        content.addComponent(createSectionTitle("固定资产统计"));
        Object[] as = db.getFixedAssetStatistics();

        DirectionalLayout aRow = new DirectionalLayout(getContext());
        aRow.setOrientation(Component.HORIZONTAL);
        aRow.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT));
        aRow.addComponent(createStatCard("资产总数", String.valueOf(as[0]), 0xFF1E88E5));
        aRow.addComponent(createStatCard("正常", String.valueOf(as[1]), 0xFF43A047));
        aRow.addComponent(createStatCard("维修", String.valueOf(as[2]), 0xFFFF9800));
        aRow.addComponent(createStatCard("报废", String.valueOf(as[3]), 0xFFE53935));
        content.addComponent(aRow);

        // 总价值
        double totalValue = (double) as[4];
        DirectionalLayout valueCard = new DirectionalLayout(getContext());
        valueCard.setOrientation(Component.HORIZONTAL);
        valueCard.setPadding(16, 0, 16, 0);
        valueCard.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 50));

        ShapeElement valBg = new ShapeElement();
        valBg.setRgbColor(RgbColor.fromArgbInt(0xFFFFFFFF));
        valBg.setCornerRadius(8f);
        valueCard.setBackground(valBg);
        valueCard.setMarginTop(8);

        Text valLabel = new Text(getContext());
        valLabel.setText("固定资产总价值: ");
        valLabel.setTextSize(16);
        valLabel.setTextColor(new Color(0xFF333333));
        valueCard.addComponent(valLabel);

        Text valAmount = new Text(getContext());
        valAmount.setText("Y" + String.format("%.2f", totalValue));
        valAmount.setTextSize(20);
        valAmount.setTextColor(new Color(0xFFE53935));
        valueCard.addComponent(valAmount);

        content.addComponent(valueCard);

        scrollView.addComponent(content);
        root.addComponent(scrollView);
        setUIContent(root);
    }

    private Text createSectionTitle(String title) {
        Text text = new Text(getContext());
        text.setText(title);
        text.setTextSize(18);
        text.setTextColor(new Color(0xFF333333));
        text.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 40));
        text.setMarginTop(16);
        text.setMarginBottom(8);
        return text;
    }

    private DirectionalLayout createStatCard(String title, String value, int color) {
        DirectionalLayout card = new DirectionalLayout(getContext());
        card.setOrientation(Component.VERTICAL);
        card.setAlignment(TextAlignment.CENTER);
        card.setPadding(8, 12, 8, 12);

        DirectionalLayout.LayoutConfig cConf = new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 70);
        cConf.weight = 1;
        cConf.setMargins(4, 4, 4, 4);
        card.setLayoutConfig(cConf);

        ShapeElement cardBg = new ShapeElement();
        cardBg.setRgbColor(RgbColor.fromArgbInt(0xFFFFFFFF));
        cardBg.setCornerRadius(8f);
        card.setBackground(cardBg);

        Text valueText = new Text(getContext());
        valueText.setText(value);
        valueText.setTextSize(22);
        valueText.setTextColor(new Color(color));
        valueText.setTextAlignment(TextAlignment.CENTER);
        card.addComponent(valueText);

        Text titleText = new Text(getContext());
        titleText.setText(title);
        titleText.setTextSize(12);
        titleText.setTextColor(new Color(0xFF999999));
        titleText.setTextAlignment(TextAlignment.CENTER);
        card.addComponent(titleText);

        return card;
    }

    private DirectionalLayout createCatRow(String category, String count, String total) {
        DirectionalLayout row = new DirectionalLayout(getContext());
        row.setOrientation(Component.HORIZONTAL);
        row.setPadding(16, 0, 16, 0);
        row.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, 36));

        ShapeElement rowBg = new ShapeElement();
        rowBg.setRgbColor(RgbColor.fromArgbInt(0xFFFFFFFF));
        rowBg.setCornerRadius(4f);
        row.setBackground(rowBg);
        row.setMarginBottom(2);

        Text catText = new Text(getContext());
        catText.setText(category);
        catText.setTextSize(14);
        catText.setTextColor(new Color(0xFF333333));
        DirectionalLayout.LayoutConfig catCfg = new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_PARENT);
        catCfg.weight = 1;
        catText.setLayoutConfig(catCfg);
        row.addComponent(catText);

        Text cntText = new Text(getContext());
        cntText.setText(count + " 种 | 库存 " + total);
        cntText.setTextSize(13);
        cntText.setTextColor(new Color(0xFF666666));
        cntText.setLayoutConfig(new DirectionalLayout.LayoutConfig(160,
                DirectionalLayout.LayoutConfig.MATCH_PARENT));
        row.addComponent(cntText);

        return row;
    }
}
