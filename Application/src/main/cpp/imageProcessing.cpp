#include <jni.h>
#include <arm_neon.h>
#include "utils.h"
#include "imageTransformer.h"

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

    uint8_t *srcBuffer = warpPerspective(data, rot);

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

extern "C" JNIEXPORT jint JNICALL
Java_com_example_android_camera2video_CodecUtils_perspectiveTransform(JNIEnv *env, jclass typeX, jbyteArray imageBytes,
                                                                 jfloatArray  rotation, jobject dstRes) {
    NativeImage *tgt = getNativeImage(env, dstRes);

    auto data = static_cast<uint8_t *> (env->GetPrimitiveArrayCritical(imageBytes, nullptr));
    auto rot = static_cast<float32_t *> (env->GetPrimitiveArrayCritical(rotation, nullptr));

    uint8_t *srcBuffer = perspectiveTransform(data, rot);

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

extern "C" JNIEXPORT void JNICALL
Java_com_example_android_camera2video_CodecUtils_bytesToImage(JNIEnv *env, jclass typeX, jbyteArray imageBytes, jobject dstRes) {
    NativeImage *tgt = getNativeImage(env, dstRes);

    auto data = static_cast<uint8_t *> (env->GetPrimitiveArrayCritical(imageBytes, nullptr));

    uint8_t *srcBuffer = data;

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
//    env->ReleasePrimitiveArrayCritical(rotation, rot, JNI_ABORT);

//    return 0;
}
