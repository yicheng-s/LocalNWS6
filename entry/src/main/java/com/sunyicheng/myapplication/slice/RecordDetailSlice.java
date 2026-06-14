package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.db.DatabaseManager;
import com.sunyicheng.myapplication.model.StockRecord;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.colors.RgbColor;
import ohos.agp.components.*;
import ohos.agp.components.element.ShapeElement;
import ohos.agp.utils.Color;
import ohos.agp.utils.TextAlignment;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.media.image.common.Size;

import java.io.File;
import java.io.FileInputStream;

/**
 * 出入库记录详情页面 - 含签名图片显示
 */
public class RecordDetailSlice extends AbilitySlice {

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        int recordId = intent.getIntParam("record_id", -1);
        if (recordId <= 0) { terminate(); return; }

        StockRecord record = DatabaseManager.getInstance().getStockRecordById(recordId);
        if (record == null) { terminate(); return; }

        buildUI(record);
    }

    private void buildUI(StockRecord record) {
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

        int titleColor = "in".equals(record.getType()) ? 0xFF43A047 : 0xFFE53935;
        ShapeElement titleBg = new ShapeElement();
        titleBg.setRgbColor(RgbColor.fromArgbInt(titleColor));
        titleBar.setBackground(titleBg);
        titleBar.setPadding(8, 8, 8, 8);

        Button btnBack = new Button(getContext());
        btnBack.setText("返回");
        btnBack.setTextSize(14);
        btnBack.setTextColor(Color.WHITE);
        btnBack.setBackground(titleBg);
        btnBack.setLayoutConfig(new DirectionalLayout.LayoutConfig(60, 40));
        btnBack.setClickedListener(c -> terminate());
        titleBar.addComponent(btnBack);

        Text titleText = new Text(getContext());
        titleText.setText(record.getTypeDisplay() + "详情");
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

        // 内容（使用 ScrollView 以便滚动查看签名）
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

        content.addComponent(createInfoRow("商品名称", record.getProductName() != null ? record.getProductName() : "未知"));
        content.addComponent(createInfoRow("条码", record.getProductBarcode() != null ? record.getProductBarcode() : "无"));
        content.addComponent(createInfoRow("操作类型", record.getTypeDisplay()));
        content.addComponent(createInfoRow("数量", String.valueOf(record.getQuantity())));
        content.addComponent(createInfoRow("部门", record.getDepartment() != null ? record.getDepartment() : "无"));
        content.addComponent(createInfoRow("经办/领取人", record.getPersonName() != null ? record.getPersonName() : "无"));
        content.addComponent(createInfoRow("备注", record.getRemark() != null ? record.getRemark() : "无"));
        content.addComponent(createInfoRow("操作时间", record.getCreatedAt()));

        // 签名图片
        if (record.getSignaturePath() != null) {
            Text signLabel = new Text(getContext());
            signLabel.setText("经办人签名:");
            signLabel.setTextSize(15);
            signLabel.setTextColor(new Color(0xFF666666));
            signLabel.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                    DirectionalLayout.LayoutConfig.MATCH_PARENT, 36));
            signLabel.setMarginTop(12);
            content.addComponent(signLabel);

            // 尝试加载签名图片
            PixelMap signPm = loadSignatureImage(record.getSignaturePath());
            if (signPm != null) {
                Image signImage = new Image(getContext());
                signImage.setPixelMap(signPm);
                signImage.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                        DirectionalLayout.LayoutConfig.MATCH_PARENT, 280));
                signImage.setScaleMode(Image.ScaleMode.CENTER);

                ShapeElement imgBg = new ShapeElement();
                imgBg.setRgbColor(RgbColor.fromArgbInt(0xFFFFFFFF));
                imgBg.setCornerRadius(8f);
                signImage.setBackground(imgBg);
                signImage.setMarginTop(4);
                content.addComponent(signImage);
            } else {
                Text failText = new Text(getContext());
                failText.setText("签名文件: " + record.getSignaturePath());
                failText.setTextSize(12);
                failText.setTextColor(new Color(0xFF999999));
                content.addComponent(failText);
            }
        }

        scrollView.addComponent(content);
        root.addComponent(scrollView);
        setUIContent(root);
    }

    /**
     * 从文件路径加载签名图片为 PixelMap
     */
    private PixelMap loadSignatureImage(String filePath) {
        FileInputStream fis = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return null;
            }

            fis = new FileInputStream(file);
            ImageSource.SourceOptions srcOpts = new ImageSource.SourceOptions();
            ImageSource imageSource = ImageSource.create(fis, srcOpts);
            if (imageSource == null) {
                return null;
            }

            ImageSource.DecodingOptions decOpts = new ImageSource.DecodingOptions();
            // 缩放到合适尺寸显示
            decOpts.desiredSize = new Size(720, 280);

            PixelMap pm = imageSource.createPixelmap(decOpts);
            return pm;

        } catch (Exception e) {
            return null;
        } finally {
            try { if (fis != null) fis.close(); } catch (Exception ignored) {}
        }
    }

    private DirectionalLayout createInfoRow(String label, String value) {
        DirectionalLayout row = new DirectionalLayout(getContext());
        row.setOrientation(Component.HORIZONTAL);
        row.setLayoutConfig(new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT));
        row.setPadding(12, 8, 12, 8);

        ShapeElement rowBg = new ShapeElement();
        rowBg.setRgbColor(RgbColor.fromArgbInt(0xFFFFFFFF));
        rowBg.setCornerRadius(8f);
        row.setBackground(rowBg);
        row.setMarginTop(3);
        row.setMarginBottom(3);

        Text labelText = new Text(getContext());
        labelText.setText(label + ":");
        labelText.setTextSize(15);
        labelText.setTextColor(new Color(0xFF666666));
        labelText.setLayoutConfig(new DirectionalLayout.LayoutConfig(110,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT));
        row.addComponent(labelText);

        Text valueText = new Text(getContext());
        valueText.setText(value);
        valueText.setTextSize(15);
        valueText.setTextColor(new Color(0xFF333333));
        DirectionalLayout.LayoutConfig vConf = new DirectionalLayout.LayoutConfig(
                DirectionalLayout.LayoutConfig.MATCH_PARENT,
                DirectionalLayout.LayoutConfig.MATCH_CONTENT);
        vConf.weight = 1;
        valueText.setLayoutConfig(vConf);
        row.addComponent(valueText);

        return row;
    }
}
