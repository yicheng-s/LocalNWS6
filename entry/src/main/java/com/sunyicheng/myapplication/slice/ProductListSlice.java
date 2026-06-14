package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.ResourceTable;
import com.sunyicheng.myapplication.db.DatabaseManager;
import com.sunyicheng.myapplication.model.Product;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.*;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.utils.Color;
import ohos.agp.utils.TextAlignment;

import java.util.List;

/**
 * 商品列表页面 (适配 API 6)
 */
public class ProductListSlice extends AbilitySlice {

    private ListContainer listContainer;
    private TextField searchField;
    private DatabaseManager db;
    private List<Product> productList;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        db = DatabaseManager.getInstance();
        setUIContent(ResourceTable.Layout_slice_product_list);

        listContainer = (ListContainer) findComponentById(ResourceTable.Id_product_list);
        searchField = (TextField) findComponentById(ResourceTable.Id_search_field);

        findComponentById(ResourceTable.Id_btn_back).setClickedListener(c -> terminate());
        findComponentById(ResourceTable.Id_btn_add_product).setClickedListener(c -> {
            presentForResult(new ProductEditSlice(), new Intent(), 100);
        });
        findComponentById(ResourceTable.Id_btn_search).setClickedListener(c -> loadProductList());

        loadProductList();
    }

    @Override
    protected void onResult(int requestCode, Intent resultIntent) {
        if (requestCode == 100) {
            loadProductList();
        }
    }

    @Override
    protected void onActive() {
        super.onActive();
        loadProductList();
    }

    private void loadProductList() {
        String keyword = searchField.getText();
        if (keyword != null && !keyword.isEmpty()) {
            productList = db.searchProducts(keyword);
        } else {
            productList = db.getAllProducts();
        }

        ProductItemProvider provider = new ProductItemProvider(productList);
        listContainer.setItemProvider(provider);

        listContainer.setItemClickedListener((list, component, position, id) -> {
            Product product = productList.get(position);
            Intent intent = new Intent();
            intent.setParam("product_id", product.getId());
            presentForResult(new ProductEditSlice(), intent, 100);
        });
    }

    /**
     * 商品列表项适配器 - API 6: 使用 ComponentContainer 而非 ComponentParent
     */
    private class ProductItemProvider extends BaseItemProvider {
        private final List<Product> items;

        ProductItemProvider(List<Product> items) { this.items = items; }

        @Override
        public int getCount() { return items.size(); }

        @Override
        public Object getItem(int position) { return items.get(position); }

        @Override
        public long getItemId(int position) { return items.get(position).getId(); }

        @Override
        public Component getComponent(int position, Component convertView, ComponentContainer parent) {
            Product product = items.get(position);

            DirectionalLayout layout = new DirectionalLayout(getContext());
            layout.setOrientation(Component.HORIZONTAL);
            layout.setPadding(16, 12, 16, 12);

            // API 6: 使用 DirectionalLayout.LayoutConfig
            DirectionalLayout.LayoutConfig layoutCfg = new DirectionalLayout.LayoutConfig(
                    DirectionalLayout.LayoutConfig.MATCH_PARENT, 80);
            layout.setLayoutConfig(layoutCfg);

            // 左侧信息区域
            DirectionalLayout infoLayout = new DirectionalLayout(getContext());
            infoLayout.setOrientation(Component.VERTICAL);
            DirectionalLayout.LayoutConfig infoCfg = new DirectionalLayout.LayoutConfig(
                    DirectionalLayout.LayoutConfig.MATCH_PARENT,
                    DirectionalLayout.LayoutConfig.MATCH_PARENT);
            infoCfg.weight = 1;
            infoLayout.setLayoutConfig(infoCfg);

            Text nameText = new Text(getContext());
            nameText.setText(product.getName());
            nameText.setTextSize(18);
            nameText.setTextColor(new Color(0xFF333333));
            infoLayout.addComponent(nameText);

            Text barcodeText = new Text(getContext());
            barcodeText.setText("条码: " + (product.getBarcode() != null ? product.getBarcode() : "无"));
            barcodeText.setTextSize(14);
            barcodeText.setTextColor(new Color(0xFF999999));
            infoLayout.addComponent(barcodeText);

            layout.addComponent(infoLayout);

            // 右侧库存
            DirectionalLayout stockLayout = new DirectionalLayout(getContext());
            stockLayout.setOrientation(Component.VERTICAL);
            stockLayout.setAlignment(TextAlignment.CENTER);
            DirectionalLayout.LayoutConfig stockCfg = new DirectionalLayout.LayoutConfig(100,
                    DirectionalLayout.LayoutConfig.MATCH_PARENT);
            stockLayout.setLayoutConfig(stockCfg);

            Text qtyText = new Text(getContext());
            qtyText.setText(String.valueOf(product.getStockQuantity()));
            qtyText.setTextSize(22);
            qtyText.setTextColor(product.getStockQuantity() < 10 ?
                    new Color(0xFFE53935) : new Color(0xFF43A047));
            qtyText.setTextAlignment(TextAlignment.CENTER);
            stockLayout.addComponent(qtyText);

            Text unitText = new Text(getContext());
            unitText.setText(product.getUnit() != null ? product.getUnit() : "个");
            unitText.setTextSize(12);
            unitText.setTextColor(new Color(0xFF999999));
            unitText.setTextAlignment(TextAlignment.CENTER);
            stockLayout.addComponent(unitText);

            layout.addComponent(stockLayout);

            return layout;
        }
    }
}
