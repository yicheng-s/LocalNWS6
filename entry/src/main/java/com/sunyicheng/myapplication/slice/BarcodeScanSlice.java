package com.sunyicheng.myapplication.slice;

import com.sunyicheng.myapplication.ResourceTable;
import com.sunyicheng.myapplication.util.BarcodeDecoder;
import ohos.aafwk.ability.AbilitySlice;
import ohos.aafwk.content.Intent;
import ohos.agp.components.*;
import ohos.agp.components.surfaceprovider.SurfaceProvider;
import ohos.agp.graphics.Surface;
import ohos.agp.graphics.SurfaceOps;
import ohos.agp.window.dialog.ToastDialog;
import ohos.eventhandler.EventHandler;
import ohos.eventhandler.EventRunner;
import ohos.media.camera.CameraKit;
import ohos.media.camera.device.Camera;
import ohos.media.camera.device.CameraConfig;
import ohos.media.camera.device.CameraInfo;
import ohos.media.camera.device.CameraStateCallback;
import ohos.media.camera.device.FrameConfig;
import ohos.media.image.Image;
import ohos.media.image.ImageReceiver;
import ohos.media.image.common.ImageFormat;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * 条码扫描页 — 使用 HarmonyOS 原生 CameraKit API (API 6)
 *
 * 流程: 摄像头实时预览 → 点击拍照 → 对焦条码并解码 → 返回结果
 * 兜底: 摄像头不可用时可通过手动输入条码
 */
public class BarcodeScanSlice extends AbilitySlice {

    // ---- 相机组件 ----
    private CameraKit cameraKit;
    private Camera camera;
    private SurfaceProvider surfaceProvider;
    private ImageReceiver imageReceiver;
    private EventHandler cameraHandler;
    private EventRunner cameraRunner;

    // ---- 状态 ----
    private boolean cameraReady = false;
    private boolean previewStarted = false;
    private File captureFile;

    // ---- UI 组件 ----
    private Text scanHint;
    private DirectionalLayout manualInputLayout;
    private TextField manualBarcodeInput;
    private StackLayout previewContainer;

    // ---- 常量 ----
    private static final int CAPTURE_WIDTH = 1280;
    private static final int CAPTURE_HEIGHT = 720;

    // ================================================================
    //  生命周期
    // ================================================================

    @Override
    protected void onStart(Intent intent) {
        super.onStart(intent);
        setUIContent(ResourceTable.Layout_slice_barcode_scan);

        scanHint = (Text) findComponentById(ResourceTable.Id_scan_hint);
        manualInputLayout = (DirectionalLayout) findComponentById(ResourceTable.Id_manual_input_layout);
        manualBarcodeInput = (TextField) findComponentById(ResourceTable.Id_manual_barcode_input);
        previewContainer = (StackLayout) findComponentById(ResourceTable.Id_camera_preview_container);

        manualInputLayout.setVisibility(Component.VISIBLE);
        updateHint("正在初始化摄像头...");

        initButtons();
        initCamera();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releaseCamera();
    }

    // ================================================================
    //  按钮事件
    // ================================================================

    private void initButtons() {
        findComponentById(ResourceTable.Id_btn_cancel_scan).setClickedListener(c -> terminate());

        findComponentById(ResourceTable.Id_btn_capture).setClickedListener(c -> {
            if (cameraReady && previewStarted) {
                takePicture();
            } else {
                showToast("摄像头未就绪，请手动输入条码");
            }
        });

        findComponentById(ResourceTable.Id_btn_manual_input).setClickedListener(c -> {
            manualInputLayout.setVisibility(Component.VISIBLE);
            manualBarcodeInput.setText("");
        });

        findComponentById(ResourceTable.Id_btn_manual_cancel).setClickedListener(c ->
                manualInputLayout.setVisibility(Component.HIDE));

        findComponentById(ResourceTable.Id_btn_manual_confirm).setClickedListener(c -> {
            String bc = manualBarcodeInput.getText();
            if (bc != null && !bc.trim().isEmpty()) {
                returnResult(bc.trim());
            } else {
                showToast("请输入条形码");
            }
        });
    }

    // ================================================================
    //  相机初始化 (HarmonyOS 原生 CameraKit)
    // ================================================================

    private void initCamera() {
        try {
            // 1. 创建后台事件线程
            cameraRunner = EventRunner.create("CameraBg");
            cameraHandler = new EventHandler(cameraRunner);

            // 2. 获取 CameraKit 实例
            cameraKit = CameraKit.getInstance(getContext());
            if (cameraKit == null) {
                updateHint("CameraKit 不可用，请手动输入");
                return;
            }

            // 3. 获取可用摄像头列表
            String[] cameraIds = cameraKit.getCameraIds();
            if (cameraIds == null || cameraIds.length == 0) {
                updateHint("未检测到摄像头，请手动输入");
                return;
            }

            // 4. 选择后置摄像头 (FacingType.CAMERA_FACING_BACK)
            String targetId = null;
            for (String id : cameraIds) {
                CameraInfo info = cameraKit.getCameraInfo(id);
                if (info != null && info.getFacingType() == CameraInfo.FacingType.CAMERA_FACING_BACK) {
                    targetId = id;
                    break;
                }
            }
            if (targetId == null) {
                targetId = cameraIds[0]; // fallback 到第一个
            }

            // 5. 创建预览 SurfaceProvider 并添加到布局
            surfaceProvider = new SurfaceProvider(getContext());
            StackLayout.LayoutConfig spCfg = new StackLayout.LayoutConfig(
                    StackLayout.LayoutConfig.MATCH_PARENT,
                    StackLayout.LayoutConfig.MATCH_PARENT);
            surfaceProvider.setLayoutConfig(spCfg);
            surfaceProvider.pinToZTop(false);
            previewContainer.addComponent(surfaceProvider);

            // 6. 创建 ImageReceiver 用于拍照接收
            // ImageReceiver.create(width, height, format, capacity)
            imageReceiver = ImageReceiver.create(CAPTURE_WIDTH, CAPTURE_HEIGHT,
                    ImageFormat.JPEG, 1);
            imageReceiver.setImageArrivalListener(this::onImageArrived);

            // 7. 打开摄像头
            final String camId = targetId;
            cameraKit.createCamera(camId, new CameraStateCallback() {
                @Override
                public void onCreated(Camera c) {
                    camera = c;
                    // 摄像头已创建，等待 Surface 就绪后配置
                    waitForSurfaceAndConfigure();
                }

                @Override
                public void onCreateFailed(String cameraId, int errorCode) {
                    getUITaskDispatcher().asyncDispatch(() ->
                            updateHint("摄像头打开失败(" + errorCode + ")，请手动输入"));
                }

                @Override
                public void onConfigured(Camera c) {
                    // 配置成功 → 启动预览
                    startPreview();
                }

                @Override
                public void onConfigureFailed(Camera c, int errorCode) {
                    getUITaskDispatcher().asyncDispatch(() ->
                            updateHint("摄像头配置失败(" + errorCode + ")，请手动输入"));
                }

                @Override
                public void onFatalError(Camera c, int errorCode) {
                    getUITaskDispatcher().asyncDispatch(() ->
                            updateHint("摄像头致命错误(" + errorCode + ")，请手动输入"));
                    camera = null;
                    cameraReady = false;
                    previewStarted = false;
                }

                @Override
                public void onReleased(Camera c) {
                    camera = null;
                    cameraReady = false;
                    previewStarted = false;
                }
            }, cameraHandler);

        } catch (Exception e) {
            updateHint("摄像头初始化异常: " + e.getClass().getSimpleName() + "，请手动输入");
            cameraReady = false;
        }
    }

    /**
     * 等待 SurfaceProvider 的 Surface 就绪后配置 Camera
     */
    private void waitForSurfaceAndConfigure() {
        if (camera == null || surfaceProvider == null) return;

        // 尝试获取 Surface
        Surface previewSurface = getPreviewSurface();
        if (previewSurface != null) {
            configureCamera(previewSurface);
        } else {
            // Surface 尚未就绪，延迟重试
            getUITaskDispatcher().asyncDispatch(() -> {
                try { Thread.sleep(150); } catch (Exception ignored) {}
                waitForSurfaceAndConfigure();
            });
        }
    }

    /**
     * 从 SurfaceProvider 获取预览 Surface
     */
    private Surface getPreviewSurface() {
        try {
            Optional<SurfaceOps> opsOptional = surfaceProvider.getSurfaceOps();
            if (opsOptional != null && opsOptional.isPresent()) {
                SurfaceOps ops = opsOptional.get();
                if (ops != null) {
                    return ops.getSurface();
                }
            }
        } catch (Exception e) {
            // Surface 尚未就绪
        }
        return null;
    }

    /**
     * 配置 Camera：将预览 Surface 添加到 CameraConfig
     */
    private void configureCamera(Surface previewSurface) {
        try {
            // camera.getCameraConfigBuilder() → CameraConfig.Builder
            CameraConfig.Builder configBuilder = camera.getCameraConfigBuilder();
            configBuilder.addSurface(previewSurface);
            CameraConfig config = configBuilder.build();
            camera.configure(config);
            // 配置完成后 onConfigured() 会被回调，在那里启动预览

        } catch (Exception e) {
            getUITaskDispatcher().asyncDispatch(() ->
                    updateHint("配置失败: " + e.getClass().getSimpleName()));
        }
    }

    /**
     * 启动预览：创建 FRAME_CONFIG_PREVIEW 类型的 FrameConfig 并触发循环采集
     */
    private void startPreview() {
        try {
            if (camera == null) return;

            Surface previewSurface = getPreviewSurface();
            if (previewSurface == null) return;

            // camera.getFrameConfigBuilder(int) — 参数是 FrameConfigType 常量
            FrameConfig.Builder fcBuilder = camera.getFrameConfigBuilder(
                    Camera.FrameConfigType.FRAME_CONFIG_PREVIEW);
            fcBuilder.addSurface(previewSurface);
            FrameConfig fc = fcBuilder.build();
            camera.triggerLoopingCapture(fc);

            previewStarted = true;
            cameraReady = true;
            getUITaskDispatcher().asyncDispatch(() ->
                    updateHint("摄像头已就绪，将条码对准后点击【拍照识别】"));

        } catch (Exception e) {
            getUITaskDispatcher().asyncDispatch(() ->
                    updateHint("预览启动失败: " + e.getClass().getSimpleName()));
        }
    }

    // ================================================================
    //  拍照 & 解码
    // ================================================================

    private void takePicture() {
        if (!cameraReady || camera == null || !previewStarted) {
            showToast("摄像头未就绪");
            return;
        }

        updateHint("正在拍照识别...");

        try {
            // FrameConfigType.FRAME_CONFIG_PICTURE 用于静态拍照
            // getRecevingSurface() ← 注意 SDK 中的拼写
            Surface captureSurface = imageReceiver.getRecevingSurface();

            FrameConfig.Builder fcBuilder = camera.getFrameConfigBuilder(
                    Camera.FrameConfigType.FRAME_CONFIG_PICTURE);
            fcBuilder.addSurface(captureSurface);
            FrameConfig fc = fcBuilder.build();
            camera.triggerSingleCapture(fc);

        } catch (Exception e) {
            updateHint("拍照失败: " + e.getClass().getSimpleName() + "，请重试");
        }
    }

    /**
     * ImageReceiver 的图片到达回调（在后台线程执行）
     */
    private void onImageArrived(ImageReceiver receiver) {
        Image image = null;
        try {
            // 读取最新的图片
            image = receiver.readLatestImage();
            if (image == null) {
                image = receiver.readNextImage();
            }
            if (image == null) {
                getUITaskDispatcher().asyncDispatch(() ->
                        updateHint("未获取到图像，请重试"));
                return;
            }

            // getComponent(ImageFormat.ComponentType.JPEG) → Image.Component
            Image.Component component = image.getComponent(ImageFormat.ComponentType.JPEG);
            if (component == null) {
                getUITaskDispatcher().asyncDispatch(() ->
                        updateHint("图像组件为空，请重试"));
                return;
            }

            // component.getBuffer() → ByteBuffer
            ByteBuffer buffer = component.getBuffer();
            if (buffer == null) {
                getUITaskDispatcher().asyncDispatch(() ->
                        updateHint("图像数据为空，请重试"));
                return;
            }

            byte[] jpgBytes = new byte[buffer.remaining()];
            buffer.get(jpgBytes);

            // 写入临时文件供 ZXing 解码
            File dir = new File(getContext().getCacheDir(), "scan");
            if (!dir.exists()) dir.mkdirs();
            captureFile = new File(dir, "capture_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(captureFile);
            fos.write(jpgBytes);
            fos.close();

            // ZXing 解码
            String barcode = BarcodeDecoder.decodeFromFile(captureFile.getAbsolutePath());
            if (barcode != null && !barcode.isEmpty()) {
                getUITaskDispatcher().asyncDispatch(() -> returnResult(barcode));
            } else {
                getUITaskDispatcher().asyncDispatch(() -> {
                    updateHint("未识别到条码，请调整角度重试或手动输入");
                    showToast("未识别到条码，请调整角度重试");
                });
            }

        } catch (Exception e) {
            getUITaskDispatcher().asyncDispatch(() ->
                    updateHint("图像处理出错: " + e.getClass().getSimpleName()));
        } finally {
            if (image != null) {
                try { image.release(); } catch (Exception ignored) {}
            }
        }
    }

    // ================================================================
    //  辅助方法
    // ================================================================

    private void returnResult(String barcode) {
        Intent ri = new Intent();
        ri.setParam("barcode", barcode);
        setResult(ri);
        terminate();
    }

    private void updateHint(String msg) {
        if (scanHint != null) {
            getUITaskDispatcher().asyncDispatch(() -> scanHint.setText(msg));
        }
    }

    private void showToast(String msg) {
        getUITaskDispatcher().asyncDispatch(() ->
                new ToastDialog(getContext()).setText(msg).setDuration(2000).show());
    }

    /**
     * 释放所有相机资源
     */
    private void releaseCamera() {
        cameraReady = false;
        previewStarted = false;

        // 先停预览再释放
        try {
            if (camera != null) {
                camera.stopLoopingCapture();
            }
        } catch (Exception ignored) {}

        try {
            if (camera != null) {
                camera.release();
                camera = null;
            }
        } catch (Exception ignored) {}

        try {
            if (imageReceiver != null) {
                imageReceiver.release();
                imageReceiver = null;
            }
        } catch (Exception ignored) {}

        try {
            if (surfaceProvider != null && previewContainer != null) {
                surfaceProvider.removeFromWindow();
                previewContainer.removeComponent(surfaceProvider);
            }
        } catch (Exception ignored) {}

        try {
            if (cameraRunner != null) {
                cameraRunner.stop();
                cameraRunner = null;
            }
        } catch (Exception ignored) {}
    }
}
