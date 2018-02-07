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
    private static Scalar black = new Scalar(0, 0, 0);
    private int mWidth;
    private int mHeight;
    private Size mSize;
    private double[] mBuff;

    public ImageWarp(int width, int height) {
        mWidth = width;
        mHeight = height;
        mSize = new Size(width, height);
        mBuff = new double[2 * mWidth * mHeight];
    }

    public Mat warp(Mat src, Mat rotation) {

        int type = src.type();

        Mat dst = new Mat(mSize, type, black);

        Mat mx = new Mat(mSize, CvType.CV_32F);
        Mat my = new Mat(mSize, CvType.CV_32F);

        for (int i = 0; i < mHeight; i++) {
            for (int j = 0; j < mWidth; j++) {
                mBuff[2 * (i * mWidth + j)] = i;
                mBuff[2 * (i * mWidth + j) + 1] = j;
            }
        }

        Mat src1 = new Mat(mHeight * mWidth, 1, CvType.CV_64FC2);
        src1.put(0, 0, mBuff);

        Mat dst1 = new Mat(mHeight * mWidth, 1, CvType.CV_64FC2);

        Core.perspectiveTransform(src1, dst1, rotation);

        List<Mat> m = new ArrayList<Mat>(2);
        Core.split(dst1, m);

        Mat m0 = m.get(1).reshape(1, mHeight);
        Mat m1 = m.get(0).reshape(1, mHeight);

        m0.convertTo(mx, CvType.CV_32F);
        m1.convertTo(my, CvType.CV_32F);

        Imgproc.remap(src, dst, mx, my, INTER_LINEAR);

        return dst;
    }
}