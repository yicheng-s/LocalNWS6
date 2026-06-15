package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.ResourceTable;
import com.sunyicheng.myapplication.util.BarcodeDecoder;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.aafwk.content.Operation;
import ohos.agp.components.*;
import ohos.agp.window.dialog.ToastDialog;

import java.io.File;

/**
 * 条码扫描页 — 系统相机 Intent + ZXing 解码方案
 *
 * 废弃原因：CameraKit + SurfaceProvider 方案在 HarmonyOS 3.0 设备上
 * surfaceProvider.getSurfaceOps() 始终返回空，导致预览无法初始化。
 *
 * 新方案：调用系统相机拍照 → 保存到临时文件 → ZXing 解码 → 返回条码
 * 同时保留手动输入兜底。
 */
public class BarcodeScanSlice extends AbilitySlice {

    private static final int REQUEST_CAMERA_CAPTURE = 999;

    private Text scanHint;
    private DirectionalLayout manualInputLayout;
    private TextField manualBarcodeInput;

    /** 系统相机拍照输出路径 */
    private File captureFile;

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_slice_barcode_scan);

        scanHint = (Text) findComponentById(ResourceTable.Id_scan_hint);
        manualInputLayout = (DirectionalLayout) findComponentById(ResourceTable.Id_manual_input_layout);
        manualBarcodeInput = (TextField) findComponentById(ResourceTable.Id_manual_barcode_input);

        initButtons();
    }

    private void initButtons() {
        // 返回
        findComponentById(ResourceTable.Id_btn_cancel_scan).setClickedListener(c -> terminate());

        // 拍照识别 — 调用系统相机
        findComponentById(ResourceTable.Id_btn_capture).setClickedListener(c -> launchSystemCamera());

        // 显示手动输入
        findComponentById(ResourceTable.Id_btn_manual_input).setClickedListener(c -> {
            manualInputLayout.setVisibility(Component.VISIBLE);
            manualBarcodeInput.setText("");
        });

        // 手动输入 — 取消
        findComponentById(ResourceTable.Id_btn_manual_cancel).setClickedListener(c ->
                manualInputLayout.setVisibility(Component.HIDE));

        // 手动输入 — 确认
        findComponentById(ResourceTable.Id_btn_manual_confirm).setClickedListener(c -> {
            String barcode = manualBarcodeInput.getText();
            if (barcode != null && !barcode.trim().isEmpty()) {
                returnResult(barcode.trim());
            } else {
                showToast("请输入条形码");
            }
        });
    }

    /**
     * 启动系统相机进行拍照
     */
    private void launchSystemCamera() {
        try {
            // 1. 准备临时输出文件
            File scanDir = new File(getContext().getCacheDir(), "scan");
            if (!scanDir.exists()) {
                scanDir.mkdirs();
            }
            captureFile = new File(scanDir, "capture_" + System.currentTimeMillis() + ".jpg");

            // 2. 构造 Intent，指定输出文件路径
            Intent intent = new Intent();
            Operation operation = new Intent.OperationBuilder()
                    .withAction("android.media.action.IMAGE_CAPTURE")
                    .build();
            intent.setOperation(operation);
            // 通过参数传递输出路径（标准 EXTRA_OUTPUT 键）
            intent.setParam("output", captureFile.getAbsolutePath());

            // 3. 启动系统相机
            startAbilityForResult(intent, REQUEST_CAMERA_CAPTURE);

        } catch (Exception e) {
            scanHint.setText("无法启动相机，请使用手动输入");
            showToast("无法启动系统相机，请手动输入条码");
        }
    }

    /**
     * 接收系统相机返回的结果
     */
    @Override
    protected void onResult(int requestCode, Intent resultIntent) {
        if (requestCode != REQUEST_CAMERA_CAPTURE) {
            return;
        }

        // 1. 优先从我们指定的输出文件解码
        if (captureFile != null && captureFile.exists() && captureFile.length() > 0) {
            decodeAndReturn(captureFile.getAbsolutePath());
            return;
        }

        // 2. 尝试从系统相机返回的参数中读取文件路径
        if (resultIntent != null) {
            String returnedPath = resultIntent.getStringParam("output");
            if (returnedPath != null && !returnedPath.isEmpty()) {
                File f = new File(returnedPath);
                if (f.exists() && f.length() > 0) {
                    decodeAndReturn(f.getAbsolutePath());
                    return;
                }
            }
            // 也尝试 data 参数
            String dataUri = resultIntent.getStringParam("data");
            if (dataUri != null && !dataUri.isEmpty()) {
                String path = dataUri;
                if (dataUri.startsWith("file://")) {
                    path = dataUri.substring(7);
                }
                if (path.startsWith("/")) {
                    File f2 = new File(path);
                    if (f2.exists() && f2.length() > 0) {
                        decodeAndReturn(f2.getAbsolutePath());
                        return;
                    }
                }
            }
        }

        // 3. 都失败了
        scanHint.setText("未获取到照片，请重试或使用手动输入");
        showToast("拍照未成功，请重试或手动输入");
    }

    /**
     * 从指定路径解码并返回条码
     */
    private void decodeAndReturn(String imagePath) {
        scanHint.setText("正在识别条码...");
        String barcode = BarcodeDecoder.decodeFromFile(imagePath);

        if (barcode != null && !barcode.isEmpty()) {
            returnResult(barcode);
        } else {
            scanHint.setText("未识别到条码，请调整光线重试或使用手动输入");
            showToast("未识别到条码，请重试");
        }
    }

    private void returnResult(String barcode) {
        cleanTempFile();
        Intent resultIntent = new Intent();
        resultIntent.setParam("barcode", barcode);
        setResult(resultIntent);
        terminate();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cleanTempFile();
    }

    private void cleanTempFile() {
        try {
            if (captureFile != null && captureFile.exists()) {
                captureFile.delete();
            }
        } catch (Exception ignored) {}
    }

    private void showToast(String msg) {
        new ToastDialog(getContext())
                .setText(msg)
                .setDuration(2000)
                .show();
    }
}
