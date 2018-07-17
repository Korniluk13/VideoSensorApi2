package com.example.android.camera2video;


public class GyroIntegratorLinear {

    private static final String TAG = "GyroIntegratorLinear";
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final int mSize = 5000;
    private static final double mSigma = 70;
    private static final double mTruncate = 4.0;

    private float[] mAxeX;
    private float[] mAxeY;
    private float[] mAxeZ;
    private long[] mTimestamps;
    private float[] mSmoothedAxeX;
    private float[] mSmoothedAxeY;
    private float[] mSmoothedAxeZ;
    private int mFilterRadius;

    private float mPrevX = 0.0f;
    private float mPrevY = 0.0f;
    private float mPrevZ = 0.0f;
    private long mPrevTimestamp = 0;
    private int mCount = 0;
    private int mSmoothedCount = 0;

    public GyroIntegratorLinear() {
        mAxeX = new float[mSize];
        mAxeY = new float[mSize];
        mAxeZ = new float[mSize];

        mSmoothedAxeX = new float[mSize];
        mSmoothedAxeY = new float[mSize];
        mSmoothedAxeZ = new float[mSize];

        mTimestamps = new long[mSize];

        mFilterRadius = (int)(mSigma * mTruncate + 0.5f);
    }

    public void newData(float x, float y, float z, long timestamp) {
        if (mCount == 0) {
            mAxeX[0] = 0.0f;
            mAxeY[0] = 0.0f;
            mAxeZ[0] = 0.0f;

            mTimestamps[mCount] = timestamp;
        } else {

            float averageX = (mPrevX + x) / 2;
            float averageY = (mPrevY + y) / 2;
            float averageZ = (mPrevZ + z) / 2;

            // TODO: тут можно не домножать на константу для оптимизации
            mAxeX[mCount] = mAxeX[mCount - 1] + averageX * (timestamp - mPrevTimestamp) * NS2S;
            mAxeY[mCount] = mAxeY[mCount - 1] + averageY * (timestamp - mPrevTimestamp) * NS2S;
            mAxeZ[mCount] = mAxeZ[mCount - 1] + averageZ * (timestamp - mPrevTimestamp) * NS2S;

            mTimestamps[mCount] = timestamp;

            if (mCount + 1 > mFilterRadius) {
                mSmoothedAxeX[mSmoothedCount] = GaussianSmooth.gaussian_filter1d(mAxeX, mSigma, mTruncate, mSmoothedCount);
                mSmoothedAxeY[mSmoothedCount] = GaussianSmooth.gaussian_filter1d(mAxeY, mSigma, mTruncate, mSmoothedCount);
                mSmoothedAxeZ[mSmoothedCount] = GaussianSmooth.gaussian_filter1d(mAxeZ, mSigma, mTruncate, mSmoothedCount);

                mSmoothedCount++;
            }
        }

        mPrevX = x;
        mPrevY = y;
        mPrevZ = z;
        mPrevTimestamp = timestamp;
        mCount++;
    }

    public boolean isReadyRotation(long frameTimestamp) {
        return (frameTimestamp < mTimestamps[mSmoothedCount]);
    }

    public float[] getRotationMatrix(long frameTimestamp) {
        // TODO: можно ли искать быстрее?
        int i = mCount - 1;
        while (mTimestamps[i] > frameTimestamp)
            i--;
        float[] resAngle = {mAxeX[i] - mSmoothedAxeX[i], mAxeY[i] - mSmoothedAxeY[i], mAxeZ[i] - mSmoothedAxeZ[i]};
        return resAngle;
    }

    public void release() {
        // TODO: сделать аккуратное использование
    }
}
