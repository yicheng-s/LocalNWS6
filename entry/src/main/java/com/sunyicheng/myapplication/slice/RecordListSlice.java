package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.ResourceTable;
import com.sunyicheng.myapplication.db.DatabaseManager;
import com.sunyicheng.myapplication.model.StockRecord;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.*;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.utils.Color;
import ohos.agp.utils.TextAlignment;

import java.util.List;

/**
 * 出入库记录列表页面 (适配 API 6)
 */
public class RecordListSlice extends AbilitySlice {

    private ListContainer listContainer;
    private TextField searchField;
    private DatabaseManager db;
    private List<StockRecord> recordList;
    private Button btnAll, btnIn, btnOut;
    private String currentFilter = "all";

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        db = DatabaseManager.getInstance();
        setUIContent(ResourceTable.Layout_slice_record_list);

        listContainer = (ListContainer) findComponentById(ResourceTable.Id_record_list);
        searchField = (TextField) findComponentById(ResourceTable.Id_search_field);
        btnAll = (Button) findComponentById(ResourceTable.Id_btn_filter_all);
        btnIn = (Button) findComponentById(ResourceTable.Id_btn_filter_in);
        btnOut = (Button) findComponentById(ResourceTable.Id_btn_filter_out);

        findComponentById(ResourceTable.Id_btn_back).setClickedListener(c -> terminate());

        ShapeElement activeBg = new ShapeElement();
        activeBg.setRgbColor(RgbColor.fromArgbInt(0xFF1E88E5));
        ShapeElement inactiveBg = new ShapeElement();
        inactiveBg.setRgbColor(RgbColor.fromArgbInt(0xFF757575));

        btnAll.setClickedListener(c -> { currentFilter = "all"; btnAll.setBackground(activeBg); btnIn.setBackground(inactiveBg); btnOut.setBackground(inactiveBg); loadRecords(); });
        btnIn.setClickedListener(c -> { currentFilter = "in"; btnAll.setBackground(inactiveBg); btnIn.setBackground(activeBg); btnOut.setBackground(inactiveBg); loadRecords(); });
        btnOut.setClickedListener(c -> { currentFilter = "out"; btnAll.setBackground(inactiveBg); btnIn.setBackground(inactiveBg); btnOut.setBackground(activeBg); loadRecords(); });

        findComponentById(ResourceTable.Id_btn_search).setClickedListener(c -> loadRecords());

        listContainer.setItemClickedListener((list, component, position, id) -> {
            StockRecord record = recordList.get(position);
            Intent intent1 = new Intent();
            intent1.setParam("record_id", record.getId());
            present(new RecordDetailSlice(), intent1);
        });

        loadRecords();
    }

    @Override
    protected void onActive() {
        super.onActive();
        loadRecords();
    }

    private void loadRecords() {
        String keyword = searchField.getText();
        if (keyword != null && !keyword.isEmpty()) {
            recordList = db.searchStockRecords(keyword);
        } else {
            switch (currentFilter) {
                case "in": recordList = db.getStockRecordsByType("in"); break;
                case "out": recordList = db.getStockRecordsByType("out"); break;
                default: recordList = db.getAllStockRecords(); break;
            }
        }
        RecordItemProvider provider = new RecordItemProvider(recordList);
        listContainer.setItemProvider(provider);
    }

    private class RecordItemProvider extends BaseItemProvider {
        private final List<StockRecord> items;
        RecordItemProvider(List<StockRecord> items) { this.items = items; }
        @Override
        public int getCount() { return items.size(); }
        @Override
        public Object getItem(int position) { return items.get(position); }
        @Override
        public long getItemId(int position) { return items.get(position).getId(); }

        @Override
        public Component getComponent(int position, Component convertView, ComponentContainer parent) {
            StockRecord record = items.get(position);

            DirectionalLayout layout = new DirectionalLayout(getContext());
            layout.setOrientation(Component.HORIZONTAL);
            layout.setPadding(16, 10, 16, 10);
            DirectionalLayout.LayoutConfig lcfg = new DirectionalLayout.LayoutConfig(
                    DirectionalLayout.LayoutConfig.MATCH_PARENT, 70);
            layout.setLayoutConfig(lcfg);

            // 类型标记
            Text typeTag = new Text(getContext());
            typeTag.setText(record.getTypeDisplay());
            typeTag.setTextSize(12);
            typeTag.setTextColor(Color.WHITE);
            typeTag.setPadding(8, 4, 8, 4);
            typeTag.setTextAlignment(TextAlignment.CENTER);
            DirectionalLayout.LayoutConfig tagCfg = new DirectionalLayout.LayoutConfig(48, 32);
            tagCfg.setMargins(0, 0, 12, 0);
            typeTag.setLayoutConfig(tagCfg);

            ShapeElement tagBg = new ShapeElement();
            tagBg.setRgbColor(RgbColor.fromArgbInt(
                    "in".equals(record.getType()) ? 0xFF43A047 : 0xFFE53935));
            tagBg.setCornerRadius(8f);
            typeTag.setBackground(tagBg);
            layout.addComponent(typeTag);

            // 信息
            DirectionalLayout infoLayout = new DirectionalLayout(getContext());
            infoLayout.setOrientation(Component.VERTICAL);
            DirectionalLayout.LayoutConfig infoCfg = new DirectionalLayout.LayoutConfig(
                    DirectionalLayout.LayoutConfig.MATCH_PARENT,
                    DirectionalLayout.LayoutConfig.MATCH_PARENT);
            infoCfg.weight = 1;
            infoLayout.setLayoutConfig(infoCfg);

            String pname = record.getProductName() != null ? record.getProductName() : "商品#" + record.getProductId();
            Text nameText = new Text(getContext());
            nameText.setText(pname + " ×" + record.getQuantity());
            nameText.setTextSize(16);
            nameText.setTextColor(new Color(0xFF333333));
            infoLayout.addComponent(nameText);

            String sub = (record.getPersonName() != null ? record.getPersonName() : "") +
                    (record.getDepartment() != null ? " | " + record.getDepartment() : "");
            Text subText = new Text(getContext());
            subText.setText(sub);
            subText.setTextSize(13);
            subText.setTextColor(new Color(0xFF999999));
            infoLayout.addComponent(subText);

            layout.addComponent(infoLayout);

            // 时间
            String time = record.getCreatedAt();
            if (time != null && time.length() > 16) time = time.substring(0, 16);
            Text timeText = new Text(getContext());
            timeText.setText(time);
            timeText.setTextSize(12);
            timeText.setTextColor(new Color(0xFF999999));
            DirectionalLayout.LayoutConfig timeCfg = new DirectionalLayout.LayoutConfig(130,
                    DirectionalLayout.LayoutConfig.MATCH_PARENT);
            timeText.setLayoutConfig(timeCfg);
            layout.addComponent(timeText);

            return layout;
        }
    }
}
