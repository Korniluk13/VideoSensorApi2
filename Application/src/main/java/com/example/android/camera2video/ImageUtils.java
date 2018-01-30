package com.example.android.camera2video;
import android.graphics.ImageFormat;

import org.opencv.core.CvType;
import org.opencv.core.Mat;


public class ImageUtils {

    public static Mat imageToMat(ExtractedImage image) {
        byte[] buffer;
        int rowStride;
        int pixelStride;
        int width = image.getWidth();
        int height = image.getHeight();
        int offset = 0;

        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        byte[] rowData = new byte[image.getRowStride(0)];

        for (int i = 0; i < 3; i++) {
            rowStride = image.getRowStride(i);
            pixelStride = image.getPixelStride(i);
            buffer = image.getPlane(i);

            int w = (i == 0) ? width : width / 2;
            int h = (i == 0) ? height : height / 2;

            int bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;

            // Hardcoded. Check rowStride == length. Needed for performance
            if (pixelStride == bytesPerPixel) {
                int length = w * h * bytesPerPixel;
                System.arraycopy(buffer, 0, data, 0, length);
                offset += length;
            } else {
                int buffOffset = 0;
                for (int row = 0; row < h; row++) {
                    if (h - row == 1) {
                        System.arraycopy(buffer, buffOffset, rowData, 0, width - pixelStride + 1);
                    } else {
                        System.arraycopy(buffer, buffOffset, rowData, 0, rowStride);
                    }

                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                    buffOffset += rowStride;
                }
            }
        }

        Mat mat = new Mat(height + height / 2, width, CvType.CV_8UC1);
        mat.put(0, 0, data);

        return mat;
    }
}
