package com.example.android.camera2video;

import static java.lang.Math.*;

public class GaussianSmooth {

    private static double gauss(double sigma, double x) {
        double expVal = -0.5 * (pow(x, 2) / pow(sigma, 2));
        return exp(expVal);
    }

    private static double[] gaussKernel1d(double sigma, int lw) {
        double[] weights = new double[lw * 2 + 1];
        double sum = 0.0;

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

    public static double[] gaussian_filter1d(double[] array, float sigma, float truncate) {
        int lw = (int)(truncate * sigma + 0.5f);
        return correlate1d(array, gaussKernel1d(sigma, lw));
    }
}
