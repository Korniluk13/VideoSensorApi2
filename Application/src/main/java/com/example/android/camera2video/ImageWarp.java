package com.example.android.camera2video;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.INTER_LINEAR;

public class ImageWarp {

    private static final String TAG = "ImageWarp";

    public static Mat warp(Mat src, Mat rotation) {
        Size size = src.size();
        int width = (int)size.width;
        int height = (int)size.height;
        Log.d(TAG, "Size: " + height + "x" + width);

        int type = src.type();
        Log.d(TAG, "Type: " + type);

        Mat dst = new Mat(size, type, new Scalar(0, 0, 0));
        Log.d(TAG, "Image base created.");

        Mat mx = new Mat(size, CvType.CV_32F);
        Mat my = new Mat(size, CvType.CV_32F);

        Log.d(TAG, "create array");
        double buff[] = new double[2 * width * height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                buff[2 * (i * width + j)] = i;
                buff[2 * (i * width + j) + 1] = j;
            }
        }

        Mat src1 = new Mat(height * width, 1, CvType.CV_64FC2);
        src1.put(0, 0, buff);

        Mat dst1 = new Mat(src1.size(), src1.type());

        Log.d(TAG, "array converted to mat");
        Core.perspectiveTransform(src1, dst1, rotation);
        Log.d(TAG, "perspectiveTransform");

        List<Mat> m = new ArrayList<Mat>(2);
        Core.split(dst1, m);
        Log.d(TAG, "splited");

        Mat m0 = m.get(1).reshape(1, height);
        Mat m1 = m.get(0).reshape(1, height);
        Log.d(TAG, "reshaped");

        m0.convertTo(mx, CvType.CV_32F);
        m1.convertTo(my, CvType.CV_32F);
        Log.d(TAG, "converted");

        Imgproc.remap(src, dst, mx, my, INTER_LINEAR);
        Log.d(TAG, "remaped");

        return dst;
    }
}