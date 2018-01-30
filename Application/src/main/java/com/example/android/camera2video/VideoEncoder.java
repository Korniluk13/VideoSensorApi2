package com.example.android.camera2video;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.support.v4.util.CircularArray;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class VideoEncoder {

    private static final String TAG = "VideoEncoder";

    private int mWidth;
    private int mHeight;
    private int mBitRate;
    private String mOutputPath;

    private static final int TIMEOUT_USEC = 10000;

    private static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 30;

    private static final int IFRAME_INTERVAL = 5;

    private MediaCodec mEncoder;
    private MediaMuxer mMuxer;
    private int mFrameCount;
    private int mVideoTrack = -1;
    private boolean mMuxerStarted = false;
    private CircularArray<ExtractedImage> mImageArray;

    public VideoEncoder(int width, int height, int bitRate, CircularArray<ExtractedImage> imageArray) {
        mWidth = width;
        mHeight = height;
        mBitRate = bitRate;
        mFrameCount = 0;
        mImageArray = imageArray;
    }

    public void setOutputPath(String path) {
        mOutputPath = path;
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
            mMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMuxerStarted = false;
    }

    public void addImage() {
        ExtractedImage img = null;
        synchronized (mImageArray) {
            Log.d(TAG, "size " + mImageArray.size());
            img = mImageArray.popLast();
        }

        if (img != null) {
            Mat matYUV = ImageUtils.imageToMat(img);
            int inputBufferId = mEncoder.dequeueInputBuffer(TIMEOUT_USEC);

            if (inputBufferId >= 0) {
                Log.d(TAG, "input buffer" + inputBufferId);
                ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferId);
                int size = inputBuffer.remaining();

                Imgproc.line(matYUV, new Point(0, 0), new Point(255, 255),
                        new Scalar(0, 0, 255), 5);

                Image inputImage = mEncoder.getInputImage(inputBufferId);
                CodecUtils.copyMatToImage(matYUV.dataAddr(), inputImage);

                mEncoder.queueInputBuffer(inputBufferId, 0, size, mFrameCount * 1000000 / FRAME_RATE, 0);
                mFrameCount++;
            }
            drainEncoder();
        }
    }

    public void drainEncoder() {
        while (true) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferId= mEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if (outputBufferId >= 0) {
                ByteBuffer outputBuffer = mEncoder.getOutputBuffer(outputBufferId);
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
        Log.d(TAG, "Frame count: " + mFrameCount);
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
    }
}