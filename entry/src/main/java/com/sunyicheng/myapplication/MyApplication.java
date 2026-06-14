package com.sunyicheng.myapplication;

import com.sunyicheng.myapplication.db.DatabaseManager;
import ohos.aafwk.ability.AbilityPackage;

/**
 * 应用入口类
 * 负责应用级别的初始化工作
 */
public class MyApplication extends AbilityPackage {
    @Override
    public void onInitialize() {
        super.onInitialize();
        // 初始化数据库
        DatabaseManager.getInstance().init(getContext());
    }

    @Override
    public void onEnd() {
        super.onEnd();
        // 关闭数据库
        DatabaseManager.getInstance().close();
    }
}
