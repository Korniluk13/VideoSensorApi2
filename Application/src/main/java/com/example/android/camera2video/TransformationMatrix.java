package com.example.android.camera2video;

//TODO: оптимизировать, чтобы не создавать каждый раз объекты A и T и не выполнять лишних умножений

public class TransformationMatrix {

    public static float[] getTransformationMatrix(float[] rotationMatrix, int width, int height, int focalLength) {

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

        float[] A1 = new float[16];
        android.opengl.Matrix.multiplyMM(A1, 0, T, 0, A, 0);

        float[] A2 = {
                focalLength, 0, 0, 0,
                0, focalLength, 0, 0,
                width / 2, height / 2, 1.0f, 0,
                0, 0, 0, 0
        };

        float[] A3 = new float[16];
        android.opengl.Matrix.multiplyMM(A3, 0, A2, 0, rotationMatrix, 0);

        float[] A4 = new float[16];
        android.opengl.Matrix.multiplyMM(A4, 0, A3, 0, A1, 0);

        float[] transformationMatrix = new float[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                transformationMatrix[3 * j + i] = A4[4 * i + j];
            }
        }

        return transformationMatrix;
    }
}