#ifndef SCOPED_LOCAL_REF_H_included
#define SCOPED_LOCAL_REF_H_included
#include "jni.h"
#include <stddef.h>
#include <sys/types.h>
// A smart pointer that deletes a JNI local reference when it goes out of scope.

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

NativeImage *getNativeImage(JNIEnv *env, jobject image, jobject area = NULL);

template<typename T>
class ScopedLocalRef {
public:
    ScopedLocalRef(JNIEnv* env, T localRef) : mEnv(env), mLocalRef(localRef) {
    }
    ~ScopedLocalRef() {
        reset();
    }
    void reset(T ptr = NULL) {
        if (ptr != mLocalRef) {
            if (mLocalRef != NULL) {
                mEnv->DeleteLocalRef(mLocalRef);
            }
            mLocalRef = ptr;
        }
    }
    T release() __attribute__((warn_unused_result)) {
        T localRef = mLocalRef;
        mLocalRef = NULL;
        return localRef;
    }
    T get() const {
        return mLocalRef;
    }
private:
    JNIEnv* mEnv;
    T mLocalRef;
    // Disallow copy and assignment.
    ScopedLocalRef(const ScopedLocalRef&);
    void operator=(const ScopedLocalRef&);
};
#endif // SCOPED_LOCAL_REF_H_included