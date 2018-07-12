#include "imageTransformer.h"

#include <opencv2/opencv.hpp>
#include <opencv2/core.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/calib3d/calib3d.hpp>
#include "imageTransformer_p.h"

using namespace cv;

uint8_t *warpPerspective(uint8_t *data_ptr, float32_t *rotation_ptr) {
    int mHeight = 480;
    int mWidth = 640;
    Mat yuv(mHeight * 3 / 2, mWidth, CV_8UC1, data_ptr);
    Mat matRot(4, 4, CV_32F, rotation_ptr);
    Mat matRot1 = matRot.t();
    Mat rgb;
    cv::cvtColor(yuv, rgb, COLOR_YUV2RGB_I420);
    Mat res = warpPerspective(rgb, matRot1);
    cv::cvtColor(res, yuv, COLOR_RGB2YUV_I420);
    return yuv.data;
}

uint8_t *warpPerspectiveEuler(uint8_t *data_ptr, float32_t *rotation_ptr) {
    int mHeight = 480;
    int mWidth = 640;
    Mat yuv(mHeight * 3 / 2, mWidth, CV_8UC1, data_ptr);

    Mat eulerAngels(3, 1, CV_32F, rotation_ptr);
    Mat matRot(3, 3, CV_32F);
    cv::Rodrigues(eulerAngels, matRot);

    Mat matRot1 = matRot.t();

// TODO: решить как оптимальнее расширить матрицу до 4х4
//  cv::Mat row = cv::Mat::zeros(1, 3, CV_32F);
//    matRot1.push_back(row);
//    cv::Mat column = cv::Mat::zeros(1, 4, CV_32F);
//    matRot1.push_back(column);

    Mat matRot2(4, 4, CV_32F, Scalar(0));
    matRot1.copyTo(matRot2(Rect(0, 0, 3, 3)));

    Mat rgb;
    cv::cvtColor(yuv, rgb, COLOR_YUV2RGB_I420);
    Mat res = warpPerspective(rgb, matRot2);
    cv::cvtColor(res, yuv, COLOR_RGB2YUV_I420);
    return yuv.data;
}

uint8_t *perspectiveTransform(uint8_t *data_ptr, float32_t *rotation_ptr) {
    int mHeight = 480;
    int mWidth = 640;
    Mat yuv(mHeight * 3 / 2, mWidth, CV_8UC1, data_ptr);
    Mat matRot(4, 4, CV_32F, rotation_ptr);
    Mat rgb;
    cv::cvtColor(yuv, rgb, COLOR_YUV2RGB_I420);
    Mat dst = perspectiveTransform(rgb, matRot);
    cv::cvtColor(dst, yuv, COLOR_RGB2YUV_I420);
    return yuv.data;
}
