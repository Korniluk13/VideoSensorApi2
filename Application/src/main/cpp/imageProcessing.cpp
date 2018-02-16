#include <jni.h>
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
        Java_com_example_android_camera2video_CodecUtils_transformImage(JNIEnv *env, jclass type, jbyteArray imageBytes,
        jfloatArray  rotation, jobject dst) {
    NativeImage *tgt = getNativeImage(env, dst);

    auto data = static_cast<uint8_t *> (env->GetPrimitiveArrayCritical(imageBytes, nullptr));
    auto rot = static_cast<float32_t *> (env->GetPrimitiveArrayCritical(rotation, nullptr));

    Mat yuv(1080, 960, CV_8UC1, data);
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
