package com.example.android.camera2video;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.support.v4.util.CircularArray;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoProcessor {

    static {
        System.loadLibrary("native-lib");
    }

    private static final String TAG = "VideoProcessor";

    private int mWidth;
    private int mHeight;
    private int mBitRate;
    private String mOutputPath;

    private static final int TIMEOUT_USEC = 10000;

    private static final String MIME_TYPE = "video/avc";
    private static final int FRAME_RATE = 15;

    private static final int IFRAME_INTERVAL = 5;

    private MediaCodec mEncoder;
    private MediaMuxer mMuxer;
    private int mFrameCount;
    private int mVideoTrack = -1;
    private boolean mMuxerStarted = false;
    private ImageUtils mImageUtils = null;
    private ExecutorService mExecutorService;
    private long mGyroCounter;
    private GyroIntegratorLinear mGyroIntegrator;

    public VideoProcessor(int width, int height, int bitRate, GyroIntegratorLinear gyroIntegrator) {
        mWidth = width;
        mHeight = height;
        mBitRate = bitRate;
        mGyroIntegrator = gyroIntegrator;
        mFrameCount = 0;
        mExecutorService = Executors.newFixedThreadPool(1);
        mGyroCounter = 0;
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

    public void addImage(ExtractedImage image) {
        mExecutorService.submit(createRunnable(image));
    }

    private Runnable createRunnable(final ExtractedImage image){

        Runnable aRunnable = new Runnable(){
            public void run(){
                addImageToEnc(image);
            }
        };

        return aRunnable;
    }

    private void addImageToEnc(ExtractedImage img) {

        if (img != null) {

            if (mImageUtils == null) {
                mImageUtils = new ImageUtils(img);
            }

            int inputBufferId = mEncoder.dequeueInputBuffer(TIMEOUT_USEC);

            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferId);
                int size = inputBuffer.remaining();

                long frameTimestamp = img.getFrameTimestamp();

                while (!mGyroIntegrator.isReadyRotation(frameTimestamp)) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                float[] rotationData;
                synchronized (mGyroIntegrator) {
                    rotationData = mGyroIntegrator.getRotationMatrix(frameTimestamp);
                }

                byte[] byteImage = mImageUtils.imageToByteArray(img);

                Image inputImage;
                inputImage = mEncoder.getInputImage(inputBufferId);
                int ts = CodecUtils.warpPerspectiveEuler(byteImage, rotationData, inputImage);
                mEncoder.queueInputBuffer(inputBufferId, 0, size, mFrameCount * 1000000 / FRAME_RATE, 0);
                Log.d(TAG, "frame count: " + mFrameCount);
                mFrameCount++;

            }

            drainEncoder();
        }
    }

    public void drainEncoder() {
        while (true) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferId;
            synchronized (mEncoder) {
                outputBufferId = mEncoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            }
            if (outputBufferId >= 0) {
                ByteBuffer outputBuffer;
                synchronized (mEncoder) {
                    outputBuffer = mEncoder.getOutputBuffer(outputBufferId);
                }
                mMuxer.writeSampleData(mVideoTrack, outputBuffer, bufferInfo);
                synchronized (mEncoder) {
                    mEncoder.releaseOutputBuffer(outputBufferId, false);
                }
            } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mEncoder.getOutputFormat();
                mVideoTrack = mMuxer.addTrack(newFormat);
                mMuxer.start();
            } else {
                break;
            }
        }
    }

    private void releaseA() {
        Log.e(TAG, "Gyro count: " + mGyroCounter);
        Log.e(TAG, "Gyro count: " + mGyroCounter);
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

    public void release() {
//        Log.e(TAG, "Gyro count: " + mFrameCount);

        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "Gyro count: " + mFrameCount);
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
        });
    }
}
