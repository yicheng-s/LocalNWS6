package com.sunyicheng.myapplication.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.media.image.common.Rect;
import ohos.media.image.common.Size;

import java.io.File;
import java.io.FileInputStream;
import java.util.EnumMap;
import java.util.Map;

/**
 * 条码解码工具类 - 适配 HarmonyOS API 6
 */
public class BarcodeDecoder {

    private static final Map<DecodeHintType, Object> HINTS;

    static {
        HINTS = new EnumMap<>(DecodeHintType.class);
        HINTS.put(DecodeHintType.POSSIBLE_FORMATS,
                java.util.Arrays.asList(
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.EAN_8,
                        BarcodeFormat.UPC_A,
                        BarcodeFormat.UPC_E,
                        BarcodeFormat.CODE_128,
                        BarcodeFormat.CODE_39,
                        BarcodeFormat.CODE_93,
                        BarcodeFormat.CODABAR,
                        BarcodeFormat.ITF,
                        BarcodeFormat.QR_CODE,
                        BarcodeFormat.DATA_MATRIX,
                        BarcodeFormat.PDF_417
                ));
        HINTS.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        HINTS.put(DecodeHintType.CHARACTER_SET, "UTF-8");
    }

    /**
     * 从图片文件解码条码
     */
    public static String decodeFromFile(String imageFilePath) {
        try {
            File file = new File(imageFilePath);
            if (!file.exists()) {
                return null;
            }

            FileInputStream fis = new FileInputStream(file);
            ImageSource.SourceOptions srcOpts = new ImageSource.SourceOptions();
            srcOpts.formatHint = "image/jpeg";

            ImageSource imageSource = ImageSource.create(fis, srcOpts);

            ImageSource.DecodingOptions decOpts = new ImageSource.DecodingOptions();
            decOpts.desiredSize = new Size(1920, 1080);

            PixelMap pixelMap = imageSource.createPixelmap(decOpts);
            fis.close();

            if (pixelMap == null) {
                return null;
            }

            String result = decodeFromPixelMap(pixelMap);
            pixelMap.release();
            return result;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 PixelMap 解码条码
     */
    private static String decodeFromPixelMap(PixelMap pixelMap) {
        try {
            int width = pixelMap.getImageInfo().size.width;
            int height = pixelMap.getImageInfo().size.height;

            // API 6: readPixels 使用 Rect 参数
            int[] pixels = new int[width * height];
            Rect rect = new Rect(0, 0, width, height);
            pixelMap.readPixels(pixels, 0, width, rect);

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            MultiFormatReader reader = new MultiFormatReader();
            reader.setHints(HINTS);

            Result result = reader.decode(bitmap);
            return result.getText();

        } catch (NotFoundException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String decodeFromFileWithRotation(String imageFilePath) {
        String result = decodeFromFile(imageFilePath);
        if (result != null) {
            return result;
        }
        return null;
    }
}
