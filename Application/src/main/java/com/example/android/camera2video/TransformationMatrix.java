package com.example.android.camera2video;

//TODO: оптимизировать, чтобы не создавать каждый раз объекты A и T и не выполнять лишних умножений

import java.lang.reflect.Array;

public class TransformationMatrix {

    private int mWidth;
    private int mHeight;
    private int mFocalLength;
    private float[] A2;
    private float[] A1;

    float[] A3 = new float[16];
    float[] A4 = new float[16];
    float[] transformationMatrix = new float[9];

    public TransformationMatrix(int width, int height, int focalLength) {
        mWidth = width;
        mHeight = height;
        mFocalLength = focalLength;

        float[] A = {
                1.0f, 0, 0, 0,
                0, 1.0f, 0, 0,
                -width / 2, -height / 2, 0, 1.0f,
                0, 0, 0, 0
        };

        float[] T = {
                1.0f, 0, 0, 0,
                0, 1.0f, 0, 0,
                0, 0, 1.0f, 0,
                0, 0, focalLength, 1.0f
        };

        //TODO: Погуглить и написать инициализацию в нормальном виде
        A1 = new float[16];
        android.opengl.Matrix.multiplyMM(A1, 0, T, 0, A, 0);

        A2 = new float[16];
        A2[0] = focalLength;
        for (int i = 1; i <= 4; i++)
            A2[i] = 0;
        A2[5] = focalLength;
        A2[6] = 0;
        A2[7] = 0;
        A2[8] = width / 2;
        A2[9] = height / 2;
        A2[10] = 1.0f;
        A2[11] = 0;
        for (int i = 12; i < 16; i++)
            A2[i] = 0;
    }

    public float[] getTransformationMatrix(float[] rotationMatrix) {

        android.opengl.Matrix.multiplyMM(A3, 0, A2, 0, rotationMatrix, 0);

        android.opengl.Matrix.multiplyMM(A4, 0, A3, 0, A1, 0);

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                transformationMatrix[3 * j + i] = A4[4 * i + j];
            }
        }

        return transformationMatrix;
    }
}