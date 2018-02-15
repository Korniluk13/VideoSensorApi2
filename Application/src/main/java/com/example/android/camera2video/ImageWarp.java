package com.example.android.camera2video;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.warpPerspective;

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
        for (int i = 0; i < mHeight; i++) {
            for (int j = 0; j < mWidth; j++) {
                mBuff[2 * (i * mWidth + j)] = i;
                mBuff[2 * (i * mWidth + j) + 1] = j;
            }
        }
    }

    public Mat warp(ExtractedImage img, Mat rotation) {
        byte[] Y = img.getPlane(0);
        Mat y = new Mat(mHeight, mWidth, CV_8UC1);
        y.put(0, 0, Y);
        Mat dstY = new Mat(mSize, CV_8UC1);
        warpPerspective(y, dstY, rotation, y.size());

        byte[] U = img.getPlane(1);
        Mat u = new Mat(mHeight / 2, mWidth / 2, CV_8UC1);
        u.put(0, 0, U);
        Mat dstU = new Mat(u.size(), CV_8UC1);
        warpPerspective(u, dstU, rotation, u.size());

        byte[] V = img.getPlane(2);
        Mat v = new Mat(mHeight / 2, mWidth / 2, CV_8UC1);
        v.put(0, 0, V);
        Mat dstV = new Mat(v.size(), CV_8UC1);
        warpPerspective(v, dstV, rotation, v.size());

        return null;
    }

    public Mat warp(Mat src, Mat rotation) {

        Log.d(TAG, "after init");
        int type = src.type();

        Mat dst = new Mat(mSize, type);
//
//        Mat mx = new Mat(mSize, CvType.CV_32F);
//        Mat my = new Mat(mSize, CvType.CV_32F);
//
//
//        Mat src1 = new Mat(mHeight * mWidth, 1, CvType.CV_64FC2);
//        src1.put(0, 0, mBuff);
//
//        Log.d(TAG, "copy");
//
//        Mat dst1 = new Mat(mHeight * mWidth, 1, CvType.CV_64FC2);

//        Mat dst = src;
        warpPerspective(src, dst, rotation, src.size());
        Log.d(TAG, "perspective transfa");
//
//        List<Mat> m = new ArrayList<Mat>(2);
//        Core.split(dst1, m);
//
//        Mat m0 = m.get(1).reshape(1, mHeight);
//        Mat m1 = m.get(0).reshape(1, mHeight);
//
//        m0.convertTo(mx, CvType.CV_32F);
//        m1.convertTo(my, CvType.CV_32F);
//
//        Imgproc.remap(src, dst, mx, my, INTER_LINEAR);

        return dst;
    }
}