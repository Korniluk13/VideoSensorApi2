#include "imageTransformer.h"

#include <opencv2/opencv.hpp>
#include <opencv2/core.hpp>
#include <opencv2/core/mat.hpp>
#include "imageTransformer_p.h"

using namespace cv;

uint8_t *warpPerspective(uint8_t *data_ptr, float32_t *rotation_ptr) {
    int mHeight = 720;
    int mWidth = 960;
    Mat yuv(mHeight * 3 / 2, mWidth, CV_8UC1, data_ptr);
    Mat matRot(4, 4, CV_32F, rotation_ptr);
    Mat matRot1 = matRot.t();
    Mat rgb;
    cv::cvtColor(yuv, rgb, COLOR_YUV2RGB_I420);
    Mat res = warpPerspective(rgb, matRot1);
    cv::cvtColor(res, yuv, COLOR_RGB2YUV_I420);
    return yuv.data;
}

uint8_t *perspectiveTransform(uint8_t *data_ptr, float32_t *rotation_ptr) {
    int mHeight = 720;
    int mWidth = 960;
    Mat yuv(mHeight * 3 / 2, mWidth, CV_8UC1, data_ptr);
    Mat matRot(4, 4, CV_32F, rotation_ptr);
    Mat rgb;
    cv::cvtColor(yuv, rgb, COLOR_YUV2RGB_I420);
    Mat dst = perspectiveTransform(rgb, matRot);
    cv::cvtColor(dst, yuv, COLOR_RGB2YUV_I420);
    return yuv.data;
}
