package com.sunyicheng.myapplication;

import com.sunyicheng.myapplication.slice.*;
import ohos.aafwk.ability.Ability;
import ohos.aafwk.content.Intent;

/**
 * 主 Ability (Page Ability)
 * 使用 Java UI 模式，管理所有页面路由
 */
public class MainAbility extends Ability {

    @Override
    public void onStart(Intent intent) {
        super.onStart(intent);

        // 设置主页面路由
        super.setMainRoute(MainSlice.class.getName());

        // 注册各功能页面路由
        addActionRoute("action.product.list", ProductListSlice.class.getName());
        addActionRoute("action.product.edit", ProductEditSlice.class.getName());
        addActionRoute("action.stock.in", StockInSlice.class.getName());
        addActionRoute("action.stock.out", StockOutSlice.class.getName());
        addActionRoute("action.record.list", RecordListSlice.class.getName());
        addActionRoute("action.record.detail", RecordDetailSlice.class.getName());
        addActionRoute("action.asset.list", FixedAssetListSlice.class.getName());
        addActionRoute("action.asset.detail", FixedAssetDetailSlice.class.getName());
        addActionRoute("action.asset.edit", FixedAssetEditSlice.class.getName());
        addActionRoute("action.statistics", StatisticsSlice.class.getName());
        addActionRoute("action.signature", SignatureSlice.class.getName());
        addActionRoute("action.barcode.scan", BarcodeScanSlice.class.getName());
    }
}
