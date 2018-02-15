package com.example.android.camera2video;

import android.hardware.SensorManager;
import android.util.Log;

public class GyroIntegrator {

    private static final String TAG = "GyroIntegration";
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];

    private long mLastTimestamp = -1;
    private float EPSILON = 0.000000001f;
    float[] deltaMatrix = new float[16];
    float[] result = new float[16];

    private float[] mRotationMatrix = {
            1.0f, 0, 0, 0,
            0, 1.0f, 0, 0,
            0, 0, 1.0f, 0,
            0, 0, 0, 1.0f
    };

    public GyroIntegrator() {

        Log.d(TAG, "gyro integrator inizialization");
    }

    public void newData(float x, float y, float z, long timestamp) {
        if (mLastTimestamp <= 0) {
            mLastTimestamp = timestamp;
            return;
        }

        float dt = (timestamp - mLastTimestamp) * NS2S;
        float omegaMagnitude = (float) Math.sqrt(x * x + y * y + z * z);

        if (omegaMagnitude > EPSILON) {
            x /= omegaMagnitude;
            y /= omegaMagnitude;
            z /= omegaMagnitude;
        }

        double thetaOverTwo = omegaMagnitude * dt / 2.0f;
        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
        deltaRotationVector[0] = sinThetaOverTwo * x;
        deltaRotationVector[1] = sinThetaOverTwo * y;
        deltaRotationVector[2] = sinThetaOverTwo * z;
        deltaRotationVector[3] = cosThetaOverTwo;

        SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaRotationVector);

        android.opengl.Matrix.multiplyMM(result, 0, mRotationMatrix, 0, deltaMatrix, 0);
        System.arraycopy(result, 0, mRotationMatrix, 0, 16);

        mLastTimestamp = timestamp;
    }

    public float[] getRotationMatrix() {
        float[] res = new float[16];
        System.arraycopy(mRotationMatrix, 0, res, 0, 16);
        return res;
    }

    public void release() {
        mLastTimestamp = -1;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (i == j) {
                    mRotationMatrix[4 * i + j] = 1.0f;
                } else {
                    mRotationMatrix[4 * i + j] = 0;
                }
            }
        }
    }
}