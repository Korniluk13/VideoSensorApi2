package com.stabilizer.stabilizer;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import com.stabilizer.stabilizer.CodecUtils;
import com.stabilizer.stabilizer.ImageUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class VideoEncoder implements Runnable {

    private static final String TAG = "VideoEncoder";

    private int mWidth;
    private int mHeight;
    private int mBitRate;

    private static final int TIMEOUT_USEC = 10000;

    private static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 30;
    // TODO: нормально оформить путь
    private static final String outputPath = "/test.mp4";

    // TODO: Разобраться c IFrame и inputSurface
    private static final int IFRAME_INTERVAL = 5;

    private MediaCodec mEncoder;
    private MediaMuxer mMuxer;
    private int mFrameCount;
    private int mVideoTrack = -1;
    private boolean mMuxerStarted = false;

    private LinkedList<Mat> frameBuffer;

    public VideoEncoder(int width, int height, int bitRate) {
        mWidth = width;
        mHeight = height;
        mBitRate = bitRate;
        mFrameCount = 0;

        frameBuffer = new LinkedList<>();
    }

    public void prepare() {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        try {
            mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mEncoder.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();

        try {
            mMuxer = new MediaMuxer(Environment.getExternalStorageDirectory().getPath() + "/tmp.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMuxerStarted = false;
    }

    public void addImage(Image image)
    {
        Mat img = ImageUtils.imageToMat(image);
        frameBuffer.add(img);
    }

    public void addImageToVideo(Image image) {
        int inputBufferId = mEncoder.dequeueInputBuffer(TIMEOUT_USEC);

        if (inputBufferId >= 0) {
            ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferId);
            int size = inputBuffer.remaining();


            Image inputImage = mEncoder.getInputImage(inputBufferId);

            CodecUtils.copyFlexYUVImage(inputImage, image);

            Mat img = ImageUtils.imageToMat(image);

            Mat imgBGR = new Mat();

            Imgproc.cvtColor(img, imgBGR, Imgproc.COLOR_YUV2BGR_I420);

            Imgcodecs.imwrite("/storage/emulated/0/st" + "x" + ".jpg", imgBGR
            );

            Imgproc.line(imgBGR, new Point(0, 0), new Point(255, 255),
                   new Scalar(0, 0, 255), 5);

            /* DO SMTH with imageBGR*/

            Mat yuvImage = new Mat();
            Imgproc.cvtColor(imgBGR, yuvImage, Imgproc.COLOR_BGR2YUV_I420);
            CodecUtils.copyMatToImage(yuvImage.dataAddr(), inputImage);

            mEncoder.queueInputBuffer(inputBufferId, 0, size, mFrameCount * 1000000 / FRAME_RATE, 0);
            mFrameCount++;
        }
        Log.d(TAG, "add" + Integer.toString(mFrameCount));
        drainEncoder();
    }

    public void drainEncoder() {
        while (true) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferId= mEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            Log.d(TAG, "oid" + Integer.toString(outputBufferId));
            if (outputBufferId >= 0) {
                ByteBuffer outputBuffer = mEncoder.getOutputBuffer(outputBufferId);

                if (outputBuffer == null) {
                    Log.d(TAG, "output buffer was null");
                } else {
                    Log.d(TAG, Integer.toString(bufferInfo.size));
                }

                mMuxer.writeSampleData(mVideoTrack, outputBuffer, bufferInfo);
                mEncoder.releaseOutputBuffer(outputBufferId, false);
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mEncoder.getOutputFormat();
                mVideoTrack = mMuxer.addTrack(newFormat);
                mMuxer.start();
            } else {
                break;
            }
        }
    }

    public void release() {
        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
        if (mMuxer != null) {
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
        Log.d(TAG, "frames count: " + Integer.toString(mFrameCount));
    }

    @Override
    public void run() {
        try {
            Thread.sleep(500);


        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
