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

    private static final String TAG = "VideoEncoder";

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
    private CircularArray<ExtractedImage> mImageArray;
    private ImageUtils mImageUtils = null;
    private ExecutorService mExecutorService;


    public VideoProcessor(int width, int height, int bitRate, CircularArray<ExtractedImage> imageArray) {
        mWidth = width;
        mHeight = height;
        mBitRate = bitRate;
        mFrameCount = 0;
        mImageArray = imageArray;
        mExecutorService = Executors.newFixedThreadPool(1);
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
//        ExtractedImage img = null;
//        synchronized (mImageArray) {
//            img = mImageArray.popLast();
////                    Log.e(TAG, "Buffer length: "+ mImageArray.size());
//        }

//                Log.e(TAG, "Buffer length: "+ img);


        if (img != null) {

            if (mImageUtils == null) {
                mImageUtils = new ImageUtils(img);
            }

            int inputBufferId = mEncoder.dequeueInputBuffer(TIMEOUT_USEC);

            if (inputBufferId >= 0) {
                ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferId);
                int size = inputBuffer.remaining();

                byte[] byteImage = mImageUtils.imageToByteArray(img);

                Image inputImage;
//                        synchronized (mEncoder) {
                inputImage = mEncoder.getInputImage(inputBufferId);
//                        }
                CodecUtils.bytesToImage(byteImage, inputImage);
//                        Log.e(TAG, "fcnt " + mFrameCount);
//                Log.e(TAG, "warp_persp " + ts);

//                        synchronized (mEncoder) {
                mEncoder.queueInputBuffer(inputBufferId, 0, size, mFrameCount * 1000000 / FRAME_RATE, 0);
                mFrameCount++;
//                        }
            }
            drainEncoder();
//                    Log.e(TAG, "Done.");

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

    public void release() {
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
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
        });
    }
}
