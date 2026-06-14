package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.ResourceTable;
import com.sunyicheng.myapplication.component.SignatureView;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.Component;
import ohos.agp.components.DirectionalLayout;
import ohos.agp.components.StackLayout;
import ohos.agp.window.dialog.ToastDialog;

import java.io.File;

/**
 * 手写签名页面 (适配 API 6)
 */
public class SignatureSlice extends AbilitySlice {

    private SignatureView signatureView;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_slice_signature);

        StackLayout container = (StackLayout) findComponentById(ResourceTable.Id_signature_container);

        signatureView = new SignatureView(getContext());
        // API 6: 使用 StackLayout.LayoutConfig
        StackLayout.LayoutConfig config = new StackLayout.LayoutConfig(
                StackLayout.LayoutConfig.MATCH_PARENT,
                StackLayout.LayoutConfig.MATCH_PARENT);
        signatureView.setLayoutConfig(config);
        container.addComponent(signatureView);

        initButtons();
    }

    private void initButtons() {
        findComponentById(ResourceTable.Id_btn_sign_clear).setClickedListener(c -> {
            if (signatureView != null) {
                signatureView.clear();
            }
        });

        findComponentById(ResourceTable.Id_btn_sign_confirm).setClickedListener(c -> {
            if (!signatureView.hasContent()) {
                showToast("请先在下方区域签名");
                return;
            }

            // 尝试多条路径保存签名，确保至少有一条可用
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
            String fileName = "sign_" + sdf.format(new java.util.Date());

            String[] saveDirs = {
                "/storage/emulated/0/WarehouseSignatures",   // 平板内部存储
                "/sdcard/WarehouseSignatures",                // 兼容旧路径
                getContext().getFilesDir().getAbsolutePath() + "/signatures",  // 应用私有目录(兜底)
            };

            String savePath = null;
            for (String dir : saveDirs) {
                savePath = signatureView.saveToFile(dir, fileName);
                if (savePath != null) break;
            }

            if (savePath != null) {
                Intent resultIntent = new Intent();
                resultIntent.setParam("signature_path", savePath);
                // API 6: setResult 只接受 Intent
                setResult(resultIntent);
                terminate();
            } else {
                showToast("签名保存失败，请重试");
            }
        });

        findComponentById(ResourceTable.Id_btn_sign_cancel).setClickedListener(c -> terminate());
    }

    private void showToast(String message) {
        new ToastDialog(getContext())
                .setText(message)
                .setDuration(2000)
                .show();
    }
}
