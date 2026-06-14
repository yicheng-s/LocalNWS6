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
 * 条码扫描页 — HarmonyOS 原生 CameraKit API (API 6)
 *
 * 修复要点:
 *  1. 使用 onActive() 启动相机（确保 SurfaceProvider 已完成布局）
 *  2. 所有相机操作统一在 cameraHandler 线程执行
 *  3. SurfaceProvider 就绪后才配置 Camera
 *  4. 实时预览 + 拍照解码 + 手动输入兜底
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
    private boolean cameraOpened = false;
    private boolean previewStarted = false;
    private Surface previewSurface;       // 获取到的预览 Surface
    private File captureFile;

    // ---- UI 组件 ----
    private Text scanHint;
    private DirectionalLayout manualInputLayout;
    private TextField manualBarcodeInput;
    private StackLayout previewContainer;

    // ---- 常量 ----
    private static final int CAPTURE_WIDTH = 1280;
    private static final int CAPTURE_HEIGHT = 720;
    private static final int MAX_RETRY = 20;         // 最多重试 20 次获取 Surface

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
        updateHint("正在准备摄像头...");

        initButtons();
        setupCameraInfra();
    }

    @Override
    protected void onActive() {
        super.onActive();
        // onActive 时布局已全部完成，此时 SurfaceProvider 的 Surface 可获取
        // 延迟一小段时间确保 SurfaceProvider 已绑定到窗口
        getUITaskDispatcher().asyncDispatch(() -> {
            try { Thread.sleep(300); } catch (Exception ignored) {}
            startCameraInit();
        });
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
            if (previewStarted && camera != null) {
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
    //  基础设施初始化（在 onStart 中调用）
    // ================================================================

    private void setupCameraInfra() {
        try {
            // 1. 创建后台事件线程
            cameraRunner = EventRunner.create("CameraBg");
            cameraHandler = new EventHandler(cameraRunner);

            // 2. 获取 CameraKit
            cameraKit = CameraKit.getInstance(getContext());

            // 3. 创建 SurfaceProvider 并添加到布局
            surfaceProvider = new SurfaceProvider(getContext());
            StackLayout.LayoutConfig spCfg = new StackLayout.LayoutConfig(
                    StackLayout.LayoutConfig.MATCH_PARENT,
                    StackLayout.LayoutConfig.MATCH_PARENT);
            surfaceProvider.setLayoutConfig(spCfg);
            surfaceProvider.pinToZTop(false);
            previewContainer.addComponent(surfaceProvider);

            // 4. 创建 ImageReceiver 用于拍照
            imageReceiver = ImageReceiver.create(CAPTURE_WIDTH, CAPTURE_HEIGHT,
                    ImageFormat.JPEG, 1);
            imageReceiver.setImageArrivalListener(this::onImageArrived);

        } catch (Exception e) {
            updateHint("相机准备失败: " + e.getClass().getSimpleName() + "，请手动输入");
        }
    }

    // ================================================================
    //  相机启动（在 onActive 后调用，UI 线程）
    // ================================================================

    private void startCameraInit() {
        if (cameraKit == null) {
            updateHint("CameraKit 不可用，请手动输入");
            return;
        }

        try {
            // 尝试获取预览 Surface（此时 SurfaceProvider 应已绑定到窗口）
            previewSurface = getPreviewSurface();
            if (previewSurface == null) {
                // 仍然未就绪，调度重试
                retryGetSurface(0);
                return;
            }

            // Surface 已就绪，打开摄像头
            updateHint("正在打开摄像头...");

            // 查找后置摄像头
            String[] cameraIds = cameraKit.getCameraIds();
            if (cameraIds == null || cameraIds.length == 0) {
                updateHint("未检测到摄像头，请手动输入");
                return;
            }

            String targetId = null;
            for (String id : cameraIds) {
                CameraInfo info = cameraKit.getCameraInfo(id);
                if (info != null && info.getFacingType() == CameraInfo.FacingType.CAMERA_FACING_BACK) {
                    targetId = id;
                    break;
                }
            }
            if (targetId == null) targetId = cameraIds[0];

            final String camId = targetId;
            cameraKit.createCamera(camId, new CameraStateCallback() {
                @Override
                public void onCreated(Camera c) {
                    camera = c;
                    cameraOpened = true;
                    // 摄像头已创建 → 在 camera 线程配置预览
                    configurePreview();
                }

                @Override
                public void onCreateFailed(String cameraId, int errorCode) {
                    getUITaskDispatcher().asyncDispatch(() ->
                            updateHint("摄像头打开失败(" + errorCode + ")，请手动输入"));
                }

                @Override
                public void onConfigured(Camera c) {
                    // 预览配置完成 → 启动预览
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
                    cameraOpened = false;
                    previewStarted = false;
                }

                @Override
                public void onReleased(Camera c) {
                    cameraOpened = false;
                    previewStarted = false;
                }
            }, cameraHandler);

        } catch (Exception e) {
            updateHint("摄像头启动异常: " + e.getClass().getSimpleName() + "，请手动输入");
        }
    }

    /**
     * 重试获取 Surface（最多 MAX_RETRY 次，每次间隔 200ms）
     * 运行在 cameraHandler 线程
     */
    private void retryGetSurface(int attempt) {
        if (attempt >= MAX_RETRY) {
            getUITaskDispatcher().asyncDispatch(() ->
                    updateHint("摄像头预览未就绪，请手动输入"));
            return;
        }

        previewSurface = getPreviewSurface();
        if (previewSurface != null) {
            // Surface 已就绪，继续在 camera 线程执行
            cameraHandler.postTask(() -> startCameraInitOnCameraThread());
        } else {
            // 继续重试
            cameraHandler.postTask(() -> {
                try { Thread.sleep(200); } catch (Exception ignored) {}
                retryGetSurface(attempt + 1);
            }, 250);
        }
    }

    /**
     * 在 camera 线程上完成相机初始化（Surface 已就绪）
     */
    private void startCameraInitOnCameraThread() {
        try {
            String[] cameraIds = cameraKit.getCameraIds();
            if (cameraIds == null || cameraIds.length == 0) {
                getUITaskDispatcher().asyncDispatch(() ->
                        updateHint("未检测到摄像头，请手动输入"));
                return;
            }

            String targetId = null;
            for (String id : cameraIds) {
                CameraInfo info = cameraKit.getCameraInfo(id);
                if (info != null && info.getFacingType() == CameraInfo.FacingType.CAMERA_FACING_BACK) {
                    targetId = id;
                    break;
                }
            }
            if (targetId == null) targetId = cameraIds[0];

            getUITaskDispatcher().asyncDispatch(() ->
                    updateHint("正在打开摄像头..."));

            cameraKit.createCamera(targetId, new CameraStateCallback() {
                @Override
                public void onCreated(Camera c) {
                    camera = c;
                    cameraOpened = true;
                    configurePreview();
                }

                @Override
                public void onCreateFailed(String cameraId, int errorCode) {
                    getUITaskDispatcher().asyncDispatch(() ->
                            updateHint("摄像头打开失败(" + errorCode + ")，请手动输入"));
                }

                @Override
                public void onConfigured(Camera c) {
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
                    cameraOpened = false;
                    previewStarted = false;
                }

                @Override
                public void onReleased(Camera c) {
                    cameraOpened = false;
                    previewStarted = false;
                }
            }, cameraHandler);

        } catch (Exception e) {
            getUITaskDispatcher().asyncDispatch(() ->
                    updateHint("相机启动失败: " + e.getClass().getSimpleName()));
        }
    }

    // ================================================================
    //  Surface 获取
    // ================================================================

    /**
     * 从 SurfaceProvider 获取预览 Surface
     * 必须在 SurfaceProvider 绑定到窗口后才能成功
     */
    private Surface getPreviewSurface() {
        try {
            if (surfaceProvider == null) return null;
            Optional<SurfaceOps> opsOptional = surfaceProvider.getSurfaceOps();
            if (opsOptional != null && opsOptional.isPresent()) {
                SurfaceOps ops = opsOptional.get();
                if (ops != null) {
                    Surface s = ops.getSurface();
                    if (s != null) return s;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ================================================================
    //  相机配置 & 预览（均在 cameraHandler 线程执行）
    // ================================================================

    /**
     * 配置 Camera 的预览 Surface
     * 在 onCreated 回调中调用（cameraHandler 线程）
     */
    private void configurePreview() {
        try {
            if (camera == null) return;

            // 确保预览 Surface 有效
            if (previewSurface == null) {
                previewSurface = getPreviewSurface();
            }
            if (previewSurface == null) {
                getUITaskDispatcher().asyncDispatch(() ->
                        updateHint("预览Surface不可用，请手动输入"));
                return;
            }

            CameraConfig.Builder configBuilder = camera.getCameraConfigBuilder();
            configBuilder.addSurface(previewSurface);
            CameraConfig config = configBuilder.build();
            camera.configure(config);
            // 成功后系统回调 onConfigured()

        } catch (Exception e) {
            getUITaskDispatcher().asyncDispatch(() ->
                    updateHint("配置摄像头失败: " + e.getClass().getSimpleName()));
        }
    }

    /**
     * 启动循环预览
     * 在 onConfigured 回调中调用（cameraHandler 线程）
     */
    private void startPreview() {
        try {
            if (camera == null) return;

            if (previewSurface == null) {
                previewSurface = getPreviewSurface();
            }
            if (previewSurface == null) return;

            // FRAME_CONFIG_PREVIEW = 循环帧，用于实时预览
            FrameConfig.Builder fcBuilder = camera.getFrameConfigBuilder(
                    Camera.FrameConfigType.FRAME_CONFIG_PREVIEW);
            fcBuilder.addSurface(previewSurface);
            FrameConfig fc = fcBuilder.build();
            camera.triggerLoopingCapture(fc);

            previewStarted = true;
            getUITaskDispatcher().asyncDispatch(() ->
                    updateHint("摄像头已就绪，将条码对准后点击【拍照识别】"));

        } catch (Exception e) {
            getUITaskDispatcher().asyncDispatch(() ->
                    updateHint("启动预览失败: " + e.getClass().getSimpleName()));
        }
    }

    // ================================================================
    //  拍照 & 解码
    // ================================================================

    private void takePicture() {
        if (!previewStarted || camera == null) {
            showToast("摄像头未就绪");
            return;
        }

        updateHint("正在拍照识别...");

        // 在 cameraHandler 线程执行拍照
        cameraHandler.postTask(() -> {
            try {
                // getRecevingSurface() ← SDK 中的拼写
                Surface captureSurface = imageReceiver.getRecevingSurface();

                FrameConfig.Builder fcBuilder = camera.getFrameConfigBuilder(
                        Camera.FrameConfigType.FRAME_CONFIG_PICTURE);
                fcBuilder.addSurface(captureSurface);
                FrameConfig fc = fcBuilder.build();
                camera.triggerSingleCapture(fc);

            } catch (Exception e) {
                getUITaskDispatcher().asyncDispatch(() ->
                        updateHint("拍照失败: " + e.getClass().getSimpleName()));
            }
        });
    }

    /**
     * ImageReceiver 的图片到达回调
     */
    private void onImageArrived(ImageReceiver receiver) {
        Image image = null;
        try {
            image = receiver.readLatestImage();
            if (image == null) {
                image = receiver.readNextImage();
            }
            if (image == null) {
                getUITaskDispatcher().asyncDispatch(() ->
                        updateHint("未获取到图像，请重试"));
                return;
            }

            Image.Component component = image.getComponent(ImageFormat.ComponentType.JPEG);
            if (component == null) {
                getUITaskDispatcher().asyncDispatch(() ->
                        updateHint("图像组件为空，请重试"));
                return;
            }

            ByteBuffer buffer = component.getBuffer();
            if (buffer == null) {
                getUITaskDispatcher().asyncDispatch(() ->
                        updateHint("图像数据为空，请重试"));
                return;
            }

            byte[] jpgBytes = new byte[buffer.remaining()];
            buffer.get(jpgBytes);

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
        previewStarted = false;
        cameraOpened = false;

        if (cameraHandler != null) {
            cameraHandler.postTask(() -> {
                try {
                    if (camera != null) {
                        camera.stopLoopingCapture();
                        camera.release();
                        camera = null;
                    }
                } catch (Exception ignored) {}
            });
        }

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

        previewSurface = null;
    }
}
