package com.example.android.camera2video;

import android.util.Log;

public class GyroIntegratorLinear {

    private static final String TAG = "GyroIntegratorLinear";
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final int mSize = 5000;

    private float[] mAxeX;
    private float[] mAxeY;
    private float[] mAxeZ;
    private long[] mTimestamps;

    private float mPrevX = 0.0f;
    private float mPrevY = 0.0f;
    private float mPrevZ = 0.0f;
    private long mPrevTimestamp = 0;
    private int mCount = 0;

    public GyroIntegratorLinear() {
        mAxeX = new float[mSize];
        mAxeY = new float[mSize];
        mAxeZ = new float[mSize];

        mTimestamps = new long[mSize];
    }

    public void newData(float x, float y, float z, long timestamp) {
        if (mCount == 0) {
            mAxeX[0] = 0.0f;
            mAxeY[0] = 0.0f;
            mAxeZ[0] = 0.0f;
        } else {

            float averageX = (mPrevX + x) / 2;
            float averageY = (mPrevY + y) / 2;
            float averageZ = (mPrevZ + z) / 2;

            // TODO: тут можно не домножать на константу для оптимизации
            mAxeX[mCount] = mAxeX[mCount - 1] + averageX * (timestamp - mPrevTimestamp) * NS2S;
            mAxeY[mCount] = mAxeY[mCount - 1] + averageY * (timestamp - mPrevTimestamp) * NS2S;
            mAxeZ[mCount] = mAxeZ[mCount - 1] + averageZ * (timestamp - mPrevTimestamp) * NS2S;

            mTimestamps[mCount] = timestamp;
        }

        mPrevX = x;
        mPrevY = y;
        mPrevZ = z;
        mPrevTimestamp = timestamp;
        mCount++;
    }

    public float[] getRotationMatrix(long offset) {
        // TODO: можно ли искать быстрее?
        long timestamp = mTimestamps[mCount - 1] - offset;
        int i = mCount - 1;
        while (mTimestamps[i] > timestamp)
            i--;
        float[] resAngle = {mAxeX[i], mAxeY[i], mAxeZ[i]};
        return resAngle;
    }

    public void release() {
        // TODO: сделать аккуратное использование
    }
}
