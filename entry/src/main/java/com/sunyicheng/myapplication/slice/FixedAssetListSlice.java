package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.db.DatabaseManager;
import com.sunyicheng.myapplication.model.FixedAsset;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.*;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.utils.Color;
import ohos.agp.utils.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定资产列表页面
 */
public class FixedAssetListSlice extends AbilitySlice {

    private ListContainer listContainer;
    private DatabaseManager db;
    private List<FixedAsset> assetList = new ArrayList<>();

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        db = DatabaseManager.getInstance();
        buildUI();
        loadAssets();
    }

    @Override
    protected void onActive() {
        super.onActive();
        loadAssets();
    }

    @Override
    protected void onResult(int requestCode, Intent resultIntent) {
        loadAssets();
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
        ShapeElement titleBg = new ShapeElement();
        titleBg.setRgbColor(RgbColor.fromArgbInt(0xFF1E88E5));
        titleBar.setBackground(titleBg);
        titleBar.setPadding(8, 8, 8, 8);

        Button btnBack = new Button(getContext());
        btnBack.setText("返回"); btnBack.setTextSize(14); btnBack.setTextColor(Color.WHITE);
        btnBack.setBackground(titleBg);
        btnBack.setLayoutConfig(new DirectionalLayout.LayoutConfig(60, 40));
        btnBack.setClickedListener(c -> terminate());
        titleBar.addComponent(btnBack);

        Text titleText = new Text(getContext());
        titleText.setText("固定资产管理");
        titleText.setTextSize(20); titleText.setTextColor(Color.WHITE);
        titleText.setTextAlignment(TextAlignment.CENTER);
        DirectionalLayout.LayoutConfig tConf = new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, DirectionalLayout.LayoutConfig.MATCH_PARENT);
        tConf.weight = 1; titleText.setLayoutConfig(tConf);
        titleBar.addComponent(titleText);

        Button btnAdd = new Button(getContext());
        btnAdd.setText("+ 新增"); btnAdd.setTextSize(14); btnAdd.setTextColor(Color.WHITE);
        ShapeElement addBg = new ShapeElement();
        addBg.setRgbColor(RgbColor.fromArgbInt(0xFF43A047));
        btnAdd.setBackground(addBg);
        btnAdd.setLayoutConfig(new DirectionalLayout.LayoutConfig(80, 40));
        btnAdd.setClickedListener(c -> presentForResult(new FixedAssetEditSlice(), new Intent(), 400));
        titleBar.addComponent(btnAdd);
        root.addComponent(titleBar);

        // 列表
        listContainer = new ListContainer(getContext());
        DirectionalLayout.LayoutConfig listCfg = new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT, DirectionalLayout.LayoutConfig.MATCH_PARENT);
        listCfg.weight = 1;
        listContainer.setLayoutConfig(listCfg);

        listContainer.setItemClickedListener((list, component, position, id) -> {
            if (position >= 0 && position < assetList.size()) {
                FixedAsset asset = assetList.get(position);
                Intent intent = new Intent();
                intent.setParam("asset_id", asset.getId());
                presentForResult(new FixedAssetDetailSlice(), intent, 400);
            }
        });

        root.addComponent(listContainer);
        setUIContent(root);
    }

    private void loadAssets() {
        assetList = db.getAllFixedAssets();
        if (assetList == null) assetList = new ArrayList<>();
        listContainer.setItemProvider(new AssetItemProvider(assetList));
    }

    private class AssetItemProvider extends BaseItemProvider {
        private final List<FixedAsset> items;
        AssetItemProvider(List<FixedAsset> items) { this.items = items; }
        @Override
        public int getCount() { return items.size(); }
        @Override
        public Object getItem(int position) { return items.get(position); }
        @Override
        public long getItemId(int position) { return items.get(position).getId(); }

        @Override
        public Component getComponent(int position, Component convertView, ComponentContainer parent) {
            FixedAsset asset = items.get(position);
            DirectionalLayout layout = new DirectionalLayout(getContext());
            layout.setOrientation(Component.HORIZONTAL);
            layout.setPadding(16, 10, 16, 10);
            layout.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                    DirectionalLayout.LayoutConfig.MATCH_PARENT, 70));

            String status = asset.getStatus() != null ? asset.getStatus() : "正常";
            int statusColor;
            switch (status) {
                case "维修": statusColor = 0xFFFF9800; break;
                case "报废": statusColor = 0xFFE53935; break;
                default: statusColor = 0xFF43A047; break;
            }

            ShapeElement indicatorBg = new ShapeElement();
            indicatorBg.setRgbColor(RgbColor.fromArgbInt(statusColor));
            indicatorBg.setCornerRadius(4f);

            Component indicator = new Component(getContext());
            indicator.setLayoutConfig(new DirectionalLayout.LayoutConfig(6, 50));
            indicator.setBackground(indicatorBg);
            indicator.setMarginRight(12);
            layout.addComponent(indicator);

            DirectionalLayout infoLayout = new DirectionalLayout(getContext());
            infoLayout.setOrientation(Component.VERTICAL);
            DirectionalLayout.LayoutConfig iConf = new DirectionalLayout.LayoutConfig(
                    DirectionalLayout.LayoutConfig.MATCH_PARENT, DirectionalLayout.LayoutConfig.MATCH_PARENT);
            iConf.weight = 1;
            infoLayout.setLayoutConfig(iConf);

            Text nameText = new Text(getContext());
            nameText.setText(asset.getName());
            nameText.setTextSize(16); nameText.setTextColor(new Color(0xFF333333));
            infoLayout.addComponent(nameText);

            String sub = (asset.getLocation() != null ? asset.getLocation() : "") +
                    " | " + (asset.getDepartment() != null ? asset.getDepartment() : "") + " | " + status;
            Text subText = new Text(getContext());
            subText.setText(sub);
            subText.setTextSize(13); subText.setTextColor(new Color(0xFF999999));
            infoLayout.addComponent(subText);
            layout.addComponent(infoLayout);

            Text valueText = new Text(getContext());
            valueText.setText("¥" + String.format("%.0f", asset.getValue()));
            valueText.setTextSize(15); valueText.setTextColor(new Color(0xFFE53935));
            valueText.setLayoutConfig(new DirectionalLayout.LayoutConfig(80,
                    DirectionalLayout.LayoutConfig.MATCH_PARENT));
            layout.addComponent(valueText);

            return layout;
        }
    }
}
