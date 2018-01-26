#include "utils.h"
#include <stdint.h>
#include <sys/types.h>
#include <jni.h>
#include <string>


typedef ssize_t offs_t;
struct NativeImage {
    struct crop {
        int left;
        int top;
        int right;
        int bottom;
    } crop;

    struct plane {
        const uint8_t *buffer;
        size_t size;
        ssize_t colInc;
        ssize_t rowInc;
        offs_t cropOffs;
        size_t cropWidth;
        size_t cropHeight;
    } plane[3];

    int width;
    int height;
    int format;
    long timestamp;
    size_t numPlanes;
};


struct ChecksumAlg {
    virtual void init() = 0;
    virtual void update(uint8_t c) = 0;
    virtual uint32_t checksum() = 0;
    virtual size_t length() = 0;
protected:
    virtual ~ChecksumAlg() {}
};


struct Adler32 : ChecksumAlg {
    Adler32() {
        init();
    }
    void init() {
        a = 1;
        len = b = 0;
    }
    void update(uint8_t c) {
        a += c;
        b += a;
        ++len;
    }
    uint32_t checksum() {
        return (a % 65521) + ((b % 65521) << 16);
    }
    size_t length() {
        return len;
    }
private:
    uint32_t a, b;
    size_t len;
};


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
        jclass imageClazz = env->FindClass("com/example/android/camera2video/CodecImage");
        gFields.methodWidth  = env->GetMethodID(imageClazz, "getWidth", "()I");
        gFields.methodHeight = env->GetMethodID(imageClazz, "getHeight", "()I");
        gFields.methodFormat = env->GetMethodID(imageClazz, "getFormat", "()I");
        gFields.methodTimestamp = env->GetMethodID(imageClazz, "getTimestamp", "()J");
        gFields.methodPlanes = env->GetMethodID(
                imageClazz, "getPlanes", "()[Lcom/example/android/camera2video/CodecImage$Plane;");
        gFields.methodCrop   = env->GetMethodID(
                imageClazz, "getCropRect", "()Landroid/graphics/Rect;");
        env->DeleteLocalRef(imageClazz);
        imageClazz = NULL;
    }
    {   // Image.Plane
        jclass planeClazz = env->FindClass("com/example/android/camera2video/CodecImage$Plane");
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


NativeImage *getNativeImage(JNIEnv *env, jobject image, jobject area = NULL) {
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

extern "C"
{
JNIEXPORT void JNICALL
Java_com_example_android_camera2video_CodecUtils_copyFlexYUVImage(
        JNIEnv *env, jclass type, jobject target, jobject source) {

    NativeImage *tgt = getNativeImage(env, target);
    NativeImage *src = getNativeImage(env, source);

    for (size_t ix = 0; ix < tgt->numPlanes; ++ix) {

        // указатель на буфер картинки, в которую копируем. Используется для обхода строк
        uint8_t *row = const_cast<uint8_t *>(tgt->plane[ix].buffer) + tgt->plane[ix].cropOffs;


        //       cropHeight - граница валидных пикселей сверху

        for (size_t y = 0; y < tgt->plane[ix].cropHeight; ++y) {

            // еще один указатель на начало строки, спользуется для обхода пикселей
            uint8_t *col = row;

            // colInc - pixelStride
            ssize_t colInc = tgt->plane[ix].colInc;

            // указатель на строчку в исходной картинки
            const uint8_t *srcRow = (src->plane[ix].buffer + src->plane[ix].cropOffs
                                     + src->plane[ix].rowInc * (y % src->plane[ix].cropHeight));


            for (size_t x = 0; x < tgt->plane[ix].cropWidth; ++x) {
                *col = srcRow[src->plane[ix].colInc * (x % src->plane[ix].cropWidth)];

                // colInc - pixelStride
                col += colInc;
            }
            row += tgt->plane[ix].rowInc;
        }
    }
}

JNIEXPORT void JNICALL
Java_com_example_android_camera2video_CodecUtils_matToImage(JNIEnv *env, jclass type, jlong addr,
                                                     jobject dst) {


    // указатель на данные исходной картинки
    uint8_t *srcBuffer = (uint8_t *) addr;

    int counter = 0;

    NativeImage *tgt = getNativeImage(env, dst);

    for (size_t ix = 0; ix < tgt->numPlanes; ++ix) {
        uint8_t *row = const_cast<uint8_t *>(tgt->plane[ix].buffer) + tgt->plane[ix].cropOffs;


        /*
         * cropHeight - граница валидных пикселей сверху
         * */
        for (size_t y = 0; y < tgt->plane[ix].cropHeight; ++y) {

            // еще один указатель на начало строки, спользуется для обхода пикселей
            uint8_t *col = row;

            ssize_t colInc = tgt->plane[ix].colInc;

            for (size_t x = 0; x < tgt->plane[ix].cropWidth; ++x) {
                *col = srcBuffer[counter];
                counter++;
                // colInc == pixelStride
                col += colInc;
            }
            row += tgt->plane[ix].rowInc;
        }

    }
}
}