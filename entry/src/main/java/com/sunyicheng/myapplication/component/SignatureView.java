package com.sunyicheng.myapplication.component;

import ohos.agp.components.Component;
import ohos.agp.render.Canvas;
import ohos.agp.render.Paint;
import ohos.agp.render.Path;
import ohos.agp.utils.Color;
import ohos.agp.utils.Point;
import ohos.app.Context;
import ohos.multimodalinput.event.TouchEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 手写签名自定义组件 - API 6
 * 记录触摸点轨迹，保存时手工光栅化写 BMP 文件
 */
public class SignatureView extends Component
        implements Component.DrawTask, Component.TouchEventListener {

    private final Path drawPath;
    private final Paint paint;
    private final Paint bgPaint;
    private final Point prevPoint;
    private final Point prevCtrlPoint;
    private int viewWidth;
    private int viewHeight;
    private boolean hasContent = false;

    // 记录触摸轨迹点（用于保存时手工光栅化）
    private final List<float[]> touchPoints = new ArrayList<>();

    private static final float STROKE_WIDTH = 6.0f;

    public SignatureView(Context context) {
        super(context);

        drawPath = new Path();
        prevPoint = new Point();
        prevCtrlPoint = new Point();

        paint = new Paint();
        paint.setColor(new Color(0xFF000000));
        paint.setStrokeWidth(STROKE_WIDTH);
        paint.setStyle(Paint.Style.STROKE_STYLE);
        paint.setStrokeCap(Paint.StrokeCap.ROUND_CAP);
        paint.setAntiAlias(true);

        bgPaint = new Paint();
        bgPaint.setColor(new Color(0xFFFFFFFF));
        bgPaint.setStyle(Paint.Style.FILL_STYLE);
        bgPaint.setAntiAlias(true);

        addDrawTask(this);
        setTouchEventListener(this);

        viewWidth = getWidth();
        viewHeight = getHeight();
    }

    @Override
    public void onDraw(Component component, Canvas canvas) {
        int w = viewWidth > 0 ? viewWidth : component.getWidth();
        int h = viewHeight > 0 ? viewHeight : component.getHeight();
        if (w <= 0) w = 800;
        if (h <= 0) h = 400;

        canvas.drawRect(0, 0, w, h, bgPaint);
        canvas.drawPath(drawPath, paint);
    }

    @Override
    public boolean onTouchEvent(Component component, TouchEvent event) {
        int action = event.getAction();
        float x = event.getPointerPosition(event.getIndex()).getX();
        float y = event.getPointerPosition(event.getIndex()).getY();

        int w = component.getWidth();
        int h = component.getHeight();
        if (w > 0 && h > 0) { viewWidth = w; viewHeight = h; }

        // 记录触摸点
        touchPoints.add(new float[]{x, y, (float) action});

        switch (action) {
            case TouchEvent.PRIMARY_POINT_DOWN:
                drawPath.moveTo(x, y);
                prevPoint.position[0] = x;
                prevPoint.position[1] = y;
                hasContent = true;
                return true;

            case TouchEvent.POINT_MOVE:
                float midX = (x + prevPoint.position[0]) / 2;
                float midY = (y + prevPoint.position[1]) / 2;
                prevCtrlPoint.position[0] = midX;
                prevCtrlPoint.position[1] = midY;
                drawPath.quadTo(prevPoint, prevCtrlPoint);
                prevPoint.position[0] = x;
                prevPoint.position[1] = y;
                invalidate();
                return true;
        }
        return false;
    }

    public void clear() {
        drawPath.reset();
        touchPoints.clear();
        hasContent = false;
        invalidate();
    }

    public boolean hasContent() {
        return hasContent;
    }

    /**
     * 保存签名为 BMP 文件（纯 java.io）
     */
    public String saveToFile(String saveDir, String fileName) {
        if (!hasContent || touchPoints.isEmpty()) {
            return null;
        }

        FileOutputStream fos = null;
        try {
            int width = viewWidth > 0 ? viewWidth : 800;
            int height = viewHeight > 0 ? viewHeight : 400;

            int rowSize = ((width * 3 + 3) / 4) * 4;
            int imageSize = rowSize * height;

            // ARGB 像素数组，白色背景
            int[] argb = new int[width * height];
            for (int i = 0; i < argb.length; i++) {
                argb[i] = 0xFFFFFFFF;
            }

            // 光栅化触摸轨迹
            float lastX = -1, lastY = -1;
            boolean isDown = false;
            int radius = Math.max(1, (int) (STROKE_WIDTH / 2)) + 1;

            for (float[] pt : touchPoints) {
                float px = pt[0], py = pt[1];
                int action = (int) pt[2];

                if (action == TouchEvent.PRIMARY_POINT_DOWN) {
                    drawCircle(argb, width, height, (int) px, (int) py, radius, 0xFF000000);
                    lastX = px; lastY = py; isDown = true;
                } else if (isDown && action == TouchEvent.POINT_MOVE) {
                    drawThickLine(argb, width, height,
                            (int) lastX, (int) lastY, (int) px, (int) py, radius, 0xFF000000);
                    lastX = px; lastY = py;
                } else if (action == TouchEvent.PRIMARY_POINT_UP) {
                    isDown = false;
                }
            }

            // 写 BMP 文件
            File dir = new File(saveDir);
            if (!dir.exists()) dir.mkdirs();
            String filePath = new File(saveDir, fileName + ".bmp").getAbsolutePath();
            fos = new FileOutputStream(filePath);

            // BMP 文件头 (14 bytes)
            int fileSize = 14 + 40 + imageSize;
            writeLE(fos, 0x4D42, 2);
            writeLE(fos, fileSize, 4);
            writeLE(fos, 0, 2);
            writeLE(fos, 0, 2);
            writeLE(fos, 14 + 40, 4);

            // DIB 头 (40 bytes)
            writeLE(fos, 40, 4);
            writeLE(fos, width, 4);
            writeLE(fos, height, 4);
            writeLE(fos, 1, 2);
            writeLE(fos, 24, 2);
            writeLE(fos, 0, 4);
            writeLE(fos, imageSize, 4);
            writeLE(fos, 2835, 4);
            writeLE(fos, 2835, 4);
            writeLE(fos, 0, 4);
            writeLE(fos, 0, 4);

            // 像素数据（底行→顶行，BGR）
            byte[] pad = new byte[rowSize - width * 3];
            for (int y = height - 1; y >= 0; y--) {
                for (int x = 0; x < width; x++) {
                    int c = argb[y * width + x];
                    fos.write((c >> 16) & 0xFF);
                    fos.write((c >> 8) & 0xFF);
                    fos.write(c & 0xFF);
                }
                fos.write(pad);
            }

            fos.close();
            return filePath;

        } catch (Exception e) {
            return null;
        } finally {
            try { if (fos != null) fos.close(); } catch (Exception ignored) {}
        }
    }

    private void writeLE(FileOutputStream fos, int value, int bytes) throws Exception {
        for (int i = 0; i < bytes; i++) {
            fos.write((value >> (i * 8)) & 0xFF);
        }
    }

    private void drawCircle(int[] pixels, int w, int h, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx * dx + dy * dy <= r * r) {
                    int px = cx + dx, py = cy + dy;
                    if (px >= 0 && px < w && py >= 0 && py < h) {
                        pixels[py * w + px] = color;
                    }
                }
            }
        }
    }

    private void drawThickLine(int[] pixels, int w, int h,
                               int x0, int y0, int x1, int y1, int r, int color) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            drawCircle(pixels, w, h, x0, y0, r, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = err * 2;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }
}
