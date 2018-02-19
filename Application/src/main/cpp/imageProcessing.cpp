#include <jni.h>
#include <ctime>
#include <chrono>
#include <opencv2/opencv.hpp>
#include "utils.h"

using namespace cv;

JNIEXPORT void JNICALL
Java_com_example_android_camera2video_CodecUtils_copyFlexYUVImage(
        JNIEnv *env, jclass type, jobject target, jobject source) {

    NativeImage *tgt = getNativeImage(env, target);
    NativeImage *src = getNativeImage(env, source);

    for (size_t ix = 0; ix < tgt->numPlanes; ++ix) {

        uint8_t *row = const_cast<uint8_t *>(tgt->plane[ix].buffer) + tgt->plane[ix].cropOffs;

        for (size_t y = 0; y < tgt->plane[ix].cropHeight; ++y) {

            uint8_t *col = row;
            ssize_t colInc = tgt->plane[ix].colInc;

            const uint8_t *srcRow = (src->plane[ix].buffer + src->plane[ix].cropOffs
                                     + src->plane[ix].rowInc * (y % src->plane[ix].cropHeight));

            for (size_t x = 0; x < tgt->plane[ix].cropWidth; ++x) {
                *col = srcRow[src->plane[ix].colInc * (x % src->plane[ix].cropWidth)];
                col += colInc;
            }
            row += tgt->plane[ix].rowInc;
        }
    }
}

JNIEXPORT void JNICALL
Java_com_example_android_camera2video_CodecUtils_matToImage(JNIEnv *env, jclass type, jlong addr,
jobject dst) {

    uint8_t *srcBuffer = (uint8_t *) addr;

    int counter = 0;

    NativeImage *tgt = getNativeImage(env, dst);

    for (size_t ix = 0; ix < tgt->numPlanes; ++ix) {

        uint8_t *row = const_cast<uint8_t *>(tgt->plane[ix].buffer) + tgt->plane[ix].cropOffs;

        for (size_t y = 0; y < tgt->plane[ix].cropHeight; ++y) {

            uint8_t *col = row;
            ssize_t colInc = tgt->plane[ix].colInc;

            for (size_t x = 0; x < tgt->plane[ix].cropWidth; ++x) {
                *col = srcBuffer[counter];
                counter++;
                col += colInc;
            }
            row += tgt->plane[ix].rowInc;
        }

    }

}

extern "C" JNIEXPORT jint JNICALL
        Java_com_example_android_camera2video_CodecUtils_warpPerspective(JNIEnv *env, jclass type, jbyteArray imageBytes,
        jfloatArray  rotation, jobject dstRes) {
    NativeImage *tgt = getNativeImage(env, dstRes);

    auto data = static_cast<uint8_t *> (env->GetPrimitiveArrayCritical(imageBytes, nullptr));
    auto rot = static_cast<float32_t *> (env->GetPrimitiveArrayCritical(rotation, nullptr));

    int mHeight = 720;
    int mWidth = 960;
    Mat yuv(mHeight * 3 / 2, mWidth, CV_8UC1, data);
    Mat matRot(3, 3, CV_32F, rot);
    Mat rgb;
    cv::cvtColor(yuv, rgb, COLOR_YUV2RGB_I420);
    Mat res;
    cv::warpPerspective(rgb, res, matRot, rgb.size());
    cv::cvtColor(res, yuv, COLOR_RGB2YUV_I420);

    uint8_t *srcBuffer = yuv.data;

    int counter = 0;

    for (size_t ix = 0; ix < tgt->numPlanes; ++ix) {
        uint8_t *row = const_cast<uint8_t *>(tgt->plane[ix].buffer) + tgt->plane[ix].cropOffs;

        for (size_t y = 0; y < tgt->plane[ix].cropHeight; ++y) {
            uint8_t *col = row;
            ssize_t colInc = tgt->plane[ix].colInc;

            for (size_t x = 0; x < tgt->plane[ix].cropWidth; ++x) {
                *col = srcBuffer[counter];
                counter++;
                col += colInc;
            }
            row += tgt->plane[ix].rowInc;
        }
    }

    env->ReleasePrimitiveArrayCritical(imageBytes, data, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(rotation, rot, JNI_ABORT);

    return 0;
}
//
//double getMs(std::chrono::time_point<std::chrono::steady_clock, std::chrono::duration> &t1, std::chrono::time_point<std::chrono::steady_clock, std::chrono::duration> &t2) {
//    auto diff = t2 - t1;
//    return std::chrono::duration<double, std::milli>(diff).count();
//}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_example_android_camera2video_CodecUtils_perspectiveTransform(JNIEnv *env, jclass typeX, jbyteArray imageBytes,
                                                                 jfloatArray  rotation, jobject dstRes) {
    NativeImage *tgt = getNativeImage(env, dstRes);

    auto startTime = std::chrono::steady_clock::now();

    auto data = static_cast<uint8_t *> (env->GetPrimitiveArrayCritical(imageBytes, nullptr));
    auto rot = static_cast<float32_t *> (env->GetPrimitiveArrayCritical(rotation, nullptr));

    int mHeight = 720;
    int mWidth = 960;
    Mat yuv(mHeight * 3 / 2, mWidth, CV_8UC1, data);
    Mat matRot(3, 3, CV_32F, rot);
    Mat rgb;
    cv::cvtColor(yuv, rgb, COLOR_YUV2RGB_I420);

    int type = rgb.type();
    Size mSize = rgb.size();

    Scalar black(0, 0, 0);
//    Mat dst(mSize, type, black); operator =

    auto timestamp1 = std::chrono::steady_clock::now();

    Mat dst(mSize, type);

    static struct Init {
        Init(int mWidth, int mHeight) {
            mBuff = new float32_t[2 * mWidth * mHeight];
            for (int i = 0; i < mHeight; i++) {
                for (int j = 0; j < mWidth; j++) {
                    mBuff[2 * (i * mWidth + j)] = (float32_t)i;
                    mBuff[2 * (i * mWidth + j) + 1] = (float32_t)j;
                }
            }
        }

        float32_t * mBuff;
    } sBuff(mWidth, mHeight);

    Mat src1(mHeight * mWidth, 1, CV_32FC2, sBuff.mBuff);

    Mat dst1(mHeight * mWidth, 1, CV_32FC2);


    perspectiveTransform(src1, dst1, matRot);
    auto timestamp2 = std::chrono::steady_clock::now();

    Mat m[2];
    split(dst1, m);

    auto timestamp3 = std::chrono::steady_clock::now();

//    auto timestamp4 = std::chrono::steady_clock::now();


//    Mat mx(mSize, CV_32F);
//    Mat my(mSize, CV_32F);
    Mat mx = m[1].reshape(1, mHeight);
    Mat my = m[0].reshape(1, mHeight);

//    m0.convertTo(mx, CV_32F);
//    m1.convertTo(my, CV_32F);


    auto timestamp5 = std::chrono::steady_clock::now();

    remap(rgb, dst, mx, my, INTER_LINEAR);

    auto timestamp6 = std::chrono::steady_clock::now();

    cv::cvtColor(dst, yuv, COLOR_RGB2YUV_I420);

    uint8_t *srcBuffer = yuv.data;

    int counter = 0;

    for (size_t ix = 0; ix < tgt->numPlanes; ++ix) {
        uint8_t *row = const_cast<uint8_t *>(tgt->plane[ix].buffer) + tgt->plane[ix].cropOffs;

        for (size_t y = 0; y < tgt->plane[ix].cropHeight; ++y) {
            uint8_t *col = row;
            ssize_t colInc = tgt->plane[ix].colInc;

            for (size_t x = 0; x < tgt->plane[ix].cropWidth; ++x) {
                *col = srcBuffer[counter];
                counter++;
                col += colInc;
            }
            row += tgt->plane[ix].rowInc;
        }
    }

    env->ReleasePrimitiveArrayCritical(imageBytes, data, JNI_ABORT);
    env->ReleasePrimitiveArrayCritical(rotation, rot, JNI_ABORT);

    int size = 5;
    jint timestamps[size];
    auto diff = timestamp1 - startTime;
    timestamps[0] = (int)std::chrono::duration<double, std::milli>(diff).count();
    diff = timestamp2 - timestamp1;
    timestamps[1] = (int)std::chrono::duration<double, std::milli>(diff).count();
    diff = timestamp3 - timestamp2;
    timestamps[2] = (int)std::chrono::duration<double, std::milli>(diff).count();
    diff = timestamp5 - timestamp3;
    timestamps[3] = (int)std::chrono::duration<double, std::milli>(diff).count();
    diff = timestamp6 - timestamp5;
    timestamps[4] = (int)std::chrono::duration<double, std::milli>(diff).count();


//
//    timestamps[0] = getMs(startTime, timestamp1);
//    timestamps[1] = getMs(timestamp1, timestamp2);
//    timestamps[2] = getMs(timestamp2, timestamp3);
//    timestamps[3] = getMs(timestamp3, timestamp4);
//    timestamps[4] = getMs(timestamp4, timestamp5);
//    timestamps[5] = getMs(timestamp5, timestamp6);

    jintArray result;
    result = env->NewIntArray(size);
    env->SetIntArrayRegion(result, 0, size, timestamps);
    return result;
}

