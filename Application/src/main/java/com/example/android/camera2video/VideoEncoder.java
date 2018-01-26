package com.example.android.camera2video;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoEncoder implements Runnable {

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

    public VideoEncoder(int width, int height, int bitRate) {
        mWidth = width;
        mHeight = height;
        mBitRate = bitRate;
        mFrameCount = 0;
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

    public void addImage(Image image) {
        int inputBufferId = mEncoder.dequeueInputBuffer(TIMEOUT_USEC);

        if (inputBufferId >= 0) {
            ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferId);
            int size = inputBuffer.remaining();

            Image inputImage = mEncoder.getInputImage(inputBufferId);
            CodecUtils.copyFlexYUVImage(inputImage, image);

            mEncoder.queueInputBuffer(inputBufferId, 0, size, mFrameCount * 1000000 / FRAME_RATE, 0);
            mFrameCount++;
        }
        drainEncoder();
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

    @Override
    public void run() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}