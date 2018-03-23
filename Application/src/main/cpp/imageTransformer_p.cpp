#include <opencv2/core/mat.hpp>
#include <opencv2/imgproc.hpp>

using namespace cv;

cv::Mat get_transform_mat(cv::Mat & rotation_mat) {

    float focalLength = 800;
    float width = 960;
    float height = 720;

    float A1_array[12] = {
                1.0f, 0, -width / 2,
                0, 1.0f, -height / 2,
                0, 0, 0,
                0, 0, 1.0f
                };

    cv::Mat A1(4, 3, CV_32F, A1_array);

    float T_array[16] = {1.0f, 0, 0, 0,
                0, 1.0f, 0, 0,
                0, 0, 1.0f, focalLength,
                0, 0, 0, 1.0f
                };

    cv::Mat T(4, 4, CV_32F, T_array);
    cv::Mat internal = T * A1;

    float A2_array[12] = {
                focalLength, 0, width / 2, 0,
                0, focalLength, height / 2, 0,
                0, 0, 1.0f, 0
                };

    cv::Mat A2(3, 4, CV_32F, A2_array);

    cv::Mat transform_mat = A2 * (rotation_mat * internal);
    return transform_mat;
}

cv::Mat warpPerspective(cv::Mat & mat_rgb, cv::Mat & mat_rot) {
    cv::Mat res;
    cv::Mat transform_mat = get_transform_mat(mat_rot);
    warpPerspective(mat_rgb, res, transform_mat, mat_rgb.size());
    return res;
}

cv::Mat perspectiveTransform(cv::Mat & mat_rgb, cv::Mat & mat_rot) {

    int type = mat_rgb.type();
    Size mSize = mat_rgb.size();

    int mHeight = mSize.height;
    int mWidth = mSize.width;

    Mat dst(mSize, type);

    static struct Init {
        Init(int mWidth, int mHeight) {
            mBuff = new float32_t[2 * mWidth * mHeight];
            for (int i = 0; i < mHeight; i++) {
                for (int j = 0; j < mWidth; j++) {
                    mBuff[2 * (i * mWidth + j)] = (float32_t)j;
                    mBuff[2 * (i * mWidth + j) + 1] = (float32_t)i;
                }
            }
        }

        float32_t * mBuff;

    } sBuff(mWidth, mHeight);

    Mat src1(1, mHeight * mWidth, CV_32FC2, sBuff.mBuff);
    Mat dst1(1, mHeight * mWidth, CV_32FC2);

    cv::Mat transform_mat = get_transform_mat(mat_rot);
    perspectiveTransform(src1, dst1, transform_mat);

    dst1 = dst1.reshape(2, mHeight);

    Mat empty;
    remap(mat_rgb, dst, dst1, empty, INTER_LINEAR);
    return dst;
}
