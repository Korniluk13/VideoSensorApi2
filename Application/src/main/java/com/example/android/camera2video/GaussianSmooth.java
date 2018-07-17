package com.example.android.camera2video;

import static java.lang.Math.*;

public class GaussianSmooth {

    // TODO: сделать создание один раз
    private static float gauss(double sigma, double x) {
        double expVal = -0.5 * (pow(x, 2) / pow(sigma, 2));
        return (float)exp(expVal);
    }

    private static float[] gaussKernel1d(double sigma, int lw) {
        float[] weights = new float[lw * 2 + 1];
        float sum = 0.0f;

        // count weights
        for (int i = -lw; i <= lw; i++) {
            weights[lw + i] = gauss(sigma, -i);
            sum += weights[lw +i];
        }

        // normalize (sum != 0)
        for (int i = -lw; i <= lw; i++) {
            weights[lw + i] /= sum;
        }

        return  weights;
    }

    private static double[] correlate1d(double[] input, double[] weights) {
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            double sum = 0.0;
            int radius = weights.length / 2;
            for (int j = 0; j < weights.length; j++) {
                if (i - radius + j < 0) {
                    sum += input[-(i - radius + j) - 1] * weights[j];
                } else if (i - radius + j >= input.length) {
                    sum += input[2 * input.length - (i - radius + j) - 1] * weights[j];
                } else {
                    sum += input[i - radius + j] * weights[j];
                }
            }
            output[i] = sum;
        }

        return output;
    }

    private static float correlate1d(float[] input, float[] weights, int index) {
        int i = index;
        float sum = 0.0f;
        int radius = weights.length / 2;
        for (int j = 0; j < weights.length; j++) {
            if (i - radius + j < 0) {
                sum += input[-(i - radius + j) - 1] * weights[j];
            } else if (i - radius + j >= input.length) {
                sum += input[2 * input.length - (i - radius + j) - 1] * weights[j];
            } else {
                sum += input[i - radius + j] * weights[j];
            }
        }
        return sum;
    }

    public static float gaussian_filter1d(float[] array, double sigma, double truncate, int index) {
        int lw = (int)(truncate * sigma + 0.5f);
        return (float)correlate1d(array, gaussKernel1d(sigma, lw), index);
    }
}
