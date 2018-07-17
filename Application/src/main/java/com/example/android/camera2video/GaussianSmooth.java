package com.example.android.camera2video;

import static java.lang.Math.*;

public class GaussianSmooth {

    private double mTruncate = 4.0;
    private double mSigma;
    private int mRadius;
    private float[] mWeights;

    GaussianSmooth(double sigma) {
        mSigma = sigma;
        mRadius = (int)(mTruncate * mSigma + 0.5f);
        mWeights = gaussKernel1d();
    }

    private float gauss(double x) {
        double expVal = -0.5 * (pow(x, 2) / pow(mSigma, 2));
        return (float)exp(expVal);
    }

    private float[] gaussKernel1d() {
        float[] weights = new float[mRadius * 2 + 1];
        float sum = 0.0f;

        // count weights
        for (int i = -mRadius; i <= mRadius; i++) {
            weights[mRadius + i] = gauss(-i);
            sum += weights[mRadius +i];
        }

        // normalize (sum != 0)
        for (int i = -mRadius; i <= mRadius; i++) {
            weights[mRadius + i] /= sum;
        }

        return weights;
    }

    private double[] correlate1d(double[] input, double[] weights) {
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            double sum = 0.0;
            for (int j = 0; j < weights.length; j++) {
                if (i - mRadius + j < 0) {
                    sum += input[-(i - mRadius + j) - 1] * weights[j];
                } else if (i - mRadius + j >= input.length) {
                    sum += input[2 * input.length - (i - mRadius + j) - 1] * weights[j];
                } else {
                    sum += input[i - mRadius + j] * weights[j];
                }
            }
            output[i] = sum;
        }

        return output;
    }

    private float correlate1d(float[] input, float[] weights, int index) {
        int i = index;
        float sum = 0.0f;
        for (int j = 0; j < weights.length; j++) {
            if (i - mRadius + j < 0) {
                sum += input[-(i - mRadius + j) - 1] * weights[j];
            } else if (i - mRadius + j >= input.length) {
                sum += input[2 * input.length - (i - mRadius + j) - 1] * weights[j];
            } else {
                sum += input[i - mRadius + j] * weights[j];
            }
        }
        return sum;
    }

    public float gaussian_filter1d(float[] array, int index) {
        return (float)correlate1d(array, mWeights, index);
    }
}
