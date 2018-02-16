#include <stdint.h>
#include <sys/types.h>
#include <jni.h>
#include <string>
#include "utils.h"

static struct ImageFieldsAndMethods {
    // android.graphics.ImageFormat
    int YUV_420_888;
    // android.media.Image
    jmethodID methodWidth;
    jmethodID methodHeight;
    jmethodID methodFormat;
    jmethodID methodTimestamp;
    jmethodID methodPlanes;
    jmethodID methodCrop;
    // android.media.Image.Plane
    jmethodID methodBuffer;
    jmethodID methodPixelStride;
    jmethodID methodRowStride;
    // android.graphics.Rect
    jfieldID fieldLeft;
    jfieldID fieldTop;
    jfieldID fieldRight;
    jfieldID fieldBottom;
} gFields;

static bool gFieldsInitialized = false;

void initializeGlobalFields(JNIEnv *env) {
    if (gFieldsInitialized) {
        return;
    }
    {   // ImageFormat
        jclass imageFormatClazz = env->FindClass("android/graphics/ImageFormat");
        const jfieldID fieldYUV420888 = env->GetStaticFieldID(imageFormatClazz, "YUV_420_888", "I");
        gFields.YUV_420_888 = env->GetStaticIntField(imageFormatClazz, fieldYUV420888);
        env->DeleteLocalRef(imageFormatClazz);
        imageFormatClazz = NULL;
    }
    {   // Image
        jclass imageClazz = env->FindClass("com/example/android/camera2video/ImageJNI");
        gFields.methodWidth  = env->GetMethodID(imageClazz, "getWidth", "()I");
        gFields.methodHeight = env->GetMethodID(imageClazz, "getHeight", "()I");
        gFields.methodFormat = env->GetMethodID(imageClazz, "getFormat", "()I");
        gFields.methodTimestamp = env->GetMethodID(imageClazz, "getTimestamp", "()J");
        gFields.methodPlanes = env->GetMethodID(
                imageClazz, "getPlanes", "()[Lcom/example/android/camera2video/ImageJNI$Plane;");
        gFields.methodCrop   = env->GetMethodID(
                imageClazz, "getCropRect", "()Landroid/graphics/Rect;");
        env->DeleteLocalRef(imageClazz);
        imageClazz = NULL;
    }
    {   // Image.Plane
        jclass planeClazz = env->FindClass("com/example/android/camera2video/ImageJNI$Plane");
        gFields.methodBuffer = env->GetMethodID(planeClazz, "getBuffer", "()Ljava/nio/ByteBuffer;");
        gFields.methodPixelStride = env->GetMethodID(planeClazz, "getPixelStride", "()I");
        gFields.methodRowStride = env->GetMethodID(planeClazz, "getRowStride", "()I");
        env->DeleteLocalRef(planeClazz);
        planeClazz = NULL;
    }
    {   // Rect
        jclass rectClazz = env->FindClass("android/graphics/Rect");
        gFields.fieldLeft   = env->GetFieldID(rectClazz, "left", "I");
        gFields.fieldTop    = env->GetFieldID(rectClazz, "top", "I");
        gFields.fieldRight  = env->GetFieldID(rectClazz, "right", "I");
        gFields.fieldBottom = env->GetFieldID(rectClazz, "bottom", "I");
        env->DeleteLocalRef(rectClazz);
        rectClazz = NULL;
    }
    gFieldsInitialized = true;
}


NativeImage *getNativeImage(JNIEnv *env, jobject image, jobject area) {
    if (image == NULL) {
        return NULL;
    }

    initializeGlobalFields(env);

    NativeImage *img = new NativeImage;
    img->format = env->CallIntMethod(image, gFields.methodFormat);
    img->width  = env->CallIntMethod(image, gFields.methodWidth);
    img->height = env->CallIntMethod(image, gFields.methodHeight);
    img->timestamp = env->CallLongMethod(image, gFields.methodTimestamp);

    jobject cropRect = NULL;
    if (area == NULL) {
        cropRect = env->CallObjectMethod(image, gFields.methodCrop);
        area = cropRect;
    }

    img->crop.left   = env->GetIntField(area, gFields.fieldLeft);
    img->crop.top    = env->GetIntField(area, gFields.fieldTop);
    img->crop.right  = env->GetIntField(area, gFields.fieldRight);
    img->crop.bottom = env->GetIntField(area, gFields.fieldBottom);

    if (img->crop.right == 0 && img->crop.bottom == 0) {
        img->crop.right  = img->width;
        img->crop.bottom = img->height;
    }
    if (cropRect != NULL) {
        env->DeleteLocalRef(cropRect);
        cropRect = NULL;
    }

    if (img->format != gFields.YUV_420_888) {
        delete img;
        img = NULL;
        return NULL;
    }

    img->numPlanes = 3;

    ScopedLocalRef<jobjectArray> planesArray(
            env, (jobjectArray)env->CallObjectMethod(image, gFields.methodPlanes));

    int xDecim = 0;
    int yDecim = 0;

    // итерируемся по плоскастям картинки
    for (size_t ix = 0; ix < img->numPlanes; ++ix) {

        // достаем плоскость под номером ix
        ScopedLocalRef<jobject> plane(
                env, env->GetObjectArrayElement(planesArray.get(), (jsize) ix));

        img->plane[ix].colInc = env->CallIntMethod(plane.get(), gFields.methodPixelStride);
        img->plane[ix].rowInc = env->CallIntMethod(plane.get(), gFields.methodRowStride);

        ScopedLocalRef<jobject> buffer(
                env, env->CallObjectMethod(plane.get(), gFields.methodBuffer));
        // указатель на начало буфера плоскости
        img->plane[ix].buffer = (const uint8_t *)env->GetDirectBufferAddress(buffer.get());
        // узнаем его размер
        img->plane[ix].size = env->GetDirectBufferCapacity(buffer.get());

        // опытнам путем выяснил, что оно равняется нулю, на ipnutImage точно
        img->plane[ix].cropOffs =
                (img->crop.left >> xDecim) * img->plane[ix].colInc
                + (img->crop.top >> yDecim) * img->plane[ix].rowInc;

        // рофл: кадры всегда берутя в landscape
        // для первой плоскости просто высота, для двух других половина +- 1
        img->plane[ix].cropHeight =
                ((img->crop.bottom + (1 << yDecim) - 1) >> yDecim) - (img->crop.top >> yDecim);
        img->plane[ix].cropWidth =
                ((img->crop.right + (1 << xDecim) - 1) >> xDecim) - (img->crop.left >> xDecim);

        // sanity check on increments, дейсвтитльно sanity, но походу юзлесс или я даун
        ssize_t widthOffs =
                (((img->width + (1 << xDecim) - 1) >> xDecim) - 1) * img->plane[ix].colInc;
        ssize_t heightOffs =
                (((img->height + (1 << yDecim) - 1) >> yDecim) - 1) * img->plane[ix].rowInc;

        if (widthOffs < 0 || heightOffs < 0
            || widthOffs + heightOffs >= (ssize_t)img->plane[ix].size) {
            /*       jniThrowException(
                           env, "java/lang/IndexOutOfBoundsException", "plane exceeds bytearray");*/
            delete img;
            img = NULL;
            return NULL;
        }

        xDecim = yDecim = 1;
    }
    return img;
}
