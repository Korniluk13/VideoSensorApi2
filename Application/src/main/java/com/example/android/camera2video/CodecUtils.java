package com.example.android.camera2video;

import android.media.Image;

import java.nio.ByteBuffer;

public class CodecUtils  {
    private static final String TAG = "CodecUtils";

    private static class ImageWrapper extends ImageJNI {
        private final Image mImage;
        private final Plane[] mPlanes;

        private ImageWrapper(Image image) {
            mImage = image;
            Image.Plane[] planes = mImage.getPlanes();

            mPlanes = new Plane[planes.length];
            for (int i = 0; i < planes.length; i++) {
                mPlanes[i] = new PlaneWrapper(planes[i]);
            }
        }

        public static ImageWrapper createFromImage(Image image) {
            return new ImageWrapper(image);
        }

        @Override
        public int getFormat() {
            return mImage.getFormat();
        }

        @Override
        public int getWidth() {
            return mImage.getWidth();
        }

        @Override
        public int getHeight() {
            return mImage.getHeight();
        }

        @Override
        public long getTimestamp() {
            return mImage.getTimestamp();
        }

        @Override
        public Plane[] getPlanes() {
            return mPlanes;
        }

        @Override
        public void close() {
            mImage.close();
        }

        private static class PlaneWrapper extends ImageJNI.Plane {
            private final Image.Plane mPlane;

            PlaneWrapper(Image.Plane plane) {
                mPlane = plane;
            }

            @Override
            public int getRowStride() {
                return mPlane.getRowStride();
            }

            @Override
            public int getPixelStride() {
                return mPlane.getPixelStride();
            }

            @Override
            public ByteBuffer getBuffer() {
                return mPlane.getBuffer();
            }
        }
    }

    public native static void copyFlexYUVImage(ImageJNI target, ImageJNI source);

    public native static void matToImage(long addr, ImageJNI dst);

    public native static int warpPerspective(byte[] byteImage, float[] rotation, ImageJNI dst);

    public native static void bytesToImage(byte[] byteImage, ImageJNI dst);

    public native static int perspectiveTransform(byte[] byteImage, float[] rotation, ImageJNI dst);

    public static void copyMatToImage(long addr, Image dst)
    {
        matToImage(addr, ImageWrapper.createFromImage(dst));
    }

    public static int warpPerspective(byte[] byteImage, float[] rotation, Image dst) {
        return warpPerspective(byteImage, rotation, ImageWrapper.createFromImage(dst));
    }

    public static int perspectiveTransform(byte[] byteImage, float[] rotation, Image dst) {
        return perspectiveTransform(byteImage, rotation, ImageWrapper.createFromImage(dst));
    }

    public static void copyFlexYUVImage(Image target, Image source) {
        copyFlexYUVImage(
                ImageWrapper.createFromImage(target),
                ImageWrapper.createFromImage(source));
    }

    public static void bytesToImage(byte[] byteImage, Image dst) {
        bytesToImage(byteImage, ImageWrapper.createFromImage(dst));
    }

}